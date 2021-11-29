package sapkdp.messages;


import common.Utils;

import java.io.*;

public class PlainPBPayment extends PlainMsgSAPKDP {

    private final int n3Prime;
    private final int n4;
    private final byte[] paymentCoin;

    public PlainPBPayment(int nonce1, int nonce2, byte[] paymentCoin) {
        super(Type.PB_PAYMENT);
        this.n3Prime = nonce1;
        this.n4 = nonce2;
        this.paymentCoin = paymentCoin;
    }

    public int getN3Prime() {
        return n3Prime;
    }

    public int getN4() {
        return n4;
    }

    public byte[] getPaymentCoin() {
        return paymentCoin;
    }

    @Override
    public String toString() {
        return "PBPayment{" +
                "nonce1=" + n3Prime +
                ", nonce2=" + n4 +
                ", paymentCoin=" + Utils.encodeHexString(paymentCoin) +
                '}';
    }

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {
            PlainPBPayment msg = (PlainPBPayment) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.n3Prime);
                dao.writeInt(msg.n4);
                dao.writeInt(msg.paymentCoin.length);
                dao.write(msg.paymentCoin);
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
            byte[] paymentCoin = null;

            try {
                nonce1 = dai.readInt();
                nonce2 = dai.readInt();
                int coinSize = dai.readInt();
                paymentCoin = new byte[coinSize];
                dai.read(paymentCoin);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainPBPayment(nonce1, nonce2, paymentCoin);
        }
    };
}
