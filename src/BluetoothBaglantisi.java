import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class BluetoothBaglantisi extends Thread {

    private StreamConnection baglanti;
    private String baglantiAdi;
    private InputStream in;
    private OutputStream out;
    private ArrayList<String> mesajGecmisi = new ArrayList<String>();


    public String baglantiAdi() {
        return baglantiAdi;
    }

    public ArrayList<String> getHistory() {
        return mesajGecmisi;
    }

    private volatile boolean baglantiyiDinle = false;

    public BluetoothBaglantisi(StreamConnection conn) {
        // Telefon ile bağlantı kuruluyor
        this.baglanti = conn;
        baglantiAdi = baglantiAdiniAl(conn);
        System.out.println("Bağlanan cihaz adı: " + baglantiAdi);
    }

    // Bağlantı kurulduktan sonra ilk çalışan metod
    @Override
    public void run() {
        try {
            // Bağlantıyı gelen/giden mesajlar için sürekli dinliyoruz
            in = baglanti.openInputStream();
            out = baglanti.openOutputStream();

            mesajlariDinle();

            System.out.println(baglantiAdi + "bağlantısı kapatılıyor...");
            if (baglanti != null) {
                in.close();
                out.close();
                baglanti.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private String baglantiAdiniAl(StreamConnection conn) {
        String devName;
        try {
            RemoteDevice rd = RemoteDevice.getRemoteDevice(conn);
            devName = rd.getFriendlyName(false);
        } catch (IOException e) {
            devName = "device ??";
        }
        return devName;
    }

    private void mesajlariDinle() {
        baglantiyiDinle = true;
        String line;
        while (baglantiyiDinle) {
            if ((line = mesajiOku()) == null) {
                baglantiyiDinle = false;
            } else {
                System.out.println("  " + baglantiAdi + " --> " + line);
            }
        }
    }

    private String mesajiOku() {
        // Mesajı okuyoruz.
        byte[] data = null;
        try {
            int len = in.read();
            if (len <= 0) {
                System.out.println("Mesaj uzunluğu hatası: " + baglantiAdi);
                return "";
            } else {
                data = new byte[len];
                len = 0;
                while (len != data.length) {
                    int ch = in.read(data, len, data.length - len);
                    if (ch == -1) {
                        System.out.println("Mesaj okuma hatası: " + baglantiAdi);
                        return null;
                    }
                    len += ch;
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            return null;
        }

        // Gelen mesajı mesaj geçmişine ekliyoruz.
        String message = new String(data).trim();

        mesajGecmisi.add("  " + baglantiAdi + " --> " + message);
        return message;
    }

    public boolean mesajiGonder(String message) {
        try {
            // Mesajı telefona gönderiyoruz.
            out.write(message.length());
            out.write(message.getBytes());
            out.flush();
            // Gönderilen mesajı mesaj geçmişine ekliyoruz.
            mesajGecmisi.add("  " + baglantiAdi + " <-- " + message);
            System.out.println("  " + baglantiAdi + " <-- " + message);
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }

    }

    public void baglantiyiKapat() {
        baglantiyiDinle = false;
    }
}
