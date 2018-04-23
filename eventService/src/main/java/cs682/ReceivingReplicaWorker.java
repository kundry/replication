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
        synchronized (this) {
            while (beingFollower) {
                try {
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
            }
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
            logger.debug(System.lineSeparator() + "/create replicated: ");
            logger.debug(event.toString());
            EventData.VERSION = version;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private void executePurchase(Write incomingWrite){
        try {
            String requestBody = incomingWrite.getJsonBody();
            JSONParser parser = new JSONParser();
            JSONObject jsonObj=  (JSONObject) parser.parse(requestBody);
            int eventId = ((Long)jsonObj.get("eventid")).intValue();
            int tickets = ((Long)jsonObj.get("tickets")).intValue();
            int version = ((Long)jsonObj.get("versionid")).intValue();
            boolean updateTickets = EventServlet.eventData.updateNumTickets(eventId, tickets);
            if (updateTickets) logger.debug(System.lineSeparator() + incomingWrite.getPath());
            Event event = EventServlet.eventData.getEventDetails(eventId);
            logger.debug(event.toString());
            EventData.VERSION = version;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
