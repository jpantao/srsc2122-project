package sapkdp.messages;


import common.Utils;

import java.io.*;
import java.util.Arrays;

public class PlainSSAuthReq extends PlainMsgSAPKDP {

    private final int n1;
    private final byte[] salt;
    private final int counter;

    public PlainSSAuthReq(int n1, byte[] salt, int counter) {
        super(Type.SS_AUTHREQ);
        this.n1 = n1;
        this.salt = salt;
        this.counter = counter;
    }


    public int getN1() {
        return n1;
    }

    public byte[] getSalt() {
        return salt;
    }

    public int getCounter() {
        return counter;
    }

    @Override
    public String toString() {
        return "SSAuthenticationRequest{" +
                "n1=" + n1 +
                ", salt=" + Arrays.toString(salt) +
                ", counter=" + counter +
                '}';
    }

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {
            PlainSSAuthReq msg = (PlainSSAuthReq) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.n1);
                Utils.writeByteArray(dao, msg.salt);
                dao.writeInt(msg.counter);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int nonce = 0;
            int counter = 0;
            byte[] salt = null;

            try {
                nonce = dai.readInt();
                salt = Utils.readByteArray(dai);
                counter = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainSSAuthReq(nonce, salt, counter);
        }
    };

}
