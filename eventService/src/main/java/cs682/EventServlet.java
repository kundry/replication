package cs682;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.util.concurrent.CountDownLatch;

/**
 * Servlet in which all the requests about events are mapped
 * */
public class EventServlet extends HttpServlet {

    protected static final EventData eventData = EventData.getInstance();
    protected static final Membership membership = Membership.getInstance();
    private static HashMap<Integer, SendingReplicaWorker> sendingReplicaChannel = new HashMap<>();
    public static final ReceivingReplicaWorker receiverWorker = new ReceivingReplicaWorker();
    final static Logger logger = Logger.getLogger(EventServlet.class);
    /**
     * Handles the POST Requests of creating new events and
     * purchasing tickets for registered events
     * @param request http request
     * @param response http response
     * */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/create")) {
            if (Membership.PRIMARY){
                synchronized (this) {
                    String requestBody = getRequestBody(request);
                    logger.debug(System.lineSeparator() + pathInfo);
                    Event event = createEvent(requestBody);
                    if (event != null) {
                        EventData.VERSION++;
                        int vId = EventData.VERSION;
                        boolean replicationSuccess = replicateWrite(requestBody, pathInfo, vId);
                        if (replicationSuccess) {
                            respondCreate(response, event);
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            } else {
                    applyWrite(request, pathInfo);
                    response.setStatus(HttpServletResponse.SC_OK);
            }
        } else if(pathInfo.equals("/members/register")){
           membership.registerServer(request, response);
        } else if (pathInfo.equals("/members/add")) {
            membership.addNotifiedServer(request, response);
        } else if (pathInfo.matches("/newprimary")) {
            processNewPrimary(request, response);
        } else {
            if (pathInfo.matches("/purchase/([\\d]+)")) {
                if (Membership.PRIMARY) {
                    synchronized (this) {
                        String requestBody = getRequestBody(request);
                        logger.debug(System.lineSeparator() + pathInfo);
                        boolean success = purchaseTicket(requestBody);
                        if (success){
                            EventData.VERSION++;
                            int vId = EventData.VERSION;
                            boolean replicationSuccess = replicateWrite(requestBody, pathInfo, vId);
                            if (replicationSuccess) {
                                response.setStatus(HttpServletResponse.SC_OK);
                            }
                        } else {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        }
                    }
                } else {
                    System.out.println(pathInfo);
                    applyWrite(request, pathInfo);
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            } else {
                System.out.println("Invalid Path");
            }
        }
    }

    /**
     * Handles the GET Requests of listing information about all the
     * events registered and showing details about a given event
     * @param request http request
     * @param response http response
     * */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/list")) {
            listAllEvents(response);
        } else if (pathInfo.equals("/heartbeat")){
            replyAlive(response);
        } else if (pathInfo.matches("/election/([\\d]+)")) {
            int senderPid = Integer.parseInt(pathInfo.substring(10));
            logger.debug("Election Message received ");
            membership.processElectionMessage(senderPid, response);
        } else if (pathInfo.matches("/members/data")) {
            sendDataToNewMember(response);
            logger.debug("Data sent");
        } else {
            showOneEvent(request, response);
        }
    }

    /**
     * Creates a new event. It validates the existence of the user by communicating with the
     * User Service through the UserServiceLink (API) It returns the event
     * object created
     * @param requestBody http request
     * */

    private Event createEvent(String requestBody) {
        Event event = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            int userId = ((Long)jsonObj.get("userid")).intValue();
            String eventName = (String) jsonObj.get("eventname");
            int numTickets = ((Long)jsonObj.get("numtickets")).intValue();
            UserServiceLink userService = new UserServiceLink();
            if (userService.isValidUserId(userId)) {
                int id = eventData.getLastEventId() + 1;
                event = new Event(id, eventName, userId, numTickets);
                eventData.addEvent(event);
                logger.debug("Writing locally");
                logger.debug(event.toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return event;
    }

    private void respondCreate(HttpServletResponse response, Event event) {
        try {
            JSONObject json = createJsonResponseNewEvent(event.getId());
            String jsonResponse = json.toString();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(jsonResponse);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the jason of the body of the request and converted into string
     * @param request http request
     * @return json received in the request converted into a string
     * */
    private String getRequestBody(HttpServletRequest request) {
        BufferedReader in;
        String line;
        String body = null;
        try {
            in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuffer sb = new StringBuffer();
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            body = sb.toString();
            in.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    /**
     * Creates the JsonObject with the id assigned to the new event created
     * This Json is part of the response of the request
     * @param id even id
     * @return jason object
     * * */
    private JSONObject createJsonResponseNewEvent(int id){
        JSONObject json = new JSONObject();
        json.put("eventid",id);
        return json;
    }

    /**
     * Sends the data of all the events registered in the Event Service
     * @param response http response
     * */
    private void listAllEvents(HttpServletResponse response) {
        JSONArray array = eventData.createJsonEventsList();
        String jsonEventsList = array.toString();
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(jsonEventsList);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends the details of the given event id provided in the query string of
     * the request. It queries the data structure of events and sends the data
     * @param request http request
     * @param response http response
     * */
    private void showOneEvent(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getPathInfo();
            int eventId = Integer.parseInt(path.substring(1));
            if (eventData.isRegistered(eventId)) {
                JSONObject json = eventData.createJsonOneEvent(eventId);
                String jsonResponse  = json.toString();
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.write(jsonResponse);
                out.flush();
                out.close();
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
        } catch (IOException e) {
                e.printStackTrace();
        }
    }
    /**
     * Allows a user to purchase tickets for a given event. It parses the data from the request.
     * Updates the amount of tickets of the event communicates with the User Service to add the
     * tickets to the corresponding user.
     * @param requestBody request Body string
     * @return true or false depending on the success of the operation
     * */
    private boolean purchaseTicket(String requestBody) {
        boolean success = false;
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj=  (JSONObject) parser.parse(requestBody);
            int userId = ((Long)jsonObj.get("userid")).intValue();
            int eventId = ((Long)jsonObj.get("eventid")).intValue();
            int tickets = ((Long)jsonObj.get("tickets")).intValue();
            UserServiceLink userService = new UserServiceLink();
            boolean ticketsUpdatedSuccessfully, ticketsAddedSuccessfully;
            if (userService.isValidUserId(userId) && eventData.isRegistered(eventId)) {
                ticketsUpdatedSuccessfully = eventData.updateNumTickets(eventId, tickets);
                if (ticketsUpdatedSuccessfully) {
                    ticketsAddedSuccessfully = userService.addTicketsToUser(userId, eventId, tickets);
                    if (ticketsAddedSuccessfully) {
                        success = true;
                        Event event = eventData.getEventDetails(eventId);
                        logger.debug("Writing locally");
                        logger.debug(event.toString());
                    } else {
                        eventData.undoUpdateNumTickets(eventId, tickets);
                        success = false;
                    }
                } else {
                    success = false;
                }
            } else {
                success = false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * Reply Alive to the heartbeat received
     * @param response http response
     * */
    private void replyAlive(HttpServletResponse response){
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public static void registerInChannel(Integer pid, SendingReplicaWorker worker){
        sendingReplicaChannel.put(pid,worker);
    }

    public static void deregisterFromChannel(int pid){
        sendingReplicaChannel.remove(pid);
    }

    private boolean replicateWrite(String jsonBody, String pathInfo, int vId) {
        boolean okInAll = false;
        try {
            Write write = new Write(pathInfo, jsonBody, vId);
            logger.debug("Replication started");
            final CountDownLatch latch = new CountDownLatch(sendingReplicaChannel.size());
            write.setLatch(latch);
            for (Map.Entry<Integer, SendingReplicaWorker> entry : sendingReplicaChannel.entrySet()) {
                entry.getValue().queueWrite(write);
            }
            latch.await();
            okInAll = true;
            logger.debug("Replication finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return okInAll;
    }
    private void applyWrite(HttpServletRequest request, String pathInfo){
        String jsonBody = getRequestBody(request);
        Write write = new Write(pathInfo, jsonBody);
        receiverWorker.queueWrite(write);
    }
    private void processNewPrimary(HttpServletRequest request, HttpServletResponse response){
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(requestBody);
            String primaryHost = (String) json.get("primaryhost");
            int primaryPort = ((Long)json.get("primaryport")).intValue();
            int version = ((Long)json.get("version")).intValue();
            logger.debug("New Primary " + primaryHost + ":" + primaryPort);
            membership.removePrimary();
            membership.updatePrimary(primaryHost, primaryPort);
            eventData.initEventData(json);
            logger.debug("Data Updated to version "+ version);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void sendDataToNewMember(HttpServletResponse response) {
        try {
            JSONObject responseJson = new JSONObject();
            JSONArray eventsArray = eventData.createJsonEventsList();
            responseJson.put("version", EventData.VERSION);
            responseJson.put("data", eventsArray);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(responseJson.toString());
            out.flush();
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
