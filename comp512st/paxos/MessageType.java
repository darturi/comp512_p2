package comp512st.paxos;

public enum MessageType {
    PROPOSE,
    PROMISE,
    ACCEPT_Q,
    ACCEPT_ACK,
    CONFIRM,
    REFUSE,
    DENY
}
