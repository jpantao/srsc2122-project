package proxybox;

import common.SecureDatagramSocket;
import common.Utils;
import sapkdp.messages.PlainTicketCreds;
import srtsp.messages.*;

import javax.crypto.Mac;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class ProxyHandshake {

    private static final String CONFIG_FILE = "config/srtsp.properties";
    public static final String CRYPTOCONF_FILE = "config/config.properties";

    private static final int VERSION = 1;
    private static final SecureRandom random = new SecureRandom();
    private long expectedNonce;

    private final Properties properties;
    private final Mac mac;
    private DatagramSocket socket;
    private final int servicePort;
    private final InetAddress serverAddr;

    private final byte[] payloadUsedInSignature;

    private int initFrameMark;


    public ProxyHandshake(String serverAddr, int clientPort, int servicePort, byte[] payloadUsedInSignature) throws IOException {
        Utils.loadBC();
        this.properties = Utils.loadConfig(CONFIG_FILE);
        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        this.mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);

        this.socket = new DatagramSocket(null);
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(clientPort));

        this.serverAddr = InetAddress.getByName(serverAddr);
        this.servicePort = servicePort;
        this.payloadUsedInSignature = payloadUsedInSignature;
    }

    public int getInitFrameMark() {
        return initFrameMark;
    }

    public void start(PlainTicketCreds plainTicket, byte[] ticket, byte[] sigBytes) throws Exception {
        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);

        try {
            // (round 1)
            socket.send(round1Packet(ticket, sigBytes));

            // (round 2)
            Utils.writeCryptoConf(plainTicket, CRYPTOCONF_FILE);
            socket.close();
            System.out.println("listen port " + plainTicket.getPort());
            socket = Utils.secureDatagramSocketWithReusableAddress(plainTicket.getPort());
            socket.receive(inPacket);
            PlainRTSSVerification verification = processRound2(inPacket);

            // (round 3)
            socket.send(round3Packet(verification.getNa2(), inPacket.getAddress().getHostAddress(), inPacket.getPort()));

            // (round 4)
            socket.receive(inPacket);
            PlainRTSSSyncInitFrame syncInitFrame = processRound4(inPacket);

            initFrameMark = syncInitFrame.getInitMarkFrame();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }


    }

    private PlainRTSSVerification processRound2(DatagramPacket inPacket) throws Exception {
        ByteArrayInputStream bai = new ByteArrayInputStream(inPacket.getData());
        DataInputStream dai = new DataInputStream(bai);

        int msgType = PlainMsgSRTSP.Type.RTSS_VER.value;
        byte[] payload = Utils.readConsumingHeader(dai, msgType);
        PlainRTSSVerification verification = (PlainRTSSVerification) PlainMsgSRTSP.deserialize(msgType, payload);

        if (verification.getNa1Prime() != expectedNonce)
            throw new Exception("na1' != na1 + 1");
        if (!verification.isTicketValid())
            throw new Exception("Ticket is not valid");
        Utils.logReceived(verification);

        return verification;
    }


    private DatagramPacket round1Packet(byte[] ticket, byte[] sigBytes) throws IOException {
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int na1 = ThreadLocalRandom.current().nextInt();
        expectedNonce = na1 + 1;

        PlainPBReqAndCreds msg = new PlainPBReqAndCreds(ticket, na1);
        byte[] payload = PlainMsgSRTSP.serialize(msg);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), payload);
        Utils.writeByteArray(dos, payloadUsedInSignature);
        Utils.writeByteArray(dos, sigBytes);
        Utils.writeIntCheck(dos, mac, payload);
        Utils.logSent(msg);

        byte[] packet = baos.toByteArray();
        return new DatagramPacket(packet, packet.length, serverAddr, servicePort);
    }

    private DatagramPacket round3Packet(int na2, String ip, int port) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int n3 = ThreadLocalRandom.current().nextInt();
        expectedNonce = n3 + 1;
        PlainPBAckVerification msg = new PlainPBAckVerification(na2 + 1, n3);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), PlainMsgSRTSP.serialize(msg));

        byte[] packet = baos.toByteArray();
        Utils.logSent(msg);
        System.out.println(ip + port);
        return new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port);
    }

    private PlainRTSSSyncInitFrame processRound4(DatagramPacket inPacket) throws Exception {
        ByteArrayInputStream bai = new ByteArrayInputStream(inPacket.getData());
        DataInputStream dai = new DataInputStream(bai);

        int msgType = PlainMsgSRTSP.Type.RTSS_SYNC.value;
        byte[] payload = Utils.readConsumingHeader(dai, msgType);
        PlainRTSSSyncInitFrame sync = (PlainRTSSSyncInitFrame) PlainMsgSRTSP.deserialize(msgType, payload);

        if (sync.getNa3Prime() != expectedNonce)
            throw new Exception("na3' != na3 + 1");
        Utils.logReceived(sync);

        return sync;



    }


}
