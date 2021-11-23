package sigserver.sapkdp;

import common.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.Key;
import java.util.Properties;

public class ProtoSAPKDP {

    public static void logProtoInstance(ProtoSAPKDP p) {
        System.out.println("--- New SAPKDP instance ---");
        System.out.println("sigserver:\t" + p.sigserver);
        System.out.println("proxyBoxID:\t" + p.proxyBoxID);
        System.out.println("proxyBoxID:\t" + p.proxyBoxID);
        System.out.println("userID:\t" + p.userID);
        System.out.println("userPW:\t" + p.userPW);
        System.out.println("pub key:\t" + Utils.toHex(p.pub.getEncoded()));
        System.out.println("prv key:\t" + Utils.toHex(p.prv.getEncoded()));
        System.out.println("---------------------------");
    }

    private String sigserver, proxyBoxID, userID, userPW;
    private Key pub, prv;

    public ProtoSAPKDP(String proxyInfoFile, String userID, String userPW, Key pub, Key prv) {
        this.userID = userID;
        this.userPW = userPW;
        this.pub = pub;
        this.prv = prv;

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(proxyInfoFile));

            this.sigserver = properties.getProperty("sigserver");
            this.proxyBoxID = properties.getProperty("proxyBoxID");

            logProtoInstance(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handshake() {
        String[] addr = sigserver.split(":");

        try (Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]))) {


            //TODO: (round 1) send PB-Hello




            //TODO: (round 2) recv SS-AuthenticationRequest
            //TODO: (round 3) send PB-Authentication
            //TODO: (round 4) recv SS-PaymentRequest
            //TODO: (round 5) send PB-Payment
            //TODO: (round 6) recv PB-Payment SS-TicketCredentials

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
