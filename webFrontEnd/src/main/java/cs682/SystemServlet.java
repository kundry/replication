package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SystemServlet extends HttpServlet {
    final static Logger logger = Logger.getLogger(SystemServlet.class);
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            if (pathInfo.equals("/heartbeat")) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.debug("Invalid Path");
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        processNewPrimary(request, response);
    }
    private void processNewPrimary(HttpServletRequest request, HttpServletResponse response){
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(requestBody);
            String primaryHost = (String) json.get("primaryhost");
            int primaryPort = ((Long)json.get("primaryport")).intValue();
            logger.debug("New Primary " + primaryHost+":"+primaryPort);
            Membership.removePrimary();
            Membership.updatePrimary(primaryHost, primaryPort);
        } catch (ParseException e) {
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
}
