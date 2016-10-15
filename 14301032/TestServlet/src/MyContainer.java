import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import javax.servlet.Servlet;

public class MyContainer {
	public static void main(String[] args){
		try {
			ServerSocket server=new ServerSocket(8080);
			ParseWebXml parseWebXml=new ParseWebXml();
			parseWebXml.toParseXml();
			Map<String,String> servletMap=parseWebXml.getServletMap();
			while(true){
				Socket socket=server.accept();
				
				DataInputStream dis=new DataInputStream(socket.getInputStream()); 
				byte[] bt=new byte[32000];
				dis.read(bt); 
				String str=new String(bt);
				String theURL=((str.split(" "))[1]).split("[?]")[0];
				
				if(theURL.indexOf(".htm")!=-1){
					File file=new File("src"+theURL);
					
					if(file.isFile() && file.exists()){
						InputStreamReader read = new InputStreamReader(new FileInputStream(file),"utf-8");
			            BufferedReader bufferedReader = new BufferedReader(read);
			            String text = "";
			            String lineText=new String();
			            while((lineText = bufferedReader.readLine()) != null){
			            	text+=lineText;
			            }
			            read.close();
			            String content="HTTP/1.x 200 OK\r\nContent-Type: text/html\r\n\r\n"+text;
			            OutputStream ops=socket.getOutputStream();
			            ops.write(content.getBytes());
			            ops.close();
					}
				}else if(theURL.indexOf(".ico")!=-1){
					
				}else{
					MyRequest request=new MyRequest(str);
					MyResponse response=new MyResponse();
					
					String[] servletName=(servletMap.get(theURL)).split("[.]");
					Class servletClass=Class.forName(servletName[0]+"."+servletName[1]);
					Servlet servlet = (Servlet) servletClass.newInstance();
					servlet.service(request, response);
					String content="HTTP/1.x 200 OK\r\nContent-Type: text/html\r\n\r\nbye";
					OutputStream ops=socket.getOutputStream();
		            ops.write(content.getBytes());
		            ops.close();
				}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
