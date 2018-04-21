package cs682;

import org.json.simple.JSONObject;

public class Member {
    private String host;
    private String port;
    private String type;
    private boolean isPrimary;
    private int pId;

    public Member( String host, String port, String type, boolean isPrimary, int pId){
        this.host = host;
        this.port = port;
        this.type = type;
        this.isPrimary = isPrimary;
        this.pId = pId;
    }

    public String getHost(){
        return this.host;
    }
    public String getPort(){
        return this.port;
    }
    public String getType(){
        return this.type;
    }
    public boolean getIsPrimary(){
        return this.isPrimary;
    }
    public int getPId(){
        return this.pId;
    }

    public void setHost(String host){
        this.host = host;
    }
    public void setPort(String port){
        this.port = port;
    }
    public void setType(String type){
        this.type = type;
    }
    public void setIsPrimary(boolean isPrimary){
        this.isPrimary = isPrimary;
    }
    public void setPId(int pId){ this.pId = pId; }

    /**
     * Creates a json representation of a member object
     * @return JSON Obj representation of the member
     */
    public JSONObject generateJson() {
        JSONObject obj = new JSONObject();
        obj.put("host", this.host);
        obj.put("port", this.port);
        obj.put("type", this.type);
        obj.put("isPrimary", this.isPrimary);
        obj.put("pid", this.pId);
        return obj;
    }

    /**
     * Shows the String representation of a member object
     * @return string representation of the member
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(host).append(":").append(port).append(", ");
        sb.append("Type: ").append(type).append(", ");
        sb.append("Primary: ").append(isPrimary).append(", ");
        sb.append("PID:  ").append(pId).append("]").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Converts the json representation of a member into a member object
     * @param json json object that contains data of a member
     * @return member object
     */
    public static Member fromJsonToMemberObj(JSONObject json){
        boolean primary;
        if (json.get("isPrimary").equals("true")) primary = true;
        else primary = false;
        Member member = new Member((String)json.get("host"), (String)json.get("port"), (String)json.get("type"),primary, ((Long)json.get("pid")).intValue());
        return member;
    }
}
