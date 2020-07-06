import java.util.Arrays;
import java.util.Objects;


// Abstract class of structure which represents a message with its content and the process number
// of the original sender
public abstract class MessageStructure {

	// Number of integers to send per message
	public static int BUCKET_SIZE = 20;	
	
	// Content and process number
    protected int[] message;
    protected int processNumber; // One byte encoding    
    	
    public int[] getMessage(){
        return message;
    }
    
    public int getProcessNumber(){
        return processNumber;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(Arrays.hashCode(message),processNumber);
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof MessageStructure) {
    		if (Arrays.equals(((MessageStructure) o).getMessage(),message)) {
    			if (((MessageStructure) o).getProcessNumber() == processNumber) {
        			return true;
    				
    			}
    		}
    	}
    	return false;
    }
    
    public abstract byte[] messageToBytes();
    
    public abstract int getMessageSize();
        
    // From int to its 4 byte representation
	public static final byte[] intToByteArray(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}
	
	// From a 4 byte representation to int
	public static int fromBytesToInt(byte[] bytes) {
	     return ((bytes[0] & 0xFF) << 24) | 
	            ((bytes[1] & 0xFF) << 16) | 
	            ((bytes[2] & 0xFF) << 8 ) | 
	            ((bytes[3] & 0xFF) << 0 );
	}

}
