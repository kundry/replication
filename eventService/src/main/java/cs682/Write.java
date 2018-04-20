package cs682;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.concurrent.CountDownLatch;

public class Write {
    private String path;
    private String jsonBody;
    public CountDownLatch latch;

    public Write(String path, String json, int vId) {
        this.path = path;
        this.jsonBody = addVersionId(vId, json);
    }

    public Write(String path, String json) {
        this.path = path;
        this.jsonBody = json;
    }

    public String getPath(){
        return this.path;
    }
    public String getJsonBody(){ return this.jsonBody; }

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

    public void setLatch(CountDownLatch latch){
        this.latch = latch;
    }

    public void decrementLatch(){
        this.latch.countDown();
    }
}
