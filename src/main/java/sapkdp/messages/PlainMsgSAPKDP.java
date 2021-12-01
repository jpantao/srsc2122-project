package sapkdp.messages;


public abstract class PlainMsgSAPKDP {

    public enum Type {
        PB_HELLO(1, PlainPBHello.serializer),
        SS_AUTHREQ(2, PlainSSAuthReq.serializer),
        PB_AUTH(3, PlainPBAuth.serializer),
        SS_PAYREQ(4, PlainSSPaymentReq.serializer),
        PB_PAYMENT(5, PlainPBPayment.serializer),
        PB_TKCREDS(6, PlainTicketCreds.serializer);


        public final int value;
        private final Serializer<PlainMsgSAPKDP> serializer;

        private static final Type[] msgcodeIdx;

        static {
            int maxOpcode = -1;
            for (Type type : Type.values())
                maxOpcode = Math.max(maxOpcode, type.value);
            msgcodeIdx = new Type[maxOpcode + 1];
            for (Type type : Type.values()) {
                if (msgcodeIdx[type.value] != null)
                    throw new IllegalStateException("Duplicate opcode");
                msgcodeIdx[type.value] = type;
            }
        }

        Type(int value, Serializer<PlainMsgSAPKDP> serializer) {
            this.value = value;
            this.serializer = serializer;
        }

        public static Type fromOpcode(int msgcode) {
            if (msgcode >= msgcodeIdx.length || msgcode < 0)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            Type t = msgcodeIdx[msgcode];
            if (t == null)
                throw new AssertionError(String.format("Unknown msgcode %d", msgcode));
            return t;
        }
    }

    private final Type type;

    public PlainMsgSAPKDP(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlainMsgSAPKDP)) return false;
        PlainMsgSAPKDP msg = (PlainMsgSAPKDP) o;
        return type == msg.type;
    }


    public static byte[] serialize(PlainMsgSAPKDP msg) {
        return msg.type.serializer.serialize(msg);
    }

    public static PlainMsgSAPKDP deserialize(int msgType, byte[] msg) {
        Type type = Type.fromOpcode(msgType);
        return type.serializer.deserialize(msg);
    }


}
