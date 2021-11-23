package sigserver.sapkdp.messages;

import java.io.*;

public class PBPayment extends MessageSAPKDP{

    private final int nonce1;
    private final int nonce2;
    //TODO: fix coin
    private final int paymentCoin;

    public PBPayment(int nonce1, int nonce2, int paymentCoin) {
        super(Type.PB_PAYMENT);
        this.nonce1 = nonce1;
        this.nonce2 = nonce2;
        this.paymentCoin = paymentCoin;
    }

    public int getNonce1() {
        return nonce1;
    }

    public int getNonce2() {
        return nonce2;
    }

    public int getPaymentCoin() {
        return paymentCoin;
    }

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {
            PBPayment msg = (PBPayment) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.nonce1);
                dao.writeInt(msg.nonce2);
                dao.writeInt(msg.paymentCoin);
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
            int paymentCoin = 0;

            try {
                nonce1 = dai.readInt();
                nonce2 = dai.readInt();
                paymentCoin = dai.readInt();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PBPayment(nonce1, nonce2, paymentCoin);
        }
    };
}
