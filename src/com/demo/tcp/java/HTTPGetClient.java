package com.demo.tcp.java;

import java.net.*;

import javax.net.ssl.SSLSocketFactory;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.*;

public class HTTPGetClient {

	public static void main(String[] args) throws Exception  {
		
		
		InputStream input = Server.class.getResourceAsStream("../config/config.xml");
		JacksonXmlModule module = new JacksonXmlModule();
		// 核心对象
		XmlMapper mapper = new XmlMapper(module);
		// 读取字节流并返回一个 JavaBean
		Config config = (Config)mapper.readValue(input, Config.class);

		try {

			// 使用互联网协议地址类，获取本地local主机
			InetAddress inetAddress = InetAddress.getLocalHost();
//			String serverName = inetAddress.getHostName();
//			int port = 4000;
			String serverName = config.ip;
			int port = config.port;

			System.out.println("serverName：" + serverName + ";port:" + port);
			// http下就用 Socket
			Socket client = new Socket(serverName, port);
			// https用如下： ssl socket
			// Socket client =
			// SSLSocketFactory.getDefault().createSocket(serverName,port);
			System.out.println("socket address：" + client.getRemoteSocketAddress());

			// 写buffer:OutputStream --> OutputStreamWriter --> BufferedWriter
			String requestUrlPath = "/";
			OutputStreamWriter streamWriter = new OutputStreamWriter(client.getOutputStream());
			// 用 BufferedWriter
			BufferedWriter bufferedWriter = new BufferedWriter(streamWriter);
			bufferedWriter.write("GET " + requestUrlPath + " HTTP/1.1\r\n");
			bufferedWriter.write("Host: " + serverName + "\r\n");
			bufferedWriter.write("\r\n");
			bufferedWriter.flush();

			System.out.println("client是否处于关闭状态：" + client.isClosed());

			// 读buffer： InputStream-->BufferedInputStream-->InputStreamReader
			// --> BufferedReader
			System.out.println("准备接收html");
			BufferedInputStream streamReader = new BufferedInputStream(client.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
			System.out.println("接收完毕准备close");
			bufferedReader.close();
			bufferedWriter.close();
			client.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
