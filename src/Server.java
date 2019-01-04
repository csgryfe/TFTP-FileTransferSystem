import java.io.*;
import java.net.*;
import java.util.*;
/**
 *
 * @author Caleb & Brandon
 *
 */
public class Server implements Runnable {
	private static PacketTypes type;
	public static final int TFTP_LISTEN_PORT = 6900; // default port
	public static final int requestPort = 6901;
	public static final int MAX_LENGTH = 512; // maximum packet length
	private int maxLen = 512;
	public static final int MIN_LENGTH = 4; // minimum packet length
	private DatagramSocket socket;
	private boolean verbose = true;
	private String modes = "netascii";
	private static final String defaultFoler = System.getProperty("user.dir")  + File.separator + "server_files";
	private String folder = defaultFoler;
	private static int returnPort;
	private InetAddress inetAddress;
	private String sendReceive = "receive";
	private static boolean acceptConnection = true;
	private int dataLen;
	private boolean scanning = false;
	private int threadCount = 0;
	private ArrayList<RequestHandler> threads = new ArrayList<>();
	private ArrayList<String> currentFiles = new ArrayList<>();
	private boolean verboseMode = false;
	private String filePath;
	Server() {

		try {
			socket = new DatagramSocket(6900);

		} catch (SocketException e) {

			e.printStackTrace();
		}
	}
	/**
	 *
	 * @return
	 */
	private String getFolder() {
		return folder;
	}

	public String getDirectory(){
		return filePath;
	}
	/**
	 *
	 * @param filename
	 * @return
	 */
	public String getFilePath(String filename) {
		return getFolder() + filename;
	}

	public void removeFile(String file){
		currentFiles.remove(file);
	}

	/**
	* The command menu for server side of things.
	*/
	public void commandPrompt() {
		System.out.println("");
		System.out.println("Server is now up and running.");
		System.out.println("These are the available commands:\n");
		System.out.println("Exit");
		System.out.println("Mode\n");
	}
	/**
	 *
	 * @throws IOException
	 */
	private void waitForCommand() throws IOException {


		Scanner s = new Scanner(System.in);
		commandPrompt();
		while (true) {
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase();
			switch (cmdLine) {
			case "exit":
			System.out.println("HIT");
				shutdown(); // close the scanner
				return;
			case "directory":
					System.out.println("Enter new directory\n");

					cmdLine = s.nextLine().toLowerCase();
					filePath = cmdLine;
					break;


			case "mode":
			if (verboseMode){
				verboseMode = false;
			}else{
				verboseMode = true;
			}
			continue;
			default:
				System.out.println("Invalid command, please try again!\n");
				commandPrompt();
				//System.out.println("These are the available commands:\nExit\n");
			}
		}
	}
	/**
	 *
	 * @param data
	 * @return
	 */
	public boolean isReadRequest(byte[] data) {
		return data != null && data[1] == 1;
	}
	/**
	 *
	 * @param data
	 * @return
	 */
	public boolean isWriteRequest(byte[] data) {
		return data != null && data[1] == 2;
	}

	private String getErrorMsg(int code){
		if (code == 0){
			return "Error Code " + code + ": Undefined error.";
		}else if(code == 1){
			return "Error Code " + code +": File not found.";
		}else if(code == 2){
			return "Error Code " + code +": Access violation.";
		}else if(code == 3){
			return "Error Code "+code + ": Disk full or allocation exceeded.";
		}else if(code == 4){
			return "Error Code " + code + " Illegal TFTP Operation";
		}else if(code == 5){
			return "Error Code " + code + " Unidentified TID";
		}else if(code == 6){
			return "Error Code "+ code + ": File already exists..";
		}
		return "Error Code " + code + ": Undefined error.";

	}

	public boolean getMode() { return verboseMode; }

	/**
	* Method designed to send error.
	*
	* @param code
	*/
	private void sendError(int code){
		System.out.println("Sending Error Packet");

		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		BAOS.write(0);
		BAOS.write(5);
		BAOS.write(0);
		BAOS.write(code);

		try {
			BAOS.write(getErrorMsg(code).getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BAOS.write(0);

		DatagramPacket p = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, inetAddress, returnPort);

		try{
			System.out.println("Sending error");
			socket.send(p);
		}catch(IOException e){
			e.printStackTrace();
		}

		if(verboseMode == true){
			System.out.println("Error Sent to address: " + inetAddress + ".");
			System.out.println("Error Sent to port: " + returnPort + ".");
		}

	}//end sendError

	/**
	 *
	 * @param packet
	 * @throws IOException
	 */
	public void verboseInformation() throws IOException {

			System.out.println("\nDestination: ");
			System.out.println("IP address: " + inetAddress);
			System.out.println("Port: " + returnPort);
			System.out.println("Information in this packet: ");
			//System.out.println("Block number: " + packet.blockNum);
			System.out.println("Data length: " + dataLen + "\n");
			return;

	}

/**
* Method that is designed to get mode of operation from data bytes.
*
* @param data[]
*/
	private String extractMode(byte[] data) {
		int i = 1;
		StringBuilder modeBuilder = new StringBuilder(); //string builder for filename
		while (data[++i] != 0) {
			//Ignore filename
		}
		while (data[++i] != 0) {
			modeBuilder.append((char) data[i]);
		}
		return modeBuilder.toString(); //filename is converted to a string
	}

	/**
	* Method designed to get the file name from data of bytes
	*
	* @param data[]
	*/
	private String extractFileName(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder(); //string builder for filename
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]); //append the filename in the correct data section
		}
		return filenameBuilder.toString(); //filename is converted to a string
	}

	public void shutdown(){
		System.out.println("IN");
		acceptConnection = false;
		while (true){
			try{
				Thread.sleep(4000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			System.out.println("Waiting for threads to close: " + threads.size() + " thread(s) still running: " + acceptConnection);
			//WAIT FOR THREADS TO CLOSE
			if (threads.size() == 0){
				System.exit(1);
			}
		}

	}

/**
* Method that starts the server.
*/
	public void run() {
		DatagramPacket packet = new DatagramPacket(new byte[MAX_LENGTH], MAX_LENGTH);
		String fileName = "TheTestFile.txt";

		outer:
		try{
			if (!isScanning()){
				try {
					scanning = true;
					waitForCommand();
				}catch(IOException e){
					e.printStackTrace();
				}
			}else{

			

				if (acceptConnection){
					try{
						Thread.sleep(4000);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
					socket.receive(packet);
					returnPort = packet.getPort();
					inetAddress = packet.getAddress();
					dataLen = packet.getLength();


					if (verbose){
						try{
							verboseInformation();
						}catch(IOException e){
							System.out.println("");
						}
					}

					int newPort;
					boolean usedPort = false;
					do{
						Random rand = new Random();


		        newPort = (rand.nextInt(6000) + 3000);

						usedPort = false;
						for (RequestHandler t: threads){
							if (t.getPort() == newPort){
								usedPort = true;
								break;
							}
						}

					}while (usedPort);

					if (packet.getData()[1] == 5){
						System.out.println("Received an error packet");
						break outer;
					}

					String modeName = extractMode(packet.getData());
					System.out.println("MODE: " + modeName.toLowerCase());
					if(!modeName.toLowerCase().equals("netascii")){
						System.out.println("Sending Error");
						sendError(4);
						break outer;
					}if (packet.getData()[1] != 1 && packet.getData()[1] != 2){
							System.out.println("Ignoring duplicate packet");
							sendError(4);
					}else{
						System.out.println("HIT HERE");
						String file = extractFileName(packet.getData());
						boolean inUse = false;
						for (String s: currentFiles){
							if (s.equals(file)){
								System.out.println("Cannot access file, file in use");
								inUse = true;
								sendError(2);
							}
						}
						if (!inUse){
							currentFiles.add(file);
							RequestHandler newClient = new RequestHandler(this, packet, packet.getAddress(), newPort);
							threads.add(newClient);
							newClient.start();
						}
					}
				}else{
					System.out.println("\n\n\n\nRefusing connection!\n\n\n\n");
				}
			}
		}catch(IOException e){
			System.out.println("Catch exception on socket receive\n");
		}
	}

	/**
	* Method designed to check if the server is scanning.
	*
	*/
	public synchronized boolean isScanning(){
		return scanning;
	}

	/**
	* Method designed to add thead.
	*
	*/
	public void addThread(){
		threadCount += 1;
	}


	/**
	* Method designed to remove thread.
	*
	*/
	public void removeThread(RequestHandler r){
		threadCount -= 1;
		threads.remove(r);
	}

	/**
	* Method designed get thread count.
	*
	*/
	public int getThreadCount(){
		return threadCount;
	}

	/**
	 *
	 * @param args
	 */
public static void main(String[] args) {

		//Create a thread to accept user input
		Server server = new Server();
		Thread input = new Thread(server);
		input.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e ) {
			e.printStackTrace();
			System.exit(1);
		}

		while (acceptConnection){
			server.run();
		}
	}
}
