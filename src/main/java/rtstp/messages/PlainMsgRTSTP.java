package rtstp.messages;



public abstract class PlainMsgRTSTP {

    public enum Type {
        PB_REQ_N_AUTH(1, PlainPBReqAndCreds.serializer),
        RTSS_VER(2, PlainRTSSVerification.serializer),
        PB_VER_ACK(3, PlainPBAckVerification.serializer),
        RTSS_SYNC(4, PlainRTSSSyncInitFrame.serializer);

        public final int value;
        private final Serializer<PlainMsgRTSTP> serializer;

        private static final PlainMsgRTSTP.Type[] msgcodeIdx;

        static {
            int maxMsgcode = -1;
            for (PlainMsgRTSTP.Type type : PlainMsgRTSTP.Type.values())
                maxMsgcode = Math.max(maxMsgcode, type.value);
            msgcodeIdx = new PlainMsgRTSTP.Type[maxMsgcode + 1];
            for (PlainMsgRTSTP.Type type : PlainMsgRTSTP.Type.values()) {
                if (msgcodeIdx[type.value] != null)
                    throw new IllegalStateException("Duplicate msgcode");
                msgcodeIdx[type.value] = type;
            }
        }

        Type(int value, Serializer<PlainMsgRTSTP> serializer) {
            this.value = value;
            this.serializer = serializer;
        }

        public static PlainMsgRTSTP.Type fromOpcode(int msgcode) {
            if (msgcode >= msgcodeIdx.length || msgcode < 0)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            PlainMsgRTSTP.Type t = msgcodeIdx[msgcode];
            if (t == null)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            return t;
        }
    }

    private final PlainMsgRTSTP.Type type;

    public PlainMsgRTSTP(PlainMsgRTSTP.Type type) {
        this.type = type;
    }

    public PlainMsgRTSTP.Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlainMsgRTSTP)) return false;
        PlainMsgRTSTP msg = (PlainMsgRTSTP) o;
        return type == msg.type;
    }

    public static byte[] serialize(PlainMsgRTSTP msg) {
        return msg.type.serializer.serialize(msg);
    }

    public static PlainMsgRTSTP deserialize(int msgType, byte[] msg) {
        PlainMsgRTSTP.Type type = PlainMsgRTSTP.Type.fromOpcode(msgType);
        return type.serializer.deserialize(msg);
    }


}
