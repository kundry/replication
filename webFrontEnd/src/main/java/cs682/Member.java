package cs682;

/**
 * Class that holds the information of a member of the architecture
 * all the configuration information needed
 */
public class Member {
    private String host;
    private String port;
    private String type;
    private boolean isPrimary;
    private int pId;

    /**
     * Constructor
     * @param host host
     * @param port port
     * @param type type
     * @param isPrimary true or false if primary
     * @param pId
     */
    public Member( String host, String port, String type, boolean isPrimary, int pId){
        this.host = host;
        this.port = port;
        this.type = type;
        this.isPrimary = isPrimary;
        this.pId = pId;
    }

    /**
     * Getters
     */
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

    /**
     * Setters
     */
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
    public void setPId(int pId){
        this.pId = pId;
    }

    /**
     * Shows the String representation of a member object
     * @return string representation of the event
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(host).append(":").append(port).append(", ");
        sb.append("Type: ").append(type).append(", ");
        sb.append("Primary: ").append(isPrimary).append(", ");
        sb.append("PID:  ").append(pId).append("]").append(System.lineSeparator());
        return sb.toString();
    }
}
