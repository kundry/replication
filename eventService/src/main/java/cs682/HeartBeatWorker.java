package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HeartBeatWorker implements Runnable {
    private String url;
    private Member member;
    protected static final Membership membership = Membership.getInstance();
    final static Logger logger = Logger.getLogger(NotificationWorker.class);

    public HeartBeatWorker(Member m){
        this.member = m;
        this.url = "http://" + m.getHost() + ":" + m.getPort() + "/heartbeat";
    }

    @Override
    public void run() {
        //logger.debug("Sending HeartBeat to " + url);
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    //logger.debug("Server Alive:  " + url);
                    break;
                default:
                    //logger.debug("Server not alive. Status unknown: " + url);
                    break;
            }
        } catch (IOException e) {
            //logger.debug("Server Unreachable. HeartBeat has failed: " + url);
            if (member.getIsPrimary()) {
                logger.debug("PRIMARY Down");
                membership.startElection();
            } else {
                logger.debug(member.getType() + " SERVER Down" );
                membership.removeServerDown(member.getHost(), member.getPort());
                logger.debug("Membership table updated" );
                if(member.getType().equals("EVENT")){
                   //membership.deregisterFromChannel(member.getPId());
                }
            }
        }
    }

}
