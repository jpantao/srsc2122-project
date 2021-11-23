package sigserver.sapkdp.messages;

import java.io.*;

import static sigserver.sapkdp.messages.Utils.*;

public class SSTicketCredentials extends MessageSAPKDP {

    private final String ip;
    private final int port;
    private final String movieID;
    private final String ciphersuiteConf;
    private final String cryptoSA;
    private final String sessionKey;
    private final String macKey;
    private final int nonce;

    public SSTicketCredentials(String ip, int port, String movieID, String ciphersuiteConf, String cryptoSA, String sessionKey, String macKey, int nonce) {
        super(Type.PB_TKCREDS);
        this.ip = ip;
        this.port = port;
        this.movieID = movieID;
        this.ciphersuiteConf = ciphersuiteConf;
        this.cryptoSA = cryptoSA;
        this.sessionKey = sessionKey;
        this.macKey = macKey;
        this.nonce = nonce;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getMovieID() {
        return movieID;
    }

    public String getCiphersuiteConf() {
        return ciphersuiteConf;
    }

    public String getCryptoSA() {
        return cryptoSA;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getMacKey() {
        return macKey;
    }

    public int getNonce() {
        return nonce;
    }

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {
            SSTicketCredentials msg = (SSTicketCredentials) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.ip.length());
                dao.writeChars(msg.ip);
                dao.writeInt(msg.port);
                dao.writeInt(msg.movieID.length());
                dao.writeChars(msg.movieID);
                dao.writeInt(msg.ciphersuiteConf.length());
                dao.writeChars(msg.ciphersuiteConf);
                dao.writeInt(msg.cryptoSA.length());
                dao.writeChars(msg.cryptoSA);
                dao.writeInt(msg.sessionKey.length());
                dao.writeChars(msg.sessionKey);
                dao.writeInt(msg.macKey.length());
                dao.writeChars(msg.macKey);
                dao.writeInt(msg.nonce);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public MessageSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            String ip = "";
            int port = 0;
            String movieID = "";
            String ciphersuiteConf = "";
            String cryptoSA = "";
            String sessionKey = "";
            String macKey = "";
            int nonce = 0;

            try {
                ip = getString(dai);
                port = dai.readInt();
                movieID = getString(dai);
                ciphersuiteConf = getString(dai);
                cryptoSA = getString(dai);
                sessionKey = getString(dai);
                macKey = getString(dai);
                nonce = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return new SSTicketCredentials(ip, port, movieID, ciphersuiteConf, cryptoSA, sessionKey, macKey, nonce);
        }


    };

}
