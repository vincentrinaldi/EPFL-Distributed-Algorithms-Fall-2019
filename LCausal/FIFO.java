

import java.util.HashMap;

/*

Book: Page 101/102

The specification of reliable broadcast does not state anything about the order in
which multiple messages are delivered. A FIFO-order is one of the simplest possible
orderings and guarantees that messages from the same sender are delivered in
the same sequence as they were broadcast by the sender. Note, this does not affect
messages from different senders.

Properties:

FRB1: Validity: If a correct process p broadcasts a message m, then p eventually
delivers m.
FRB2: No duplication: No message is delivered more than once.
FRB3: No creation: If a process delivers a message m with sender s, then m was
previously broadcast by process s.
FRB4: Uniform agreement: If a message m is delivered by some process (whether
correct or faulty), then m is eventually delivered by every correct process.
FRB5: FIFO delivery: If some process broadcasts message m1 before it broadcasts
messagem2, then no correct process delivers m2 unless it has already deliveredm1.

Events:

Request:  frb, Broadcast | m : Broadcasts a message m to all processes.
Indication:  frb, Deliver | p, m : Delivers a message m broadcast by process p.

Uses:

UniformReliableBroadcast

*/ 
public class FIFO{

    // Instance variables

    private static FIFO FIFO = new FIFO();

    // Every process maintains a sequence number lsn for the frb-broadcast
    // messages, and urb broadcasts the value lsn together with the content
    // of the message
    // This variable is used for broadcasting
    private int my_lsn;

    // This array will contain an entry for every process p, with the 
    // sequence number of the next message to be frb-delivered from sender p.
    // This variable is used for delivering
    private int[] next;

    // This variable will contain a mapping between the processes and the
    // messages that are received that belong to this process.
    // Example: {1: [Structure3, Strucutre1, Structure2]} Process 1 containing
    // 3 un-ordered structures

    private  HashMap<Integer, HashMap<Integer,FIFOMessageStructure>> pending;

    // Constructor
    private FIFO(){
        // We are setting this to private in an effort to follow
		// the singleton design pattern
    }

    public void initializeObject(){
        // Initializing the variable listen to zero
        // This value will increment every time we broadcast a message
        my_lsn = 0 ;

        // Since the next message to be delivered are all labeled as 1 (starting point)
        // We initialize the next array with values 1 depending on the
        // number of processes we have
        int nProcesses = Da_proc.getObject().getOtherProcessList().size();

        // Initializing the array to 1, which is the first sequence number
        // to be expected
        next = new int[nProcesses+1];
        pending = new HashMap<>();

        for(int i = 0; i< nProcesses+1; i++){
            next[i] = 1;
            pending.put(i+1, new HashMap<Integer,FIFOMessageStructure>());
        }

    }

    public static FIFO getObject(){
		return FIFO;
	}

    public void broadcast(int[] message){
        // Since every process maintains a sequence number lsn for the broadcast
        // The URB broadcasts the value lsn together with the message.
        my_lsn = my_lsn + 1;
        int myProcessNumber = Da_proc.getObject().getMyProcessNumber();
        MessageStructure myMessage = new FIFOMessageStructure(message, myProcessNumber,my_lsn);
        URB.getObject().broadcast(myMessage);
    }

    public void deliver(MessageStructure message){

    	FIFOMessageStructure fifoMessage = (FIFOMessageStructure) message;
    	int lsn = fifoMessage.getLsn();
        // Fetching the process number that will be used to
        // know which sequence number is current for this process
        int processNumber = fifoMessage.getProcessNumber();

        // The current sequence number that we need to deliver
        int sn = next[processNumber-1];

        // This is here to somewhat improve performance
        // We only need to go to the while loop in case
        // we know for sure that we can deliver this message and 
        // possibly other messages
        if(sn == lsn){
        	
        	Da_proc.getObject().deliver(message);
        	
            // Fetching all the messages that are pending for this process
        	HashMap<Integer,FIFOMessageStructure> processMessages = pending.get(processNumber);
        	
        	// While we can continue delivering the next messages in order
        	while (processMessages.containsKey(sn+1)) {
        		sn++;
        		FIFOMessageStructure messageToDeliver = processMessages.get(sn);
        		Da_proc.getObject().deliver(messageToDeliver);
        		processMessages.remove(sn);
        	}
        	
        	// Update the sequence number of next expected message from process
        	next[processNumber-1] = sn+1;

        } else {
        	
            // Adding the message into the hashmap pending which
            // stores the messages and their corresponding sequence number
            pending.get(processNumber).put(lsn, fifoMessage);
        }   

    }
}