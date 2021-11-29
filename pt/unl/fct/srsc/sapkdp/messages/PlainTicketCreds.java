package pt.unl.fct.srsc.sapkdp.messages;


import java.io.*;


public class PlainTicketCreds extends PlainMsgSAPKDP {

    private final String ip;
    private final int port;
    private final String movieID;
    private final String ciphersuiteConf;
    private final String cryptoSA;
    private final String sessionKey;
    private final String macsuite;
    private final String macKey;
    private final String iv;
    private final int nonce;

    public PlainTicketCreds(String ip, int port, String movieID, String ciphersuiteConf, String cryptoSA, String sessionKey, String macsuite, String macKey, String iv, int nonce) {
        super(Type.PB_TKCREDS);
        this.ip = ip;
        this.port = port;
        this.movieID = movieID;
        this.ciphersuiteConf = ciphersuiteConf;
        this.cryptoSA = cryptoSA;
        this.sessionKey = sessionKey;
        this.macsuite = macsuite;
        this.macKey = macKey;
        this.iv = iv;
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

    @Override
    public String toString() {
        return "SSTicketCredentials{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", movieID='" + movieID + '\'' +
                ", ciphersuiteConf='" + ciphersuiteConf + '\'' +
                ", cryptoSA='" + cryptoSA + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", macKey='" + macKey + '\'' +
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
                dao.writeInt(msg.ip.getBytes().length);
                dao.write(msg.ip.getBytes());
                dao.writeInt(msg.port);
                dao.writeInt(msg.movieID.getBytes().length);
                dao.write(msg.movieID.getBytes());
                dao.writeInt(msg.ciphersuiteConf.getBytes().length);
                dao.write(msg.ciphersuiteConf.getBytes());
                dao.writeInt(msg.cryptoSA.getBytes().length);
                dao.write(msg.cryptoSA.getBytes());
                dao.writeInt(msg.sessionKey.getBytes().length);
                dao.write(msg.sessionKey.getBytes());
                dao.writeInt(msg.macKey.getBytes().length);
                dao.write(msg.macKey.getBytes());
                dao.writeInt(msg.nonce);
                dao.writeInt(msg.iv.getBytes().length);
                dao.write(msg.iv.getBytes());
                dao.writeInt(msg.macsuite.getBytes().length);
                dao.write(msg.macsuite.getBytes());
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
            String movieID = "";
            String ciphersuiteConf = "";
            String cryptoSA = "";
            String sessionKey = "";
            String macKey = "";
            int nonce = 0;
            String iv = "";
            String macsuite = "";

            try {
                ip = getString(dai);
                port = dai.readInt();
                movieID = getString(dai);
                ciphersuiteConf = getString(dai);
                cryptoSA = getString(dai);
                sessionKey = getString(dai);
                macKey = getString(dai);
                nonce = dai.readInt();
                iv = getString(dai);
                macsuite = getString(dai);
            } catch (IOException e) {
                e.printStackTrace();
            }


            return new PlainTicketCreds(ip, port, movieID, ciphersuiteConf, cryptoSA, sessionKey, macsuite, macKey, iv, nonce);
        }


    };

}
