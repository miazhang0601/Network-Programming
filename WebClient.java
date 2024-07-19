

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WebClient {

	private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void getObject(String url){
        
        // Split each part of URL: protocol://hostname[:port]/pathname
        // Split protocol
        String[] part1 = url.split(":");   
        String protocol = part1[0];

        // Split hostname and port
        String[] part2 = url.split("/",4);  
        String hostname;
        String port;
        if(part2[2].contains(":")){
            String[] part3 = part2[2].split(":");
            hostname = part3[0];
            port = part3[1];
        }else{
            hostname = part2[2];
            port = "80";
        }

        // Split pathname and file name
        String pathname = "/" + part2[3];
        String[] file = pathname.split("/");
        String outName = file[file.length-1];
        
        // Create HTTP request
        String request = "GET " + pathname + " HTTP/1.1\r\n" 
                        + "HOST: " + hostname + "\r\n" 
                        + "Connection: close\r\n\r\n";

        // Print request line and header line
        System.out.println("GET " + pathname + "HTTP/1.1\r\n" + "HOST: " + hostname + "\r\nConnection: close\r\n\r\n");

        // Protocol = HTTP
        if (protocol.equalsIgnoreCase("HTTP")){
            try {
                // Create socket for HTTP connection
                Socket socket = new Socket(hostname, 2025);
                // Send the request to web through socket
                byte[] byteRequest = request.getBytes("US-ASCII");
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(byteRequest);

                // Flush
                outputStream.flush();

                // Read response message
                DataInputStream datainputStream = new DataInputStream(socket.getInputStream());
                String responseStatus = null;
                int fileLength = 0;
                boolean finishRead = false;

                while (!finishRead) {
                    char sign = 0;
                    String newLine = "";
                    // Read the stats and header line
                    while (sign != -1) {
                        sign = (char) datainputStream.read();
                        if (sign=='\r') {
                            sign = (char) datainputStream.read();
                            if (sign == '\n')
                                break;
                        }
                        newLine += sign;
                    }
                    // Print the response message
                    System.out.println(newLine);
                    if(newLine.contains("HTTP/")) {
                        // status code and status phrase
                		responseStatus = newLine.split(" ", 2)[1];
                	}else if(newLine.contains("Content-Length")){
                		// Content length
                		fileLength = Integer.parseInt(newLine.split(" ")[1]);
                	}else if(newLine.isEmpty()) {
                		// Reaches the end of status line and response header line
                		finishRead = true;
                	}
                }
                // Check if the status is ok to download the object
                if (responseStatus.equals("200 OK")){
                    byte[] buff = new byte[fileLength];
                    DataOutputStream dataOutputStream = new DataOutputStream(Files.newOutputStream(Path.of(outName)));
                
                    // Download the object: Read and Write
                    for(int i = 0; i < fileLength; i++){
                        datainputStream.read(buff, i, 1);
                }
                    dataOutputStream.write(buff, 0, fileLength);
                    // Flush
                    dataOutputStream.flush();
                    dataOutputStream.close();

                }
                else{
                    System.out.println("Fail! The response status is " + responseStatus);
                }
                // Close socket
                socket.close();   

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
             catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Protocol = HTTPS

        else if(protocol.equalsIgnoreCase("HTTPS")){
            // Create socket for HTTPS connection
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();   
            try {
                SSLSocket sslSocket = (SSLSocket)factory.createSocket(hostname, 443);

                // Send the request to web through socket
                byte[] byteRequest = request.getBytes("US-ASCII");
                OutputStream outputStream = sslSocket.getOutputStream();
                outputStream.write(byteRequest);

                // Flush
                outputStream.flush();

                // Read response message
                DataInputStream datainputStream = new DataInputStream(sslSocket.getInputStream());
                String responseStatus = new String();
                int fileLength = 0;
                boolean finishRead = false;

                while (!finishRead) {
                    char sign = 0;
                    String newLine = "";
                    // Read the stats and header line
                    while (sign != -1) {
                        sign = (char) datainputStream.read();
                        if (sign=='\r') {
                            sign = (char) datainputStream.read();
                            if (sign == '\n')
                                break;
                        }
                        newLine += sign;
                    }
                    // Print the response message
                    System.out.println(newLine);
                    if(newLine.contains("HTTP/")) {
                        // status code and status phrase
                		responseStatus = newLine.split(" ")[1];
                	}else if(newLine.contains("Content-Length")){
                		// Content length
                		fileLength = Integer.parseInt(newLine.split(" ")[1]);
                	}else if(newLine.isEmpty()) {
                		// Reaches the end of status line and response header line
                		finishRead = true;
                	}
                }
                // Check if the status is ok to download the object
                if (responseStatus.equals("200")){
                    byte[] buff = new byte[fileLength];
                    DataOutputStream dataOutputStream = new DataOutputStream(Files.newOutputStream(Path.of(outName)));
                
                    // Download the object: Read and Write
                    for(int i = 0; i < fileLength; i++){
                        datainputStream.read(buff, i, 1);
                }
                    dataOutputStream.write(buff, 0, fileLength);
                    // Flush
                    dataOutputStream.flush();
                    dataOutputStream.close();

                }
                else{
                    System.out.println("Fail! The response status is " + responseStatus);
                }
                // Close socket
                sslSocket.close();   
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }           
        }
    }

}
