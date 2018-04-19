package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class SendingReplicaWorker implements Runnable {
    boolean beingPrimary;
    private String hostAndPort;
    private Queue<Write> writesQueue; // it should be threadsafe
    final static Logger logger = Logger.getLogger(SendingReplicaWorker.class);

    /** Constructor of the class that initialize the  parameters needed to establish the
     *  communication with the corresponding follower
     *  @param hostAndPort host and port to replicate

     */
    public SendingReplicaWorker(String hostAndPort) {
        this.hostAndPort = hostAndPort;
        this.writesQueue = new LinkedList<>();
        this.beingPrimary = true;
    }

    /** Method that adds the write received to a queue and performs the notify
     *  to activate the processing of the incoming write
     *  @param write received by the remote host
     */
    public void queueWrite(Write write){
        synchronized(this) {
            writesQueue.add(write);
            this.notify();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            while (beingPrimary) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!writesQueue.isEmpty()) {
                    Write incomingWrite = writesQueue.remove();
                    String url = hostAndPort + incomingWrite.getPath();
                    try {
                        URL urlObj = new URL(url);
                        HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
                        setPostRequestProperties(conn);
                        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                        out.write(incomingWrite.getJsonBody());
                        out.flush();
                        out.close();
                        int responseCode = conn.getResponseCode();
                        switch (responseCode) {
                            case HttpServletResponse.SC_OK:
                                //logger.debug("Write to " + url + " was replicated successfully");
                                break;
                            default:
                                //logger.debug("Status Code Received Unknown when replicating write to: " + url);
                                break;
                        }
                        incomingWrite.decrementLatch();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // deregisterFromChannel();
            //how to exit the while ? here or when I am notified of not being primary anymore
        }
    }

    /** Method that sets false the flag that keeps the thread waiting for
     * incoming writes to be propagated to the corresponding follower
     */
    public void stop(){
        beingPrimary = false;
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
}
