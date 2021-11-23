package sigserver.sapkdp.messages;

import java.io.*;

public class SSAuthenticationRequest extends MessageSAPKDP {

    private final int nonce;
    private final byte[] salt;
    private final int counter;


    public SSAuthenticationRequest(int nonce, byte[] salt, int counter) {
        super(Type.SS_AUTHREQ);
        this.nonce = nonce;
        this.salt = salt;
        this.counter = counter;
    }


    public int getNonce() {
        return nonce;
    }

    public byte[] getSalt() {
        return salt;
    }

    public int getCounter() {
        return counter;
    }

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {
            SSAuthenticationRequest msg = (SSAuthenticationRequest) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.nonce);
                dao.writeInt(msg.salt.length);
                dao.write(msg.salt);
                dao.writeInt(msg.counter);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public MessageSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int nonce = 0;
            int counter = 0;
            byte[] salt = null;

            try {

                nonce = dai.readInt();

                int saltSize = dai.readInt();
                salt = new byte[saltSize];
                int read = dai.read(salt);
                if (read != saltSize)
                    throw new IOException("read " + read + " should have been " + saltSize);

                counter = dai.readInt();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return new SSAuthenticationRequest(nonce, salt, counter);
        }
    };

}
