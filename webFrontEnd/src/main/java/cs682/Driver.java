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

    private static  Membership membership = new Membership();
    final static Logger logger = Logger.getLogger(Driver.class);


    public static void main(String[] args) {
        try {
            Properties config = loadConfig("config.properties");
            //membership.loadInitMembers(config);
            membership.loadSelfConfiguration(config);
            Server jettyHttpServer = new Server(Membership.SELF_FRONT_END_PORT);
            ServletHandler jettyHandler = new ServletHandler();
            jettyHandler.addServletWithMapping(new ServletHolder(new UserServlet()), "/users/*");
            jettyHandler.addServletWithMapping(new ServletHolder(new EventServlet()), "/events");
            jettyHandler.addServletWithMapping(new ServletHolder(new EventServlet()), "/events/*");
            jettyHandler.addServletWithMapping(new ServletHolder(new SystemServlet()), "/heartbeat");
            jettyHandler.addServletWithMapping(new ServletHolder(new SystemServlet()), "/newprimary");
            jettyHttpServer.setHandler(jettyHandler);
            jettyHttpServer.start();
            membership.loadInitMembers(config);
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
