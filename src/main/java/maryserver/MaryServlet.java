package maryserver;

import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.PropertyConfigurator;
import marytts.server.Mary;

@SuppressWarnings("serial")
public class MaryServlet extends HttpServlet {
  private Mac mac = null; 

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    String secret = System.getenv("SECRET");
    if (secret != null) {
      initRequestMac(secret);
    }
    initLog4J();
    startMaryIfNeeded();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    startMaryIfNeeded();
    new MaryRequest(req, resp, mac).doGet();
  }

  public void destroy() {
    if (Mary.currentState() != Mary.STATE_OFF
        && Mary.currentState() != Mary.STATE_SHUTTING_DOWN)
    {
      try {
        Mary.shutdown();
      } catch (IllegalStateException  e) {
        log("Exception shutting down Mary", e);
      }
    }
  }

  private void initRequestMac(String secret) {
    try {
      mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA1"));
    } catch (Exception e) {
      log("Exception inializing authentication", e);
    }
  }

  private void initLog4J() {
    // We need to configure log4j so that Mary doesn't try to configure it
    Properties props = new Properties();
    props.setProperty("log4j.rootLogger", "DEBUG, A1");
    props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
    PropertyConfigurator.configure(props);
  }

  private void startMaryIfNeeded() {
    if (Mary.currentState() != Mary.STATE_RUNNING
        && Mary.currentState() != Mary.STATE_STARTING)
    {
      try {
        Mary.startup(false);
      } catch (Exception e) {
        log("Exception starting Mary", e);
      }
    }
  }
}
