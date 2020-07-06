

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Objects;

// Message Structure for FIFO Broadcast
public class FIFOMessageStructure extends MessageStructure{

	public static int MAX_MESSAGE_SIZE = 1 + 4*(BUCKET_SIZE+1); // in bytes
	
	// Lsn used for the message
	private int lsn;
    
	// Constructor with wanted attributes as input
    public FIFOMessageStructure(int[] message, int processNumber, int lsn)
    {
        this.message = message;
        
        if (processNumber<0 || processNumber > 255) {
        	throw new IllegalArgumentException("Process Number has to be between 0 and 255");
        }
        this.processNumber = processNumber;
        this.lsn = lsn;

    }
	// Constructor taking datagram packet as argument
    public FIFOMessageStructure(DatagramPacket packet) {
    	byte[] messageByte = packet.getData();
    	int[] messageInt = new int[BUCKET_SIZE];
    	
    	this.lsn = fromBytesToInt(Arrays.copyOfRange(messageByte,2,6));
    	for (int i = 0;i<BUCKET_SIZE;i++) {
    		messageInt[i] = fromBytesToInt(Arrays.copyOfRange(messageByte,6 +i*4,10+i*4));
    	}
    	this.processNumber = (int) messageByte[1];
    	this.message = messageInt;
    }
    
    public int getLsn() {
    	return lsn;
    }
    
    // The message is set as : [processNumber (1 byte), lsn (4 bytes), Message (4*bucketSize bytes)]
    public byte[] messageToBytes() {
    	
    	byte[] result = new byte[MAX_MESSAGE_SIZE];
    	result[0] = (byte) processNumber;
    	
    	int i = 1;
    	for (byte b : intToByteArray(lsn)) {
    		result[i] = b;
    		i++;
    	}
    	for (int integer : message) {
        	for (byte b : intToByteArray(integer)) {
        		result[i] = b;
        		i++;
        	}
    	}

    	return result;
    	
    }
    
    public int getMessageSize() {
    	return MAX_MESSAGE_SIZE;
    }
    
    
    @Override
    public int hashCode() {
    	return Objects.hash(super.hashCode(),lsn);
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof FIFOMessageStructure) {
    		if (super.equals(o)) {
    			if (((FIFOMessageStructure) o).getLsn() == lsn) {
        				return true;
    			}
    		}
    	}
    	return false;
    }
  
}
