package srtsp.messages;

import java.io.*;

public class PlainRTSSVerification extends PlainMsgSRTSP {

    private final int na1Prime;
    private final int na2;
    private final boolean ticketValidity;

    public PlainRTSSVerification(int na1Prime, int na2, boolean ticketValidityConfirmation) {
        super(Type.RTSS_VER);
        this.na1Prime = na1Prime;
        this.na2 = na2;
        this.ticketValidity = ticketValidityConfirmation;
    }

    public int getNa1Prime() {
        return na1Prime;
    }

    public int getNa2() {
        return na2;
    }

    public boolean isTicketValidity() {
        return ticketValidity;
    }

    @Override
    public String toString() {
        return "PlainRTSSVerification{" +
                "na1Prime=" + na1Prime +
                ", na2=" + na2 +
                ", ticketValidityConfirmation=" + ticketValidity +
                '}';
    }

    public static Serializer<PlainMsgSRTSP> serializer = new Serializer<PlainMsgSRTSP>() {
        @Override
        public byte[] serialize(PlainMsgSRTSP plainMsgSRTSP) {
            PlainRTSSVerification msg = (PlainRTSSVerification) plainMsgSRTSP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.na1Prime);
                dao.writeInt(msg.na2);
                dao.writeBoolean(msg.ticketValidity);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSRTSP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int na1Prime = 0;
            int na2 = 0;
            boolean ticketValidity = false;

            try {
                na1Prime = dai.readInt();
                na2 = dai.readInt();
                ticketValidity = dai.readBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainRTSSVerification(na1Prime, na2, ticketValidity);
        }
    };


}
