import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class FirstServer {
	public static void main(String[] args) throws IOException{
		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(3333);
		
		while(true){
			Socket socket = server.accept();
			toConn(socket);
		}
	}
	
	private static void toConn(final Socket client) throws IOException{
		new Thread(new Runnable() {  
            public void run() {  
                BufferedReader in = null;  
                PrintWriter out = null;  
                try {  
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));  
                    out = new PrintWriter(client.getOutputStream());  
                    
                    
                    String str=in.readLine();
                    out.println(reverse(str));
                    out.flush();
                } catch(IOException ex) {  
                    ex.printStackTrace();  
                } finally {
                	try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
                    try {  
                        out.close();  
                    } catch (Exception e) {
                    	e.printStackTrace();
                    }  
                    try {  
                        client.close();  
                    } catch (Exception e) {
                    	e.printStackTrace();
                    }  
                }  
            }  
        }).start();
	}
	
	private static String reverse(String str){
		char[] ch=str.toCharArray();
		String s="";
		for(int i=ch.length-1;i>=0;i--){
			s+=ch[i];
		}
		
		return s;
	}
}
