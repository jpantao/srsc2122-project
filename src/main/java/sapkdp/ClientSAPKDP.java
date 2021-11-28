package sapkdp;

import common.Utils;
import sapkdp.messages.HeaderSAPKDP;
import sapkdp.messages.PlainMsgSAPKDP;
import sapkdp.messages.PlainPBHello;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class ClientSAPKDP {

    public static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/sapkdp.properties";

    //TODO: convert alias to args or props!
    // Pubkey alias: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private final Map<String, PublicKey> keyring;
    private final KeyPair keyPair;
    private final String pboxID;
    private final String userID;
    private final String sigserverAddr;

    public ClientSAPKDP(String pboxID, String userID, String keystoreFile, char[] storepass, String sigserverAddr) {
        this.sigserverAddr = sigserverAddr;
        this.pboxID = pboxID;
        this.userID = userID;

        // load keypair from keystore
        keyPair = Utils.getKeyPair(keystoreFile, storepass, PROXYBOX_ALIAS);

        // load public keys into keyring
        keyring = new HashMap<>();
        keyring.put(SIGSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, SIGSERVER_ALIAS));
        keyring.put(COINISSUER_ALIAS, Utils.getPubKey(keystoreFile, storepass, COINISSUER_ALIAS));
        keyring.put(STRSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, STRSERVER_ALIAS));
    }



    public void handshake() {
        String[] addr = sigserverAddr.split(":");
        try (Socket sock = new Socket(addr[0], Integer.parseInt(addr[1])) ){
            DataInputStream in = new DataInputStream(sock.getInputStream());
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());

            byte[] payload;

            //TODO: (round 1)
            PlainPBHello hello = new PlainPBHello(userID, pboxID);
            payload = PlainMsgSAPKDP.serialize(hello);
            sendWithHeader(out, hello.getType(), payload);
            Utils.logSent(hello);

            //TODO: (round 2)
            //TODO: (round 3)
            //TODO: (round 4)
            //TODO: (round 5)
            //TODO: (round 6)


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void sendWithHeader(DataOutputStream out, PlainMsgSAPKDP.Type type, byte[] payload) throws IOException {
        HeaderSAPKDP header = new HeaderSAPKDP(VERSION, type.msgType, (short) payload.length);
        out.write(header.encode());
        out.write(payload);
    }


    private boolean rcvAuthReq(DataInputStream in){
        byte[] headerBytes = new byte[HeaderSAPKDP.BYTE_LEN];

        try {
            in.read(headerBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        HeaderSAPKDP header= new HeaderSAPKDP(headerBytes);


        return true;
    }


}
