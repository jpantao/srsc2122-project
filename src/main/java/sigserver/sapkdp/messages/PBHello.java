package sigserver.sapkdp.messages;

import java.io.*;
import java.nio.charset.StandardCharsets;


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

    @Override
    public String toString() {
        return "PBHello{" +
                "userID='" + userID + '\'' +
                ", proxyBoxID='" + proxyBoxID + '\'' +
                '}';
    }

    public static Serializer<MessageSAPKDP> serializer = new Serializer<MessageSAPKDP>() {
        @Override
        public byte[] serialize(MessageSAPKDP messageSAPKDP) {

            PBHello msg = (PBHello) messageSAPKDP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);


            try {
                dao.writeInt(msg.userID.getBytes().length);
                dao.write(msg.userID.getBytes());
                dao.writeInt(msg.proxyBoxID.getBytes().length);
                dao.write(msg.proxyBoxID.getBytes());
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
