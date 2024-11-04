package comp512st.paxos;

import java.io.Serializable;

public class Message implements Serializable {
    MessageType type;

    int ballotID1;

    int ballotID2;

    Object value;

    public Message(int ballotID, MessageType type, Object value) {
        this.ballotID1 = ballotID;
        this.ballotID2 = -1;
        this.type = type;
        this.value = value;
    }
    public Message(int ballotID, int ballotID_2, MessageType type, Object value) {
        this.ballotID1 = ballotID;
        this.ballotID2 = ballotID_2;
        this.type = type;
        this.value = value;
    }
    public Message(int ballotID, MessageType type) {
        this.ballotID1 = ballotID;
        this.ballotID2 = -1;
        this.type = type;
        this.value = null;
    }

}
