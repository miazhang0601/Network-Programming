
/**
 * StreamClient Class
 * 
 * CPSC 441
 * Assignment 1
 *
 */


import java.util.logging.*;
import java.util.*;
import java.net.*;
import java.io.*;



public class StreamClient {

	private static final Logger logger = Logger.getLogger("StreamClient"); // global logger

	private String serverName;
	private int serverPort;
	private int bufferSize;

	/**
	 * Constructor to initialize the class.
	 *
	 * @param serverName remote server name
	 * @param serverPort remote server port number
	 * @param bufferSize buffer size used for read/write
	 */
	public StreamClient(String serverName, int serverPort, int bufferSize) {
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.bufferSize = bufferSize;
	}

	/**
	 * Compress the specified file via the remote server.
	 *
	 * @param inName  name of the input file to be processed
	 * @param outName name of the output file
	 */
	public void getService(int serviceCode, String inName, String outName) {

		try {
				Socket socket = new Socket(this.serverName, this.serverPort);

				OutputStream outputStream = socket.getOutputStream();			// socket: outputstream	and inputstream
				
				outputStream.write(serviceCode);								// Send the service code
				outputStream.flush();

				FileReader reader = new FileReader(inName, socket, bufferSize);
				Writer writer = new Writer(outName, socket, bufferSize);

				Thread thread1 = new Thread(reader);							// Create threads
				thread1.start();
				Thread thread2 = new Thread(writer);
				thread2.start();

				try{	
					
					thread1.join();												
					thread2.join();
		
				}catch (InterruptedException e) {
					throw new RuntimeException(e);
				}	
		
				outputStream.close();

		} catch (Exception e){
			System.out.println("error get service");
		}




	}
}