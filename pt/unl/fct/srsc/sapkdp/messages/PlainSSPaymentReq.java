package pt.unl.fct.srsc.sapkdp.messages;


import java.io.*;

public class PlainSSPaymentReq extends PlainMsgSAPKDP {

    private final float price;
    private final int n2Prime;
    private final int n3;


    public PlainSSPaymentReq(float price, int n2Prime, int n3) {
        super(Type.SS_PAYREQ);
        this.price = price;
        this.n2Prime = n2Prime;
        this.n3 = n3;
    }

    public float getPrice() {
        return price;
    }

    public int getN2Prime() {
        return n2Prime;
    }

    public int getN3() {
        return n3;
    }

    @Override
    public String toString() {
        return "SSPaymentRequest{" +
                "price=" + price +
                ", n2Prime=" + n2Prime +
                ", n3=" + n3 +
                '}';
    }

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {
            PlainSSPaymentReq msg = (PlainSSPaymentReq) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeFloat(msg.price);
                dao.writeInt(msg.n2Prime);
                dao.writeInt(msg.n3);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            float price = 0;
            int nonce1 = 0;
            int nonce2 = 0;

            try {
                price = dai.readFloat();
                nonce1 = dai.readInt();
                nonce2 = dai.readInt();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainSSPaymentReq(price, nonce1, nonce2);
        }
    };
}
