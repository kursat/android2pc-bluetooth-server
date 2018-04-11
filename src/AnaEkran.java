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

public class AnaEkran {
    private JPanel FormPanel;
    private JPanel solPanel;
    private JPanel sagPanel;
    private JList bagliCihazlarListesi;
    private JButton serveriBaslatButonu;
    private JTextArea tumMesajlar;
    private JButton gonderButonu;
    private JTextField mesajAlani;
    private JLabel mesajEtiketi;

    private static final String UUID_STRING = "11111111111111111111111111111111"; // 32 hex digits

    public ArrayList<BluetoothBaglantisi> baglantilar = new ArrayList<BluetoothBaglantisi>();
    private volatile boolean isRunning = false;

    private StreamConnectionNotifier server;

    // İlk çalışan fonksiyon
    public AnaEkran() {

        serveriBaslatButonu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // Server'ı başlatan thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new ServeriBaslat().execute();
                    }
                });

                // Arayüzü sürekli güncelleyen thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new ArayuzuGuncelle().execute();
                    }
                });

                // Arayüz değişiklikleri
                solPanel.setVisible(true);
                sagPanel.setVisible(true);
                serveriBaslatButonu.setVisible(false);
            }
        });

        gonderButonu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                for (BluetoothBaglantisi handler : baglantilar) {
                    // Mesajı tüm bağlantılara gönder
                    handler.mesajiGonder(mesajAlani.getText());
                }
            }
        });

        bagliCihazlarListesi.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
            }
        });
    }

    public static void main(String[] args) {

        // Ana panel gösteriliyor
        JFrame frame = new JFrame("AnaEkran");

        frame.setContentPane(new AnaEkran().FormPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void cihaziHazirla() {
        try {
            // Bluetooth cihazı açarak görünür yapıyoruz.
            LocalDevice cihaz = LocalDevice.getLocalDevice();

            System.out.println("Cihaz adı: " + cihaz.getFriendlyName());
            System.out.println("Cihaz MAC adresi: " + cihaz.getBluetoothAddress());

            boolean sonuc = cihaz.setDiscoverable(DiscoveryAgent.GIAC);

            System.out.println("Cihaz görünürlüğü: " + sonuc);
        } catch (BluetoothStateException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void baglantiyiBaslat() {
        try {
            System.out.println("Bağlantı dinleniyor...");

            server = (StreamConnectionNotifier) Connector.open(
                    "btspp://localhost:11111111111111111111111111111111;" +
                            "name=echoserver;authenticate=false");
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void baglantilariYonet() {
        isRunning = true;
        try {
            while (isRunning) {
                System.out.println("Bağlantı bekleniyor...");
                StreamConnection conn = server.acceptAndOpen();

                // Bağlantı başlatılıyor
                BluetoothBaglantisi baglanti = new BluetoothBaglantisi(conn);
                baglantilar.add(baglanti);
                baglanti.start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void serveriKapat() {
        System.out.println("Server kapanıyor");
        if (isRunning) {
            isRunning = false;
            try {
                server.close();
            } catch (IOException e) {
                System.err.println(e);
            }

            // Tüm bağlantılar kapatılıyor
            for (BluetoothBaglantisi baglanti : baglantilar) {
                baglanti.baglantiyiKapat();
            }
            baglantilar.clear();
        }
    }

    public class ServeriBaslat extends SwingWorker<String, Void> {

        public ServeriBaslat() {
        }

        @Override
        public String doInBackground() {

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    serveriKapat();
                }
            });
            cihaziHazirla();
            baglantiyiBaslat();
            baglantilariYonet();

            return "";
        }
    }

    public class ArayuzuGuncelle extends SwingWorker<String, Void> {

        public ArayuzuGuncelle() {
        }

        @Override
        public String doInBackground() {

            while (true) {

                final DefaultListModel baglantiAdlari = new DefaultListModel();

                for (BluetoothBaglantisi handler : baglantilar) {
                    baglantiAdlari.addElement(handler.baglantiAdi());
                    tumMesajlar.setText(String.join("\n", handler.getHistory()));
                }

                bagliCihazlarListesi.setModel(baglantiAdlari);
            }

        }
    }
}
