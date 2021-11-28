package sapkdp.messages;


import java.io.*;

public class PlainSSPaymentReq extends PlainMsgSAPKDP {

    private final float price;
    private final int nonce1;
    private final int nonce2;


    public PlainSSPaymentReq(float price, int nonce1, int nonce2) {
        super(Type.SS_PAYREQ);
        this.price = price;
        this.nonce1 = nonce1;
        this.nonce2 = nonce2;
    }

    public float getPrice() {
        return price;
    }

    public int getNonce1() {
        return nonce1;
    }

    public int getNonce2() {
        return nonce2;
    }

    @Override
    public String toString() {
        return "SSPaymentRequest{" +
                "price=" + price +
                ", nonce1=" + nonce1 +
                ", nonce2=" + nonce2 +
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
                dao.writeInt(msg.nonce1);
                dao.writeInt(msg.nonce2);
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
