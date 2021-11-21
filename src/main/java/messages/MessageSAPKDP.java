package messages;

import java.io.Serializable;

public abstract class MessageSAPKDP implements Serializable {

    private int msgType;

    public MessageSAPKDP(int msgType) {
        this.msgType = msgType;
    }

    public int getMsgType() {
        return msgType;
    }

}
