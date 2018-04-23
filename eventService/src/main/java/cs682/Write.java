package cs682;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.concurrent.CountDownLatch;

/**
 * Class that represents a write operation and holds all the data needed
 * for the change to be made in the data structure
 */
public class Write {
    private String path;
    private String jsonBody;
    public CountDownLatch latch;

    /**
     * Constructor
     * @param path request path
     * @param json request body
     * @param vId version time stamp
     */
    public Write(String path, String json, int vId) {
        this.path = path;
        this.jsonBody = addVersionId(vId, json);
    }

    /**
     * Constructor
     * @param path request path
     * @param json request body
     */
    public Write(String path, String json) {
        this.path = path;
        this.jsonBody = json;
    }

    /**
     * Get method for path
     * @return path
     */
    public String getPath(){
        return this.path;
    }

    /**
     * Get method for the jsonBody
     * @return jsonBody
     */
    public String getJsonBody(){ return this.jsonBody; }

    /**
     * Adds the version id of the data structure to the json of the
     * data or content of the data structure
     * @param vId version time stamp
     * @param json  json object with all the events registered
     * @return jsonBody
     */
    private String addVersionId(int vId, String json){
        String modifiedJson = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject newJson = (JSONObject)parser.parse(json);
            newJson.put("versionid", vId);
            modifiedJson = newJson.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return modifiedJson;
    }

    /**
     * Sets  a value for the latch
     * @param latch counter
     */
    public void setLatch(CountDownLatch latch){
        this.latch = latch;
    }


    /**
     * Decrements the latch
     */
    public void decrementLatch(){
        this.latch.countDown();
    }
}
