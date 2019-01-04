	     ITERATION 5
-----------------------------------
	Project Members - Group 12
	Brandon Ward 	 
	Caleb Gryfe      
	Zubaer Ahmed     
	Birat Dhungana   

PREP
---------------------
In Project Directory (i.e workspace), 
	Create 2 folders called "client_files" and "server_files"
	in the newly created folders:
		Create test.txt in the client_files folder, 
		Create serverFiletest.txt in the server_files folder

IMPORTANT NOTES
---------------------
- Server times out in 35 seconds, this will allow the TAs enough time to corrupt packets accordingly
- We overwrite files on read and write requests

TESTING
---------------------

 - run Server
 - run ErrorSimulator
 - run Client

	On Client
 - press Enter to start
 - to Read, choose the "Read" option
 - to write, choose the "Write" option
 - to quit gracefully, choose the "q" option
	On ErrorSimulator
 - Error simlator is waiting for new requests.
	On Server
 - Server times out in 35 seconds, this will allow the TAs enough time to corrupt packets accordingly
 - to accept the write request, choose "get" option
 - to accept the read request, choose "send" option
 - to quit gracefully, choose the "q" option

FLOW
----------------------

	On Client
1. menu - show the menu
	-This menu is displayed again. The same method that is utilised in order to generate the client prompt menu is reused.
2. exit - stop the client 
	- The client is stopped from making any other requests or actions. The inputting capabilities is closed and it is displayed to the user on the client that the 
3. mode - switch Print Mode(Verbose,Quiet) 
	- the mode in which the output is displayed is changed from quiet to verbose and vice versa, various case methods matching the inputted entries from the client are used in order for accomodation
4. switch - switch Client mode
	- The mode in which the client is operating test/normal mode is switched. 
5. reset - This will clear the command window 
	- Wipe the prompt that is being displayed to the client (the client menu). 
6. read <filename> - send RRQ(i.e. read text.txt)
	-A read request for a file on the server. The read request is sent to the error simulator as a packet type 01 (Read request) The filename is also specified (a file that must be on the server side).
7. write <filename> - send WRQ(i.e. write text.txt)
	-A write request of a file on the client side to be written on the server. The write request is sent to the error simulator as a packet type 02 (Write request) The filename is also specified (a file that must be on the server side).

	On ErrorSimulator
-Determine the type of error you would like to generate 
Enter The Error You Wish To Simulate
Enter 0: Error 4: Corrupt the opcode 
	- Will be prompted to change Opcode to an specific integer, once a valid opcode has been inputted by the user the op code portion of the packet data will be corrupted with the inputted opcode
Enter 1: Error 4: Corrupt the block number 
	- User will be prompted to change the block number to an integer, once a block number (even large(20)) has been inputted the block number of the packet will be corrupted with the inputted block number
Enter 2: Error 4: Force (data size > 512) 
	- Force the data portion of the packet to accomodate more than the usual 512 bytes size (520 bytes). Access the areas in which the size of the data portion and overwrite the size as over 520 bytes.
Enter 3: Error 5: Corrupt the tid 
	- User will be prompted to change the transfer ID to a specific integer, once a TID has been inputted the transfer ID of the packet will be corrupted with the inputted TID
Enter 4: Delay the Packet 
	- The user will be prompted to input the amount of seconds to delay the packet by. Following an input the thread to sending to the packet is put to sleep for that inputtede amount of time.
Enter 5: Duplicate the Packet 
	- Packet that was most recently sent is sent again, as a duplicate packet.A control boolean variable to differentiate a duplicate packet is used in order to generate a duplicated packet 
Enter 6: Lose the Packet 
	- Packet that was most recently sent is "lost". The packet that is to be sent to client (read/write request) or server (read/write request) is instead sent to an undefined location (NULL).
Press Enter to Send the Packet 
	- Prompting whether the packet should be passed through or an error (corruption) can be implemented on the current packet going through the error simulator. "Enter" will allow for the packet currently 
	  in the error simulator to be passed through to the client or the server. If one of the above prompt are entered the packet will be affected accordingly. 

	On Server
These are the available commands:
Exit
	- The server is shut down swiftly 
Verbose
	-The information that is displayed on the commands shows specific information about the actions and componenets running on the server. 
Quiet
	-The reead or write request is executed and carried through without displaying much information on the command.

FILE NAMING
----------------------
- Client.java: TFTP Client
- ErrorSimulator.java: TFTP ErrorSimulator
- Server.java: TFTP Server 
- RequestHandler.java: handles WRQ and RRQ request
- Packet.java: basic functionality of packets                                 
- PacketTypes.java: creates the ability to differentiate the different types of packets
- RPacket.java: packet type for read requests,when read is requested this class is relied upon
- WRPacket.java: packet type for write requests,when write is requested this is relied upon

DELEGATIONS
----------------------
- Birat Dhungana: Server,Client, improving verbose Mode across system. 
- Zubaer Ahmed: Debugging and Commenting, Iteration 5 Report, Testing
- Caleb Gryfe: Testing, TFTP muli-computer communication through the IP address, fix op code error. 
- Brandon Ward: Testing, Debugging and Fixes, Error handling from previous iterations, block number fixes, ErrorSimulator

DIAMGRAMS
--------------------
Alongside the source code and delegations we have also provided error timing diagrams and UML class diagrams for iteration 4. 
These diagrams are titled; "Iteration4TimingDiagrams" and "Iteration4UML".


