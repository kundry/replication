package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedList;
import java.util.Queue;

public class ReceivingReplicaWorker implements Runnable {
    boolean beingFollower = true;
    private Queue<Write> writesQueue = new LinkedList<>();
    final static Logger logger = Logger.getLogger(SendingReplicaWorker.class);

    /** Method that adds the write received to a queue and performs the notify
     *  to activate the processing of the incoming write
     *  @param write received by the remote host
     */
    public void queueWrite(Write write){
        synchronized(this) {
            writesQueue.add(write);
            this.notify();
        }
    }

    @Override
    public void run() {
        //long timeout = 50000;
        synchronized (this) {
            while (beingFollower) {
                try {
                    //this.wait(timeout);
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!writesQueue.isEmpty()) {
                    Write incomingWrite = writesQueue.remove();
                    if (incomingWrite.getPath().startsWith("/create")){
                        executeCreate(incomingWrite);
                    } else {
                        executePurchase(incomingWrite);
                    }
                }
                //timeout = 50000;
            }
            // deregisterFromChannel();
            //how to exit the while ? here or when I am notified of not being primary anymore
        }
    }
    private void executeCreate(Write incomingWrite){
        try {
            String requestBody = incomingWrite.getJsonBody();
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            int userId = ((Long)jsonObj.get("userid")).intValue();
            String eventName = (String) jsonObj.get("eventname");
            int numTickets = ((Long)jsonObj.get("numtickets")).intValue();
            int id = EventServlet.eventData.getLastEventId() + 1;
            int version = ((Long)jsonObj.get("versionid")).intValue();
            Event event = new Event(id, eventName, userId, numTickets);
            EventServlet.eventData.addEvent(event);
            logger.debug("/create replicated: " + requestBody);
            EventData.VERSION = version;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private void executePurchase(Write incomingWrite){

    }
}
