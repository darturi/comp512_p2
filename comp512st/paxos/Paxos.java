package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.HashSet;
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
	AtomicInteger BID;
	AtomicInteger BID2;
	Object[] value;
	HashSet<String> majorityPromiseHashSet;
	HashSet<String> majorityAcceptAckHashSet;
	String myProcess;
	int sizeOfGroup;



	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck) throws IOException, UnknownHostException
	{
		// Remember to call the failCheck.checkFailure(..) with appropriate arguments throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger);

		// Save myProcess
		this.myProcess = myProcess;

		// Initialize added class variables
		this.BID = new AtomicInteger(1);
		this.BID2 = new AtomicInteger(-1);
		this.majorityPromiseHashSet = new HashSet<>();
		this.majorityAcceptAckHashSet = new HashSet<>();
		this.sizeOfGroup = allGroupProcesses.length;

		// Start running acceptor thread
		AcceptorThread acceptorThread = new AcceptorThread(this.gcl, this.BID, this.BID2, this.majorityPromiseHashSet, this.majorityAcceptAckHashSet, this.sizeOfGroup, this.myProcess);
        acceptorThread.run();
    }

	// This is what the application layer is going to call to send a message/value, such as the player and the move
	synchronized public void broadcastTOMsg(Object val)
	{
		// This is just a place holder.
		// Extend this to build whatever Paxos logic you need to make sure the messaging system is total order.
		// Here you will have to ensure that the CALL BLOCKS, and is returned ONLY when a majority (and immediately upon majority) of processes have accepted the value.

		// Update BID and BID2
		synchronized (this.BID) {this.BID.incrementAndGet();}
		synchronized (this.BID2) {this.BID2.set(-1);}

		// Clear HashSets
		synchronized (this.majorityPromiseHashSet) {this.majorityPromiseHashSet.clear();}
		synchronized (this.majorityAcceptAckHashSet) {this.majorityAcceptAckHashSet.clear();}

		ProposerThread proposerThread = new ProposerThread(this.BID.get(), val, this.gcl);
		proposerThread.start();


		// gcl.broadcastMsg(val);
	}

	// This is what the application layer is calling to figure out what is the next message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in the same order.
	public Object acceptTOMsg() throws InterruptedException
	{
		// This is just a placeholder.
		GCMessage gcmsg = gcl.readGCMessage();
		return gcmsg.val;
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos()
	{
		gcl.shutdownGCL();
	}
}

