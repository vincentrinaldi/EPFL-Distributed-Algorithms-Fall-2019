

import java.util.HashSet;

/*

Book: Page 37/38

With the stubborn links abstraction, it is up to the target process to check whether
a given message has already been delivered or not. Adding mechanisms for detecting
and suppressing message duplicates, in addition to mechanisms for message
retransmission, allows us to build an even higher-level primitive: the perfect links
abstraction, sometimes also called the reliable links abstraction.

Properties:

PL1: Reliable delivery: If a correct process p sends a message m to a correct
process q, then q eventually delivers m.
PL2: No duplication: No message is delivered by a process more than once.
PL3: No creation: If some process q delivers a message m with sender p, then m
was previously sent to q by process p.

Events:

Request:  pl, Send | q, m : Requests to send message m to process q.
Indication:  pl, Deliver | p, m : Delivers message m sent by process p.

Uses:

StubbornPointToPointLinks
*/
public class PL{

    // Instance variables

    private static PL PL = new PL();

    private HashSet<CompleteStructure> delivered;

    private PL(){
        // We are setting this to private in an effort to follow
        // the singleton design pattern
    }

    public void initializeObject(){

        delivered = new HashSet<>();

    }

    public static PL getObject(){
		return PL;
    }

    public void send(MessageStructure message, ProcessStructure singleProcess){
        SPTPL.getObject().send(message, singleProcess);
    }

    // If we didn't already deliver the message, deliver it to the layer above
    public void deliver(CompleteStructure message){
    	
	    if (!delivered.contains(message)){
	        delivered.add(message);
	        BEB.getObject().deliver(message.getMessageStructure(), message.getProcessStructure());
	    }

    }
}