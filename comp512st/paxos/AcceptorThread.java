package comp512st.paxos;

import comp512.gcl.GCL;
import comp512.gcl.GCMessage;
import comp512.utils.FailCheck;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AcceptorThread implements Runnable{
    // General Class Variables
    private GCL gcl;
    private AtomicInteger BID;
    private AtomicInteger BID2;
    private Object[] value;
    private HashSet<String> majorityPromiseHashSet;
    private HashSet<String> majorityAcceptAckHashSet;
    private int sizeOfGroup;
    private String playerIdNum;

    // Acceptor Specific Variables
    private Queue<MessageObject> msgQueue;
    private Integer MaxBid;
    private Object[] valuePrime;
    private int AcceptBid;

    public AcceptorThread(GCL gcl, AtomicInteger currProposerBID, AtomicInteger BID2, HashSet<String> majorityPromiseHashSet, HashSet<String> majorityAcceptAckHashSet, int sizeOfGroup, String playerIdNum){
        // Initialize class variables for general use
        this.gcl = gcl;
        this.BID = currProposerBID; // SHARED (proposer can access to update with initialization)
        this.BID2 = BID2; // SHARED (main thread must reset this upon initialization (upon TOMulticast))
        this.majorityPromiseHashSet = majorityPromiseHashSet; // SHARED (main thread can access to clear (upon TOMulticast invocation))
        this.majorityAcceptAckHashSet = majorityAcceptAckHashSet; // SHARED (main thread can access to clear (upon TOMulticast invocation))
        this.sizeOfGroup = sizeOfGroup;
        this.playerIdNum = playerIdNum;

        // Initialize acceptor specific variables
        this.msgQueue = new LinkedList<>();
        this.MaxBid = -1;
    }

    @Override
    public void run() {
        while(true){
            // Collect a message
            GCMessage gcmsg;
            try {
                gcmsg = gcl.readGCMessage();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // parse out the actual message type and sending process
            MessageObject parsedMessage = (MessageObject) gcmsg.val;
            String sendingProcess = gcmsg.senderProcess;

            // Determine What to do based on message type
            MessageType msgType = parsedMessage.getMsgType();
            if (msgType == MessageType.PROMISE) {
                int msgBID = parsedMessage.getBID();
                if (msgBID != this.BID.get()) continue; // means it's from a previous proposal that was already ruled on
                else {
                    // TODO: If there are two bids must act accordingly
                    // TODO: NOT SETTING NEW BID2
                    if (parsedMessage.isSecondBID() && parsedMessage.getBID2() > this.BID2.get())
                        synchronized (this.value) {this.value = parsedMessage.getValue();}

                    // add promise process to promise hash
                    synchronized (this.majorityPromiseHashSet) {this.majorityPromiseHashSet.add(sendingProcess);}

                    // TODO: If majority reached notify proposer
                    if (this.majorityPromiseHashSet.size() > this.sizeOfGroup / 2) notify();
                }
            }
            else if (msgType == MessageType.ACCEPT_ACK) {
                int msgBID = parsedMessage.getBID();
                if (msgBID != this.BID.get()) continue; // means it's from a previous proposal that was already ruled on
                else {
                    // add promise process to acceptAck hash
                    synchronized (this.majorityAcceptAckHashSet) {this.majorityAcceptAckHashSet.add(sendingProcess);}

                    // TODO: If majority reached notify proposer
                    if (this.majorityAcceptAckHashSet.size() > this.sizeOfGroup / 2) notify();
                }
            }
            else if (msgType == MessageType.PROPOSE) {
                int msgBID = parsedMessage.getBID();
                if (msgBID < this.MaxBid) // TODO: Send refuse
                    continue;
                else { // msgBid is higher than any seen so far
                    if (this.value == null){ // If value == null
                        try { // send promise with just the ballot id
                            this.gcl.sendMsg(new MessageObject(MessageType.PROMISE, msgBID), sendingProcess);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        }
                    } else { // if value is not null send the double ballot id kind with the old value
                        try {
                            this.gcl.sendMsg(new MessageObject(MessageType.PROMISE, msgBID, this.AcceptBid, this.value), sendingProcess);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // In either case set new MaxBid to the received bid
                    this.MaxBid = msgBID;
                }
            }
            else if (msgType == MessageType.ACCEPT_Q) {
                int msgBID = parsedMessage.getBID();
                if (msgBID == this.MaxBid){
                    this.value = parsedMessage.getValue(); // override previously accepted value
                    this.AcceptBid = msgBID; // set new accepted bid
                    // Send AcceptAck to proposer
                    try { // send promise with just the ballot id
                        this.gcl.sendMsg(new MessageObject(MessageType.ACCEPT_ACK, msgBID), sendingProcess);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                } else { // has accepted higher ballotID since then
                    // TODO: send deny ballotID to proposer
                    continue;
                }
            }
            else if (msgType == MessageType.CONFIRM) continue;
            else throw new IllegalArgumentException("invalid message type"); // TODO: NOT ACCOUNTING FOR DENY AND REFUSE
        }
    }
}
