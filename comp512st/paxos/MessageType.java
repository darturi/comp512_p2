package comp512st.paxos;

public enum MessageType {
    PROPOSE,
    PROMISE,
    ACCEPT,
    ACCEPTACK,
    CONFIRM,
    REFUSE,
    DENY
}