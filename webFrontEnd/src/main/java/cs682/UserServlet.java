package cs682;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;



public class UserServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        System.out.println(pathInfo);

        if (pathInfo == null) {
            System.out.println("Invalid Path");
        } else {
            if (pathInfo.equals("/create")) {
                createUser(request, response);
            } else {
                System.out.println("Transfer tickets");
                if (pathInfo.matches("/([\\d]+)/tickets/transfer")) {
                    transferTickets(request, response);
                } else {
                    System.out.println("Invalid Path");
                }
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String host = Membership.USER_SERVICE_HOST + ":" + String.valueOf(Membership.USER_SERVICE_PORT);
        String path = request.getPathInfo();
        String url = host + path;
        String jsonResponse;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    jsonResponse = getResponseBody(conn);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");
                    JSONObject updatedJson = buildJsonWithEventDetails(jsonResponse);
                    PrintWriter out = response.getWriter();
                    out.write(updatedJson.toString());
                    out.flush();
                    out.close();
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    System.out.println("400: User not found");
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
     * Performs the operation of creating a user and it communicates with the user service
     * @param request request
     * @param response response
     * */
    private void createUser(HttpServletRequest request, HttpServletResponse response) {
        String requestBody, jsonResponse;
        String path = "/create";
        String host = Membership.USER_SERVICE_HOST + ":" + String.valueOf(Membership.USER_SERVICE_PORT);
        String url = host + path;
        try {
            requestBody = getRequestBody(request);
            System.out.println(requestBody);
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(requestBody);
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    jsonResponse = getResponseBody(conn);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter outResponse = response.getWriter();
                    outResponse.write(jsonResponse);
                    outResponse.flush();
                    outResponse.close();
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }
    }

    /**
     * Performs the operation of transfering tickets
     * @param request request
     * @param response response
     * */
    private void transferTickets(HttpServletRequest request, HttpServletResponse response) {
        String requestBody;
        String path = request.getPathInfo();
        String host = Membership.USER_SERVICE_HOST + ":" + String.valueOf(Membership.USER_SERVICE_PORT);
        String url = host + path;
        try {
            requestBody = getRequestBody(request);
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(requestBody);
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    response.setStatus(HttpServletResponse.SC_OK);
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }
    }

    /**
     * Builds a JSon Object with the datails of the events
     * @param jsonFromUser Json with format of a string  returned by the User Service
     * @return jsonArray of events
     * */
    private JSONObject buildJsonWithEventDetails(String jsonFromUser){
        JSONObject jsonUpdated = null;
        JSONArray jsonArrayOfTickets;
        JSONArray jsonArrayEvents;
        JSONParser parser = new JSONParser();
        ArrayList<Integer> eventIdList = new ArrayList();
        try {

            JSONObject userServJson = (JSONObject) parser.parse(jsonFromUser);
            jsonArrayOfTickets = (JSONArray)userServJson.get("tickets");
            for (int i = 0; i < jsonArrayOfTickets.size(); i++) {
                JSONObject row = (JSONObject) jsonArrayOfTickets.get(i);
                int eventId =  ((Long)row.get("eventid")).intValue();
                eventIdList.add(eventId);
            }
            jsonArrayEvents = getEventArray(eventIdList);
            userServJson.replace("tickets",jsonArrayEvents);
            jsonUpdated = userServJson;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return jsonUpdated;
    }
    /**
     * Generates a JsonArray of events
     * @param eventIdList list of event ids
     * @return jsonArray of events
     * */
    private JSONArray getEventArray(ArrayList<Integer> eventIdList){
        String host = Membership.PRIMARY_HOST + ":" + String.valueOf(Membership.PRIMARY_PORT);
        JSONArray jsonArray = new JSONArray();
        try {
            for(int i = 0; i<eventIdList.size(); i++) {
                int eventId = eventIdList.get(i);
                String id = String.valueOf(eventId);
                String url = host + "/" + id;
                URL urlObj = new URL(url);
                HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpServletResponse.SC_OK) {
                    String jsonResponse = getResponseBody(conn);
                    JSONParser parser = new JSONParser();
                    JSONObject oneEventJson = (JSONObject) parser.parse(jsonResponse);
                    jsonArray.add(oneEventJson);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonArray;
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
}
