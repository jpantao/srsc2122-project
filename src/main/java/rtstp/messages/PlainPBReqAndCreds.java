package rtstp.messages;

import common.Utils;

import java.io.*;

public class PlainPBReqAndCreds extends PlainMsgRTSTP {

    private final byte[] ticket;
    private final int na1;


    public PlainPBReqAndCreds(byte[] ticket, int na1) {
        super(Type.PB_REQ_N_AUTH);
        this.ticket = ticket;
        this.na1 = na1;
    }

    public byte[] getTicket() {
        return ticket;
    }

    public int getNa1() {
        return na1;
    }

    @Override
    public String toString() {
        return "PlainPBReqAndCreds{" +
                "ticket=" + Utils.encodeHexString(ticket) +
                ", na1=" + na1 +
                '}';
    }

    public static Serializer<PlainMsgRTSTP> serializer = new Serializer<PlainMsgRTSTP>() {
        @Override
        public byte[] serialize(PlainMsgRTSTP plainMsgRTSTP) {
            PlainPBReqAndCreds msg = (PlainPBReqAndCreds) plainMsgRTSTP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                Utils.writeByteArray(dao, msg.ticket);
                dao.writeInt(msg.na1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgRTSTP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            byte[] ticket = null;
            int na1 = 0;

            try {
                ticket = Utils.readByteArray(dai);
                na1 = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainPBReqAndCreds(ticket, na1);
        }
    };
}
