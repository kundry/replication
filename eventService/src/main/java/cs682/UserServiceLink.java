package cs682;

import org.json.simple.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStreamWriter;

/**
 * Class that communicates with the UserService and
 * send requests to validate information about users
 * It is an UserService API for the EventService
 * */
public class UserServiceLink {
    //private  String USER_SERVICE_HOST = "http://localhost:4357";
    /**
     * Method that verifies if a given user id
     * is registered in the UserService by sending a GET Request
     * */
    public boolean isValidUserId(int userId) {
        String host = Membership.USER_SERVICE_HOST + ":" + String.valueOf(Membership.USER_SERVICE_PORT);
        String path = "/" + String.valueOf(userId);
        String url = host + path;
        boolean isValid = false;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpServletResponse.SC_OK) isValid = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isValid;
    }
    /**
     * Method that adds tickets to a given user
     * by sending a POST Request to the UserService
     * @param userId id of the user
     * @param eventId event id
     * @param numTickets amount of tickets to add
     * @return true if success false if failure
     * */
    public boolean addTicketsToUser(int userId, int eventId, int numTickets) {
        boolean success = false;
        String host = Membership.USER_SERVICE_HOST + ":" + String.valueOf(Membership.USER_SERVICE_PORT);
        String path = "/" + String.valueOf(userId) + "/tickets/add";
        String url = host + path;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("eventid", eventId);
            jsonObj.put("tickets", numTickets);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(jsonObj.toString());
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpServletResponse.SC_OK) success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

}
