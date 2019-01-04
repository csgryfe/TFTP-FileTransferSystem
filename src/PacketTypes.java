
import java.io.ByteArrayOutputStream;


public enum PacketTypes {
	RRQ(1, "RRQ"),WRQ(2, "WRQ"),DATA(3, "DATA"),ACK(4, "ACK"),ERROR(5, "ERROR");
	//defining the various types of packets in our tftp system

	private int OPCODE; //set up the overlying code for packet type and request
	private String type; //set the string representation of the tftp packet type

	/**
	 * Constructor
	 *
	 * @param OPCODE
	 * @param type
	 */
	private PacketTypes(final int OPCODE, final String type) {
		this.OPCODE = OPCODE;
		this.type = type;
	}

	/**
	 * Convert the OPCODE into byte array of length 2. This will is setting up the beginning of our the opcode
	 * which will be decoded to show a read,write,data,acknowledge or error packet request.
	 *
	 * @return byteArray
	 */
	public byte[] OPCODE() {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(0);
		steam.write(OPCODE);
		return steam.toByteArray();
	}

	/**
	 * Check if the given OPCODE is valid by checking if the OPCODE match
	 * the OPCODE standard(1 for RRQ, 2 for WRQ, etc.)
	 *
	 * @return true if the OPCODE matches the standard, false otherwise
	 */
	public static boolean validOPCODE(PacketTypes t, int OPCODE) {
		return OPCODE == t.OPCODE;
	}

	/**
	 * Getter
	 *
	 * @return type
	 */
	public String type() {
		return type;
	}
}
