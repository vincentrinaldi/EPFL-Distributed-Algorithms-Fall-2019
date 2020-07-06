

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/*

Book: Page 82/83/85

We now introduce a stronger definition of reliable broadcast, called uniform
reliable broadcast. This definition is stronger in the sense that it guarantees that the
set of messages delivered by faulty processes is always a subset of the messages
delivered by correct processes. Many other abstractions also have such uniform
variants.

Properties:

URB1: Validity: If a correct process p broadcasts a message m, then p eventually
delivers m.
URB2: No duplication: No message is delivered more than once.
URB3: No creation: If a process delivers a message m with sender s, then m was
previously broadcast by process s.
URB4: Uniform agreement: If a message m is delivered by some process (whether
correct or faulty), then m is eventually delivered by every correct process.

Events:

Request:  urb, Broadcast | m : Broadcasts a message m to all processes.
Indication:  urb, Deliver | p, m : Delivers a message m broadcast by process p.

Uses:

BestEffortBroadcast

*/
public class URB{
	
    // Instance variable
    private static URB URB = new URB();

    // This variable gathers the set of processes that the process knows
    // have seen the message
    private ConcurrentHashMap<MessageStructure, AtomicInteger> ack;
    // This variable is used to filter out duplicate messages
    private HashSet<MessageStructure> delivered;
    // This variable is used to collect the messages that have been beb-delivered (lower layer)
    // and seen but still need to be urb-delivered (this layer)
    private Set<MessageStructure> pending;
    
    private int numbOfProcesses;

    private URB(){
        // We are setting this to private in an effort to follow
		// the singleton design pattern
    }

    public void initializeObject(){
        // Fetching the Best effort broadcast object

        // Initializing the variables used in this class
        ack = new ConcurrentHashMap<>();
        delivered = new HashSet<>();
        pending = Collections.newSetFromMap(new ConcurrentHashMap<MessageStructure, Boolean>());
        
        numbOfProcesses = Da_proc.getObject().getOtherProcessList().size()+1;
    }

    public static URB getObject(){
		return URB;
	}

    public void broadcast(MessageStructure messageProcess) {

    	
        // Adding the message structure to the pending variable
        pending.add(messageProcess);
        ack.put(messageProcess,new AtomicInteger(1));

        //Best effort broadcast
        BEB.getObject().broadcast(messageProcess);
    }

    public void deliver(MessageStructure message, ProcessStructure singleProcess){

        // We need to count how many times we've received this message
        // and check if we already know this message or not.

    	if (!delivered.contains(message)) {
    		
    		ack.computeIfAbsent(message, k -> new AtomicInteger(0));
            ack.get(message).incrementAndGet();
            
            // Now we need to check if the message we received is in 
            // our pending variable. If NOT then that means that this
            // message was broadcasted by a process that is not "self"
            // and hence we need to re-broadcast it to other processes
            // to satisfy the uniform agreement property

            // This is the case when the message is not in our pending variable
            if (!pending.contains(message)){

                // Adding the message to the pending variable
                pending.add(message);
                // Re-broadcasting the message to other processes
                ack.get(message).incrementAndGet();
                BEB.getObject().broadcast(message);
            } else {
            	if (canDeliver(message)) {
            		delivered.add(message);
            		ack.remove(message);
            		pending.remove(message);
            		
                    if (Da_proc.getObject().getAlgoType().equals("FIFO")){
                        FIFO.getObject().deliver(message);
                    }
                    else {
                        LCB.getObject().deliver(message);
                    }
            	}
            }
    	}
    }

    // This function needs to check the majority-ack for the URB
    public boolean canDeliver(MessageStructure message){

        // Checking the amount of processes that have seen the message
        // if the message is not in ack, we assign 0
        int i = ack.getOrDefault(message,new AtomicInteger(0)).get();

        // Check if the amount of seen processes is greater than
        // the number of processes divided by half. Hence more than
        // half the processes should have seen it as a condition of
        // uniform agreement.
        boolean response = (i > numbOfProcesses/2);

        return response;

    }
}