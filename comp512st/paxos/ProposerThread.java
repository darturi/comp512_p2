package comp512st.paxos;

import comp512.gcl.GCL;

import java.util.HashSet;

public class ProposerThread extends Thread {
    private boolean hasMajorityPromise;
    private boolean hasMajorityAcceptAck;
    private boolean shouldBail;
    private Integer proposeBID;
    private Object value;
    private GCL gcl;

    public ProposerThread(Integer bid, Object value, GCL gcl){
        this.hasMajorityPromise = false;
        this.hasMajorityAcceptAck = false;
        this.shouldBail = false;
        this.proposeBID = bid;
        this.value = value;
        this.gcl = gcl;
    }

    synchronized private Object safeValueAccess(){return this.value;}
    synchronized private Integer safeBIDAccess(){return this.proposeBID;}

    public void run(){
        // Create proposal message
        MessageObject proposalMessage;
        try {
            proposalMessage = new MessageObject(MessageType.PROPOSE, safeBIDAccess());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        // multicast proposal message
        gcl.broadcastMsg(proposalMessage);

        // Wait for majority of promises (value may change) TODO: ADD TIMEOUT HERE
        while (!this.hasMajorityPromise && !this.shouldBail) {
            try {
                wait(); // COULD ADD A TIMER TO THIS
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // TODO: ADD LOGIC TO CATCH BAIL SIGNAL

        // Create accept_q message
        MessageObject accept_qMessage;
        try {
            accept_qMessage = new MessageObject(MessageType.ACCEPT_Q, safeBIDAccess(), safeValueAccess());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        // Multicast accept_q message
        gcl.broadcastMsg(accept_qMessage);

        // Wait for majority of AcceptAck's
        while (!this.hasMajorityAcceptAck && !this.shouldBail) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // TODO: ADD LOGIC TO CATCH BAIL SIGNAL

        // Create Confirm Message
        MessageObject confirmMessage;
        try {
            confirmMessage = new MessageObject(MessageType.CONFIRM, safeBIDAccess());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        // Broadcast confirm message
        gcl.broadcastMsg(confirmMessage);
    }
}
