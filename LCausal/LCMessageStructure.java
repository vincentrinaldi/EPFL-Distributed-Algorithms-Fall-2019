import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerArray;


// Message Structure used by Local Causal Broadcast
public class LCMessageStructure extends MessageStructure {

	// Attribute unique to this class (vs FIFO structure)
	public static int MAX_MESSAGE_SIZE;
	
	// Vector Clock for LCB
	private int[] VC;

	// Constructor with wanted attributes as input
	public LCMessageStructure(int[] message, int processNumber, AtomicIntegerArray VC)
    {
        this.message = message;
        
        if (processNumber<0 || processNumber > 255) {
        	throw new IllegalArgumentException("Process Number has to be between 0 and 255");
        }
        this.processNumber = processNumber;
        int numberProcesses = Da_proc.getObject().getOtherProcessList().size()+1;
        
        this.VC = new int[numberProcesses];
        for (int i=0;i<VC.length();i++) {
            this.VC[i] = VC.get(i);
        }
	}
    
	// Constructor taking datagram packet as argument
    public LCMessageStructure(DatagramPacket packet) {
    	byte[] messageByte = packet.getData();
    	
    	processNumber = (int) messageByte[1];
    	
    	int numberProcesses = Da_proc.getObject().getOtherProcessList().size()+1;
    	
    	VC = new int[numberProcesses];
    	message = new int[BUCKET_SIZE];
    	
    	for (int i = 0;i<numberProcesses;i++) {
    		VC[i] = fromBytesToInt(Arrays.copyOfRange(messageByte,2+i*4,6+i*4));
    	}
    	
    	for (int i = 0;i<BUCKET_SIZE;i++) {
    		message[i] = fromBytesToInt(Arrays.copyOfRange(messageByte,2+numberProcesses*4+i*4,6+numberProcesses*4+i*4));
    	}

    }
    
    // Message Size setter
	public static void setMessageSize(int numberProcesses) {
		MAX_MESSAGE_SIZE = 1 + 4*(BUCKET_SIZE+numberProcesses);
	}
    
    // The message is set as : [processNumber (1 byte), VC (4*numberOfProcesses bytes), Message (4*bucketSize bytes)]
    public byte[] messageToBytes() {
    	
    	byte[] result = new byte[MAX_MESSAGE_SIZE];
    	result[0] = (byte) processNumber;
    	
    	int i = 1;
    	for (int lsn_p : VC) {
        	for (byte b : intToByteArray(lsn_p)) {
        		result[i] = b;
        		i++;
        	}
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
    
    public int[] getVC() {
    	return VC;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(super.hashCode(),Arrays.hashCode(VC));
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof LCMessageStructure) {
    		if (super.equals(o)) {
    			if (Arrays.equals(((LCMessageStructure) o).getVC(),VC)) {
        			return true;
    			}
    		}
    	}
    	return false;
    }
    
    
    
}
