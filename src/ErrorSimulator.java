import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 * @author Birat & Brandon & Caleb
 *
 */
public class ErrorSimulator implements Runnable {

	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	private int maxLen = 512;
	DatagramSocket receiveSocket, sendReceiveSocket;
	private static final int Default_Server_Port = 6900; //Port 69 is deemed as the default server port to be used
	private InetAddress clientAddress = null; //client address is assigned to null
	private InetAddress serverAddress = null;
	private int clientPort; //the client port that sends packets
	private int serverPort = 6900; //the port on the server that receives packets
	private int serverResponsePort= 0;
	private int blockNumber;
	private boolean listeningToServer = false;
	private DatagramPacket packet;
	private boolean duplicatePacket = false;
	private boolean corruptedRequest = false;
	private int timeout = 0;


	//Variable to select what packet to corrupt
	private boolean corruptDataPacket = false;
	private boolean corruptAckPacket = false;
	private boolean corruptRequestPacket = false;
	private boolean lose = false;

	private int corruptableBlockNum;

	static int i=1;
	/**
	 * Constructor for ErrorSimulator
	 */
	public ErrorSimulator() {
		try {
			receiveSocket = new DatagramSocket(2300);
			sendReceiveSocket = new DatagramSocket();
			blockNumber = 0;
		}catch(SocketException e){
			e.printStackTrace();
			System.exit(1);
		}
	}//end constructor ErrorSimulator()

	/**
 	* Method to check if the given packet is an aknowledgment packet.
	*
 	* @param packet
 	* @return
 	*/
	private boolean isAckPacket(DatagramPacket packet) {
		byte [] data = packet.getData();
		return data[0]==0 & data[1]==4;
	}//end isAckPacket

	/**
	 * Check if it is read request.
	 *
	 * @param data
	 * @return
	 */
	public boolean isReadRequest(byte[] data) {
		return data != null && data[1] == 1;
	}//end isReadRequest

	/**
	 * Check if is it write request.
	 *
	 * @param data
	 * @return
	 */
	public boolean isWriteRequest(byte[] data) {
		return data != null && data[1] == 2;
	}//end isWriteRequest

	/**
	 * Menu of the commands available to simulate by the user.
	 *
	 */
	private void commands(DatagramPacket p){

		if(p.getData()[1] == 5){
			System.out.println("\nEnter The Error You Wish To Simulate:");
			System.out.println("\nEnter 0: Corrupt the opcode - Error 4.");
			System.out.println("\nEnter 2: Force (data size > 512) - Error 4");
			System.out.println("\nEnter 3: Corrupt the tid - Error 5:");
			System.out.println("\nEnter 4: Delay the Packet");
			System.out.println("\nEnter 5: Duplicate the Packet");
			System.out.println("\nEnter 6: Lose the Packet");
			System.out.println("\nPress Enter to Send the Packet");
		}else{
			System.out.println("\nEnter The Error You Wish To Simulate:");
			System.out.println("\nEnter 0: Corrupt the opcode - Error 4.");
			System.out.println("\nEnter 1: Corrupt the block number -  Error 4.");
			System.out.println("\nEnter 2: Force (data size > 512) - Error 4");
			System.out.println("\nEnter 3: Corrupt the tid - Error 5:");
			System.out.println("\nEnter 4: Delay the Packet");
			System.out.println("\nEnter 5: Duplicate the Packet");
			System.out.println("\nEnter 6: Lose the Packet");
			System.out.println("\nEnter 7: Change mode (netascii - octet)");
			System.out.println("\nPress Enter to Send the Packet");
		}


	}//end commands


	/**
	 * Menu of the commands to use to select the corruptable packet
	 *
	 */
	private void packetchoices(){

		System.out.println("\nEnter The Error You Wish To Simulate with the packet:");
		System.out.println("\nEnter 0: Corrupt the ACK Packet");
		System.out.println("\nEnter 1: Corrupt the Data Packets");
		System.out.println("\nEnter 2: Corrupt the WRQ/RRQ Packets");

		System.out.println("\nPress Enter to Continue Normally");
	}//end commands

	private void chooseInetAddress(){
		Scanner input = new Scanner(System.in);
		while(true){
			System.out.println("Press Enter to Initiate Local File Transfer or Enter a Known IP address.\n");
			String ip = input.nextLine(); //currenlty implemented for static ip
			if(ip.isEmpty()){
				try{
					serverAddress = InetAddress.getLocalHost();
					System.out.println("Local File Transfer Selected.\n");
					System.out.println("Sending to IP: "+ serverAddress + "\n");
					}catch(IOException e1){
						e1.printStackTrace();
					}
				break;
			}
			try {
				serverAddress = InetAddress.getByName(ip);
				System.out.println("Sending to IP: "+ serverAddress + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}//end while
	}

	/**
	 * Method designed to get the file name from a byte of data sent.
	 * Sets filename
	 *
	 * @param data
	 */
	private String extractFileName(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder(); //instantiate a new StringBuilder for the filename
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]); //append the filename in the respective area of the data
		}//end while
		return filenameBuilder.toString(); //convert the filename obtained to a string
	}//end extractFileName

	/**
	 * Method to choose which time of error we should simulate on the packet.
	 *
	 * @param packet
	 */
	private void choosePacketToCorrupt(){
		try{
			if(serverAddress == InetAddress.getLocalHost() || serverAddress == null){
					chooseInetAddress();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		packetchoices();
		Scanner input = new Scanner(System.in);
		String cmdLine = input.nextLine().toLowerCase(); // convert all command into lower case

		String[] command = cmdLine.split("\\s+");; // split all command into array of command

		if (command.length == 0 && packet.getData()[1] != 5){ //if no command received
			packetchoices();
		}
		corruptDataPacket = false;
		corruptRequestPacket = false;
		corruptAckPacket = false;
		String packType = getPacketType(packet);
		if (packet.getData()[1] != 5){

			switch (command[0]){ //if command received

				case "exit":
					System.out.println("Terminating ErrorSim");
					input.close();
				case "0":
					corruptAckPacket = true;
					System.out.println("You've selected to corrupt an ACK Packet");//Tell the user what type of packet is currently in the errorSim
					System.out.println("What Block Number do you want to corrupt?: ");
					corruptableBlockNum = input.nextInt();
					break;
				case "":
					System.out.println("Continuing as Normal");
					break;
				case "1":
					corruptDataPacket = true;
					System.out.println("You've selected to corrupt a DATA Packet");//Tell the user what type of packet is currently in the errorSim
					System.out.println("What Block Number do you want to corrupt?: ");
					corruptableBlockNum = input.nextInt();
					break;
				case "2":
					corruptRequestPacket = true;
					corruptedRequest = true;
					System.out.println("You've selected to corrupt a Request Packet");//Tell the user what type of packet is currently in the errorSim
					corruptableBlockNum = 0;
					break;
					default:
				case "reset":
					System.out.flush();
					System.out.println("Incompatible Command, Please Make a Selection from the List.\n");
					packetchoices();
				}//end switch
			}
			System.out.println("Continuing");
	}//end chooseError

	/**
	 * Method to choose which time of error we should simulate on the packet.
	 *
	 * @param packet
	 */
	private DatagramPacket chooseError(DatagramPacket packet){
		if(duplicatePacket ==true){
			duplicatePacket = false;
		}
		commands(packet);
		Scanner input = new Scanner(System.in);
		String cmdLine = input.nextLine().toLowerCase(); // convert all command into lower case

		String[] command = cmdLine.split("\\s+");; // split all command into array of command

		if (command.length == 0 && packet.getData()[1] != 5){ //if no command received
			commands(packet);
		}
		String packType = getPacketType(packet);

		switch (command[0]){ //if command received

			case "exit":
				System.out.println("Terminating ErrorSim");
				input.close();
				return packet;
			case "0":
				System.out.println("Chose to corruot OP code");
				packet = corruptOpCode(packet);
				break;
			case "":
				return packet;
			case "1":
				if (isWriteRequest(packet.getData()) || isReadRequest(packet.getData())){
					System.out.println("Error, no block number to currupt, please choose again");
					packet = chooseError(packet);
				}else{
					packet = corruptBlockNumber(packet);
				}
				break;
				case "2":
					packet = corruptDataSize(packet);
					break;
			case "3":
				packet = corruptTID(packet);
				System.out.println("TID: " + packet.getPort());
				break;
			case "4":
				System.out.println("Enter how long would you like the "+ packType+ " packet be delayed for?");
				int tDelay = input.nextInt();
				delay(tDelay);
			return packet;

			case "5":
				System.out.println("The current Packet is " + packType + ". Press Enter to Duplicate The Packet");//Tell the user what type of packet is currently in the errorSim
				String dFlow  = input.nextLine().toLowerCase();
				switch (dFlow){
					case "":
						//duplicate function
						duplicatePacket = true;
						break;
						default:

						return packet;
			}
			break;
			case "6":
				System.out.println("The current Packet is " + packType + ". Press Enter to Lose The Packet");//Tell the user what type of packet is currently in the errorSim
				String lFlow  = input.nextLine().toLowerCase();
				switch (lFlow){
					case "":
						lose = true;
						return null;
					}
				break;
			case "7":
				return switchRequestMode(packet);
			case "reset":
				System.out.flush();
			default:
				System.out.println("Incompatible Command, Please Make a Selection from the List.\n");
				commands(packet);
				return packet = chooseError(packet);
			}//end switch

			return packet;
	}//end chooseError


	public void delay(int tDelay){
		try{
			Thread.sleep(1000*tDelay);
		}catch(InterruptedException e){
			e.printStackTrace();
		}

	}

	public DatagramPacket switchRequestMode(DatagramPacket p){
		String fileName = extractFileName(p.getData());
		if (isWriteRequest(p.getData())){
			WRPacket WRQPack = new WRPacket("octet".getBytes(),fileName.getBytes(),serverAddress,serverPort);
			return WRQPack.getDatagram();
		}else{
			RPacket RRQPack = new RPacket("octet".getBytes(),fileName.getBytes(),serverAddress,serverPort);
			return RRQPack.getDatagram();
		}


	}


	 /**
	 * Method designed to find out get packet type.
	 *
	 * @param pType
	 */
	 public String getPacketType(DatagramPacket pType){
		 if(pType.getData()[1] == 1){
			 return "Read"; //packet type 1 is the read request packet
		 } else if(pType.getData()[1] == 2){
			 return "Write"; //packet type 2 is the write request packet
		 } else if(pType.getData()[1] == 3){
			 return "Data"; //packet type 3 is the data packet
		 } else if(pType.getData()[1] == 4){
			 return "Acknowledge"; //packet type 4 is the ack packet
		 }else {
			 return "Error";
		 }
	 }//end getPacketType

	/**
	 * Method designed corrupt the opCode
	 *
	 * @param packet
	 */
	public DatagramPacket corruptOpCode(DatagramPacket packet){
		System.out.println("\nOp Code Currently: " + packet.getData()[1]);
		Scanner scanner = new Scanner( System.in );
		System.out.println("\nEnter what you would like to change it to: ");
   		 String input = scanner.nextLine();
		byte data[] = new byte[512];
		data = packet.getData();
		int opCode = packet.getData()[1];
		try {
     	opCode = Integer.parseInt(input);
    }catch (NumberFormatException e) {
			System.out.println("Invalid Entry");
    }
		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		BAOS.write(0);
		BAOS.write(opCode);
		BAOS.write(getBlockNumFromPacket(packet)>>8);
		BAOS.write(getBlockNumFromPacket(packet));
		BAOS.write(data,4,data.length-4);
		packet = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, packet.getAddress(), packet.getPort());
		System.out.println("New OP Code is: " + packet.getData()[1] + " block num: " + packet.getData()[3]);
		return packet;
	}//end corruptOpCode

	/**
	 * Method designed to corrupt the block number in the packet.
	 *
	 * @param packet
	 */
	public DatagramPacket corruptBlockNumber(DatagramPacket packet){
		System.out.println("\nBlock Number Currently: " + getBlockNumFromPacket(packet));
		Scanner scanner = new Scanner( System.in );
		System.out.println("\nEnter what you would like to change it to: ");
   		 String input = scanner.nextLine();
		byte data[] = new byte[512];
		data = packet.getData();
		int blockNum = packet.getData()[3];
		try {
     	blockNum = Integer.parseInt(input);
   	}catch (NumberFormatException e) {
			System.out.println("Invalid Entry");
			packet = chooseError(packet);
   	}
		System.out.println("BLOCK: " + blockNum);
		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		BAOS.write(0);
		BAOS.write(packet.getData()[1]);
		BAOS.write(blockNum>>8);
		BAOS.write(blockNum);
		BAOS.write(data,4,data.length-4);
		packet = new DatagramPacket(BAOS.toByteArray(), data.length, serverAddress, serverPort);
		System.out.println("New Packet: " + getBlockNumFromPacket(packet));
		return packet;
	}//end cirruptBlockNumber

	/**
	 * Method designed to corrupt the data size in the packet.
	 *
	 * @param packet
	 */
	//TODO Add code to corrupt the packet TID
	public DatagramPacket corruptDataSize(DatagramPacket packet){
		byte[] data = new byte[520];

		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		BAOS.write(0);
		BAOS.write(packet.getData()[1]);
		BAOS.write(getBlockNumFromPacket(packet));
		BAOS.write(data,4,data.length-4);
		BAOS.write(0000000000000000000000000);
		packet = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, packet.getAddress(), packet.getPort());

		System.out.println("New length is: " + packet.getData().length);
		return packet;
	}//end corruptDataSize

	/**
	 * Method designed to corrupt the port id.
	 *
	 * @param packet
	 */
	//TODO Add code to corrupt the packet TID
	public DatagramPacket corruptTID(DatagramPacket packet){
		System.out.println("\nTID Currently: " + packet.getPort());
		Scanner scanner = new Scanner( System.in );
		System.out.println("\nEnter what you would like to change it to: ");
   		 String input = scanner.nextLine();
		int newPort = packet.getPort();
		try {
     	newPort = Integer.parseInt(input);
    }catch (NumberFormatException e) {
			System.out.println("Invalid Entry");
			packet = chooseError(packet);
   	}
		packet = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(), newPort);
		return packet;
	}//end corruptTID

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
	* Method designed to send error to the server.
	*
	* @param code
	*/

	private void sendServerError(int code){
		outer:
		try{
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

			DatagramPacket p = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, serverAddress,serverPort);
			p = chooseError(p);
			if(lose){
				lose = false;
				break outer;
			}
			System.out.println("Sending error");
			sendReceiveSocket.send(p);

			System.out.println("Error Sent to address: " + serverAddress + ".");
			System.out.println("Error Sent to port: " + serverPort + ".");
		}catch(IOException e){
			e.printStackTrace();
		}

	}//end sendServerError

	/**
	* Method desgined to send error to the client.
	*
	* @param code
	*/
	private void sendClientError(int code){
		outer:
		try{
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

			DatagramPacket p = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, clientAddress,clientPort);

				p = chooseError(p);
				if(lose){
					lose = false;
					break outer;
				}
				System.out.println("Sending error");
				sendReceiveSocket.send(p);

			System.out.println("Error Sent to address: " + clientAddress + ".");
			System.out.println("Error Sent to port: " + clientPort + ".");
		}catch(IOException e){
			e.printStackTrace();
		}

	}//end sendClientError

	/**
	 * Method designed to send packet to server after errors.
	 *
	 * @param sPacket
	 * @throws IOException
	 */
	private void sendServer(DatagramPacket sPacket, InetAddress sAddress, int sPort) throws IOException {

		DatagramPacket newPacket = new DatagramPacket(sPacket.getData(), sPacket.getLength(), sAddress, sPort);

		sendReceiveSocket.send(newPacket);
	}//end sendServer

	/**
	 * Method designed to send packet to server after errors.
	 *
	 * @param sendPacket
	 * @throws IOException
	 */
	private void sendClient(DatagramPacket sendPacket) throws IOException {
		sendPacket = new DatagramPacket(sendPacket.getData(), sendPacket.getLength(), clientAddress, clientPort);
		System.out.println("NUM: " + getBlockNumFromPacket(sendPacket));
		receiveSocket.send(sendPacket);
	}//end sendClient

	/**
	 * Method to receive the acknowledgement packet from the server.
	 *
	 * @param blockNumber
	 * @return
	 */
	private DatagramPacket receiveServerAck() {
		outer:
		try {
			if(lose){
				lose  = false;
				System.out.println("Losing server's ack");
				break outer;
			}
			DatagramPacket receivePacket = new DatagramPacket(new byte[4], 4);
			System.out.println("Waiting...");
			sendReceiveSocket.receive(receivePacket);
			serverPort = receivePacket.getPort();
			System.out.println("Received!: " + receivePacket.getData()[0] + receivePacket.getData()[1] + " Block Num: " + getBlockNumFromPacket(receivePacket)  + " length: " + receivePacket.getData().length + " address: " + receivePacket.getAddress() + " port: " + receivePacket.getPort());
			return receivePacket;
		} catch (IOException e) {
			System.out.println("Ack Packet not yet Received");
			return null;
		}
		return null;
	}//end receiveServerAck

	/**
	 * Method to receive the acknowledgement packet from the client.
	 *
	 * @param blockNumber
	 * @return
	 */
	private DatagramPacket receiveClientAck() {
		outer:
		try {
			if(lose){
				System.out.println("Losing client's ack");
				lose  = false;
				break outer;
			}
			DatagramPacket receivePacket = new DatagramPacket(new byte[4], 4);
			System.out.println("Waiting...");
			receiveSocket.receive(receivePacket);
			if (receivePacket.getData()[1] == 4){
				System.out.println("Received!: " + receivePacket.getData()[0] + receivePacket.getData()[1] + " Block Num: " + getBlockNumFromPacket(receivePacket) + " length: " + receivePacket.getData().length + " address: " + receivePacket.getAddress() + " port: " + receivePacket.getPort());
			}
			return receivePacket;
		} catch (IOException e) {
			System.out.println("Ack Packet not Received");
			return null;
		}
		return null;
	}//end receiveClientAck

	public void run() {
		byte data[] = new byte[512];
		packet = new DatagramPacket (data,data.length);
		System.out.println("\n\nError simlator is waiting for new requests.");
		outer:
		try {
			if(lose){
				lose = false;
				break outer;
			}
			//received a new request
			if (corruptedRequest){
				System.out.println("Ignoring duplicate request");
				corruptedRequest = false;
			}else{
				receiveSocket.receive(packet);
				if (isWriteRequest(packet.getData()) || isReadRequest(packet.getData())){
					System.out.println("Error simulator received new requests.");
					clientPort = packet.getPort();
					clientAddress = packet.getAddress();
					blockNumber = 0;
					System.out.println("This is the port number" + packet.getAddress());
					choosePacketToCorrupt();
					sendPacket(packet);
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	* Method desgined to get block number from packet.
	*
	* @param p
	*/
	private int getBlockNumFromPacket(DatagramPacket p){
		return ((p.getData()[2]<<8)&0xff00)|(p.getData()[3]&0xff);
	}

	/**
	 * Method designed to send the packets.
	 *
	 * @param packet
	 */
	public void sendPacket(DatagramPacket packet){
		if(packet!=null){
			byte data[] = packet.getData();

				serverPort = Default_Server_Port;

				System.out.println("Sending to server");
			outer:
			try {
				if(lose){
					lose = false;
					break outer;
				}
				DatagramPacket finalPacket = null;
				if (isReadRequest(packet.getData())) {
					String fname = extractFileName(packet.getData());
					RPacket newPacket = new RPacket("netascii".getBytes(),fname.getBytes(), serverAddress, serverPort);
					if (corruptRequestPacket){
						DatagramPacket dg = chooseError(newPacket.getDatagram());
						sendServer(dg, serverAddress, serverPort);
					}else{
						sendServer(newPacket.getDatagram(), serverAddress, serverPort);
					}
					handleReadRequest();
				}else if(isWriteRequest(packet.getData())){
					String fname = extractFileName(packet.getData());
					WRPacket newPacket = new WRPacket("netascii".getBytes(), fname.getBytes(), serverAddress, serverPort);
					if (corruptRequestPacket){
						DatagramPacket dg = chooseError(newPacket.getDatagram());
						sendServer(dg, serverAddress, serverPort);
					}else{
						sendServer(newPacket.getDatagram(), serverAddress, serverPort);
					}
					handleWriteRequest();
				}else{
					if (packet.getData()[1] == 5){
						sendServerError(packet.getData()[3]);
						break outer;
					}
					DatagramPacket newPacket = new DatagramPacket(packet.getData(), packet.getData().length, serverAddress, serverPort);
					sendServer(newPacket, serverAddress, serverPort);
					try{
						System.out.println("Waiting for packet...");
						DatagramPacket p = new DatagramPacket(new byte[maxLen], maxLen);
						sendReceiveSocket.receive(p);

						DatagramPacket newP = new DatagramPacket(p.getData(), p.getData().length, clientAddress, clientPort);

						System.out.println("Sending to client...");
						sendClient(p);
					}catch(IOException e){
						e.printStackTrace();
					}

				}
				//send the WRQ packet
			} catch (UnknownHostException e){
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		}else{
			return;
		}
	}//end sendPacket

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
	 * Method created to handler write request.
	 *
	 */
	public void handleWriteRequest(){
		InetAddress serverAddress = null;
		int bytesUsed = 0;

		serverPort = Default_Server_Port;
		DatagramPacket dataPacket = null;
		byte data[] = new byte[512];
		blockNumber = 0;
		int length;
		outer:
		try{
		do {
			//RECEIVE ACK FROM SERVER
			DatagramPacket ACK = null;

			DatagramPacket packet = null;
			DatagramPacket p = null;
			data = new byte[512];
			while(packet == null || isWriteRequest(packet.getData())){
				p = null;
				while(p == null){
					ACK = receiveServerAck();
					if(ACK != null){
						if (ACK.getData()[1] != 5 && corruptAckPacket && corruptableBlockNum == getBlockNumFromPacket(ACK)){
							p = chooseError(ACK);
						}else if (ACK.getData()[1] == 5){
							sendClientError(ACK.getData()[3]);
							break outer;
						}else{
							p = ACK;
						}
					}
				}

				serverPort = ACK.getPort();
				serverAddress = ACK.getAddress();

				//SEND ACK TO CLIENT

				if(!lose){
					sendClient(p);
					System.out.println("Sending packet...");
					if (duplicatePacket){
						System.out.println("Sending duplicate...");
						sendClient(p);
					}

					blockNumber += 1;
				}else{
					System.out.println("Losing Packet...");
					lose  = false;
				}

				//Receive DATA from CLIENT
					packet = new DatagramPacket(data,data.length);
					try {
						//received a new request
						if(!lose){
							receiveSocket.receive(packet);
						}else{
							System.out.println("Losing Packet...");
							lose = false;
						}
						if (packet.getData()[1] == 5){
							sendServerError(packet.getData()[3]);
							break outer;
						}

						System.out.println("Received DATA from Client.");
						System.out.println("Type: " + packet.getData()[1] + " of length: " + packet.getData().length + " or " + packet.getLength() + " blockNumber " + getBlockNumFromPacket(packet));
						System.out.println("DATA CORRUPTION: " + corruptableBlockNum + " vs " + getBlockNumFromPacket(packet));
						if (corruptDataPacket && corruptableBlockNum == getBlockNumFromPacket(packet)){
							packet = chooseError(packet);
						}

					}catch (IOException e) {
						e.printStackTrace();
					}
				}

				data = packet.getData();

				System.out.println("Sending DATA Blk#: " + getBlockNumFromPacket(packet) + " to Server...");

				//SEND DATA TO SERVER
				try{
					if (data[1] == 5){
						sendServerError(data[3]);
						break outer;
					}

					sendServer(packet, serverAddress, serverPort);
					if (duplicatePacket){
						System.out.println("Sending duplicate...");
						sendServer(packet, serverAddress, serverPort);
					}

				}catch(IOException e){
					e.printStackTrace();
				}

				System.out.println("LENGTH: " + packet.getLength() + " vs " + packet.getData().length + " vs " + maxLen);
				length = packet.getLength();
			}	while(length == maxLen);
			System.out.println("BROkE OUT: " + packet.getLength() + " vs " + maxLen);

			//WAIT FOR SERVER'S ACK
			DatagramPacket response = receiveServerAck();

			if (response.getData()[1] == 5){
				sendClientError(response.getData()[3]);
			}else{

				//SEND ACK TO CLIENT
				DatagramPacket newResponse = null;
				while (newResponse == null){
					try {
						System.out.println("Sending Ack Packet of blknum: " + getBlockNumFromPacket(response) + " of length " + " to the Client.");
						if (corruptAckPacket && corruptableBlockNum == getBlockNumFromPacket(response)){
							newResponse = chooseError(response);
						}else{
							newResponse = response;
						}
						if (newResponse != null){
							if (newResponse.getData()[1] != 5){
								sendClient(newResponse);
								System.out.println("Sending packet...");
								if (duplicatePacket){
									System.out.println("Sending duplicate...");
									sendClient(newResponse);
								}

							}else{
								sendClientError(newResponse.getData()[3]);
							}
						}
						blockNumber = 0;
						serverPort = Default_Server_Port;
					} catch (UnknownHostException e){
						e.printStackTrace();
					}
				}
			}
		}catch (IOException e){
			System.out.println("");
		}
	}//end handleWriteRequest

	/**
	 * Method created to handle read request.
	 */
	public void handleReadRequest(){

		InetAddress serverAddress = null;
		byte[] data = new byte[512];
		blockNumber = 1;

		serverPort = Default_Server_Port;
		outer:
			do{
				try {

					//Receive DATA from SERVER
					packet = new DatagramPacket(data,data.length);
					sendReceiveSocket.receive(packet);
					if (corruptDataPacket && corruptableBlockNum == blockNumber){
						packet = chooseError(packet);
					}

					timeout = 0;
					while (packet == null){
						packet = new DatagramPacket(data,data.length);
						sendReceiveSocket.receive(packet);
						if (corruptDataPacket && corruptableBlockNum == blockNumber){
							packet = chooseError(packet);
						}
						timeout += 1;
						if (timeout == 4){
							sendServerError(5);
							break outer;
						}
					}
					timeout = 0;

					serverAddress = packet.getAddress();
					serverPort = packet.getPort();
					System.out.println("Received DATA from Server.");
					System.out.println("Type: " + packet.getData()[1] + " of length: " + packet.getLength());
					data = packet.getData();
				}catch (IOException e) {
					e.printStackTrace();
				}

				//SEND DATA TO CLIENT
				try{
					DatagramPacket dataPacket = new DatagramPacket(data, packet.getLength(), clientAddress, clientPort);

					if(!lose){
						if (packet.getData()[1] == 5){
							sendClientError(packet.getData()[3]);
							break outer;
						}
						System.out.println("Sending DATA to Client: " + dataPacket.getData()[1] + " of length: " + dataPacket.getLength());
						if (duplicatePacket){
							sendClient(dataPacket);
						}

						sendClient(dataPacket);
					}else{
						System.out.println("Losing Packet...");
						lose  = false;
					}
				}catch(IOException e){
					e.printStackTrace();
				}


			//RECEIVE ACK FROM CLIENT
			DatagramPacket ACK = null;
			DatagramPacket p = null;
			while (p == null){
				ACK = receiveClientAck();
				if (ACK != null){
					if(corruptAckPacket && corruptableBlockNum == getBlockNumFromPacket(ACK)){
						p = new DatagramPacket(ACK.getData(), ACK.getLength(), serverAddress, serverPort);
						p = chooseError(p);
					}else{
						p = ACK;
					}
				}

			}

			//SEND ACK TO SERVER
			try{

				if (ACK != null && ACK.getData()[1] == 5){
					System.out.println("Sending error to server");
					sendServerError(p.getData()[3]);
					break outer;
				}
				if (duplicatePacket){
					sendServer(p, serverAddress, p.getPort());
				}
				sendServer(p, serverAddress, serverPort);
			}catch(IOException e){
				e.printStackTrace();
			}
			blockNumber += 1;
			}while(packet.getLength() == maxLen);

	}//end handleReadRequest()

	public static void main(String [] args) {
		ErrorSimulator es = new ErrorSimulator();
		while (true){
			es.run();
		}
	}//end main
}//end ErrorSimulator
