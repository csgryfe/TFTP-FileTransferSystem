import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Packet allows for creation and detection of all possible packets encountered in the TFTP protocol (i.e. RRQ,
 * WRQ, DATA, ACK, ERROR, and UNKNOWN).
 *
 */
public abstract class Packet {

    private DatagramPacket packet;
    public enum PacketTypes { ACK, DATA, RRQ, WRQ, ERROR, UNKNOWN }
    public static int ACK_SIZE = 4; //size of ACK packet
    String fileName; //the filename from which the packet is getting created to/from
    InetAddress address; //respective address related to the packet
    int port;

    //Default constructor
    public Packet() {}

    /**
     * Constructor for the packet class.
     *
     * @param packet
     */
    public Packet(DatagramPacket packet) {
        this.packet = packet;
    }// end constructor packet (single param)

    /**
     * Constructor for the packet class.
     *
     * @param fileName
     * @param address
     * @param port
     */
    public Packet (String fileName, InetAddress address, int port) {
	   this.fileName = fileName;
	   this.address = address;
	   this.port = port;
   }// end constructor packet (multiple param)


    public abstract PacketTypes getType();//since this method is used in other classes, merely being defined
    public abstract boolean isBlockNumbered();//since this method is used in other classes, merely being defined

    /**
     * Getter method to get the packet.
     *
     * @return
     */
    public DatagramPacket getDatagram() {
        return this.packet;
    }//end getDatagram

    /**
     * Setter method to set the data.
     *
     * @param array
     */
    protected void setData(byte[] array) {
        this.packet.setData(array);
    }// end setData

    /**
     *  Recursively matches a byte array pattern with the provided form as a string where the following letters in the string are important:
     *
     * - c: stands for control and checks for the given byte with the control byte the array provided
     * - x: stands for don't care, used for skipping a dynamic input that terminates once the next pattern in line is found.
     *
     * @param data
     * @param size
     * @param form
     * @param opcode
     * @return
     */
    protected static boolean matches(byte[] data, int size, String form, byte opcode) {
        return matches(data, 0, size, form, opcode, false);
    }// end matches (single param)

    protected static boolean matches(byte[] data, int index, int size, String form, byte opcode, boolean inText) {
    	char letter = 0;

    	//base case
        if (form.isEmpty() && index == size) {
            return true;
        }

        if (index == size && form.length() == 1 && form.charAt(0) == 'x') {
            return true;
        }

        if (!form.isEmpty()) {
            letter = form.charAt(0);
        }


        if (letter == 'c' && data[index] == opcode) {
            return matches(data, ++index, size, form.substring(1), opcode, false);
        } else if (letter == '0' && data[index] == 0) {
            return matches(data, ++index, size, form.substring(1), opcode, false);
        } else if (letter == 'x') {
            return matches(data, ++index, size, form.substring(1), opcode, true);
        } else if (letter == 'n') {
            return matches(data, ++index, size, form.substring(1), opcode, false);
        } else if (inText) {
            return matches(data, ++index, size, form, opcode, true);
        } else {
            return false;
        }//end of conditions
    }// end matches(multiple params)

    /**
     *
     * PacketTypes method uses the matches method to determine the type of packet sent to our server after this it
     * returns the type as an enum temporarily. Packet type can be set with relation to the method as well
     * @param packet
     * @return
     */
    public static PacketTypes getPacketType(DatagramPacket packet) {
        byte RRQ = 1, WRQ = 2, DATA = 3, ACK = 4, ERR = 5; //packet types assigned with their respective packet values

        if (matches(packet.getData(), packet.getLength(), "0cx0x0", RRQ)) { //determine that the packet type is a read request
            return PacketTypes.RRQ;
        } else if (matches(packet.getData(), packet.getLength(), "0cx0x0", WRQ)) { //determine that the packet type is a write request
            return PacketTypes.WRQ;
        } else if (matches(packet.getData(), packet.getLength(), "0cnnx", DATA)) { //determine that the packet type is data
            return PacketTypes.DATA;
        } else if (matches(packet.getData(), packet.getLength(), "0cnn", ACK)) { //determine that the packet type is an acknowledgement
            return PacketTypes.ACK;
        } else if (matches(packet.getData(), packet.getLength(), "0cx0", ERR)) { //determine that the packet type is error
            return PacketTypes.ERROR;
        } else {
            return PacketTypes.UNKNOWN; //case if packet type fall in a different category
        }//end of conditions
    }//end getPacketType

    /**
     * Creates a DatagramPacket from received DatagramPacket.Possibly be utilised by the Error Simulator(Proxy)
     *
     * @param data byte[] for the datagram packet
     * @return datagram packet
     */
    public DatagramPacket createPacket(byte[] data) {
        return  new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
    }// end createPacket (single param)

    /**
     *
     * Generate a Packet to be sent to a specified port
     *
     * @param data
     * @param address
     * @param port
     * @return
     */
    public DatagramPacket createPacket(byte[] data, InetAddress address, int port) {
        return new DatagramPacket(data, data.length, address, port);
    }// end createPacket(multiple params)
}//end class Packet
