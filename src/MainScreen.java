import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class MainScreen {
    private JPanel FormPanel;
    private JPanel LeftPanel;
    private JPanel RightPanel;
    private JList connectedDevices;
    private JButton startServerButton;
    private JTextArea messagesTextArea;
    private JButton broadcastButton;
    private JTextField broadcastMessageTextField;
    private JLabel messageLabel;

    private static final String UUID_STRING = "11111111111111111111111111111111"; // 32 hex digits
    private static final String SERVICE_NAME = "echoserver";


    // globals
    public ArrayList<ThreadedEchoHandler> handlers = new ArrayList<ThreadedEchoHandler>();
    private volatile boolean isRunning = false;

    private StreamConnectionNotifier server;


    public MainScreen() {
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        new YourClassSwingWorker().execute();
                    }
                });

                LeftPanel.setVisible(true);
                RightPanel.setVisible(true);
                startServerButton.setVisible(false);
            }
        });
        broadcastButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                final DefaultListModel fruitsName = new DefaultListModel();

                for (ThreadedEchoHandler handler : handlers) {

                    fruitsName.addElement(handler.getClientName());

                    handler.sendMessage(broadcastMessageTextField.getText());

                    messagesTextArea.setText(String.join("\n", handler.getHistory()));
                }

                connectedDevices.setModel(fruitsName);

            }
        });
        connectedDevices.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
            }
        });
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("MainScreen");

        frame.setContentPane(new MainScreen().FormPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void initDevice() {
        try {
            LocalDevice local = LocalDevice.getLocalDevice();

            System.out.println("Device name: " + local.getFriendlyName());
            System.out.println("Bluetooth Address: " + local.getBluetoothAddress());

            boolean res = local.setDiscoverable(DiscoveryAgent.GIAC);

            System.out.println("Discoverability set: " + res);
        } catch (BluetoothStateException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void createRFCOMMConnection() {
        try {
            System.out.println("Start advertising " + SERVICE_NAME + "...");

            server = (StreamConnectionNotifier) Connector.open(
                    "btspp://localhost:" + UUID_STRING +
                            ";name=" + SERVICE_NAME + ";authenticate=false");
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void processClients() {
        isRunning = true;
        try {
            while (isRunning) {
                System.out.println("Waiting for incoming connection...");
                StreamConnection conn = server.acceptAndOpen();
                // wait for a client connection
                ThreadedEchoHandler handler = new ThreadedEchoHandler(conn);
                handlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    private void closeDown() {
        System.out.println("closing down server");
        if (isRunning) {
            isRunning = false;
            try {
                server.close();
            } catch (IOException e) {
                System.err.println(e);
            }

            // close all the handlers
            for (ThreadedEchoHandler hand : handlers) {
                hand.closeDown();
            }
            handlers.clear();
        }
    } // end of closeDown()

    public class YourClassSwingWorker extends SwingWorker<String, Void> {

        public YourClassSwingWorker() {
        }

        @Override
        public String doInBackground() {

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    closeDown();
                }
            });
            initDevice();
            createRFCOMMConnection();
            processClients();

            return "";
        }

        @Override
        public void done() {
            // Update the GUI with the updated list.
        }
    }
}
