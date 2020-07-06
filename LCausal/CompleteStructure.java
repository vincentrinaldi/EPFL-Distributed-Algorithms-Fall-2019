

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Objects;

// Structure containing information of the datagram packet to send or recieved
// It has:
//	- a MessageStructure (original message)
//	- a ProcessStructure (direct sender or process to send to)
//	- a isAck boolean attribute (1 if it is an ACK, 0 else)
public class CompleteStructure{

    private MessageStructure messageStructure;
    private ProcessStructure processStructure;
    private int isAck;
    
    public CompleteStructure(MessageStructure messageStructure, ProcessStructure processStructure,int isAck) {
        this.messageStructure = messageStructure;
        this.processStructure = processStructure;
        this.isAck = isAck;

    }
    
    public CompleteStructure(DatagramPacket packet) {
    	if (Da_proc.getObject().getAlgoType().equals("FIFO")) {
        	this.messageStructure = new FIFOMessageStructure(packet);
    	} else {
    		this.messageStructure = new LCMessageStructure(packet);
    	}
    	this.processStructure = new ProcessStructure(packet);
    	this.isAck = (int) packet.getData()[0];

    }    
    
    public MessageStructure getMessageStructure(){
        return messageStructure;
    }
    public ProcessStructure getProcessStructure(){
        return processStructure;
    }
    public int isAck() {
    	return isAck;
    }
    
    // Transforms the structure to a datagram packet
    public DatagramPacket toDatagramPacket() {
    	
    	InetAddress address = processStructure.getIpAddress();
    	int port = processStructure.getPort();
    	byte[] m = messageStructure.messageToBytes();
    	byte[] buff = new byte[m.length+1];
    	
    	for (int i=1;i<buff.length;i++) {
    		buff[i] = m[i-1];
    	}
    	buff[0]= (byte) isAck;
    	
    	return new DatagramPacket(buff,buff.length,address,port);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(messageStructure,processStructure,isAck);
    }
    
    @Override
    public boolean equals(Object o) {

    	if (o instanceof CompleteStructure) {
    		if (((CompleteStructure) o).getMessageStructure().equals(messageStructure)) {
    			if (((CompleteStructure) o).getProcessStructure().equals(processStructure)) {
        			if (((CompleteStructure) o).isAck() == isAck) {
    					return true;
        			}
    			}
    		}
    	}
    	return false;
    }
    
    public boolean equalStructures(Object o) {
    	
    	if (o instanceof CompleteStructure) {
    		if (((CompleteStructure) o).getMessageStructure().equals(messageStructure)) {
    			if (((CompleteStructure) o).getProcessStructure().equals(processStructure)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }

}