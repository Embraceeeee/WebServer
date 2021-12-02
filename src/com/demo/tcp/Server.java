package com.demo.tcp;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

/**
 * 采用多线程实现的Server
 * @author Embraceeeee
 *
 */
public class Server {

	public static void main(String[] args) throws IOException  {
		// TODO Auto-generated method stub
	
		ServerSocket server = new ServerSocket(4000);
		System.out.println("server is running...");
		
		for (;;) {
			// 循环接收客户端
            Socket socket = server.accept();
            System.out.println("connected from " + socket.getRemoteSocketAddress());
            // 将客户端丢到新开的子线程中
            Thread t = new ClientThread(socket);
            // 开启线程
            t.start();
        }
	}
}

class ClientThread extends Thread {

	private Socket socket;

	public ClientThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		
		try (InputStream in = this.socket.getInputStream()) {
			try (OutputStream out = this.socket.getOutputStream()) {
				readHTML(in, out);
			}
		} catch (Exception e) {
			try {
				this.socket.close();
			} catch (IOException ioe) {
			}
			System.out.println("client disconnected.");
		}
	}

	private void readHTML(InputStream input, OutputStream output) throws IOException {

		// 用相对路径 
		String fileName = "src\\com\\demo\\tcp\\config\\page.html";
		// 文件读取字符流
		BufferedReader freader = new BufferedReader(new FileReader(fileName));
		String fileContentLine;
		// socket 读写字符流
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		
		for (;;) {
			String s = reader.readLine();
			// 
			if (s.indexOf("GET") != -1) {
				System.out.println("准备写 html");
				// 状态行
				writer.write("HTTP/1.1 200 OK \r\n");
				// 响应头部 
				writer.write("Content-Type: text/html;charset=utf-8 \r\n");
				writer.write("Content-Length: 200 \r\n");
				// 空行 
				writer.write("\r\n");
				while((fileContentLine=freader.readLine())!=null){
					writer.write(fileContentLine+"\r\n");
				}
				writer.write("\r\n");
				writer.flush();
			}
			writer.flush();
		}
	}

}
