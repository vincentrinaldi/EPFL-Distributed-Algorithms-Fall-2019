

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Objects;

// An object that contains the information of the process
public class ProcessStructure{

    private int id;
    private InetAddress ipAddress;
    private int port;
    
    public ProcessStructure(int id, InetAddress ipAddress, int port)
    {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
    }
    
    public ProcessStructure(int id) {
    	this.id = id;
    	this.ipAddress = null;
    	this.port = -1;
    }
    
    // Retrieves the Process Structure of the direct sender from a datagram packet
    public ProcessStructure(DatagramPacket packet) {
    	this.ipAddress = packet.getAddress();
    	this.port = packet.getPort();
    	
    	boolean found = false;
    	
    	for (ProcessStructure process : Da_proc.getObject().getOtherProcessList()) {
    		if (process.getIpAddress().equals(ipAddress) && process.getPort() == port) {
    			this.id = process.getId();
    			found = true;
    			break;
    		}
    	}
    	if (!found) {
    		System.out.println("Couldn't find process in other process list");
    		this.id = -1;
    	}
    }
    
    public void setIpAdress(InetAddress address) {
    	this.ipAddress = address;
    }
    
    public void setPort(int port) {
    	this.port = port;
    }

    public int getId(){
        return id;
    }
    

    public InetAddress getIpAddress(){
        return ipAddress;
    }

    public int getPort(){
        return port;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(id,ipAddress,port);
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof ProcessStructure) {
    		if (((ProcessStructure) o).getPort() == port) {
    			if (((ProcessStructure) o).getIpAddress().equals(ipAddress)) {
    				if (((ProcessStructure) o).getId() == id) {
        				return true;
    				}
    			}
    		}
    	}
    	return false;
    }


    
}