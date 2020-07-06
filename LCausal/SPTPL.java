

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;



/*

Book: Page 35/36

With the stubborn links abstraction, it is up to the target process to check whether
a given message has already been delivered or not. Adding mechanisms for detecting
and suppressing message duplicates, in addition to mechanisms for message
retransmission, allows us to build an even higher-level primitive: the perfect links
abstraction, sometimes also called the reliable links abstraction.

Properties:

SL1: Stubborn delivery: If a correct process p sends a message m once to a correct
process q, then q delivers m an infinite number of times.
SL2: No creation: If some process q delivers a message m with sender p, then m
was previously sent to q by process p.

Events:

Request:  sl, Send | q, m : Requests to send message m to process q.
Indication:  sl, Deliver | p, m : Delivers message m sent by process p.

Uses:

FairLossPointToPointLinks
*/

public class SPTPL {
	
	// Time (in nanoseconds) to wait for a response before resending the message
	private final long ACK_TIMEOUT = 5000000 ;

    // Instance variable
    private static SPTPL SPTPL = new SPTPL();
    
    // Lists of messages to send and messages for which an ack is to be recieved
    private volatile LinkedBlockingQueue<CompleteStructure> toSend = new LinkedBlockingQueue<CompleteStructure>();
    private volatile LinkedBlockingQueue<Entry<CompleteStructure,Long>> waitingAcks = new LinkedBlockingQueue<Entry<CompleteStructure,Long>>();
    
    // Threads used and udp socket
    private Thread sendingThread;
    private Thread receivingThread;
    private DatagramSocket udpSocket;
    
    // Process information
    private ProcessStructure myProcess;
    private int messageSize;

    private SPTPL(){
        // We are setting this to private in an effort to follow
        // the singleton design pattern
    }
    
    public Thread getListener() {
    	return receivingThread;
    }
    
    public Thread getSender() {
    	return sendingThread;
    }

    // Initializes the SPTPL object
    public void initializeObject() throws SocketException{
    	
        this.myProcess = Da_proc.getObject().getMyProcessStructure();
        
        // Initializes the message size
        if (Da_proc.getObject().getAlgoType().equals("FIFO")) {
            this.messageSize = FIFOMessageStructure.MAX_MESSAGE_SIZE;
        } else {
        	this.messageSize = LCMessageStructure.MAX_MESSAGE_SIZE;
        	
        }
        
        // Creates the udp socket
        udpSocket = new DatagramSocket(myProcess.getPort(), myProcess.getIpAddress());
        
        // Starting the receiving thread
        receivingThread = new Thread(new ReceivingThread());
        //System.out.println("Process "+myProcess.getId()+": Starting Receiving Thread");
        receivingThread.start();
        
        // Starting the sending thread
    	sendingThread  = new Thread(new SendingThread()); 
        //System.out.println("Process "+myProcess.getId()+": Starting Sending Thread");
    	sendingThread.start();
    	
    }
    
    // Stops all threads
    public void stop() {
    	//System.out.println("Process "+myProcess.getId()+": Closing IO Threads and Socket");
    	receivingThread.interrupt();
    	sendingThread.interrupt();
    	udpSocket.close();
    }

    public static SPTPL getObject(){
		return SPTPL;
    }
    
    // Adds a message to send to the toSend list
    public void send(MessageStructure message, ProcessStructure singleProcess) {
    	toSend.add(new CompleteStructure(message, singleProcess,0));
    }
    
    private class ReceivingThread implements Runnable {
    	
        public void run() {
        	
        	int nMessagesRec = 0;
        	
        	byte[] buf = new byte[messageSize+1];
        	DatagramPacket dp = new DatagramPacket(buf,messageSize+1);
        	try {
        		while (!Thread.currentThread().isInterrupted()) {
        			
        			// Receive message
	        		udpSocket.receive(dp);
        			CompleteStructure recMessage = new CompleteStructure(dp);

        			// If the message is an ack, remove the message from the resending list
        			if (recMessage.isAck()==1) {
        				for (Entry<CompleteStructure,Long> entry : waitingAcks) {
        					
        					if (entry.getKey().equalStructures(recMessage)) {
        						waitingAcks.remove(entry);
        						break;
        					}
        					
        				}
        				
        			} 
        			// If the message isn't an ack, add a ack for this message to the toSend list
        			else {
            			PL.getObject().deliver(recMessage);
            			toSend.add(new CompleteStructure(recMessage.getMessageStructure(),recMessage.getProcessStructure(),1));
        			}
        			nMessagesRec++;
        			
        		}
        	} catch (IOException e) {
        		//e.printStackTrace();
        	} finally {
        		//System.out.println("Process "+myProcess.getId()+": Recieving Thread Closed, "+nMessagesRec+" messages received");
        	}
        }
    }
    
    private class SendingThread implements Runnable {
    	
        public void run() {
        	
        	int nMessagesSent = 0;
        	
        	try {

	        	while (!Thread.currentThread().isInterrupted()) {
	        		
	        		// Get the oldest message from the toSend list
	        		CompleteStructure packet = toSend.poll();
	        		
	        		if( packet != null) {
			        	DatagramPacket datagramPacket = packet.toDatagramPacket();
			        	udpSocket.send(datagramPacket);
			        	
			        	// If the message isn't an ack, put it to the resending list to wait for its ack
			        	if (packet.isAck() == 0) {
			        		waitingAcks.add(new AbstractMap.SimpleEntry<CompleteStructure, Long>(packet, System.nanoTime()));
			        	}
		        		
		        		nMessagesSent+= 1;
	        		}
	        		
	        		// Get the oldest message for which we are waiting an ack
	        		Entry<CompleteStructure,Long> oldestWaiting = waitingAcks.peek();
	        		
	        		// If the ack timeout is reached, resend it
	        		if ((oldestWaiting != null) && (oldestWaiting.getValue()< System.nanoTime()-ACK_TIMEOUT)) {
	        			waitingAcks.poll();
	        			DatagramPacket datagramPacket = oldestWaiting.getKey().toDatagramPacket();
	        			udpSocket.send(datagramPacket);
	        			oldestWaiting.setValue(System.nanoTime());
	        			waitingAcks.add(oldestWaiting);
		        		
		        		nMessagesSent+= 1;
		        		
	        		}
	        		
	        		
	        	}
        	} catch (Exception e){
        		//e.printStackTrace();
        	} finally {
        		//System.out.println("Process "+myProcess.getId()+": Sending Thread Closed, "+nMessagesSent+" messages sent");
        	}
        	
        }
    }
    
}
