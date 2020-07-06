

import java.util.ArrayList;
/*

Book: Page 75/76

With best-effort broadcast, the burden of ensuring reliability is only on the sender.
Therefore, the remaining processes do not have to be concerned with enforcing
the reliability of received messages. On the other hand, no delivery guarantees are
offered in case the sender fails.

Properties:

BEB1: Validity: If a correct process broadcasts a message m, then every correct
process eventually delivers m.
BEB2: No duplication: No message is delivered more than once.
BEB3: No creation: If a process delivers a message m with sender s, then m was
previously broadcast by process s.

Events:

Request:  beb, Broadcast | m : Broadcasts a message m to all processes.
Indication:  beb, Deliver | p, m : Delivers a message m broadcast by process p.

Uses:

PerfectPointToPointLinks
*/

public class BEB{
	
    // Instance variable
    private static BEB BEB = new BEB();

    // List of all processes we have that are going to be
    // used to broadcast
    private ArrayList<ProcessStructure> otherProcesses;

    private BEB() {
        // We are setting this to private in an effort to follow
		// the singleton design pattern
    }

    public void initializeObject(){
        
        // Fetching all the processes we have that got 
        // initialized in the beginning
        otherProcesses = Da_proc.getObject().getOtherProcessList();

    }

    public static BEB getObject(){
		return BEB;
    }
    
    // Send the message to all processes individually
    public void broadcast(MessageStructure message){
        
        for(int i = 0 ; i < otherProcesses.size(); i++){
            ProcessStructure singleProcess = otherProcesses.get(i);
            PL.getObject().send(message, singleProcess);
        }
        
    }

    public void deliver(MessageStructure message, ProcessStructure singleProcess){
        URB.getObject().deliver(message, singleProcess);
    }
}