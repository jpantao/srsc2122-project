package sigserver.sapkdp.messages;

import java.io.*;

import static sigserver.sapkdp.messages.Utils.getString;

public class PBHello extends MessageSAPKDP {


    private final String userID;
    private final String proxyBoxID;

    public PBHello(String userID, String proxyBoxID) {
        super(Type.PB_HELLO);
        this.userID = userID;
        this.proxyBoxID = proxyBoxID;
    }

    public String getUserID() {
        return userID;
    }

    public String getProxyID() {
        return proxyBoxID;
    }


    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {

            PBHello msg = (PBHello) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);


            try {
                dao.writeInt(msg.userID.length());
                dao.writeChars(msg.userID);
                dao.writeInt(msg.proxyBoxID.length());
                dao.writeChars(msg.proxyBoxID);
            } catch (IOException e) {
                e.printStackTrace();
            }


            return bao.toByteArray();
        }

        @Override
        public MessageSAPKDP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            String userID = "";
            String proxyBoxID = "";

            try {
                userID = getString(dai);
                proxyBoxID = getString(dai);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PBHello(userID, proxyBoxID);
        }
    };
}
