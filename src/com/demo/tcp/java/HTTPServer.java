package com.demo.tcp.java;

import java.net.*;
import java.io.*;

public class HTTPServer extends Thread {

	private ServerSocket serverSocket;

	public HTTPServer(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.serverSocket.setSoTimeout(100000);

	}

	public void run() {
		while (true) {
			try {
				System.out.println("等待远程连接，端口号为：" + serverSocket.getLocalPort() + "。。。");
				Socket server = serverSocket.accept();
				System.out.println("远程主机地址：" + server.getRemoteSocketAddress());
				
				String fileName = "src\\com\\demo\\tcp\\config\\page.html";
				// 用相对路径 
				BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
				String fileContentLine;
		
				// 读取信息 
				BufferedInputStream streamReader = new BufferedInputStream(server.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));
				String line = null;
				Boolean sendFlag = true;
				OutputStreamWriter streamWriter = new OutputStreamWriter(server.getOutputStream());
				// 写html 用 BufferedWriter
				BufferedWriter bufferedWriter = new BufferedWriter(streamWriter);
				while ((line = bufferedReader.readLine()) != null) { 
					
					// 无论接收到什么都发送html
					System.out.println(line);
					if(sendFlag){
						System.out.println("准备写 html");
						
						// 状态行
						bufferedWriter.write("HTTP/1.1 200 OK \r\n");
						// 响应头部 
						bufferedWriter.write("Content-Type: text/html;charset=utf-8 \r\n");
						bufferedWriter.write("Content-Length: 200 \r\n");
						// 空行 
						bufferedWriter.write("\r\n");
						// 响应数据 （响应体）
						while((fileContentLine=fileReader.readLine())!=null){
							//  System.out.println("读到的文件内容："+fileContentLine);
							bufferedWriter.write(fileContentLine+"\r\n");
						}
						bufferedWriter.write("\r\n");
						bufferedWriter.flush();
						sendFlag = false;
					
						
					}
				}
				
				
				System.out.println("准备close ");
				bufferedWriter.close();
				bufferedReader.close();
				server.close();

			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

		}

	}
	
	



	public static void main(String[] args) {

		// TODO Auto-generated method stub

		int port = 4000;
		try {
			Thread t = new HTTPServer(port);
			t.run();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
