package cs682;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that contains the data structure that holds the list
 * of all the events registered in the EventService
 */
public class EventData {
    /** Thread Safe Data Structure thar contains the list of events registered */
    private SortedMap<Integer,Event> eventsMap;
    private ReentrantLock lock;

    /** Makes sure only one EvenData is instantiated. */
    private static EventData singleton = new EventData();

    /** Constructor */
    private EventData() {
        eventsMap = Collections.synchronizedSortedMap(new TreeMap<Integer,Event>());
        lock = new ReentrantLock();
    }

    /** Makes sure only one EvenData is instantiated. Returns the Singleton */
    public static EventData getInstance(){
        return singleton;
    }

    /**
     * Method returns the last event id
     * @return last event id
     */
    public int getLastEventId() {
        lock.lock();
        try {
            if (!eventsMap.isEmpty()) {
                return eventsMap.lastKey();
            } else {
                return 0;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Method that allows to add a new event to the list of
     * events of the EventService
     * @param newEvent new event to be created
     */

     public void addEvent(Event newEvent) {
        eventsMap.put(newEvent.getId(), newEvent);
     }

    /**
     * Method that produces a list of all the
     * events registered in the EventService
     * @return  List of event objects
     */
    public List<Event> getEventsList() {
        List<Event> eventsList = new ArrayList<>();
        Set<Integer> keySet = eventsMap.keySet();
        synchronized(eventsMap) {
            for (Integer key : keySet) {
                Event eventCopy;
                eventCopy = getEventDetails(key);
                //eventsList.add(eventsMap.get(key));
                eventsList.add(eventCopy);
            }
        }
        return eventsList;
    }

    public boolean isRegistered(int id) {
      return eventsMap.containsKey(id);
    }

    /**
     * Method that returns the details of a given
     * event id
     * @param id - event id
     * @return Event object with the details of the event
     */
    public Event getEventDetails(int id) {
        lock.lock();
        Event eventCopy = new Event();
         try {
             if (isRegistered(id)) {
                 eventCopy.setId(eventsMap.get(id).getId());
                 eventCopy.setName(eventsMap.get(id).getName());
                 eventCopy.setUserId(eventsMap.get(id).getUserId());
                 eventCopy.setNumTickets(eventsMap.get(id).getNumTickets());
                 eventCopy.setAvail(eventsMap.get(id).getAvail());
                 eventCopy.setPurchased(eventsMap.get(id).getPurchased());
             } else {
                 eventCopy = null;
             }
         } finally {
             lock.unlock();
             return eventCopy;
         }
    }

    /**
     * Method that generates a JSON Array with all the events
     * registered in the map of events
     * @return jsonArray of events
     */
    public JSONArray createJsonEventsList() {
        List<Event> eventsList = getEventsList();
        JSONArray jsonArray = new JSONArray();
        for ( Event e: eventsList) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("eventid", e.getId());
            jsonObj.put("eventname",e.getName());
            jsonObj.put("userid", e.getUserId());
            jsonObj.put("avail", e.getAvail());
            jsonObj.put("purchased", e.getPurchased());
            jsonArray.add(jsonObj);
        }
        System.out.println(jsonArray.toString());
        return jsonArray;
    }
    /**
     * Method that generates a JSON object with the details
     * of the event of the given id
     * @return jsonArray of events
     */
    public JSONObject createJsonOneEvent(int id) {
        JSONObject jsonEvent = new JSONObject();
        if (isRegistered(id)) {
            Event event = getEventDetails(id);
            jsonEvent.put("eventid", event.getId());
            jsonEvent.put("eventname", event.getName());
            jsonEvent.put("userid", event.getUserId());
            jsonEvent.put("avail", event.getAvail());
            jsonEvent.put("purchased", event.getPurchased());
        }
        return jsonEvent;
    }
    /**
     * Method that returns the amount of tickets available
     * for a given event id
     * @param id event id
     * @return amount the tickets available
     */
    public int ticketsAvailable(int id) {
        lock.lock();
        try {
            if (isRegistered(id)) {
                return eventsMap.get(id).getAvail();
            } else {
                return 0;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Method that update the amount of available and purchased
     * tickets of the given event
     * @param eventId event id
     * @param numTickets amount of tickets to purchase
     * @return true if successful or false if unsuccessful
     */
    public boolean updateNumTickets(int eventId, int numTickets) {
        boolean success = false;
        lock.lock();
        try {
            if (ticketsAvailable(eventId) >= numTickets) {
                Event eventToUpdate = eventsMap.get(eventId);
                eventToUpdate.setAvail(eventToUpdate.getAvail() - numTickets);
                eventToUpdate.setPurchased(eventToUpdate.getPurchased() + numTickets);
                success = true;
            }
        } finally {
            lock.unlock();
            return success;
        }
    }

    /**
     * Method that does the roll back of the method updateNumTickets
     * @param eventId event id
     * @param numTickets amount of tickets purchased
     * @return true if successful or false if unsuccessful
     */
    public boolean undoUpdateNumTickets(int eventId, int numTickets) {
        boolean success = false;
        lock.lock();
        try {
            Event eventToUpdate = eventsMap.get(eventId);
            eventToUpdate.setAvail(eventToUpdate.getAvail() + numTickets);
            eventToUpdate.setPurchased(eventToUpdate.getPurchased() - numTickets);
            success = true;
        }finally {
            lock.unlock();
            return success;
        }
    }

    /**
     * Method that prints on the console the content
     * of the data structure of events
     */
    public void printEventList() {
        synchronized(eventsMap) {
            System.out.println("Event List: ");
            Set<Integer> keySet = eventsMap.keySet();
            for (Integer key: keySet) {
                System.out.println(eventsMap.get(key).toString());
            }
        }
    }
}
