package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * Runnable that sends a heartbeat to the given node
 * */
public class HeartBeatWorker implements Runnable {
    private String url;
    private Member member;
    protected static final Membership membership = Membership.getInstance();
    final static Logger logger = Logger.getLogger(NotificationWorker.class);

    /**
     * Constructor
     * @param m Member to send the heartbeat
     * */
    public HeartBeatWorker(Member m){
        this.member = m;
        this.url = "http://" + m.getHost() + ":" + m.getPort() + "/heartbeat";
    }

    @Override
    public void run() {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            if (member.getIsPrimary()) {
                logger.debug("PRIMARY Down");
                membership.startElection();
            } else {
                logger.debug(member.getType() + " SERVER Down" );
                membership.removeServerDown(member.getHost(), member.getPort());
                logger.debug("Membership table updated" );
                if(member.getType().equals("EVENT") && Membership.PRIMARY) {
                   EventServlet.deregisterFromChannel(member.getPId());
                }
            }
        }
    }
}
