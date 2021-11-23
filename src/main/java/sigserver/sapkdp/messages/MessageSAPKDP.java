package sigserver.sapkdp.messages;


import java.io.ByteArrayOutputStream;

public abstract class MessageSAPKDP {

    public enum Type {
        PB_HELLO(1, PBHello.serializer),
        SS_AUTHREQ(2, SSAuthenticationRequest.serializer),
        PB_AUTH(3, PBAuthentication.serializer),
        SS_PAYREQ(4, SSPaymentRequest.serializer),
        PB_PAYMENT(5, PBPayment.serializer),
        PB_TKCREDS(6, SSTicketCredentials.serializer);


        public final int msgType;
        private final Serializer<MessageSAPKDP> serializer;

        private static final MessageSAPKDP.Type[] opcodeIdx;

        static {
            int maxOpcode = -1;
            for (MessageSAPKDP.Type type : MessageSAPKDP.Type.values())
                maxOpcode = Math.max(maxOpcode, type.msgType);
            opcodeIdx = new MessageSAPKDP.Type[maxOpcode + 1];
            for (MessageSAPKDP.Type type : MessageSAPKDP.Type.values()) {
                if (opcodeIdx[type.msgType] != null)
                    throw new IllegalStateException("Duplicate opcode");
                opcodeIdx[type.msgType] = type;
            }
        }

        Type(int msgType, Serializer<MessageSAPKDP> serializer) {
            this.msgType = msgType;
            this.serializer = serializer;
        }

        public static MessageSAPKDP.Type fromOpcode(int opcode) {
            if (opcode >= opcodeIdx.length || opcode < 0)
                throw new AssertionError(String.format("Unknown opcode %d", opcode));
            MessageSAPKDP.Type t = opcodeIdx[opcode];
            if (t == null)
                throw new AssertionError(String.format("Unknown opcode %d", opcode));
            return t;
        }
        }

    private final Type type;

    public MessageSAPKDP(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageSAPKDP)) return false;
        MessageSAPKDP operation = (MessageSAPKDP) o;
        return type == operation.type;
    }


    public static byte[] serialize(MessageSAPKDP msg) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        return msg.type.serializer.serialize(msg);
    }

    public static MessageSAPKDP deserialize(int msgType, byte[] op) {
        Type type = Type.fromOpcode(msgType);
        return type.serializer.deserialize(op);
    }


}
