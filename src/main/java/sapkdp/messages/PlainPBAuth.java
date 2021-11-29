package sapkdp.messages;


import java.io.*;

public class PlainPBAuth extends PlainMsgSAPKDP {

    private final int n1Prime;
    private final int n2;
    private final String movieID;


    public PlainPBAuth(int n1Prime, int n2, String movieID) {
        super(Type.PB_AUTH);
        this.n1Prime = n1Prime;
        this.n2 = n2;
        this.movieID = movieID;
    }

    public int getN1Prime() {
        return n1Prime;
    }

    public int getN2() {
        return n2;
    }

    public String getMovieID() {
        return movieID;
    }

    @Override
    public String toString() {
        return "PBAuthentication{" +
                "n1Prime=" + n1Prime +
                ", n2=" + n2 +
                ", movieID='" + movieID + '\'' +
                '}';
    }

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {
            PlainPBAuth msg = (PlainPBAuth) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.n1Prime);
                dao.writeInt(msg.n2);
                dao.writeInt(msg.movieID.getBytes().length);
                dao.write(msg.movieID.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSAPKDP deserialize(byte[] buf) {

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

            return new PlainPBAuth(nonce1, nonce2, movieID);
        }
    };
}
