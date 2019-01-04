import java.io.*;
import java.net.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 *
 * @author Birat & Caleb & Brandon
 *
 */
public class Client{

	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"); //instantiate the date

	private enum clientMode {test,normal;} //define the modes of operation

	private int maxLen = 512;
	private DatagramPacket receivePacket; //create the ability for a receive packet
	private DatagramSocket sendReceiveSocket; //create the ability for a send receive socket
	private String modes = ("netascii"); //netascii
	private static final int Default_Server_Port = 6900; //the port on the serve that is receives to
	private static final int requestPort = 6901; //the requesting port
	private static final int Default_error_sim_port = 2300; //the port on the error simulator that receives
	private static clientMode currentMode;
	private static InetAddress connectAddress;
	private static int connectPort;
	private static final String defaultFoler = System.getProperty("user.dir")  + File.separator + "client_files"; //get directory
	private String folder = defaultFoler;
	private boolean sendDuplicate = false;
	private boolean verboseMode = false;	//default mode is quiet
	private int timeout = 0;
	/**
	 *	Constructor for client class.
	 *
	 * @throws UnknownHostException
	 */
	public Client() throws UnknownHostException{

		this(connectAddress, Default_Server_Port);

		try{
			sendReceiveSocket = new DatagramSocket(); // open socket that will be used to send and receive on the client
			sendReceiveSocket.setSoTimeout(4000); //define a time out for the socket itself
		} catch(SocketException e){
			e.printStackTrace();
			System.exit(1);
		}
	}//end constructor client

	/**
	 *	Constructor for client class.
	 *
	 * @param connectAddress
	 * @param connectPort
	 */
	public Client(InetAddress connectAddress, int connectPort) {
		Client.currentMode = clientMode.normal; //set the current mode as normal
		Client.connectAddress = connectAddress; //current address defined as the connectAddress
		Client.connectPort = connectPort;
	}// end constructor Client (multiple param)

	/**
	 *	The initial user interface menu.
	 */
	private static void clientMenu() {
		System.out.println("\nWelcome, Current Mode is "+currentMode+". Now,sending requests to Port "+connectPort);
		System.out.println("Try the following commands:");
		System.out.println("1. menu - show the menu");
		System.out.println("2. exit - stop the client");
		System.out.println("3. mode - switch Print Mode(Verbose,Quiet). Default is quiet.");
		System.out.println("4. switch - switch Client mode");
		System.out.println("5. reset - This will clear the command window");
		System.out.println("6. read <filename> - send RRQ(i.e. read text.txt)");
		System.out.println("7. write <filename> - send WRQ(i.e. write text.txt)\n");
	}//end clientMenu

	/**
	 * Method designed to choose test or normal mode.
	 */
	public void chooseClientMode () throws IOException{
		Scanner input = new Scanner(System.in);
		if (currentMode==currentMode.test) //define the current mode as test
		{

			while(true){
				System.out.println("Press Enter to Initiate Local File Transfer or Enter a Known IP address.\n");
				String ip = input.nextLine(); //currenlty implemented for static ip

				if(ip.isEmpty()){
					connectAddress = InetAddress.getLocalHost();
					System.out.println("Local File Transfer Selected.\n");
					System.out.println("Sending to IP: "+ connectAddress + "\n");
					break;
				}else {
					connectAddress = InetAddress.getByName(ip);

				}

				break;
			}//end while
			currentMode = clientMode.normal;
			connectPort = Default_Server_Port;
		}
		else if (currentMode==currentMode.normal)
		{
			currentMode = clientMode.test;
			connectPort = Default_error_sim_port;
			connectAddress = InetAddress.getLocalHost();
		}//end conditions
		System.out.println("ClientMode has been switched to: "+currentMode+". Now Sending to - port "+connectPort);
		System.out.println("Going back to Main Menu.");
	}//end chooseClientMode()

	/**
	 * Method designed to get folder.
	 *
	 * @return
	 */
	private String getFolder() {
		return folder;
	}//end getFolder

	/**
	 *	Method designed to get the file path.
	 *
	 * @param fileName
	 * @return
	 */
	private String getFilePath(String fileName) {
		return getFolder()+fileName;
	}//end file path


	/**
	 *	Method that handles any user interaction.
	 *
	 * @throws IOException
	 */
	public void interAction() throws IOException{ //used for user input

		Scanner input = new Scanner(System.in);

		while(true){
				System.out.println("Press Enter to Initiate Local File Transfer or Enter a Known IP address.\n");
			String ip = input.nextLine(); //currenlty implemented for static ip
			modes = ("netascii");
			if(ip.isEmpty()){
				connectAddress = InetAddress.getLocalHost();
				System.out.println("Local File Transfer Selected.\n");

				if (verboseMode){
					System.out.println("Sending to IP: "+ connectAddress + "\n");
				}

				break;
			}else {
				connectAddress = InetAddress.getByName(ip);
			}
			break;
		}//end while

		while(true){
			clientMenu();
			System.out.print("Command: ");
			String cmdLine = input.nextLine().toLowerCase(); // convert all command into lower case
			String[] commands = cmdLine.split("\\s+"); // split all command into array of command

			if (commands.length == 0)
				continue;

			switch (commands[0]){
				case "menu":
					clientMenu();
					continue;
				case "exit":
					System.out.println("Terminating Client");
					input.close();
					return;
				case "read":
					if (commands.length != 2) {
						System.out.println("FileName not Valid, also include file type. "+ "(i.e. read text.txt)\n");
					}
					System.out.println("Read Request Selected.\n");

					readfromServer(commands[1]);

					break;

				case "write":
					if (commands.length != 2) {
						System.out.println("FileName not valid, also include file type. "+ "(i.e. write test.txt)\n");
					}

					System.out.println("Write Request Selected.\n");

					writetoServer(commands[1]);
					break;
				case "switch":
					sendReceiveSocket.setSoTimeout(10000);
					chooseClientMode();
					continue;
				case "reset":
					System.out.flush();
					continue;
				case "mode":
					if (verboseMode){
						verboseMode = false;
					}else{
						verboseMode = true;
					}
					System.out.println("The client is now running in verbose mode.");
					continue;
				default:
					System.out.println("Incompatible Command, Please Make a Selection from the List.\n");
				}
		}//end while
	}//end interAction

/**
* Method desgined to duplicate the packets.
*/
	private void duplicatePacket(){
		if(sendDuplicate == false){
			sendDuplicate = true;
		} else if(sendDuplicate == true){
			sendDuplicate = false;
		}
	}//end duplicatePacket

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

		DatagramPacket p = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, connectAddress, connectPort);

		try{
			System.out.println("Sending error");
			sendReceiveSocket.send(p);
		}catch(IOException e){
			e.printStackTrace();
		}
		if(verboseMode == true){
			System.out.println("Error Sent to address: " + connectAddress + ".");
			System.out.println("Error Sent to port: " + connectPort + ".");
		}
	}//end sendError

	/**
	* Method designed to check the error in packet.
	*
	* @param p
	*/
	private boolean checkError(DatagramPacket p){
		if (p.getData()[1] == 5){
			System.out.println("The error packet message was: " + getErrorMsg(p.getData()[3]));
			return true;
		}else{
			return false;
		}
	}//end checkError

	/**
	 * Method to receive Ackknowledgement Packets.
	 *
	 * @param blockNumber
	 * @return
	 */
	private DatagramPacket receiveAck() {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[4], 4); //define the ack packet receiving DatagramPacket
			if (verboseMode){
				System.out.println("Waiting to receive ackPacket...");
			}
			sendReceiveSocket.receive(receivePacket); //Receive ACK Packet
			System.out.println("BLOCK NUM: " + getBlockNumFromPacket(receivePacket));
			return receivePacket; //Force the packet to be AckPacket
		} catch (IOException e) {
			System.out.println("Ack Packet not Received"); // if ack not received we need to re-send the last packet
			return null; //the ack Packet is not received

		}
	}//end receiveAck

	/**
	 * Method to send read request.
	 *
	 * @param sendR
	 * @throws IOException
	 */
	private void sendR(DatagramPacket sendPacket) throws IOException {
		if (verboseMode){
			System.out.println("Sending request...");
			System.out.println("Packet: " + sendPacket.getData()[1] + " " + sendPacket.getPort());
		}

		sendReceiveSocket.send(sendPacket);
	}//end sendR

	/**
	* Method desgined to get block number from packet.
	*
	* @param p
	*/
	private int getBlockNumFromPacket(DatagramPacket p){
		return ((p.getData()[2]<<8)&0xff00)|(p.getData()[3]&0xff);
	}

	/**
	 * Method designed to receive dataPacket.
	 * @param blockNumber
	 * @return
	 */
	private DatagramPacket receiveData() {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[maxLen], 512);

			System.out.println("Waiting to receive DataPacket...");
			sendReceiveSocket.receive(receivePacket); //receive the packet through the sendReceiveSocket
			return receivePacket;

		} catch (IOException e) {
			System.out.println("DataPacket Not Received");
			return null;
		}
	}//end receiveData

	/**
	* Method designed to get the file name from data of bytes
	*
	* @param data[]
	*/
	private void extractErrorMsg(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder(); //string builder for filename
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]); //append the filename in the correct data section
		}
	}

	private DatagramPacket makeAckPacket(int blockNumber, InetAddress address, int port){
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		ostream.write(0);
		ostream.write(4);
		ostream.write(blockNumber>>8);
		ostream.write(blockNumber);
		DatagramPacket ACK = new DatagramPacket(ostream.toByteArray(), ostream.toByteArray().length, address, port);
		return ACK;
	}

	/**
	 * Method designed to read file from server.
	 *
	 * @param fileName
	 */
	 public void readfromServer(String fileName)  {
 		String filePath = folder + File.separator +  fileName; //determine the filePath
 		File file = null; //set file as null
		String contents = "";
		outer:
 		try {
	 		FileOutputStream ostream = new FileOutputStream(filePath); //output stream for the file

	 		//create  RRQ
	 		RPacket RQPack = new RPacket("netascii".getBytes(),fileName.getBytes(),connectAddress,connectPort);
	 		DatagramPacket DGpacket = RQPack.getDatagram();

	 		sendR(DGpacket);

	 		DatagramPacket DATA = null;

	 		int blockNumber = 1;

	 		do {
	 			DATA = receiveData();
				if(DATA != null && getBlockNumFromPacket(DATA) == (blockNumber-1)){
					System.out.println("Ignoring duplicate b/c data!=null and getBlockNumFromPacket(DATA) == (blockNumber-1)");
					DATA = null;
				}
				timeout = 0;
				while (DATA == null){ // if no data recieved then we need to re send the read request
					if (blockNumber == 1){
						if(verboseMode == true){
							System.out.println("No data was received.");
							break outer;
						}
					}else{
						DatagramPacket ACK = makeAckPacket(blockNumber, connectAddress, connectPort);
						sendR(ACK);
					}
					DATA = receiveData();

					if (DATA != null){
						if(getBlockNumFromPacket(DATA) == (blockNumber-1)){
							System.out.println("Ignoring duplicate b/c DATA!=null");
							DATA = null;
						}
					}
					timeout += 1;
					if (timeout >= 4){
						break outer;
					}
				}
				timeout = 0;

	 			if(checkError(DATA)){ //check whether the received data is an error packet
	 				System.out.println("Error packet received");
					System.out.println("" + getErrorMsg(DATA.getData()[3]));
					//System.out.println(DATA[]);
	 				break outer;
	 			}
				if (verboseMode){
					System.out.println("Data received, block num: " + getBlockNumFromPacket(DATA) + " vs " + blockNumber + " and OP Code: " + DATA.getData()[1]);
				}

				if (getBlockNumFromPacket(DATA) != blockNumber){

					sendError(4); //error type 4 is sent
					break outer;
				}else if(DATA.getData()[1] != 3){

					sendError(4);
					break outer;
				}

				String bytesString = new String(DATA.getData(), 4, DATA.getLength()-4, "UTF-8");
				contents += bytesString;
				ostream.write(DATA.getData(), 4, DATA.getLength()-4);

				DatagramPacket ACK = makeAckPacket(getBlockNumFromPacket(DATA), connectAddress, DATA.getPort());
				sendR(ACK);
				if (verboseMode){
	 				System.out.println("Acknowledge Sent...");
				}
				blockNumber += 1;

				if (verboseMode){
					System.out.println("LENGTH: " + DATA.getLength() + " vs " + DATA.getData().length);
				}

	 		} while(DATA.getLength() == maxLen); //end do-while
				ostream.flush();

				System.out.println("\n" + contents + "\n");

				System.out.println("Done writing to file...");
				ostream.close();
 		} catch (SocketException e) {
 			System.out.println("Unable to Open Socket");
 			sendError(5);//Unknown TID
 			return;
 		} catch (FileNotFoundException e) {
 			System.out.println("Unable to Open file '"+fileName+"'");
 			sendError(1);//File not found
 			return;
 		} catch (IOException e) {
 			System.out.println("Error Reading file '"+fileName+"'");
 			sendError(2);//Access Violation
 			e.printStackTrace();
 			return;
 		}
 	}//end readfromServer

	/**
	 * Method designed to write to server.
	 *
	 * @param fileName
	 */
	public void writetoServer(String fileName)  {

		String filePath = folder + File.separator +  fileName;
		File file = null; //set the file as null but will be changed in this method

		outer:
		try {
			FileInputStream istream = new FileInputStream(filePath); //create a input stream by using the filepath it determine
			DatagramPacket DGpacket;

			//SEND WRQ TO SERVER

			//form WRQ
			WRPacket WRQPack = new WRPacket("netascii".getBytes(),fileName.getBytes(),connectAddress,connectPort);
			DGpacket = WRQPack.getDatagram(); //obtain the wrq packet
			if (verboseMode){
				System.out.println("Data " + DGpacket.getData() + " length: " + DGpacket.getData().length + " blockNum: " + getBlockNumFromPacket(DGpacket) + " address: " + DGpacket.getAddress() + " port: " + DGpacket.getPort());
			}

			sendR(DGpacket); // send the datagram packet

			System.out.println("WRQ Sent");

			int bytesUsed = 0;
			int blockNumber = 0;

			int totalSize = istream.available(); //determine the current space available
			byte[] data = new byte[totalSize]; //define the data array by using the current space on the stream available
			bytesUsed = istream.read(data);
			DatagramPacket dataPacket = null; //set dataPacket as null but will obtain later from receiving from method

			int tempBN;
			int tempBlock = blockNumber;

			DatagramPacket ACK = null;

			ACK = receiveAck();

			timeout = 0;
			while (ACK == null){ // if no data recieved then we need to re send the read request
				sendR(DGpacket); //This is the change
				ACK = receiveAck();
				timeout += 1;
				if (timeout >= 4){
					break outer;
				}
			}
			timeout = 0;

			if (verboseMode){
				System.out.println("Received!: " + ACK.getData() + " length: " + ACK.getData().length + " blockNum: " + getBlockNumFromPacket(ACK) + " address: " + ACK.getAddress() + " port: " + ACK.getPort());
			}

			if (ACK == null){
				System.out.println("Couldn't receive packet"); // re-send original packet if ack not received

			}else if (checkError(ACK)){
				System.out.println("\nError Packet Received, terminating process!");
				break outer;
			}

			if (ACK.getData()[1] != 4){

				sendError(4);
				break outer;
			}else if(getBlockNumFromPacket(ACK) != 0){

				sendError(4);
				break outer;
			}

			connectPort = ACK.getPort();

			blockNumber += 1;

			boolean ignored = false;
			do {

				if (bytesUsed != -1){
					int startingPoint = ((blockNumber-1)*(maxLen-4));
					ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
					BAOS.write(0);
					BAOS.write(3);
					BAOS.write(blockNumber>>8);
					BAOS.write(blockNumber);
					if (bytesUsed-startingPoint > maxLen-4){
						System.out.println("Start: " + startingPoint + " bytesUsed: " + bytesUsed);

						int end = (maxLen*blockNumber)-4;
						BAOS.write(data,startingPoint,(maxLen-4));
					}else{
						System.out.println("Start: " + startingPoint + " bytesUsed: " + bytesUsed);
						BAOS.write(data,startingPoint,bytesUsed-startingPoint);
					}

					dataPacket = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, connectAddress, connectPort);


					if (!ignored){
						sendR(dataPacket);
					}else{
						ignored = false;
					}

					if (verboseMode){
						System.out.println("Sending Data Packet with Block Num:" + getBlockNumFromPacket(dataPacket) + " of length: " + dataPacket.getLength());
					}

				}else{
					System.out.println("File you are trying to read is empty");
				}

				ACK = receiveAck();

				timeout = 0;
				while (ACK == null){ // if no data recieved then we need to re send the read request
					ACK = receiveAck();
					timeout += 1;
					if (timeout >= 4){
						break outer;
					}
				}
				timeout = 0;

				if (verboseMode){
					System.out.println("Received!: " + ACK.getData() + " length: " + ACK.getData().length + " blockNum: " + getBlockNumFromPacket(ACK) + " address: " + ACK.getAddress() + " port: " + ACK.getPort());
				}

				if (checkError(ACK)){
					//System.out.println("Message in error packet was:" + ACK.getMsg());
					System.out.println("\nReceived an error packet, terminating process!");
					break outer;
				}else if (ACK == null){

					System.out.println("Unable to receive packet.");
					// re-send original packet if unable to receive ack

				}else{

					tempBlock = getBlockNumFromPacket(ACK);
					if (tempBlock == blockNumber-1){
						ignored = true;
					}else if (tempBlock != blockNumber){

						sendError(4);
						break outer;
					}else if (ACK.getData()[1] != 4){

						sendError(4);
						break outer;
					}else{
						blockNumber += 1;
					}

				}

			}	while (dataPacket == null || dataPacket.getLength() == maxLen); //end do-while


			blockNumber = 0;
			istream.close();
			if (currentMode == clientMode.normal){
				connectPort = Default_Server_Port;
			}else{
				connectPort = Default_error_sim_port;
			}

		} catch (SocketException e) {
			System.out.println("Socket Not Created");
			sendError(5);//Unknown TID
			return;
		} catch (FileNotFoundException e) {
			System.out.println("Error Writing File '"+fileName+"', file not found.");
			return;
		} catch (IOException e) {
			System.out.println("Request not Sent");
			sendError(2);//Access Violation
			e.printStackTrace();
			return;
		}
	}// end writetoServer


	/**
	 * Main
	 *
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) throws UnknownHostException{
		Client c = new Client();
		System.out.println("TFTP Client is Functional...\n");

		try {
			c.interAction();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//end main

}// end client class
