package sapkdp.messages;


import java.io.DataInputStream;
import java.io.IOException;

public abstract class PlainMsgSAPKDP {

    public enum Type {
        PB_HELLO(1, PlainPBHello.serializer),
        SS_AUTHREQ(2, PlainSSAuthReq.serializer),
        PB_AUTH(3, PlainPBAuth.serializer),
        SS_PAYREQ(4, PlainSSPaymentReq.serializer),
        PB_PAYMENT(5, PlainPBPayment.serializer),
        PB_TKCREDS(6, PlainTicketCreds.serializer);


        public final int msgType;
        private final Serializer<PlainMsgSAPKDP> serializer;

        private static final Type[] opcodeIdx;

        static {
            int maxOpcode = -1;
            for (Type type : Type.values())
                maxOpcode = Math.max(maxOpcode, type.msgType);
            opcodeIdx = new Type[maxOpcode + 1];
            for (Type type : Type.values()) {
                if (opcodeIdx[type.msgType] != null)
                    throw new IllegalStateException("Duplicate opcode");
                opcodeIdx[type.msgType] = type;
            }
        }

        Type(int msgType, Serializer<PlainMsgSAPKDP> serializer) {
            this.msgType = msgType;
            this.serializer = serializer;
        }

        public static Type fromOpcode(int opcode) {
            if (opcode >= opcodeIdx.length || opcode < 0)
                throw new AssertionError(String.format("Unknown opcode %d", opcode));
            Type t = opcodeIdx[opcode];
            if (t == null)
                throw new AssertionError(String.format("Unknown opcode %d", opcode));
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
        PlainMsgSAPKDP operation = (PlainMsgSAPKDP) o;
        return type == operation.type;
    }


    public static byte[] serialize(PlainMsgSAPKDP msg) {
        return msg.type.serializer.serialize(msg);
    }

    public static PlainMsgSAPKDP deserialize(int msgType, byte[] msg) {
        Type type = Type.fromOpcode(msgType);
        return type.serializer.deserialize(msg);
    }


}
