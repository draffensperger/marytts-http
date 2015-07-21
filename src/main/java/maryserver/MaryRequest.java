package maryserver;

import java.util.Locale;
import java.io.IOException;
import java.security.MessageDigest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import org.apache.commons.codec.binary.Base64;

public class MaryRequest {
  private HttpServletRequest req;
  private HttpServletResponse resp; 
  private Mac mac;

  private String text;
  private String localeStr;
  private String voiceName;
  private String style;
  private String effects;
  private String gender;
  private String expiresStr;
  private String signature;

  private long expiresMS;
  private byte[] signatureBytes;

  public MaryRequest(HttpServletRequest req, HttpServletResponse resp, Mac mac) {
    this.req = req;
    this.resp = resp;
    this.mac = mac;
  }

  public void doGet() throws IOException {
    getParams();
    parseParams();
    if (!checkValidRequest()) {
      return;
    }

    resp.setContentType("audio/wave");
    try {
      MaryUtil.writeWaveAudio(resp.getOutputStream(), text, localeStr, gender, 
          voiceName, style, effects);
    } catch(Exception e) {
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
    }
  }

  private void getParams() {
    text = req.getParameter("text");
    expiresStr = req.getParameter("expires");

    localeStr = paramOrDefault("locale", requestLocaleString());
    gender = paramOrDefault("gender", "");
    voiceName = paramOrDefault("voice", "");
    style = paramOrDefault("style", "");
    effects = paramOrDefault("effects", "");
    signature = paramOrDefault("signature", "");
  }

  private String paramOrDefault(String key, String defaultVal) {
    String val = req.getParameter(key);
    return val != null ? val : defaultVal;
  }

  private String requestLocaleString() {
    Locale locale = req.getLocale();
    String countryPart = "";
    if (locale.getCountry() != "") {
      countryPart = "_" + locale.getCountry();
    }
    return locale.getLanguage() + countryPart;
  }

  private void parseParams() {
    if (expiresStr != null) {
      try {
        expiresMS = Long.parseLong(expiresStr) * 1000;
      } catch (NumberFormatException e) {}
    }

    try {
      signatureBytes = Base64.decodeBase64(signature.getBytes("UTF-8"));
    } catch (Exception e) {}
  }

  private boolean checkValidRequest() throws IOException {
    if (text == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No text specified");
      return false;
    }
    if (expiresStr != null && expiresMS < System.currentTimeMillis()) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,  "Request expired");
      return false;
    }
    if (mac != null && !isValidSignature()) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,  "Bad signature");
      return false;
    }
    return true;
  }

  private boolean isValidSignature() {
    String toSign = text + localeStr + gender + voiceName + style + effects +
      expiresStr;
    try {
      byte[] correctSignature = mac.doFinal(toSign.getBytes("UTF-8"));
      return MessageDigest.isEqual(signatureBytes, correctSignature);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
