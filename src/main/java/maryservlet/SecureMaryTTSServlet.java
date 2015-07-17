package maryservlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.OutputStream;
import marytts.server.Mary;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.Voice.Gender;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

// We need to configure log4j so that Mary doesn't try to configure it
import org.apache.log4j.PropertyConfigurator;

@SuppressWarnings("serial")
public class SecureMaryTTSServlet extends HttpServlet {
	private static final String ENCODING = "UTF-8";
	private static final long MAX_EXPIRY_IN_FUTURE_MS = 300000;
	private static final int MAX_INPUT_LENGTH = 100;
	
	private Map<String,byte[]> userKeyMap;	
		
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {				
		// Get required parameters
		String input = req.getParameter("t");				
		String localeString = req.getParameter("l");		
		String expiresStr = req.getParameter("e");
		String user = req.getParameter("u");
		String signature = req.getParameter("s");		
		if (input == null || localeString == null || user == null 
				|| signature == null || expiresStr == null) {			
			return;
		}
		
		/*
		if (input.length() > MAX_INPUT_LENGTH) {
			return;
		}
		
		// Check for expiry
		long expiresMS;
		try {
			expiresMS = Long.parseLong(expiresStr) * 1000;
		} catch (NumberFormatException e) {
			return;
		}
		long expiresMSInFuture = expiresMS - System.currentTimeMillis();
		if (expiresMSInFuture < 0 || expiresMSInFuture > MAX_EXPIRY_IN_FUTURE_MS) {
			return;
		}		
		
		// Get user Key
		//byte[] keyBytes = userKeyMap.get(user);
		String userKey = System.getenv("USER_KEY");
		if (userKey == null || userKey == "") {
			log("Must specify USER_KEY env variable");
			return;
		}
		byte[] keyBytes = userKey.getBytes();		
		*/
		// Get optional parameters
		String audioTypeName = req.getParameter("f");
		String outputTypeParams = req.getParameter("outputTypeParams");
		String style = req.getParameter("style");
		String effects = req.getParameter("effects");
		String gender = req.getParameter("gender");
		String voiceName = req.getParameter("voice");
		
		/*
		// Test signature
		String stringToSign = input + localeString + expiresStr + 
				blankIfNull(audioTypeName) + blankIfNull(outputTypeParams) + 
				blankIfNull(style) + blankIfNull(effects) + blankIfNull(gender) + 
				blankIfNull(voiceName);
		Mac mac;
		try {
			mac = Mac.getInstance("HmacSHA1");		
			mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
		} catch (Exception e) {
			log("Exception inializing authentication", e);
			return;
		}
	    byte[] correctSignature = mac.doFinal(stringToSign.getBytes(ENCODING));
	    
	    Base64 base64 = new Base64();
	    byte[] signatureBytes = Base64.decodeBase64(signature.getBytes(ENCODING));
	    if (!Arrays.equals(correctSignature, signatureBytes)) {
	    	return;
	    }
	    */	    
		
	    // Set parameter defaults
	    if (localeString == "") {
			Locale locale = req.getLocale();
			localeString = locale.getLanguage();
			if (locale.getCountry() == "US") {
				localeString  += "_US";
			}
	    }
		if (audioTypeName == null) {
			audioTypeName = "WAVE";
		}						
		if (outputTypeParams == null) {
			outputTypeParams  = "";
		}				
		if (style == null) {
			style = "";
		}		
		if (effects == null) {
			effects = "Volume amount:2.0;";
		}				
		if (gender == null) {
			gender = "";
		}				
		if (voiceName == null) {
			Locale locale = localeFromString(localeString);
			
			Voice defaultVoice;
			if (gender != "") {				
				defaultVoice = Voice.getVoice(locale, new Gender(gender));
			} else {
				defaultVoice = Voice.getDefaultVoice(locale);
			}
			
			if (defaultVoice == null) {
				locale = new Locale(locale.getLanguage());
				defaultVoice = Voice.getDefaultVoice(locale);
				if (defaultVoice == null && locale.getLanguage() == "en") {
					locale = new Locale("en", "US");
					
					defaultVoice = Voice.getDefaultVoice(locale);
					if (defaultVoice == null) {
						log("No default voice for locale: " + localeString);
						return;
					}
				}											
			}
							
			voiceName = defaultVoice.getName();
			localeString = defaultVoice.getLocale().toString();
		}		
		
		// Always convert from text to audio
		String inputTypeName = "TEXT";
		String outputTypeName = "AUDIO";		
		
		// Run Mary TTS and send output
		//resp.addHeader("Content-Disposition", "inline");
		resp.setContentType("audio/wave");
		OutputStream output = resp.getOutputStream();				
		try {
			startMaryIfNeeded();
			Mary.process(input, inputTypeName, outputTypeName, localeString, audioTypeName, 
					voiceName, style, effects, outputTypeParams, output);
		} catch (Exception e) {
			e.printStackTrace();
			log("Exception running Mary", e);			
		}				
		output.flush();
	}
	
	private static String blankIfNull(String s) {
		if (s == null) {
			return "";
		} else {
			return s;
		}
	}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
				
		userKeyMap = new HashMap<String,byte[]>();
				
		Enumeration paramNames = config.getInitParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if (paramName.startsWith("userKey")) {
				String userKey = config.getInitParameter(paramName);
				int colonLoc = userKey.indexOf(':');
				if (colonLoc > 0) {
					String user = userKey.substring(0, colonLoc);
					byte[] key = Base64.decodeBase64(userKey.substring(colonLoc));
					userKeyMap.put(user, key);
				}
			}
		}
		
		Properties props = new Properties();
		props.setProperty("log4j.rootLogger", "DEBUG, A1");
		props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		PropertyConfigurator.configure(props);
				
		//System.setProperty("MARY_BASE", getServletContext().getRealPath("/"));		
		
		startMaryIfNeeded();
	}
	
	public void destroy() {
		if (Mary.currentState() != Mary.STATE_OFF 
				&& Mary.currentState() != Mary.STATE_SHUTTING_DOWN) {
			try {			
				Mary.shutdown();			
			} catch (IllegalStateException  e) {
				log("Exception shutting down Mary", e);	
			}	
		}
	}
	
	private void startMaryIfNeeded() {
		if (Mary.currentState() != Mary.STATE_RUNNING 
				&& Mary.currentState() != Mary.STATE_STARTING) {
			try {
				Mary.startup(false);
			} catch (Exception e) { 
				log("Exception starting Mary", e);
			}
		}
	}
	
	private static Locale localeFromString(String s) {
		int underscoreLoc = s.indexOf('_');
		if (underscoreLoc == -1) {
			return new Locale(s);
		} else {
			String language = s.substring(0, underscoreLoc);
			String country = s.substring(underscoreLoc);
			return new Locale(language, country);
		}
	}
	
    public static void main(String[] args) throws Exception{
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new SecureMaryTTSServlet()),"/*");
        server.start();
        server.join();   
    }	
}
