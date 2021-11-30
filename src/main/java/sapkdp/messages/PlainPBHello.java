package sapkdp.messages;


import common.Utils;

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
                Utils.writeString(dao, msg.userID);
                Utils.writeString(dao, msg.proxyBoxID);
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
                userID = Utils.readSting(dai);
                proxyBoxID = Utils.readSting(dai);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainPBHello(userID, proxyBoxID);
        }
    };
}
