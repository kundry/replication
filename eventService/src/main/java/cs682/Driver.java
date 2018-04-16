package cs682;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.FileReader;
import java.util.Properties;
import java.util.Timer;
import org.apache.log4j.Logger;

/**
 * Class that starts the JettyServer
 */
public class Driver {

    //private static int SELF_EVENT_SERVICE_PORT;
    //public static String SELF_EVENT_SERVICE_HOST;
    //public static int USER_SERVICE_PORT;
    //public static String USER_SERVICE_HOST;

    public static Membership membership = new Membership();
    final static Logger logger = Logger.getLogger(Driver.class);

    public static void main(String[] args) {

        Properties config = loadConfig("config.properties");
        membership.loadInitMembers(config);

        Server jettyHttpServer = new Server(Membership.SELF_EVENT_SERVICE_PORT);
        System.out.println("EVENT_SERVICE_PORT: "+ Membership.SELF_EVENT_SERVICE_PORT);
        ServletHandler jettyHandler = new ServletHandler();
        jettyHandler.addServletWithMapping(new ServletHolder(new EventServlet()), "/*");
        jettyHttpServer.setHandler(jettyHandler);
        try {
            jettyHttpServer.start();
            Timer timer = new Timer("Timer");
            long delay  = 10000L;
            long period = 120000L;
            timer.scheduleAtFixedRate(new HeartBeatTimeTask(membership.getMembers()), delay, period);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It Loads the properties file with configuration information
     * Ports and Hosts of the different services
     * @param configPath name of the file
     * */
    private static Properties loadConfig(String configPath){
        Properties config = new Properties();
        try {
            config.load(new FileReader(configPath));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return config;
    }

}
