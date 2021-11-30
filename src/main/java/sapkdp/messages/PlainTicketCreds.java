package sapkdp.messages;


import common.Utils;

import java.io.*;
import java.util.Arrays;


public class PlainTicketCreds extends PlainMsgSAPKDP {

    private final String ip;
    private final int port;
    private final String ciphersuiteConf;
    private final String cryptoSA;
    private final byte[] sessionkeyBytes;
    private final byte[] mackeyBytes;
    private final int nonce;

    public PlainTicketCreds(String ip, int port, String ciphersuiteConf, String cryptoSA, byte[] sessionkeyBytes, byte[] mackeyBytes, int nonce) {
        super(Type.PB_TKCREDS);
        this.ip = ip;
        this.port = port;
        this.ciphersuiteConf = ciphersuiteConf;
        this.cryptoSA = cryptoSA;
        this.sessionkeyBytes = sessionkeyBytes;
        this.mackeyBytes = mackeyBytes;
        this.nonce = nonce;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getCiphersuiteConf() {
        return ciphersuiteConf;
    }

    public String getCryptoSA() {
        return cryptoSA;
    }

    public byte[] getSessionkeyBytes() {
        return sessionkeyBytes;
    }

    public byte[] getMackeyBytes() {
        return mackeyBytes;
    }

    public int getNonce() {
        return nonce;
    }


    @Override
    public String toString() {
        return "PlainTicketCreds{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", ciphersuiteConf='" + ciphersuiteConf + '\'' +
                ", cryptoSA='" + cryptoSA + '\'' +
                ", sessionkeyBytes=" + Utils.encodeHexString(sessionkeyBytes) +
                ", mackeyBytes=" + Utils.encodeHexString(mackeyBytes) +
                ", nonce=" + nonce +
                '}';
    }

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {
            PlainTicketCreds msg = (PlainTicketCreds) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                Utils.writeString(dao, msg.ip);
                dao.writeInt(msg.port);
                Utils.writeString(dao, msg.ciphersuiteConf);
                Utils.writeString(dao, msg.cryptoSA);
                Utils.writeByteArray(dao, msg.sessionkeyBytes);
                Utils.writeByteArray(dao, msg.mackeyBytes);
                dao.writeInt(msg.nonce);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            String ip = "";
            int port = 0;
            String ciphersuiteConf = "";
            String cryptoSA = "";
            byte[] sessionkeyBytes = null;
            byte[] mackeyBytes = null;
            int nonce = 0;

            try {
                ip = Utils.readSting(dai);
                port = dai.readInt();
                ciphersuiteConf = Utils.readSting(dai);
                cryptoSA = Utils.readSting(dai);
                sessionkeyBytes = Utils.readByteArray(dai);
                mackeyBytes = Utils.readByteArray(dai);
                nonce = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainTicketCreds(ip, port, ciphersuiteConf, cryptoSA, sessionkeyBytes, mackeyBytes, nonce);
        }


    };
}
