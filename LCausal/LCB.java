

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicIntegerArray;
/*

Book: Page 103/108/109

The causal order property for a broadcast abstraction ensures that messages are
delivered such that they respect all cause–effect relations. The happened-before
relation described earlier in this book (Sect. 2.5.1) expresses all such dependencies.
This relation is also called the causal order relation, when applied to messages
exchanged among processes and expressed by broadcast and delivery events.

Properties:

CRB1: Validity: If a correct process p broadcasts a message m, then p eventually
delivers m.
CRB2: No duplication: No message is delivered more than once.
CRB3: No creation: If a process delivers a message m with sender s, then m was
previously broadcast by process s.
CRB4: Uniform agreement: If a message m is delivered by some process (whether
correct or faulty), then m is eventually delivered by every correct process.
CRB5: Causal delivery: For any message m1 that potentially caused a message m2,
i.e., m1 → m2, no process delivers m2 unless it has already delivered m1.

Events:

Request:  curb, Broadcast | m : Broadcasts a message m to all processes.
Indication:  curb, Deliver | p, m : Delivers a message m broadcast by process p.

Uses:

UniformReliableBroadcast

*/ 
public class LCB{

    // Instance variables
    private static LCB LCB = new LCB();
    
    private int myLsn;
    private AtomicIntegerArray myVectorClock;
    private LinkedList<LCMessageStructure> pending;
    private int[] causality;
    
    // Constructor
    private LCB(){
        // We are setting this to private in an effort to follow
		// the singleton design pattern
    }

    public void initializeObject(){

        // This variable maintains the local sequence number
        myLsn = 0;
        // Number of processes we have
        int nProcesses = Da_proc.getObject().getOtherProcessList().size()+1;
        // Setting all the values in the vector clock that correspond
        // to each process to zero
        myVectorClock = new AtomicIntegerArray(nProcesses);
        for(int i = 0; i< nProcesses; i++){
            myVectorClock.set(i,0);
        }
        //Fetching the causality array that indicates to
        //which processes is this process affected by.
        // A value of 1 indicates that it affects this process
        // A value of 0 indicates otherwise
        causality = Da_proc.getObject().getCausalityList();
        
        pending = new LinkedList<LCMessageStructure>();
    }

    public static LCB getObject(){
		return LCB;
    }
    
    public static AtomicIntegerArray AtomicCopy(AtomicIntegerArray VC, int[] causality)
    {   
        // This function is used to deepcopy the atomicintegerarray in order 
        // to avoid any conflicts when manipulating values in the broadcast/deliver functions

        // We also set the causality of the process here.
        // We only copy the values that are actually causal regarding this process
        AtomicIntegerArray deepCopy = new AtomicIntegerArray(VC.length());
        for(int i= 0; i < VC.length(); i++){
            if(causality[i] == 1){
                deepCopy.set(i, VC.get(i));
            }
            else{
                deepCopy.set(i, 0);
            }
        }
        return deepCopy;
    }

    public void broadcast(int[] message){

        // Grabbing the vector that we are going to send updated
        // with the local causality constraints
        AtomicIntegerArray sendVectorClock = AtomicCopy(myVectorClock, causality);
        //Fetching our own process number
        int myProcessNumber = Da_proc.getObject().getMyProcessNumber();
        // Updating the vector clock with the causality of my own process
        sendVectorClock.set(myProcessNumber-1, myLsn);

        // Incrementing the sequence number of the process
        myLsn = myLsn + 1;
        
        // Creating the message structure that contains all the information required for
        // broadcasting in the lower abstractions
        LCMessageStructure messageProcess = new LCMessageStructure(message, myProcessNumber, sendVectorClock);
        URB.getObject().broadcast(messageProcess);

    }

    public void deliver(MessageStructure message){

        // Adding the message to our pending variable before delivering
        pending.add((LCMessageStructure) message);
        // Fetching how many processes we have in total
        int nProcesses = Da_proc.getObject().getOtherProcessList().size() + 1;

        // Iterating through all the elements of the pending variable
        // and checking if satisfies the conditions of LCB.
        // If that's the case, then we deliver the messsage to the layer above, 
        // remove the message, from our pending variable, and updating our vectorClock
        Iterator<LCMessageStructure> iterator = pending.iterator();
        while(iterator.hasNext()){

            LCMessageStructure messageToDeliver = iterator.next();

            int processNumber = messageToDeliver.getProcessNumber();
            int[] messageVC = messageToDeliver.getVC();

            boolean canDeliver = true;
            
            for (int i = 0; i < nProcesses; i++){
                
                if(messageVC[i] > myVectorClock.get(i)){
                    canDeliver = false;
                    break;
                }
            }

            if(canDeliver == true){
                iterator.remove();
                myVectorClock.incrementAndGet(processNumber - 1);
                iterator = pending.iterator();

                Da_proc.getObject().deliver(messageToDeliver);

            }

        }




}

}
