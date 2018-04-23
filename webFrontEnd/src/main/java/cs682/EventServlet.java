package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EventServlet extends HttpServlet {
    final static Logger logger = Logger.getLogger(EventServlet.class);
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            if (pathInfo.matches("/[\\d]+")) {
                getEventDetails(request, response);
            } else {
                System.out.println("Invalid Path");
            }
        } else {
            getEventList(response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/create")) {
            logger.debug(pathInfo);
            createEvent(request, response);
        } else {
            Pattern pattern = Pattern.compile("/([\\d]+)/purchase/([\\d]+)");
            Matcher match = pattern.matcher(pathInfo);
            if (match.find()) {
                //System.out.println(match.group(1));
                //System.out.println(match.group(2));
                int eventId = Integer.parseInt(match.group(1));
                int userId = Integer.parseInt(match.group(2));
                purchaseTickets(eventId, userId, request, response);
            } else {
                System.out.println("Invalid Path");
            }
        }
    }

    /**
     * Performs the operation of creating an event
     * @param request http request
     * @param response http response
     * */

    private void createEvent(HttpServletRequest request, HttpServletResponse response) {
        String host = Membership.PRIMARY_HOST + ":" + String.valueOf(Membership.PRIMARY_PORT);
        String path = request.getPathInfo();
        String url = host + path;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            String jsonRequest = getRequestBody(request);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(jsonRequest);
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    System.out.println(jsonResponse);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter outResponse = response.getWriter();
                    outResponse.write(jsonResponse);
                    outResponse.flush();
                    outResponse.close();
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    System.out.println("400: Event unsuccessfully created");
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs the operation of purchasing a ticket
     * @param request http request
     * @param response http response
     * */
    private void purchaseTickets(int eventId, int userId, HttpServletRequest request, HttpServletResponse response) {
        String host = Membership.PRIMARY_HOST + ":" + String.valueOf(Membership.PRIMARY_PORT);
        String path = "/purchase/" + eventId;
        String url = host + path;
        try {
            String jsonRequest = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(jsonRequest);
            int tickets = ((Long)jsonObj.get("tickets")).intValue();
            logger.debug("/purchase");
            logger.debug("event: " +eventId+ " tickets: " + tickets);
            JSONObject eventServiceJson = new JSONObject();
            eventServiceJson.put("userid", userId);
            eventServiceJson.put("eventid", eventId);
            eventServiceJson.put("tickets" , tickets);
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(eventServiceJson.toString());
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    System.out.println(jsonResponse);
                    response.setStatus(HttpServletResponse.SC_OK);
                    logger.debug("SC_OK");
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    System.out.println("400: Tickets could not be purchased");
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * It gets the event list
     * @param response http response
     * */
    private void getEventList( HttpServletResponse response) {
        String host = Membership.PRIMARY_HOST + ":" + String.valueOf(Membership.PRIMARY_PORT);
        String path = "/list";
        String url = host + path;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();
                    out.write(jsonResponse);
                    out.flush();
                    out.close();
                    System.out.println(jsonResponse);
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    System.out.println("400: No events found");
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }
    }
    /**
     * Performs the operation of getting details of events
     * @param request http request
     * @param response http response
     * */
    private void getEventDetails(HttpServletRequest request, HttpServletResponse response) {
        String host = Membership.PRIMARY_HOST + ":" + String.valueOf(Membership.PRIMARY_PORT);
        String path = request.getPathInfo();
        String url = host + path;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();
                    out.write(jsonResponse);
                    out.flush();
                    out.close();
                    System.out.println(jsonResponse);
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    System.out.println("400: Event not found");
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }
    }

    /**
     * Gets the jason of the body of the response and converted into string
     * @param conn http request
     * @return json received in the request converted into a string
     * */
    private String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in;
        String line, body;
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer sb = new StringBuffer();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        body = sb.toString();
        in.close();
        return body;
    }
    /**
     * Gets the jason of the body of the request and converted into string
     * @param request http request
     * @return json received in the request converted into a string
     * */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader in;
        String line, body;
        in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer sb = new StringBuffer();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        body = sb.toString();
        in.close();
        return body;
    }
}
