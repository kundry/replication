package cs682;

public class Write {
    private String path;
    private String jsonBody;

    public Write(String path, String json){
        this.path = path;
        this.jsonBody = json;
    }

    public String getPath(){
        return this.path;
    }
    public String getJsonBody(){
        return this.jsonBody;
    }

}
