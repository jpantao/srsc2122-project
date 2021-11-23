package sigserver.sapkdp.messages;

import java.io.*;
import java.lang.reflect.Type;

public class SSPaymentRequest extends MessageSAPKDP {

    private final float price;
    private final int nonce1;
    private final int nonce2;


    public SSPaymentRequest(float price, int nonce1, int nonce2) {
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

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {
            SSPaymentRequest msg = (SSPaymentRequest) messageSAPKDP;

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
        public MessageSAPKDP deserialize(byte[] buf) {

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

            return new SSPaymentRequest(price, nonce1, nonce2);
        }
    };
}
