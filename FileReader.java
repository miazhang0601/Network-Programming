
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public class FileReader implements Runnable{

    private String inName;
    private Socket socket;
    private int bufferSize;

    public FileReader(String inName, Socket socket, int bufferSize){                // Constructor for FileReader
        
        this.inName = inName;
        this.socket = socket;
        this.bufferSize = bufferSize;
        
    }
    
    public void run(){
        
        try {

            FileInputStream fileInputStream = new FileInputStream(inName);          // Get the streams for reading file
            OutputStream outputStream = socket.getOutputStream();

            
            int numBytes = 0;
            byte[] buff = new byte[bufferSize];								        // Create buff array for getting the content of file

            while ((numBytes = fileInputStream.read(buff)) != -1){                  // Keep reading the local file until receive a -1

                outputStream.write(buff, 0, numBytes);                          // Send data to server
                //outputStream.flush();
                System.out.println("W " + numBytes);
            }
            socket.shutdownOutput();
            fileInputStream.close();                                                // Close the fileinputstream and the socket
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {  
            e.printStackTrace();
        }
	
    }
}
