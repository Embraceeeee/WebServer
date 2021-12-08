package com.demo.tcp.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * 采用IO多路复用的客户端
 * 
 * @author Embraceeeee
 *
 */
public class NioClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// config 读取文件
		InputStream input = Server.class.getResourceAsStream("../config/config.xml");
		JacksonXmlModule module = new JacksonXmlModule();
		// 核心对象
		XmlMapper mapper = new XmlMapper(module);

		// 分配缓存区空间
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		SocketChannel socketChannel = null;
		try {
			// 读取字节流并返回一个 JavaBean (Config对象)
			Config config = (Config) mapper.readValue(input, Config.class);
			// 开启通道
			socketChannel = SocketChannel.open();
			// 配置下 非阻塞
			socketChannel.configureBlocking(false);
			// 连接客户端
			socketChannel.connect(new InetSocketAddress(config.ip, config.port));

			// 判断是否完成连接
			if (socketChannel.finishConnect()) {

				int i = 0;
				// 非阻塞地读和写
				while (true) {
					// 睡1秒
					TimeUnit.SECONDS.sleep(1);
					// buffer清除（重新赋值postition指针位置）
					buffer.clear();
					// 让它只写 1次 
					if (i == 0) {
						// 写数据进buffer
						buffer.put("GET / HTTP/1.1\r\n".getBytes());
						buffer.put(("Host: " + config.ip + " \r\n").getBytes());
					}
					String info = "I'm " + i++ + "-th information from client\r\n";
					buffer.put(info.getBytes());
					// filp方便将数据放入channel
					buffer.flip();
					while (buffer.hasRemaining()) {
						System.out.println(buffer);
						// 写入数据至通道 
						socketChannel.write(buffer);
					}

				}
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (socketChannel != null) {
					// 关闭通道
					socketChannel.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
