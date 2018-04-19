package cs682;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private static final ArrayList<SendingReplicaWorker> sendingReplicaChannel = new ArrayList();
    public static final ReceivingReplicaWorker receiverWorker = new ReceivingReplicaWorker();
    private Membership membership = new Membership();
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
                    logger.debug("Write received: " + pathInfo);
                    logger.debug(requestBody + System.lineSeparator());
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
        } else {
            System.out.println("Purchase tickets");
            if (pathInfo.matches("/purchase/([\\d]+)")) {
                purchaseTicket(request, response);
                if (Membership.PRIMARY) {
                    synchronized (this) {
                        String requestBody = getRequestBody(request);
                        logger.debug("Write received: " + pathInfo);
                        logger.debug(requestBody);

                    }
                } else {
                    // follower
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
        } else if (pathInfo.equals("/heartbeat") ){
            replyAlive(response);
        } else{
            System.out.println("List One");
            showOneEvent(request, response);
        }
    }

    /**
     * Creates a new event by parsing the event data from the request.
     * It validates the existence of the user by communicating with the
     * User Service through the UserServiceLink (API) It returns the event
     * object created
     * @param requestBody http request
     * */
//    private void createEvent(HttpServletRequest request, HttpServletResponse response) {
//        try {
//            String requestBody = getRequestBody(request);
//            JSONParser parser = new JSONParser();
//            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
//            int userId = ((Long)jsonObj.get("userid")).intValue();
//            String eventName = (String) jsonObj.get("eventname");
//            int numTickets = ((Long)jsonObj.get("numtickets")).intValue();
//            UserServiceLink userService = new UserServiceLink();
//            if (userService.isValidUserId(userId)) {
//                int id = eventData.getLastEventId() + 1;
//                Event event = new Event(id, eventName, userId, numTickets);
//                eventData.addEvent(event);
//                JSONObject json = createJsonResponseNewEvent(id);
//                String jsonResponse = json.toString();
//                System.out.println(jsonResponse);
//                response.setStatus(HttpServletResponse.SC_OK);
//                response.setContentType("application/json;charset=UTF-8");
//                PrintWriter out = response.getWriter();
//                out.write(jsonResponse);
//                out.flush();
//                out.close();
//                eventData.printEventList();
//            } else {
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }

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
            logger.debug(jsonResponse);
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
     * @param request http request
     * @param response http response
     * */
//    private void purchaseTicket(HttpServletRequest request, HttpServletResponse response) {
//        try {
//            String requestBody = getRequestBody(request);
//            System.out.println(requestBody);
//            JSONParser parser = new JSONParser();
//            JSONObject jsonObj=  (JSONObject) parser.parse(requestBody);
//            int userId = ((Long)jsonObj.get("userid")).intValue();
//            int eventId = ((Long)jsonObj.get("eventid")).intValue();
//            int tickets = ((Long)jsonObj.get("tickets")).intValue();
//            UserServiceLink userService = new UserServiceLink();
//            boolean ticketsUpdatedSuccessfully, ticketsAddedSuccessfully;
//            if (userService.isValidUserId(userId) && eventData.isRegistered(eventId)) {
//                ticketsUpdatedSuccessfully = eventData.updateNumTickets(eventId, tickets);
//                if (ticketsUpdatedSuccessfully) {
//                    ticketsAddedSuccessfully = userService.addTicketsToUser(userId, eventId, tickets);
//                    if (ticketsAddedSuccessfully) {
//                        response.setStatus(HttpServletResponse.SC_OK);
//                    } else {
//                        eventData.undoUpdateNumTickets(eventId, tickets);
//                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                    }
//                } else {
//                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                }
//            } else {
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }

    private void purchaseTicket(HttpServletRequest request, HttpServletResponse response) {
        try {
            String requestBody = getRequestBody(request);
            System.out.println(requestBody);
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
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        eventData.undoUpdateNumTickets(eventId, tickets);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reply Alive to the heartbeat received
     * @param response http response
     * */
    private void replyAlive(HttpServletResponse response){
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public static void registerInChannel(SendingReplicaWorker worker){
        sendingReplicaChannel.add(worker);
    }

    private boolean replicateWrite(String jsonBody, String pathInfo, int vId) {
        boolean okInAll = false;
        try {
            Write write = new Write(pathInfo, jsonBody, vId);
            logger.debug("Replication has started");
            final CountDownLatch latch = new CountDownLatch(sendingReplicaChannel.size());
            write.setLatch(latch);
            logger.debug("latch set " + latch );
            for (SendingReplicaWorker follower : sendingReplicaChannel) {
                follower.queueWrite(write);
            }
            logger.debug("latch before await " + latch );
            latch.await();
            logger.debug("latch after await " + latch );
            okInAll = true;
            logger.debug("Replication has finished");
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
}
