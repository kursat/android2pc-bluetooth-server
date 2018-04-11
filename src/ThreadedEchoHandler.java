import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ThreadedEchoHandler extends Thread {

    private StreamConnection conn;
    private String clientName;
    private InputStream in;
    private OutputStream out;
    private ArrayList<String> history = new ArrayList<String>();


    public String getClientName() {
        return clientName;
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    // globals
    private volatile boolean isRunning = false;

    public ThreadedEchoHandler(StreamConnection conn) {
        this.conn = conn;
        // store the name of the connected client
        clientName = reportDeviceName(conn);
        System.out.println("Handler spawned for client: " + clientName);
    } // end of ThreadedEchoHandler()

    private String reportDeviceName(StreamConnection conn) {
        // return the friendly name of the device being connected
        String devName;
        try {
            RemoteDevice rd = RemoteDevice.getRemoteDevice(conn);
            devName = rd.getFriendlyName(false);
        } catch (IOException e) {
            devName = "device ??";
        }
        return devName;
    }

    @Override
    public void run() {
        try {
            in = conn.openInputStream();
            out = conn.openOutputStream();

            processMsgs();

            System.out.println(" Closing " + clientName + " connection");
            if (conn != null) {
                in.close();
                out.close();
                conn.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    } // end of run()


    private void processMsgs() {
        isRunning = true;
        String line;
        while (isRunning) {
            if ((line = readData()) == null) {
                isRunning = false;
            } else { // there is some input
                System.out.println("  " + clientName + " --> " + line);

                if (line.trim().equals("bye$$")) {
                    isRunning = false;
                } else {
                    String upper = line.trim().toUpperCase();
                    if (isRunning) {
//                        System.out.println("  " + clientName + " <-- " + line);
//                        sendMessage(upper);
                    }
                }
            }
        }
    }


    private String readData() {
        byte[] data = null;
        try {
            int len = in.read(); // get the message length
            if (len <= 0) {
                System.out.println(clientName + ": Message Length Error");
                return "";
            } else {
                data = new byte[len];
                len = 0;
                // read the message
                while (len != data.length) {
                    int ch = in.read(data, len, data.length - len);
                    if (ch == -1) {
                        System.out.println(clientName + ": Message Read Error");
                        return null;
                    }
                    len += ch;
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            return null;
        }

        String message = new String(data).trim();

        history.add("  " + clientName + " --> " + message);
        return message;
    }

    public boolean sendMessage(String message) {
        try {
            history.add("  " + clientName + " <-- " + message);
            System.out.println("  " + clientName + " <-- " + message);
            out.write(message.length());
            out.write(message.getBytes());
            out.flush();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }

    }

    public void closeDown() {
        isRunning = false;
    }
}
