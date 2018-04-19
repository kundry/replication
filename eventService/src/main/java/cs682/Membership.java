package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Membership {
    private static final List<Member> members = Collections.synchronizedList(new ArrayList<Member>());
    public static boolean PRIMARY;
    public static int SELF_EVENT_SERVICE_PORT;
    public static String SELF_EVENT_SERVICE_HOST;
    public static int USER_SERVICE_PORT;
    public static String USER_SERVICE_HOST;
    public static String SELF_JOIN_MODE;
    public static String PRIMARY_HOST;
    public static int PRIMARY_PORT;
    private static volatile int ID_COUNT;
    private static ExecutorService notificationThreadPool = Executors.newFixedThreadPool(6);
    private static ExecutorService replicationThreadPool = Executors.newFixedThreadPool(6);

    final static Logger logger = Logger.getLogger(Membership.class);


    /**
     * It parses the properties file with configuration information
     * and load the data of the configuration of the initial nodes
     * @param config property object to parse
     * */
    public void loadInitMembers(Properties config) {
        SELF_EVENT_SERVICE_PORT = Integer.parseInt(config.getProperty("selfeventport"));
        SELF_EVENT_SERVICE_HOST = config.getProperty("selfeventhost");

        USER_SERVICE_HOST = "http://" + config.getProperty("userhost");
        USER_SERVICE_PORT = Integer.parseInt(config.getProperty("userport"));

        PRIMARY_HOST = "http://" + config.getProperty("primaryhost");
        PRIMARY_PORT = Integer.parseInt(config.getProperty("primaryport"));

        String primaryStatus =  config.getProperty("primary");
        SELF_JOIN_MODE = config.getProperty("eventservicejoining");

        if (SELF_JOIN_MODE.equalsIgnoreCase("on")) {
            logger.debug("New Event Service Joining ... " + SELF_EVENT_SERVICE_HOST +":"+ SELF_EVENT_SERVICE_PORT);
            ArrayList<Member> membersFromPrimary = getMembersFromPrimary();
            members.addAll(membersFromPrimary);
        } else {
            Member primary = new Member(config.getProperty("primaryhost"), config.getProperty("primaryport"), "EVENT", true, 1);
            members.add(primary);
            Member follower1 = new Member(config.getProperty("follower1host"), config.getProperty("follower1port"),"EVENT", false, 2);
            members.add(follower1);
            Member follower2 = new Member(config.getProperty("follower2host"), config.getProperty("follower2port"),"EVENT", false, 3);
            members.add(follower2);
            Member webFrontEnd = new Member(config.getProperty("frontendhost"), config.getProperty("frontendport"),"FRONT_END", false, 0);
            members.add(webFrontEnd);
            ID_COUNT = 3;
        }

        if (primaryStatus.equalsIgnoreCase("on")) {
            PRIMARY = true;
            initSendingReplicaChannel();
        } else {
            PRIMARY = false;
            replicationThreadPool.submit(EventServlet.receiverWorker);
        }
    }

    public void registerServer(HttpServletRequest request, HttpServletResponse response){
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            logger.debug("New node configuration received: " +requestBody);
            String host = (String)jsonObj.get("host");
            int port = ((Long)jsonObj.get("port")).intValue();
            String type = (String)jsonObj.get("type");
            int id = 0;
            if (type.equals("EVENT")){
                logger.debug("Process ID before: " + ID_COUNT);
                ID_COUNT++;
                id = ID_COUNT;
                logger.debug("Process ID generated: " + id);
            }
            Member member = new Member(host, String.valueOf(port), type,false, id);
            notifyOtherServers(member);
            members.add(member);
            printMemberList();
            sendMyListOfMembers(response);
            //notifyOtherServers(member); doing this before
            //send my data
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException p) {
            p.printStackTrace();
        }
    }

    private void notifyOtherServers(Member newMember){
        logger.debug("Process of notifying other servers has started ...");
        synchronized (members) {
            for (Member server : members) {
                if ((server.getType().equals("EVENT") && !server.getIsPrimary())) {
                    System.out.println("In if");
                    String url = "http://" + server.getHost() + ":" + server.getPort() + "/members/add";
                    notificationThreadPool.submit(new NotificationWorker(url, newMember.generateJson().toString()));
                }
            }
        }
    }

    private void sendMyListOfMembers(HttpServletResponse response){
        try {
            JSONObject membersList = createJSONOfMembers();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(membersList.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNotifiedServer(HttpServletRequest request, HttpServletResponse response) {
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            logger.debug("Primary has notified me (" + SELF_EVENT_SERVICE_HOST +":"+ SELF_EVENT_SERVICE_PORT + ") about a new server..." +requestBody);
            Member member = Member.fromJsonToMemberObj(jsonObj);
            members.add(member);
            logger.debug("My list of members has changed ");
            printMemberList();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e){
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private  JSONObject createJSONOfMembers(){
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        synchronized (members) {
            for (Member m : members) {
                JSONObject subObj = new JSONObject();
                subObj.put("host", m.getHost());
                subObj.put("port", m.getPort());
                subObj.put("type", m.getType());
                String isPrimary;
                if (m.getIsPrimary()) isPrimary = "true";
                else isPrimary = "false";
                subObj.put("isPrimary", isPrimary);
                subObj.put("pid", m.getPId());
                array.add(subObj);
            }
        }
        obj.put("members", array);
        return obj;
    }

    private ArrayList<Member> getMembersFromPrimary() {
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
                    logger.debug("400: New Event Server could not be registered");
                    break;
                default:
                    logger.debug("Status Code Received Unknown when registering New Event Server");
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
        json.put("host", SELF_EVENT_SERVICE_HOST);
        json.put("port", SELF_EVENT_SERVICE_PORT);
        json.put("type", "EVENT");
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
                Member member = Member.fromJsonToMemberObj(obj);
                members.add(member);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return members;
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
     * Method that prints on the console the content
     * of the data structure of members
     */
    public void printMemberList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Members List:");
        sb.append(System.lineSeparator());
        synchronized (members) {
            for (Member m : members) {
                sb.append(m);
            }
        }
        logger.debug(sb.toString());
    }

    public ArrayList<Member> getMembers(){
        ArrayList<Member> list = new ArrayList<>();
        synchronized (members) {
            for (Member m : members) {
                list.add(m);
            }
        }
        return list;
    }

    public  void initSendingReplicaChannel() {
        synchronized (members) {
            for (Member m : members) {
                if(m.getType().equals("EVENT") && !m.getIsPrimary()) {
                    String hostAndPort = "http://" + m.getHost() + ":" + m.getPort();
                    SendingReplicaWorker worker = new SendingReplicaWorker(hostAndPort);
                    EventServlet.registerInChannel(worker);
                    replicationThreadPool.submit(worker);
                }
            }
        }
    }

}
