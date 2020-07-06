import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


// Main Class that initiates a Process
// It broadcasts and delivers as the highest layer, and deals with signals
public class Da_proc {
	
	private enum AlgoType{
		FIFO,LCAUSAL
	};
	
	// Boolean attributes that inform on process state
	private boolean startBroadcast;
	private AtomicBoolean terminate = new AtomicBoolean(false);
	
	// Number of messages to send
	private int numMessages;
	
	// Algorithm type (LCAUSAL or FIFO)
	private AlgoType algoType;

	
	// Writer for output file 
	private BufferedWriter writer;
	private CopyOnWriteArrayList<String> logLines = new CopyOnWriteArrayList<String>();

	// My process structure (information), and other process structures
	private ProcessStructure myProcess;
	private ArrayList<ProcessStructure> otherProcesses = new ArrayList<ProcessStructure>(); 

	// Causality array for LCAUSAL, filled from membership file
	private int[] causality;
	
	// Static object for unique instance of the class (for call purposes)
	private static Da_proc p = new Da_proc();


	private Da_proc() {
	// We are setting this to private in an effort to follow
	// the singleton design pattern
	}
	
	// Initializes the process 
	public void initializeObject(int suppliedProcessNumber, String membershipFileName, int numMessages) throws SocketException{
		
		setSignalHandlers();
		
		// The id of the process
		this.myProcess = new ProcessStructure(suppliedProcessNumber);

		// The number of messages to send
		this.numMessages = numMessages;
        this.startBroadcast = false;
		
		// Parsing the membership file
        //System.out.println("Starting reading the membership file....");
        if (!parseMembershipFile(membershipFileName)) {
			System.exit(0);
        }
        //System.out.println("Finished reading the membership file.");
        
        // Initializes object according to algo type
        if (algoType == AlgoType.FIFO) {
        	FIFO.getObject().initializeObject();
		} else if (algoType == AlgoType.LCAUSAL){
			LCB.getObject().initializeObject();
			LCMessageStructure.setMessageSize(otherProcesses.size()+1);
		}
		URB.getObject().initializeObject();
		BEB.getObject().initializeObject();
		PL.getObject().initializeObject();
		SPTPL.getObject().initializeObject();
        
		// Initializes output file and writer
		try {
			String outputFile = "da_proc_"+myProcess.getId()+".out";
			writer = new BufferedWriter(new FileWriter(outputFile));
		} catch (IOException e) {
			//System.out.print("Could not create file writer for output");
			System.exit(0);
		}
	}

	public void run() {
		
		//System.out.println("Process " + myProcess.getId() + ": Waiting for start");
		
		
		while (!startBroadcast && !terminate.get()) {
			// Do nothing
		}
		
		int messagesSent = 0;
		
		while (!terminate.get()) {
			
			if (messagesSent < numMessages && !Thread.currentThread().isInterrupted()) {
				
				// Construct bucket of messages and log broadcast
				int[] messageBucket = new int[FIFOMessageStructure.BUCKET_SIZE];
				for (int i=0;i<FIFOMessageStructure.BUCKET_SIZE;i++) {
					if (messagesSent+i < numMessages) {
						messageBucket[i] = messagesSent+i+1;
						logLines.add("b "+(messagesSent+i+1));
					} else {
						messageBucket[i] = -1;
					}
				}
				messagesSent += FIFOMessageStructure.BUCKET_SIZE;
				
				if (algoType == AlgoType.FIFO) {
					FIFO.getObject().broadcast(messageBucket);

				} else {
					LCB.getObject().broadcast(messageBucket);
				}
				try {
					Thread.sleep(200); 
				}
				catch (InterruptedException e) {
					
				}
			}
			
		}
	}
	
	public static Da_proc getObject(){
		return p;
	}

	public int getMyProcessNumber(){
		return myProcess.getId();
	}
	public ProcessStructure getMyProcessStructure() {
		return myProcess;
	}

	public int[] getCausalityList(){
		return causality;
	}
	
	public String getAlgoType() {
		if (algoType == AlgoType.FIFO) {
			return "FIFO";
		} else {
			return "LCAUSAL";
		}
	}
	
	public ArrayList<ProcessStructure> getOtherProcessList(){
		return otherProcesses;
	}

	private boolean parseMembershipFile(String membershipFileName)
    {
		BufferedReader reader;
        try
        {   
            // Reading the number of processes which are indicated at the first line of the membership file
            reader = new BufferedReader(new FileReader(membershipFileName));
            int numberOfProcesses = Integer.parseInt(reader.readLine());

            // Looping with the number of processes, because we expect 
            // that amount of (id, ip, port) in the file

            // This for loop will extract all the required information for the FIFO 
            // but partial for the Localized Causal Broadcast

            for( int i = 0; i < numberOfProcesses; i++)
            {   
                // Starting the buffer to read the file line by line
                String newLine = reader.readLine();
                // Splitting with an arbitrary number of white spaces, not necessarily 1 white space
                String[] processSpecs = newLine.split("\\s+");
                // Fetching the process number which is the first number in the file
                int processN = Integer.parseInt(processSpecs[0]);
                

                if (myProcess.getId() == processN){
                    myProcess.setIpAdress(InetAddress.getByName(processSpecs[1]));
                    myProcess.setPort(Integer.parseInt(processSpecs[2]));
                } else {
    				// Creating a process structure object that will be stored in a an arraylist of processes
    				ProcessStructure process = new ProcessStructure(processN, InetAddress.getByName(processSpecs[1]), Integer.parseInt(processSpecs[2]));
    				otherProcesses.add(process);
                }


            }
            
            // Reading the rest of file for LCausal
            String newLine = reader.readLine();
            if (newLine == null) {
            	algoType = AlgoType.FIFO;
            } else {
            	algoType = AlgoType.LCAUSAL;
				
				causality = new int[numberOfProcesses];
				causality[myProcess.getId() - 1] = 1;

				// Reading one line for each process and storing them to causality attribute is necessary
				for(int i =0; i< numberOfProcesses; i++){
				
					String[] causalityProcesses = newLine.split("\\s+");
					int causalityProcessN = Integer.parseInt(causalityProcesses[0]);

					if(myProcess.getId() == causalityProcessN){
						for(int j =1; j < causalityProcesses.length; j++){
							int causalProcess = Integer.parseInt(causalityProcesses[j]);
							causality[causalProcess - 1] = 1;
						}
					}
					newLine = reader.readLine();
				}
				
            }

            reader.close();
        }
        catch (UnknownHostException e) {
			System.out.println("Wrong IP address in membership file.");
			return false;
		} catch (FileNotFoundException e) {
			System.out.println("Membership file is not found.");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NumberFormatException e) {
			System.out.println("Membership file entries aren't parseable.");
			return false;
		} catch (Exception e){
            System.out.println("Could not parse or read the membership file.");
            return false;
        } 
		return true;
        
    }

	
	// Deliver message by adding the log to the log lines
	public void deliver(MessageStructure message) {
		for (int i : message.getMessage()) {
			if (i != -1) {
				logLines.add("d "+message.getProcessNumber()+" "+i);
			}
		}
	}
	
	// Flushing the log lines to the output file
	private void writeOutput() {
		
		try {	
			for (String line : logLines) {
				writer.write(line + '\n');
			}
			writer.close();
		} catch (IOException e) {
			System.out.print("Could not write to output file");
			System.exit(0);
		}
		
	}
	
	
////// SIGNAL HANDLERS /////
	
	private void setSignalHandlers() {
		
		SigHandlerUsr2 sigHandlerUsr2 = new SigHandlerUsr2(this);
		SigHandlerTerm sigHandlerTerm = new SigHandlerTerm(this);
		
		Signal signalTerm = new Signal("TERM");
		Signal signalUsr2 = new Signal("USR2");
		Signal signalInt = new Signal("INT");


		Signal.handle(signalTerm, sigHandlerTerm);
		Signal.handle(signalUsr2, sigHandlerUsr2);
		Signal.handle(signalInt, sigHandlerTerm);

	}

	public static class SigHandlerUsr2 implements SignalHandler {
		Da_proc p;

		private SigHandlerUsr2(Da_proc p) {
			super();
			this.p = p;
		}

		@Override
		public void handle(Signal signal) {
			p.startBroadcast = true;
			//System.out.println("Process "+p.myProcess.getId()+": Starting broadcast");
		}
	}

	public static class SigHandlerTerm implements SignalHandler {
		Da_proc p;

		private SigHandlerTerm(Da_proc p) {
			super();
			this.p = p;
		}

		@Override
		public void handle(Signal signal) {
			//System.out.println("Process "+p.myProcess.getId()+": Terminated");
			
			p.writeOutput();
			SPTPL.getObject().stop();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			
			}
			p.terminate.set(true);
		}
	}
	

	public static void main(String args[]){
		
		int processNumber;
		int numMessages;
		
		if (args.length < 2 || args.length > 3) {
			throw new IllegalArgumentException("There should be 2 or 3 arguments");
		}
		
		try {
			processNumber = Integer.parseInt(args[0]);
			if (args.length == 3) {
				numMessages = Integer.parseInt(args[2]);
			} else {
				numMessages = Integer.MAX_VALUE;
			}
			p.initializeObject(processNumber, args[1], numMessages);
			p.run();
			//System.out.println("Finished Process "+processNumber);
			
		} catch (SocketException e) {
			System.out.println("Unable to create Socket for process");
			e.printStackTrace();
			return;
		} catch (NumberFormatException e){
			System.out.println("Argument 1 and 3 should be an Integer");
			return;
		} 
	}
}
