

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.*;

public class StopWaitFtp {
	
	int timeout;
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger	

	/**
	 * Constructor to initialize the program 
	 * 
	 * @param timeout		The time-out interval for the retransmission timer, in milli-seconds
	 */
	public StopWaitFtp(int timeout){
		this.timeout = timeout;
	}


	/**
	 * Send the specified file to the specified remote server.
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 * @return 				true if the file transfer completed successfully, false otherwise
	 */
	public boolean send(String serverName, int serverPort, String fileName){

		DataInputStream dataInputStream;
		DataOutputStream dataOutputStream;
		
		try {
			// Open a TCP connection and a UDP socket to the server
			Socket socket = new Socket(serverName, serverPort);
			DatagramSocket clientSocket = new DatagramSocket();
			
			// Handshake message
			// Send filename
			File file = new File(fileName);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(fileName);
			// Send file length
			long fileLength = file.length();
			dataOutputStream.writeLong(fileLength);
			// Send port number
			int localPort = clientSocket.getLocalPort();
			dataOutputStream.writeInt(localPort);
			// Flush
			dataOutputStream.flush();

			// Receive from server
			// Receive udp port 
			dataInputStream = new DataInputStream(socket.getInputStream());
			int serverUdpPort = dataInputStream.readInt();
			// Receive initial seq num
			int initialSeq = dataInputStream.readInt();
			
			
			// Read file
			int numByte;
			byte[] buffer = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
			BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
			int seqNum = initialSeq;
			InetAddress IP = InetAddress.getByName(serverName);

			while ((numByte = bufferedInputStream.read(buffer)) != -1) {
				// Create segments
				FtpSegment segment = new FtpSegment(seqNum, buffer, numByte);
				// Create packets
				DatagramPacket packet = FtpSegment.makePacket(segment, IP, serverUdpPort);
				// Send packet
				clientSocket.send(packet);
				System.out.println("send <" + seqNum + ">");

				// Schedule a recurring timer
				Timer timer = new Timer();
				TimeoutHandler timeoutHandler = new TimeoutHandler(clientSocket, packet, seqNum);
				timer.scheduleAtFixedRate(timeoutHandler, timeout, timeout);

				while(true){
					// Wait for ack
					DatagramPacket ackPacket = new DatagramPacket(new byte[FtpSegment.MAX_SEGMENT_SIZE], FtpSegment.MAX_SEGMENT_SIZE);
					clientSocket.receive(ackPacket);
					FtpSegment ackSegment = new FtpSegment(ackPacket);
					System.out.println("ack <" + ackSegment.getSeqNum() + ">");

					if (ackSegment.getSeqNum() == seqNum + 1) {
						//logger.info("ack <" + ackSegment.getSeqNum() + ">");
						// Receive the correct ack. Canclel the old timer.
						seqNum++;
						timer.cancel();
						timer.purge();
						break;
					} else {
						// Not receieve the correct ack. Wait for retransmisson
						logger.info("Incorrect ACK received: <" + ackSegment.getSeqNum() + "> Expected: <" + (seqNum + 1) + ">");
					}
			}
			}
			return true;

		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

	}

} 
// Timer
class TimeoutHandler extends TimerTask{
	private DatagramSocket clientSocket;
	private DatagramPacket packet;
	private int seqNum;
	private static final Logger logger = Logger.getLogger("StopWaitFtp");

	public TimeoutHandler(DatagramSocket clientSocket, DatagramPacket packet, int seqNum){
		this.clientSocket = clientSocket;
		this.packet = packet;
		this.seqNum = seqNum;
	}

	@Override
	public void run() {
		System.out.println("timeout");
		try {
			// Resend
            clientSocket.send(packet);
			System.out.println("retx <" + seqNum + ">");
            //logger.info("retx <" + seqNum + ">");
        } catch (Exception e) {
			// Fail to resend
            logger.log(Level.SEVERE, "Failed to retransmit segment", e);
        }
	}
	
}