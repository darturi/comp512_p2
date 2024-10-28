package comp512st.paxos;

public class MessageObject {
    private final MessageType thisMessageType;
    private final int BID;
    private Integer BID2;
    private Object value;

    // Constructor for proposals, promises, confirmations
    public MessageObject(MessageType newMessageType, int bid) throws InstantiationException {
        if (
                newMessageType != MessageType.PROPOSE &&
                newMessageType != MessageType.PROMISE &&
                newMessageType != MessageType.CONFIRM &&
                newMessageType != MessageType.ACCEPT_ACK &&
                newMessageType != MessageType.REFUSE &&
                newMessageType != MessageType.DENY
        ) throw new InstantiationException("Improper constructor given arguments provided");

        this.thisMessageType = newMessageType;
        this.BID = bid;
    }

    public MessageObject(MessageType newMessageType, int bid, int bid2, Object newValue) throws InstantiationException {
        if (newMessageType != MessageType.PROMISE)
            throw new InstantiationException("Improper constructor given arguments provided (only promise can use 2 BID's)");

        this.thisMessageType = newMessageType;
        this.BID = bid;
        this.BID2 = bid2;
        this.value = newValue;
    }

    public MessageObject(MessageType newMessageType, int bid, Object newValue) throws InstantiationException {
        if (newMessageType != MessageType.ACCEPT_Q)
            throw new InstantiationException("Improper constructor given arguments provided (not accept_q)");

        this.thisMessageType = newMessageType;
        this.BID = bid;
        this.value = newValue;
    }

    public MessageType getMsgType() {return this.thisMessageType; }
    public int getBID() {return this.BID; }
    public Object getValue() {return this.value; }
    public boolean isSecondBID() {return this.BID2 != null; }
    public int getBID2() {return this.BID2; }

}
