package cs682;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Timer Task that is in charge of sending the heartbeats at certain time
 * intervals previously configured
 **/
public class HeartBeatTimeTask extends TimerTask {
    protected static final Membership membership = Membership.getInstance();
    final static Logger logger = Logger.getLogger(NotificationWorker.class);
    private static ExecutorService heartbeatThreadPool = Executors.newFixedThreadPool(6);

    @Override
    public void run() {
        ArrayList<Member> memberList = membership.getMembers();
        for (Member member : memberList){
            if(!(member.getHost().equals(Membership.SELF_EVENT_SERVICE_HOST) && member.getPort().equals(String.valueOf(Membership.SELF_EVENT_SERVICE_PORT)))){
                heartbeatThreadPool.submit(new HeartBeatWorker(member));
            }
        }
    }
}
