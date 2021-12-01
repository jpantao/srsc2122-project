package srtsp.messages;



public abstract class PlainMsgSRTSP {

    public enum Type {
        PB_REQ_N_AUTH(1, PlainPBReqAndCreds.serializer),
        RTSS_VER(2, PlainRTSSVerification.serializer),
        PB_VER_ACK(3, PlainPBAckVerification.serializer),
        RTSS_SYNC(4, PlainRTSSSyncInitFrame.serializer);

        public final int value;
        private final Serializer<PlainMsgSRTSP> serializer;

        private static final PlainMsgSRTSP.Type[] msgcodeIdx;

        static {
            int maxMsgcode = -1;
            for (PlainMsgSRTSP.Type type : PlainMsgSRTSP.Type.values())
                maxMsgcode = Math.max(maxMsgcode, type.value);
            msgcodeIdx = new PlainMsgSRTSP.Type[maxMsgcode + 1];
            for (PlainMsgSRTSP.Type type : PlainMsgSRTSP.Type.values()) {
                if (msgcodeIdx[type.value] != null)
                    throw new IllegalStateException("Duplicate msgcode");
                msgcodeIdx[type.value] = type;
            }
        }

        Type(int value, Serializer<PlainMsgSRTSP> serializer) {
            this.value = value;
            this.serializer = serializer;
        }

        public static PlainMsgSRTSP.Type fromOpcode(int msgcode) {
            if (msgcode >= msgcodeIdx.length || msgcode < 0)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            PlainMsgSRTSP.Type t = msgcodeIdx[msgcode];
            if (t == null)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            return t;
        }
    }

    private final PlainMsgSRTSP.Type type;

    public PlainMsgSRTSP(PlainMsgSRTSP.Type type) {
        this.type = type;
    }

    public PlainMsgSRTSP.Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlainMsgSRTSP)) return false;
        PlainMsgSRTSP msg = (PlainMsgSRTSP) o;
        return type == msg.type;
    }

    public static byte[] serialize(PlainMsgSRTSP msg) {
        return msg.type.serializer.serialize(msg);
    }

    public static PlainMsgSRTSP deserialize(int msgType, byte[] msg) {
        PlainMsgSRTSP.Type type = PlainMsgSRTSP.Type.fromOpcode(msgType);
        return type.serializer.deserialize(msg);
    }


}
