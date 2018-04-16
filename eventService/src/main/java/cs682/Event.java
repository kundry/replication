package cs682;

/**
 * Class that represents an event and holds the id, name,
 * total tickets, available tickets and purchased tickets
 * of each event
 */
public class Event {

    private int id;
    private String name;
    private int userId;
    private int numTickets;
    private int avail;
    private int purchased;

    /**
     * Constructor
     * @param id  event id
     * @param name event name
     * @param numTickets total amount of tickets that will be sold for the event
     */
    public Event(int id, String name, int userId, int numTickets){
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.numTickets = numTickets;
        this.avail = numTickets;
        this.purchased = 0;
    }

    /**
     * Constructor with no parameters
     */
    public Event(){
        id = 0;
        name = null;
        userId = 0;
        numTickets = 0;
        avail = 0;
        purchased = 0;
    }

    /**
     * Event id setter
     * @param id - new id value
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Event name setter
     * @param name - new name value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Event userId setter
     * @param userId - new userId value
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Event numTickets setter
     * @param numTickets - new numTickets value
     */
    public void setNumTickets(int numTickets) {
        this.numTickets = numTickets;
    }

    /**
     * Event available tickets setter
     * @param avail - new avail value
     */
    public void setAvail(int avail) {
        this.avail = avail;
    }

    /**
     * Event purchased tickets setter
     * @param purchased - new purchased value
     */
    public void setPurchased(int purchased) {
        this.purchased = purchased;
    }

    /**
     * Event id getter
     * @return event id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Event name getter
     * @return event name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Event userId getter
     * @return event userId that created it
     */
    public int getUserId() {
        return this.userId;
    }

    /**
     * Event numTickets getter
     * @return event numTickets
     */
    public int getNumTickets() {
        return this.numTickets;
    }

    /**
     * Event available number of tickets getter
     * @return available number of tickets
     */
    public int getAvail() {
        return this.avail;
    }

    /**
     * Event purchased tickets getter
     * @return event purchased tickets
     */
    public int getPurchased() {
        return this.purchased;
    }

    /**
     * Shows the String representation of the event object
     * @return string representation of the event
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" - ").append(name).append(System.lineSeparator());
        sb.append("Total tickets = ").append(numTickets);
        sb.append(" Available = ").append(avail);
        sb.append(" Sold = ").append(purchased).append(System.lineSeparator());
        return sb.toString();
    }
}
