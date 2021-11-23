package sigserver.sapkdp.messages;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PBAuthentication extends MessageSAPKDP {

    private final int nonce1;
    private final int nonce2;
    private final String movieID;


    public PBAuthentication(int nonce1, int nonce2, String movieID) {
        super(Type.PB_AUTH);
        this.nonce1 = nonce1;
        this.nonce2 = nonce2;
        this.movieID = movieID;
    }

    public int getNonce1() {
        return nonce1;
    }

    public int getNonce2() {
        return nonce2;
    }

    public String getMovieID() {
        return movieID;
    }

    @Override
    public String toString() {
        return "PBAuthentication{" +
                "nonce1=" + nonce1 +
                ", nonce2=" + nonce2 +
                ", movieID='" + movieID + '\'' +
                '}';
    }

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {
            PBAuthentication msg = (PBAuthentication) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.nonce1);
                dao.writeInt(msg.nonce2);
                dao.writeInt(msg.movieID.getBytes().length);
                dao.write(msg.movieID.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public MessageSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int nonce1 = 0;
            int nonce2 = 0;
            String movieID = "";

            try {
                nonce1 = dai.readInt();
                nonce2 = dai.readInt();
                movieID = getString(dai);
                System.out.println(movieID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PBAuthentication(nonce1, nonce2, movieID);
        }
    };
}
