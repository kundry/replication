package cs682;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartBeatTimeTask extends TimerTask {
    //private ArrayList<Member> memberList;
    final static Logger logger = Logger.getLogger(NotificationWorker.class);
    private static ExecutorService heartbeatThreadPool = Executors.newFixedThreadPool(6);

//    HeartBeatTimeTask(ArrayList<Member> memberList){
//        this.memberList = memberList;
//    }

    @Override
    public void run() {
        //System.out.println("Task performed on " + new Date());
        ArrayList<Member> memberList = Membership.getMembers();
        for (Member member : memberList){
            if(!(member.getHost().equals(Membership.SELF_EVENT_SERVICE_HOST) && member.getPort().equals(String.valueOf(Membership.SELF_EVENT_SERVICE_PORT)))){
                //String url = "http://" + member.getHost() + ":" + member.getPort() + "/heartbeat";
                //heartbeatThreadPool.submit(new HeartBeatWorker(url, member.getIsPrimary()));
                heartbeatThreadPool.submit(new HeartBeatWorker(member));
            }
        }
    }
}
