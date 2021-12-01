package sapkdp.messages;


import common.Utils;

import java.io.*;
import java.util.Arrays;


public class PlainTicketCreds extends PlainMsgSAPKDP {

    private final String movieID;
    private final String ip;
    private final int clientPort;
    private final String ciphersuiteConf;
    private final String cryptoSA;
    private final byte[] sessionkeyBytes;
    private final String macsuite;
    private final byte[] mackeyBytes;
    private final int nonce;

    public PlainTicketCreds(String movieID, String ip, int clientPort, String ciphersuiteConf, String cryptoSA, byte[] sessionkeyBytes, String macsuite, byte[] mackeyBytes, int nonce) {
        super(Type.PB_TKCREDS);
        this.movieID = movieID;
        this.ip = ip;
        this.clientPort = clientPort;
        this.ciphersuiteConf = ciphersuiteConf;
        this.cryptoSA = cryptoSA;
        this.sessionkeyBytes = sessionkeyBytes;
        this.macsuite = macsuite;
        this.mackeyBytes = mackeyBytes;
        this.nonce = nonce;
    }

    public String getMovieID() {
        return movieID;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return clientPort;
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

    public String getMacsuite() {
        return macsuite;
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
                "movieID='" + movieID + '\'' +
                ", ip='" + ip + '\'' +
                ", clientPort=" + clientPort +
                ", ciphersuiteConf='" + ciphersuiteConf + '\'' +
                ", cryptoSA='" + cryptoSA + '\'' +
                ", sessionkeyBytes=" + Utils.encodeHexString(sessionkeyBytes) +
                ", macsuite='" + macsuite + '\'' +
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
                Utils.writeString(dao, msg.movieID);
                Utils.writeString(dao, msg.ip);
                dao.writeInt(msg.clientPort);
                Utils.writeString(dao, msg.ciphersuiteConf);
                Utils.writeString(dao, msg.cryptoSA);
                Utils.writeByteArray(dao, msg.sessionkeyBytes);
                Utils.writeString(dao, msg.macsuite);
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

            String movieID = "";
            String ip = "";
            int port = 0;
            String ciphersuiteConf = "";
            String cryptoSA = "";
            byte[] sessionkeyBytes = null;
            String macsuite = "";
            byte[] mackeyBytes = null;
            int nonce = 0;

            try {
                movieID = Utils.readSting(dai);
                ip = Utils.readSting(dai);
                port = dai.readInt();
                ciphersuiteConf = Utils.readSting(dai);
                cryptoSA = Utils.readSting(dai);
                sessionkeyBytes = Utils.readByteArray(dai);
                macsuite = Utils.readSting(dai);
                mackeyBytes = Utils.readByteArray(dai);
                nonce = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainTicketCreds(movieID, ip, port, ciphersuiteConf, cryptoSA, sessionkeyBytes, macsuite, mackeyBytes, nonce);
        }


    };
}
