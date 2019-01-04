import java.net.DatagramSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.Arrays;

public class RequestHandler extends Thread {
	public static int TFTP_LISTEN_PORT = 6900; // default port
	public static final int maxLen = 512; // maximum packet length
	public static final int MIN_LENGTH = 4; // minimum packet length
	private Server server; // server that this listener is working for
	private InetAddress address; // client address
	private int port; // client port
	private String fileName; // filename of the request
	private DatagramSocket socket;
	private DatagramPacket packet;
	private byte[] data; // packet data
	private int blockNumber = 0; //start from the first block number (i.e. 00) later iterated
	private static final String folder = System.getProperty("user.dir")  + File.separator + "server_files";
	private int timeout = 0;
	private boolean verboseMode = false;

	/**
	* Constructor for the class request hadnler.
	* @param server
	* @param Packet
	* @param address
	* @param port
	*
	*/
	public RequestHandler(Server server, DatagramPacket packet, InetAddress address, int port) {
		this.server = server;
		this.address = address;
		this.port = packet.getPort();
		this.packet = packet;

		this.data = packet.getData();
		TFTP_LISTEN_PORT = port;
		this.extractFileName(data);


		try{
			socket = new DatagramSocket(TFTP_LISTEN_PORT); //create the socket to handle the WRQv
			socket.setSoTimeout(7000);
		} catch (SocketException e){
			e.printStackTrace();
		}

	}//end constructor

	/**
	* Getter method designed to get the port number.
	*/
	public int getPort(){
		return port;
	}//end getPort

	/**
	* Method designed to get the file name from data of bytes
	*
	* @param data[]
	*/
	private void extractFileName(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder(); //string builder for filename
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]); //append the filename in the correct data section
		}
		fileName = filenameBuilder.toString(); //filename is converted to a string
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
	*	Method designed to send the ack packet.
	*
	* @param blockNum
	*/
	private void sendAckPacket(int blockNum) throws IOException {
		try {
			DatagramPacket somethingPacket = makeAckPacket(blockNum,address,port); //instantiate the ACK packet
			System.out.println("Block #: " + blockNum + " and " + getBlockNumFromPacket(somethingPacket));
			socket.send(somethingPacket); //send the ack packet that was just created
		} catch (IOException e){
			e.printStackTrace();
			System.out.println("Error sending packet");
		}
	}//end sendAckPacket

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

		DatagramPacket p = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, address, port);

		try{
			System.out.println("Sending error");
			socket.send(p);
		}catch(IOException e){
			e.printStackTrace();
		}
	}//end sendError

/**
* Method designed to check the error in the packet.
*
* @param p
*/
	private boolean checkError(DatagramPacket p){
		if (p.getData()[1] == 5){
			System.out.println(getErrorMsg(p.getData()[3]));
			return true;
		}else{
			return false;
		}
	}//end checkError


	private DatagramPacket receiveData() {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[maxLen], maxLen);
			System.out.println("Waiting for data...");
			socket.receive(receivePacket); //receive the packet through the socket
			System.out.println("SIZE: " + receivePacket.getData().length + " Block: " + getBlockNumFromPacket(receivePacket));
			return receivePacket; //return the DataPacket
		} catch (IOException e) {
			System.out.println("DataPacket Not Received");//WE NEED TO DECIDE WHAT WE SEND BACK INSTEAD OF HAVING AN ERROR SENT
			return null;
		}
	}//end receiveData

	/*
	* Method desgined to handle incoming request.
	*
	*/
	private void handleRequest() {
		blockNumber = 0;
		if(data[1] == 1){ //1 is for a read request
			System.out.println("Read Request received...");
			try{
				extractFileName(data); //determine the filename of the data portion of the packet
				handleReadRequest(fileName); //utilise the handleReadRequest method in this class
			}catch (IOException e){
				e.printStackTrace();
			}

		}else if(data[1] == 2){
			System.out.println("Write Request received...");
			//Then handle the data
			handleWriteRequest();
		}else if(data[1] == 3 && data[3] != 0){
			System.out.println("Ignoring Duplicate Packet");
			return;
		}else if(data[1] == 3){
			sendError(4);
			return;
		}else if(data[1] == 4 && data[3] != 0){
			System.out.println("Ignoring Duplicate Packet");
			return;
		}else if(data[1] == 4){
			sendError(4);
			return;
		}else if(data[1] == 5 && data[3] != 0){
			System.out.println("Ignoring Duplicate Packet");
			return;
		}else if(data[1] == 5){
			System.out.println("Received an Error!");
		}else {
			sendError(4);
			return;
		}
	}//end handleRequest

	/**
	 * Send the request
	 *
	 * @param packet
	 * @throws IOException
	 */
	private void sendRequest(DatagramPacket packet) throws IOException {
		socket.send(packet);
	}//end sendRequest

	private int getBlockNumFromPacket(DatagramPacket p){
		data = p.getData();
		return ((data[2]<<8)&0xff00)|(data[3]&0xff);
	}

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

	 //Handle WRQ
	 /*
	 * Method designed to hadnle handle write request.
	 */
	private void handleWriteRequest() {

		File file = null;
		String filePath = folder + File.separator +  fileName; //define file to write on the server side
		file = new File(filePath); //create a new file on the side of the server
		blockNumber = 0;

		outer:
		try{
			FileOutputStream fs = new FileOutputStream(filePath); //stream that will output the file contents
			if(!file.canRead() && file.exists()){
				sendError(2);
				fs.close();
				return;
			}

		DatagramPacket receivePacket = null; //initially set to null but later defined in the method
		ByteArrayOutputStream b; //define a new byte array output stream

		sendAckPacket(blockNumber);

		if (verboseMode){
			System.out.println("Sending ACK Packet: " + blockNumber);
		}
		blockNumber += 1;
		do {
			verboseMode = server.getMode();
			try {
				receivePacket = null; //create the ability to receieve a datapacket of max length 512 bytes

				receivePacket = receiveData();
				timeout = 0;
				while (receivePacket == null || isWriteRequest(receivePacket.getData())){
					sendAckPacket(blockNumber-1);
					receivePacket = receiveData(); //receive the data that is written to the server
					timeout += 1;
					if(timeout >= 4){
						sendError(5);
						break outer;
					}
				}
				timeout = 0;

				if(checkError(receivePacket)){
					System.out.println("Error packet received");
					break outer;
				}

				int tempBlock = getBlockNumFromPacket(receivePacket);

				if (verboseMode){
					System.out.println("Data received: " + receivePacket.getLength() + " Block Number: " + getBlockNumFromPacket(receivePacket) + " expecting: " + blockNumber);
				}

				if (getBlockNumFromPacket(receivePacket) == blockNumber-1){
					System.out.println("ignoring");
				}else if (getBlockNumFromPacket(receivePacket) != blockNumber){ //if the block number on the data packet does not match
					sendError(4); //error type 4 is sent
					break outer;
				}else{

					if (receivePacket.getData()[1] != 3){
						sendError(4); //error type 4 is sent
						break outer;
					}
					sendAckPacket(blockNumber);
					if (verboseMode){
						System.out.println("Sent ACK Packet: " + blockNumber);
					}

					if (verboseMode){
						System.out.println("PACKET WITH DATA: " + getBlockNumFromPacket(receivePacket));
					}
					fs.write(receivePacket.getData(), 4, receivePacket.getLength()-4);
					blockNumber += 1;
				}
			} catch (IOException e) {
				System.out.println("Could Not Get Data");
			}

		}while(receivePacket.getLength() == 512); //while the packets length that is received is the 512 byte size (maximum)

		blockNumber = 0;

		try {
			server.verboseInformation(); //print out the information on the server side (verbose mode)
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			System.out.println("This file is now being written to " + fileName); //print out the current packet's data length that is present

			fs.flush();
			fs.close();

		}catch(FileNotFoundException e){
			e.printStackTrace();
			sendError(1);
		}catch(IOException e){
			e.printStackTrace();
			sendError(2);
		}

	}//end handleWriteRequest

	/**
	* Method designed to handle read request.
	* @param fileName
	*/
	private void handleReadRequest(String fileName) throws IOException{
		outer:
		try {
			File file = null;

			String filePath = folder + File.separator + fileName;
			FileInputStream istream = new FileInputStream(filePath);

			int dataLength = 0;
			int bytesUsed = 0;
			int blockNumber = 1;

			byte[] data = new byte[istream.available()]; //create the ability to hold 512 bytes of data
			DatagramPacket dataPacket = null; //file contents initially set to null (to be updated during the progression of read request)
			bytesUsed = istream.read(data);
			do {
				verboseMode = server.getMode();
				if (bytesUsed != -1){
					try {
						int startingPoint = ((blockNumber-1)*(maxLen-4));
						ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
						BAOS.write(0);
						BAOS.write(3);
						BAOS.write(blockNumber>>8);
						BAOS.write(blockNumber);
						if (bytesUsed-startingPoint > maxLen-4){
							int end = (maxLen*blockNumber)-4;
							if (verboseMode){
								System.out.println("Starts: " + startingPoint + " Ends: " + end);
							}

							BAOS.write(data,startingPoint,(maxLen-4));
						}else{
							BAOS.write(data,startingPoint,bytesUsed-startingPoint);
						}

						dataPacket = new DatagramPacket(BAOS.toByteArray(), BAOS.toByteArray().length, address, port);
						socket.send(dataPacket);
						if (verboseMode){
							System.out.println("Sent File - Block Num: " + getBlockNumFromPacket(dataPacket) + " and OP Code: " + dataPacket.getData()[1]);
						}
					} catch (IOException e) {
						System.out.println("Couldn't send data");
						e.printStackTrace();
					}
				}else{
					System.out.println("Error Encountered");
					//create the error packet now
					sendError(1);
				}

				DatagramPacket receivePacket = null;
				try {
					while (receivePacket == null){
						receivePacket = new DatagramPacket(new byte[4], 4);
						socket.receive(receivePacket);
						if(checkError(receivePacket)){
							System.out.println("Error packet received");
							istream.close();
							break outer;
						}

						if (getBlockNumFromPacket(receivePacket) == blockNumber-1){
							System.out.println("");
						}else if (getBlockNumFromPacket(receivePacket) != blockNumber){
							System.out.println("BLOCK: " + getBlockNumFromPacket(receivePacket) + " vs "+ blockNumber);
							sendError(4);
							istream.close();
							break outer;
						}else if (receivePacket.getData()[1] != 4){
							System.out.println("OPCODE");
							sendError(4);
							istream.close();
							break outer;
						}else{
							if (verboseMode){
								System.out.println("Received!: OP Code: " + receivePacket.getData()[1] + " length: " + receivePacket.getData().length + " blockNum: " + getBlockNumFromPacket(receivePacket) + " address: " + receivePacket.getAddress() + " port: " + receivePacket.getPort() + "\n");
							}
							blockNumber += 1;
						}
					}
				} catch (IOException e) {
					System.out.println("Couldn't get ack packet");//IF WE CANNOT RECEIVE ACK THEN RE-SEND
					socket.send(dataPacket);
				}
			}	while (dataPacket.getLength() == 512);
			istream.close();
			System.out.println("Sent file to client");

		} catch (SocketException e) {
				System.out.println("Socket Not Created");
		} catch (FileNotFoundException e) {
				System.out.println("Error Finding File '"+fileName+"'");
				sendError(1);
		} catch (IOException e) {
				System.out.println("Request not Sent");//RE-SEND REQUEST
				handleReadRequest(fileName);
		}

		blockNumber = 0;
	}//end handleReadRequest

	/**
	* Overriden for runnable class
	*/
	@Override
	public void run() {
		server.addThread();
		handleRequest();
		server.removeThread(this);
		server.removeFile(fileName);
		socket.close();
		System.out.println("\nThread Closing, Server is Waiting for New Connection\n");
	}//end run

}//end class
