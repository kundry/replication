package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class ElectionWorker implements Runnable {
    private String url;
    private final CountDownLatch latch;
    protected static final Membership membership = Membership.getInstance();
    final static Logger logger = Logger.getLogger(ElectionWorker.class);

    ElectionWorker(String url, CountDownLatch latch){
        this.url = url;
        this.latch = latch;
    }

    @Override
    public void run() {
        logger.debug("Sending election message to " + url);
        // si ok update the boolean of reply
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    Membership.ELECTION_REPLY = true;
                    logger.debug("OK received from election message:  " + url);
                    membership.IN_ELECTION = false;
                    membership.removePrimary();
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    logger.debug("Bad request received from election message:  " + url);
                    break;
                default:
                    logger.debug("Status Code Received Unknown: " + url);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }
}
