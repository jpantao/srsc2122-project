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

public class ServerSAPKDP {

    //TODO: convert alias to args or props! Furthermore, proxybox key alias should be the ProxyBox's ID
    // Pubkey aliases: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private static ServerSAPKDP instance;

    private Map<String, PublicKey> keyring;
    private KeyPair keyPair;

    public static ServerSAPKDP getInstance() {
        return instance == null ? new ServerSAPKDP() : instance;
    }

    private ServerSAPKDP() {
    }

    public void load(String keystoreFile, char[] storepass) {
        // load keypair from keystore
        keyPair = Utils.getKeyPair(keystoreFile, storepass, SIGSERVER_ALIAS);

        // load public keys into keyring
        keyring = new HashMap<>();
        keyring.put(PROXYBOX_ALIAS, Utils.getPubKey(keystoreFile, storepass, PROXYBOX_ALIAS));
        keyring.put(COINISSUER_ALIAS, Utils.getPubKey(keystoreFile, storepass, COINISSUER_ALIAS));
        keyring.put(STRSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, STRSERVER_ALIAS));
    }

    public void handleHanshake(Socket sock) {
        try {
            DataInputStream in = new DataInputStream(sock.getInputStream());
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());

            int msgType;
            byte[] payload;

            //TODO: (round 1)
            msgType = PlainMsgSAPKDP.Type.PB_HELLO.msgType;
            payload = readConsumingHeader(in, msgType);
            PlainPBHello hello = (PlainPBHello) PlainMsgSAPKDP.deserialize(msgType, payload);
            Utils.logReceived(hello);



            //TODO: (round 2)
            //TODO: (round 3)
            //TODO: (round 4)
            //TODO: (round 5)
            //TODO: (round 6)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static byte[] readConsumingHeader(DataInputStream in, int expectedType) throws IOException {
        byte[] headerBytes = new byte[HeaderSAPKDP.BYTE_LEN];
        in.read(headerBytes); //TODO: verify read bytes
        HeaderSAPKDP header = new HeaderSAPKDP(headerBytes);

        if(header.getMsgType() != expectedType) {
            return null; // unexpected message type
        }

        PlainMsgSAPKDP.Type type = PlainMsgSAPKDP.Type.fromOpcode(header.getMsgType());
        byte[] payload = new byte[header.getPayloadSize()];
        in.read(payload); //TODO: verify read bytes

        return payload;
    }





}
