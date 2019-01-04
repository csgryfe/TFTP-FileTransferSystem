

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * RRQPacket is a possible type of packet that is used by the client in order to initiate a READ transfer from the server.
 *
 */
public class WRPacket extends Packet {
    public WRPacket(DatagramPacket packet) {
        super(packet); //packet type for write uses the RRQWRQPacket class
    }

    public WRPacket (String fileName, InetAddress address, int port) {
    	super(fileName,address,port);
    }

    public WRPacket(byte[] mode, byte[] filename, InetAddress address, int port) {
        super(new DatagramPacket(new byte[512], 512, address,port));
       byte rw= new Byte("2");
        setData(prepare(rw,mode, filename));
    }

    @Override
    public PacketTypes getType() {
        return PacketTypes.WRQ;
    }

    @Override
    public boolean isBlockNumbered() {
        return false;
    }


 protected byte[] prepare(byte rw, byte[] mode, byte[] filename) {

        byte[] request = new byte[4 + mode.length + filename.length];
        int count = 2; // filename starts at index 2
        request[0] = 0;
        request[1] = rw;

        System.arraycopy(filename, 0, request, count, filename.length);

        count += (filename.length + 1); // +1 -> there is a 0 between filename and mode

        System.arraycopy(mode, 0, request, count, mode.length);

        return request;
    }

}
