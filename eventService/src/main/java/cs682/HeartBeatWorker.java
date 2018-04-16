package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HeartBeatWorker implements Runnable {
    private String url;
    final static Logger logger = Logger.getLogger(NotificationWorker.class);

    public HeartBeatWorker(String url){
        this.url = url;
    }

    @Override
    public void run() {
        logger.debug("Sending HeartBeat " + url);
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    logger.debug("Server Alive:  " + url);
                    break;
                default:
                    logger.debug("Server not alive. Status unknown: " + url);
                    break;
            }
        } catch (IOException e) {
            logger.debug("Server Unreachable. HeartBeat has failed: " + url);
        }
    }

}
