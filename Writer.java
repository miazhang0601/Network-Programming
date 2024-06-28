import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Writer implements Runnable{

    private String outName;
    private Socket socket;
    private int bufferSize;

    public Writer(String outName, Socket socket, int bufferSize){              // Constructor for Writer 
        
        this.outName = outName;
        this.socket = socket;
        this.bufferSize = bufferSize;

    }
    
    public void run(){
        try{

            InputStream inputStream = socket.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(outName;  // Get the streams for writing file

            int numBytes = 0 ;
            byte[] buff = new byte[bufferSize];

            while ((numBytes = inputStream.read(buff)) != -1){              // Keep receiveing data from server through socket until receive a -1
                fileOutputStream.write(buff, 0, numBytes);                  // Write the data to file
                fileOutputStream.flush();
                System.out.println("R " + numBytes);
            }
            fileOutputStream.close();                                      // Close the fileOutputStream

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {  
            e.printStackTrace();
        }
    }












    char sign = 0;
    String newLine = "";
    // Read the stats and header line
                while (sign != -1) {
        sign = (char) inputStream.read();
        if (sign=='\r') {
            sign = (char) inputStream.read();
            if (sign == '\n')
                break;
        }
        newLine += sign;
    }

}
