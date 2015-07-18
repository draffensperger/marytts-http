package maryserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

public class MaryServer {
  public static void main(String[] args) throws Exception{
    int port = serverPort();
    Server server = new Server(port);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new MaryServlet()), "/*");

    server.start();
    System.out.println("MaryTTS server listening on port " + port);
    server.join();
  }

  private static int serverPort() {
    String portEnv = System.getenv("PORT");
    if (portEnv == null) {
      return 8080;
    }
    return Integer.valueOf(portEnv);
  }
}
