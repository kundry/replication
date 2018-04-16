package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class Membership {

    private static ArrayList<Member> members = new ArrayList<>();
    public static int SELF_FRONT_END_PORT;
    public static String SELF_FRONT_END_HOST;
    public static String USER_SERVICE_HOST;
    public static int USER_SERVICE_PORT;
    public static String PRIMARY_HOST;
    public static int PRIMARY_PORT;
    public static String SELF_JOIN_MODE;
    final static Logger logger = Logger.getLogger(Membership.class);


    public void add(Member member){
        members.add(member);
    }

    /**
     * It parses the properties file with configuration information
     * and load the data of the configuration of the initial nodes
     * @param config property object to parse
     * */
    public void loadInitMembers(Properties config) {
        SELF_FRONT_END_PORT = Integer.parseInt(config.getProperty("selffrontendport"));
        SELF_FRONT_END_HOST = config.getProperty("selffrontendhost");

        USER_SERVICE_HOST = "http://" + config.getProperty("userhost");
        USER_SERVICE_PORT = Integer.parseInt(config.getProperty("userport"));

        PRIMARY_HOST = "http://" + config.getProperty("primaryhost");
        PRIMARY_PORT = Integer.parseInt(config.getProperty("primaryport"));

        SELF_JOIN_MODE = config.getProperty("frontendjoining");

        if (SELF_JOIN_MODE.equalsIgnoreCase("on")) {
            logger.debug("New Web Front End Service Joining ... " + SELF_FRONT_END_HOST +":"+ SELF_FRONT_END_PORT );
            ArrayList<Member> membersFromPrimary = getMembersFromPrimary();
            members.addAll(membersFromPrimary);
            printMemberList();
        } else {
            Member primary = new Member(config.getProperty("primaryhost"), config.getProperty("primaryport"), "EVENT", true, 1);
            members.add(primary);
            Member follower1 = new Member(config.getProperty("follower1host"), config.getProperty("follower1port"),"EVENT", false, 2);
            members.add(follower1);
            Member self = new Member(config.getProperty("selffrontendhost"), config.getProperty("selffrontendport"), "FRONT_END", false, 0);
            members.add(self);
        }
    }

    private ArrayList<Member> getMembersFromPrimary(){
        ArrayList<Member> members = new ArrayList<>();
        try {
            HttpURLConnection conn = registerWithPrimary();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    logger.debug("Members received from Primary: " + jsonResponse);
                    members = parseMembers(jsonResponse);
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    logger.debug("400: New Front End Server could not be registered");
                    break;
                default:
                    logger.debug("Status Code Received Unknown when registering New Front End Server");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return members;
    }

    private  HttpURLConnection registerWithPrimary() {
        String host = PRIMARY_HOST + ":" + String.valueOf(PRIMARY_PORT);
        String path = "/members/register";
        String url = host + path;
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(url);
            conn  = (HttpURLConnection) urlObj.openConnection();
            setPostRequestProperties(conn);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            JSONObject newFrontEndConfig = createJsonWithOwnConfig();
            out.write(newFrontEndConfig.toString());
            out.flush();
            out.close();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
            return conn;
        }
    }

    private JSONObject createJsonWithOwnConfig() {
        JSONObject json = new JSONObject();
        json.put("host", SELF_FRONT_END_HOST);
        json.put("port", SELF_FRONT_END_PORT);
        json.put("type", "FRONT_END");
        return json;
    }
    private void setPostRequestProperties(HttpURLConnection conn){
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
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

    private ArrayList<Member> parseMembers(String JsonOfMembersReceived) {
        ArrayList<Member> members = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonOfMembers = (JSONObject) parser.parse(JsonOfMembersReceived);
            JSONArray arrayOfMembers = (JSONArray) jsonOfMembers.get("members");
            Iterator<JSONObject> iterator = arrayOfMembers.iterator();
            while (iterator.hasNext()) {
                JSONObject obj = iterator.next();
                boolean primary;
                if (obj.get("isPrimary") == "true") primary = true;
                else primary = false;
                Member member = new Member((String)obj.get("host"), (String)obj.get("port"), (String)obj.get("type"),primary, ((Long)obj.get("pid")).intValue());
                members.add(member);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Method that prints on the console the content
     * of the data structure of members
     */
    public void printMemberList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Members List:");
        sb.append(System.lineSeparator());
        for (Member m: members) {
            sb.append(m);
        }
        logger.debug(sb.toString());
    }

}
