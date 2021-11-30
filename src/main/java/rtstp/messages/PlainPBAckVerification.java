package rtstp.messages;

import java.io.*;

public class PlainPBAckVerification extends PlainMsgRTSTP {

    private final int n2Prime;
    private final int n3;


    public PlainPBAckVerification(int n2Prime, int n3) {
        super(Type.PB_VER_ACK);
        this.n2Prime = n2Prime;
        this.n3 = n3;
    }

    public int getN2Prime() {
        return n2Prime;
    }

    public int getN3() {
        return n3;
    }

    @Override
    public String toString() {
        return "PlainPBAckVerification{" +
                "n2Prime=" + n2Prime +
                ", n3=" + n3 +
                '}';
    }

    public static Serializer<PlainMsgRTSTP> serializer = new Serializer<PlainMsgRTSTP>() {
        @Override
        public byte[] serialize(PlainMsgRTSTP plainMsgRTSTP) {
            PlainPBAckVerification msg = (PlainPBAckVerification) plainMsgRTSTP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.n2Prime);
                dao.writeInt(msg.n3);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgRTSTP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int na2Prime = 0;
            int na3 = 0;

            try {
                na2Prime = dai.readInt();
                na3 = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainPBAckVerification(na2Prime, na3);
        }
    };

}

