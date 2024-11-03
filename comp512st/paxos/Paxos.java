package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import java.net.UnknownHostException;

// ANY OTHER classes, etc., that you add must be private to this package and not visible to the application layer.

// extend / implement whatever interface, etc. as required.
// NO OTHER public members / methods allowed. broadcastTOMsg, acceptTOMsg, and shutdownPaxos must be the only visible methods to the application layer.
//		You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos
{
	GCL gcl;
	FailCheck failCheck;

	Set<String> promised_processes = new HashSet<>();

	Set<String> accept_processes = new HashSet<>();

	Set<String> refused_processes = new HashSet<>();

	Set<String> denied_processes = new HashSet<>();

	volatile boolean majority_promised = false;

	volatile boolean majority_accepted = false;

	volatile boolean majority_refused = false;

	volatile boolean majority_denied = false;

	int maxBid = -1;

	Object accepted_val = null;

	int accepted_id = -1;

	Object previous_val = null;

	int previous_id = -1;

	AtomicInteger bID = new AtomicInteger(0);

	Map<Integer,Integer> installed_moves = new HashMap<>();

	int num_processes;
	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck) throws IOException, UnknownHostException
	{
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;

		this.num_processes = allGroupProcesses.length;
		for (int i = 1; i < num_processes+1; i++) {
			installed_moves.put(i, 0);
		}
		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger) ;

	}

	// This is what the application layer is going to call to send a message/value, such as the player and the move
	public void broadcastTOMsg(Object val)
	{
		int ballotID = bID.incrementAndGet();
		promised_processes.clear();
		accept_processes.clear();
		denied_processes.clear();
		refused_processes.clear();
		majority_promised = false;
		majority_accepted = false;
		majority_refused = false;
		majority_denied = false;
		previous_val = null;
		previous_id = -1;
		// initialize proposal
		Message proposal = new Message(ballotID, MessageType.PROPOSE);
		long TIMEOUT = 750;
		// try to send proposal
		gcl.broadcastMsg(proposal);
		failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);
		// enter a loop to check the promised_processes
		long startTime = System.currentTimeMillis();
		while(!majority_promised) {
			if (System.currentTimeMillis() - startTime > TIMEOUT) {
//				System.out.println("Timeout reached, retrying proposal with new ballot ID.");
				broadcastTOMsg(val);
				return;
			}
			if (this.majority_refused){
//				System.out.println("Majority Refused, retrying proposal with new ballot ID.");
				broadcastTOMsg(val);
				return;
			}
			// blocks here waiting for majority to promise
		}
		failCheck.checkFailure(FailCheck.FailureType.AFTERBECOMINGLEADER);
		Message accept_request = new Message(ballotID, MessageType.ACCEPT, val);

		if (previous_val!=null) {
			accept_request = new Message(ballotID,MessageType.ACCEPT, previous_val);
		}

		gcl.broadcastMsg(accept_request);
		startTime = System.currentTimeMillis();

		while(!majority_accepted) {
			if (System.currentTimeMillis() - startTime > TIMEOUT) {
//				System.out.println("Timeout reached, retrying proposal with new ballot ID.");
				broadcastTOMsg(val);
				return;
			}
			if (this.majority_denied){
//				System.out.println("Majority Denied, retrying proposal with new ballot ID.");
				broadcastTOMsg(val);
				return;
			}
		}
		failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
		Message confirm = new Message(ballotID, MessageType.CONFIRM, val);
		gcl.broadcastMsg(confirm);

		if (previous_val != null) {
			broadcastTOMsg(val);
		}

	}

	// This is what the application layer is calling to figure out what is the next message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in the same order.
	public Object acceptTOMsg() throws InterruptedException
	{
		GCMessage gcmsg = gcl.readGCMessage();
		String senderProcess = gcmsg.senderProcess;
		Message message = (Message)gcmsg.val;
//		System.out.print("got message with type"+message.type);
		switch (message.type) {
			case PROPOSE -> {
				failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);
				if (message.ballotID1 > maxBid) {
					if (accepted_val != null) {
						Message promise = new Message(message.ballotID1, accepted_id, MessageType.PROMISE, accepted_val);
						gcl.sendMsg(promise, senderProcess);
						failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
					} else {
						Message promise = new Message(message.ballotID1, MessageType.PROMISE);
						gcl.sendMsg(promise, senderProcess);
						failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
					}
					maxBid = message.ballotID1;
				} else {
					Message refuse = new Message(maxBid,MessageType.REFUSE);
					gcl.sendMsg(refuse, senderProcess);
					failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
				}
				return null;
			}
			case PROMISE -> {
				if (message.ballotID1 == bID.get()) {
					if (message.ballotID2 != -1) {
						// only take the max one
						if (previous_id < message.ballotID2) {
							previous_id = message.ballotID2;
							previous_val = message.value;
						}
					}
					promised_processes.add(senderProcess);
					if (promised_processes.size() > this.num_processes/2) {
						majority_promised = true;
					}
				}
				return null;
			}
			case ACCEPTACK -> {
				if (message.ballotID1 == bID.get()) {
					accept_processes.add(senderProcess);
					if (accept_processes.size() > this.num_processes/2) {
						majority_accepted = true;
					}
				}
				return null;
			}
			case ACCEPT -> {
				if (message.ballotID1 == maxBid) {
					accepted_val = message.value;
					accepted_id = message.ballotID1;
					Message acceptAck = new Message(message.ballotID1, MessageType.ACCEPTACK);
					gcl.sendMsg(acceptAck, senderProcess);
				}
				return null;
			}
			case REFUSE -> {
				if (message.ballotID1 == bID.get()) {
					this.refused_processes.add(senderProcess);
					if (refused_processes.size() > this.num_processes/2) {
						majority_refused = true;
					}
				}
				return null;
			}
			case CONFIRM -> {
				Object val = message.value;
				int playerNum = (int) ((Object[])val)[0];
				int moveNum = (int) ((Object[])val)[2];
				bID.set(maxBid);
				if (moveNum > installed_moves.get(playerNum)) {
					accepted_val = null;
					accepted_id = -1;
					installed_moves.put(playerNum,moveNum);
					return new Object[]{((Object[])val)[0], ((Object[])val)[1]};
				}else {
					accepted_val = null;
					accepted_id = -1;
					return null;
				}
			}
			case DENY -> {
				if (message.ballotID1 == bID.get()) {
					this.denied_processes.add(senderProcess);
					if (denied_processes.size() > this.num_processes / 2) {
						majority_denied = true;
					}
				}
				return null;
			}
			default -> {
				return null;
			}
		}
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos()
	{
		gcl.shutdownGCL();
	}
}

