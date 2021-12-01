package srtsp.messages;

import java.io.*;

public class PlainRTSSSyncInitFrame extends PlainMsgSRTSP {

    private final int na3Prime;
    private final int initMarkFrame;

    public PlainRTSSSyncInitFrame(int na3Prime, int initMarkFrame) {
        super(Type.RTSS_SYNC);
        this.na3Prime = na3Prime;
        this.initMarkFrame = initMarkFrame;
    }

    public int getNa3Prime() {
        return na3Prime;
    }

    public int getInitMarkFrame() {
        return initMarkFrame;
    }

    @Override
    public String toString() {
        return "PlainRTSSSyncInitFrame{" +
                "na3Prime=" + na3Prime +
                ", initMarkFrame=" + initMarkFrame +
                '}';
    }

    public static Serializer<PlainMsgSRTSP> serializer = new Serializer<PlainMsgSRTSP>() {
        @Override
        public byte[] serialize(PlainMsgSRTSP plainMsgSRTSP) {
            PlainRTSSSyncInitFrame msg = (PlainRTSSSyncInitFrame) plainMsgSRTSP;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DataOutputStream dao = new DataOutputStream(bao);

            try {
                dao.writeInt(msg.na3Prime);
                dao.writeInt(msg.initMarkFrame);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bao.toByteArray();
        }

        @Override
        public PlainMsgSRTSP deserialize(byte[] buf) {

            ByteArrayInputStream bai = new ByteArrayInputStream(buf);
            DataInputStream dai = new DataInputStream(bai);

            int na3Prime = 0;
            int initMarkFrame = 0;

            try {
                na3Prime = dai.readInt();
                initMarkFrame = dai.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new PlainRTSSSyncInitFrame(na3Prime, initMarkFrame);
        }

    };
}
