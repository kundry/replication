package cs682;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.FileReader;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Class that starts the JettyServer
 */
public class Driver {

    //private static int SELF_FRONT_END_PORT;
//    public static String USER_SERVICE_HOST;
//    public static int USER_SERVICE_PORT;
//    public static String PRIMARY_HOST;
//    public static int PRIMARY_PORT;
//    public static String JOIN_MODE;
    private static  Membership membership = new Membership();
    final static Logger logger = Logger.getLogger(Driver.class);


    public static void main(String[] args) {
        Properties config = loadConfig("config.properties");
        membership.loadInitMembers(config);

        Server jettyHttpServer = new Server(Membership.SELF_FRONT_END_PORT);
        ServletHandler jettyHandler = new ServletHandler();
        jettyHandler.addServletWithMapping(new ServletHolder(new UserServlet()), "/users/*");
        jettyHandler.addServletWithMapping(new ServletHolder(new EventServlet()), "/events");
        jettyHandler.addServletWithMapping(new ServletHolder(new EventServlet()), "/events/*");
        jettyHttpServer.setHandler(jettyHandler);
        try {
            jettyHttpServer.start();
            logger.debug("Web Front End Service Starting ...   " + "PORT: " + Membership.SELF_FRONT_END_PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It Loads the properties file with configuration information
     * Ports and Hosts of the different services
     * @param configPath name of the file
     * */
    private  static Properties loadConfig(String configPath){
        Properties config = new Properties();
        try {
            config.load(new FileReader(configPath));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return config;
    }


}
