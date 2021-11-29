package pt.unl.fct.srsc.sapkdp.messages;


import java.io.*;


public class PlainPBHello extends PlainMsgSAPKDP {


    private final String userID;
    private final String proxyBoxID;

    public PlainPBHello(String userID, String proxyBoxID) {
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

    public static Serializer<PlainMsgSAPKDP> serializer = new Serializer<PlainMsgSAPKDP>() {
        @Override
        public byte[] serialize(PlainMsgSAPKDP messageSAPKDP) {

            PlainPBHello msg = (PlainPBHello) messageSAPKDP;

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
        public PlainMsgSAPKDP deserialize(byte[] buf) {

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

            return new PlainPBHello(userID, proxyBoxID);
        }
    };
}
