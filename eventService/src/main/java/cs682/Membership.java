package cs682;

import com.sun.source.tree.SynchronizedTree;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Membership {
    //private static final List<Member> members = Collections.synchronizedList(new ArrayList<Member>());
    private List<Member> members;
    private ReentrantLock lock;
    public static boolean PRIMARY;
    public static int SELF_EVENT_SERVICE_PORT;
    public static String SELF_EVENT_SERVICE_HOST;
    public static int USER_SERVICE_PORT;
    public static String USER_SERVICE_HOST;
    public static String SELF_JOIN_MODE;
    public static String PRIMARY_HOST;
    public static int PRIMARY_PORT;
    public static volatile int ID_COUNT;
    public static boolean IN_ELECTION;
    public static boolean ELECTION_REPLY;
    protected static final EventData eventData = EventData.getInstance();
    private static ExecutorService notificationThreadPool = Executors.newFixedThreadPool(6);
    private static ExecutorService replicationThreadPool = Executors.newFixedThreadPool(6);
    final static Logger logger = Logger.getLogger(Membership.class);

    /** Makes sure only one Membership is instantiated. */
    private static Membership singleton = new Membership();

    /** Constructor */
    private Membership() {
        members = Collections.synchronizedList(new ArrayList<Member>());
        lock = new ReentrantLock();
    }

    /** Makes sure only one EvenData is instantiated. Returns the Singleton */
    public static Membership getInstance(){
        return singleton;
    }

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
        IN_ELECTION = false;
        ELECTION_REPLY = false;
        String primaryStatus =  config.getProperty("primary");
        SELF_JOIN_MODE = config.getProperty("eventservicejoining");

        if (SELF_JOIN_MODE.equalsIgnoreCase("on")) {
            logger.debug("Joining ...");
            ArrayList<Member> membersFromPrimary = register();
            members.addAll(membersFromPrimary);
            ID_COUNT = findMyId(membersFromPrimary);
            printMemberList();
            JSONObject data = getDataFromPrimary();
            System.out.println("data json" + data.toString());
            eventData.initEventData(data);
        } else {
            Member primary = new Member(config.getProperty("primaryhost"), config.getProperty("primaryport"), "EVENT", true, 1);
            members.add(primary);
            //Member follower1 = new Member(config.getProperty("follower1host"), config.getProperty("follower1port"),"EVENT", false, 2);
            //members.add(follower1);
            //Member follower2 = new Member(config.getProperty("follower2host"), config.getProperty("follower2port"),"EVENT", false, 3);
            //members.add(follower2);
            //Member webFrontEnd = new Member(config.getProperty("frontendhost"), config.getProperty("frontendport"),"FRONT_END", false, 0);
            //members.add(webFrontEnd);
            ID_COUNT = 1;
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
                //logger.debug("Process ID before: " + ID_COUNT);
                ID_COUNT++;
                id = ID_COUNT;
                //logger.debug("Process ID generated: " + id);
            }
            Member member = new Member(host, String.valueOf(port), type,false, id);
            notifyOtherServers(member);
            members.add(member);
            printMemberList();
            sendMyListOfMembers(response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException p) {
            p.printStackTrace();
        }
    }

    private void notifyOtherServers(Member newMember){
        //logger.debug("Process of notifying other servers has started ...");
        synchronized (members) {
            for (Member server : members) {
                if ((server.getType().equals("EVENT") && !server.getIsPrimary())) {
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

    private JSONObject createJSONOfMembers(){
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

    private ArrayList<Member> register() {
        ArrayList<Member> members = new ArrayList<>();
        try {
            HttpURLConnection conn = registerWithPrimary();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    logger.debug("Members received" );
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

    private HttpURLConnection registerWithPrimary() {
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
        synchronized (members) {
            ArrayList<Member> list = new ArrayList<>();
            for (Member m : members) {
                list.add(m);
            }
            return list;
        }
    }

    public void initSendingReplicaChannel() {
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
    private ArrayList<Member> getCandidates(){
        synchronized (members) {
            ArrayList<Member> candidates = new ArrayList<>();
            for (Member m : members) {
                if(m.getType().equals("EVENT") && !m.getIsPrimary() && (m.getPId()<ID_COUNT)) {
                 candidates.add(m);
                 logger.debug("candidate " + m.toString());
                }
            }
            return candidates;
        }
    }

     public synchronized void startElection() {
        IN_ELECTION = true;
        ArrayList<Member> candidates = getCandidates();
        try {
            if (candidates.size() > 0) {
                final CountDownLatch latch = new CountDownLatch(candidates.size());
                for (Member m : candidates) {
                    String url = "http://" + m.getHost() + ":" + m.getPort() + "/election/" + ID_COUNT;
                    ElectionWorker worker = new ElectionWorker(url, latch);
                    notificationThreadPool.submit(worker);
                }
                latch.await();
            }
            if (!ELECTION_REPLY || (candidates.size() == 0)) {
                logger.debug("Setting myself as primary");
                PRIMARY = true;
                removePrimary();
                updatePrimary(SELF_EVENT_SERVICE_HOST, SELF_EVENT_SERVICE_PORT);
                printMemberList();
                notifyNewPrimary();
                initSendingReplicaChannel();
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    public void processElectionMessage(int pId, HttpServletResponse response) {
        if (pId > ID_COUNT) {
            response.setStatus(HttpServletResponse.SC_OK);
            if (!IN_ELECTION) {
                startElection();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    public void updatePrimary (String newPrimaryHost , int newPrimaryPort){
        PRIMARY_HOST = "http://" + newPrimaryHost;
        PRIMARY_PORT = newPrimaryPort;
        synchronized (members) {
            for (Member m : members) {
                if (m.getType().equals("EVENT") && (m.getHost().equals(newPrimaryHost)) && (Integer.parseInt(m.getPort())==newPrimaryPort)) {
                    m.setIsPrimary(true);
                }
            }
        }
        logger.debug("List with new primary");
        printMemberList();
    }

    public void removeServerDown(String host, String port) {
        synchronized (members) {
            Iterator<Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                Member m = iterator.next();
                if(m.getHost().equals(host) && m.getPort().equals(port)) {
                    iterator.remove();
                }
            }
        }
    }

    public void removePrimary() {
        synchronized (members) {
            Iterator<Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                Member m = iterator.next();
               if (m.getIsPrimary()) {
                   iterator.remove();
               }
            }
        }
    }


    public JSONObject getDataFromPrimary(){
        String host = PRIMARY_HOST + ":" + String.valueOf(PRIMARY_PORT);
        String path = "/members/data";
        String url = host + path;
        JSONObject json = null;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    logger.debug("Data received");
                    String jsonReceived = getResponseBody(conn);
                    JSONParser parser = new JSONParser();
                    json = (JSONObject) parser.parse(jsonReceived);
                    break;
                default:
                    logger.debug("Problems getting data from primary " + url);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return json;
    }

    private int findMyId(ArrayList<Member> list){
        int id = 0;
        for(Member m : list) {
            if (m.getHost().equals(SELF_EVENT_SERVICE_HOST) && Integer.parseInt(m.getPort()) == SELF_EVENT_SERVICE_PORT){
                id = m.getPId();
            }
        }
       return  id;
    }

    public void notifyNewPrimary(){
        JSONObject json = new JSONObject();
        json.put("primaryhost", SELF_EVENT_SERVICE_HOST);
        json.put("primaryport", SELF_EVENT_SERVICE_PORT);
        synchronized (members) {
            for (Member m : members) {
                if (!m.getIsPrimary()){
                    logger.debug("Notifying new primary");
                    String url = "http://" + m.getHost() + ":" + m.getPort() + "/newprimary";
                    NotificationWorker worker = new NotificationWorker(url, json.toString());
                    notificationThreadPool.submit(worker);
                }

            }
        }
    }
}
