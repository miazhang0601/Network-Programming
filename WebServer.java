

/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 *
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


public class WebServer extends Thread {
	// global logger object, configures in the driver class
	private static final Logger logger = Logger.getLogger("WebServer");

	private boolean shutdown = false; // shutdown flag

	
    /**
     * Constructor to initialize the web server
     * 
     * @param port 	Server port at which the web server listens > 1024
	 * @param root	Server's root file directory
	 * @param timeout	Idle connection timeout in milli-seconds
     * 
     */

	private int port;
	private String root;
	private int timeout;

	public WebServer(int port, String root, int timeout){

		this.port = port;
		this.root = root;
		this.timeout = timeout;

	}

    /**
	 * Main method in the web server thread.
	 * The web server remains in listening mode 
	 * and accepts connection requests from clients 
	 * until it receives the shutdown signal.
	 * 
     */
	public void run(){
	// Open serversocket
		ServerSocket serverSocket;
		ExecutorService pool = Executors.newFixedThreadPool(10);

		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(timeout);

			// Listen to the client
			while (!shutdown){
				try {
					Socket socket = serverSocket.accept();

					// Print IP and Port
					int port = socket.getPort();
					InetAddress IP = socket.getInetAddress();
					System.out.println("Port: " + port + " IP: " + IP);

					// Start a workerthread
					pool.submit(new WorkerThread(socket, root, port));
			
					// Shut down 
				} catch (SocketTimeoutException ste) {

				}
				}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			pool.shutdown();
			try {
				if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
					pool.shutdownNow();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	}
    /**
     * Signals the web server to shutdown.
     * @throws InterruptedException 
	 *
     */
	
	public void shutdown() throws InterruptedException {
		shutdown = true;
	}
	
}


class WorkerThread implements Runnable{

	private Socket socket;
	private String root;
	private int timeout;

	// Constructor for workerthread
	public WorkerThread(Socket socket, String root, int timeout){
		this.socket = socket;
		this.root = root;
		this.timeout = timeout;
	}

	@Override
	public void run() {

		OutputStream outputStream;
		FileInputStream fileInputStream;
		BufferedReader bufferedReader;
		String newLine;

		try {
		
			// Header Line
			String request;
			String path;
			String date = "Date: " + ServerUtils.getCurrentDate() + "\r\n";
			String server = "Server: Mia/1.0\r\n";
			String connection = "Connection: close\r\n";
			String noObjectResponse = date + server + connection;

			// Set timeout
			socket.setSoTimeout(timeout);

			outputStream = socket.getOutputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			// Print the request message
			newLine = bufferedReader.readLine();
			System.out.println(newLine);
		
			if(newLine.contains("GET")) {
				// Get the object path
				path = (newLine.split(" ")[1]).split("/",2)[1];
				if (path.equals("")){
					path = "index.html";
				}
				// Send Not Found request
				String path_1 = "/" + path;
				File file = new File(path_1);
				boolean exists = file.exists();
				if(exists == false){
					request = "HTTP/1.1 404 Not Found\r\n" + noObjectResponse;
					System.out.println(request);
					byte[] byteRequest = request.getBytes("US-ASCII");
					outputStream.write(byteRequest);
				} 
				// Send Bad request
				else if ((newLine.split(" ")).length != 3){
					request = "HTTP/1.1 400 Bad Request\r\n" + noObjectResponse;
					System.out.println(request);
					byte[] byteRequest = request.getBytes("US-ASCII");
					outputStream.write(byteRequest);
				}
				else if (!newLine.split(" ")[2].equals("HTTP/1.1")){
					request = "HTTP/1.1 400 Bad Request\r\n" + noObjectResponse;
					System.out.println(request);
					byte[] byteRequest = request.getBytes("US-ASCII");
					outputStream.write(byteRequest);
				}

				// Send ok response
				else{

					String lastModified = "Last-Modified: " + ServerUtils.getLastModified(new File(path_1)) + "\r\n";
					String contentLength = "Content-Length: " + ServerUtils.getContentLength(new File(path_1)) + "\r\n";
					String contentType = "Content-Type: " + ServerUtils.getContentType(new File(path_1)) + "\r\n";
					String okResponse = "HTTP/1.1 200 OK\r\n" + date + server + lastModified + contentLength + contentType + "\r\n";
					System.out.println(okResponse);
					byte[] byteRequest = okResponse.getBytes("US-ASCII");
					outputStream.write(byteRequest);
					outputStream.flush();

					// Send the object
					String filepath = "/" + path;
					fileInputStream = new FileInputStream(filepath);
					int numBytes = 0;
					byte[] buff = new byte[100000];
					while ((numBytes = fileInputStream.read(buff)) != -1){   
						outputStream.write(buff,0, numBytes);                       
					}
					
				}
			}
			socket.close();
		} catch (SocketTimeoutException e){
			try {
				// Timeout Response
				String date = "Date: " + ServerUtils.getCurrentDate() + "\r\n";
				String server = "Server: Mia/1.0\r\n";
				String connection = "Connection: close\r\n";
				String noObjectResponse = date + server + connection;
				String request = "HTTP/1.1 408 Request Timeout\r\n" + noObjectResponse;
				System.out.println(request);

				byte[] byteRequest;
				byteRequest = request.getBytes("US-ASCII");
				outputStream = socket.getOutputStream();
				outputStream.write(byteRequest);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
	
		}	
}
}


