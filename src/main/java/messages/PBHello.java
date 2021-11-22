package messages;

public class PBHello extends MessageSAPKDP{

    public static final int MSG_TYPE = 1;

    private final String userID;
    private final String proxyID;

    public PBHello(String userID, String proxyID) {
        super(MSG_TYPE);
        this.userID = userID;
        this.proxyID = proxyID;
    }

    public String getUserID() {
        return userID;
    }

    public String getProxyID() {
        return proxyID;
    }
}
