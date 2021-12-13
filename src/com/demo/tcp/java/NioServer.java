package com.demo.tcp.java;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NioServer {

	private static final int BUF_SIZE = 1024;
	private static final int TIMEOUT = 3000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		InputStream input = Server.class.getResourceAsStream("../config/config.xml");

		JacksonXmlModule module = new JacksonXmlModule();
		// 核心对象
		XmlMapper mapper = new XmlMapper(module);
		// 读取字节流并返回一个 JavaBean
		Config config = (Config) mapper.readValue(input, Config.class);

		selector(config.port);

	}

	public static void handleAccept(SelectionKey key) throws IOException {

		ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssChannel.accept();
		sc.configureBlocking(false);
		sc.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(BUF_SIZE));
		System.out.println("client address:" + sc.getRemoteAddress());
	}

	public static void handleRead(SelectionKey key) throws IOException {

		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer buf = (ByteBuffer) key.attachment();
		// 从通道里面读取数据至buffer中
		long bytesRead = sc.read(buf);

		while (bytesRead > 0) {
			// 进入读取状态
			buf.flip();
			StringBuffer httpRequestBuf = new StringBuffer();
			// 循环 遍历 一个一个 byte去读
			while (buf.hasRemaining()) {
				// System.out.println((char)buf.get());
				httpRequestBuf.append((char) buf.get());
			}
			System.out.println("string buffer :" + httpRequestBuf.toString());
			// 对这个字符串进行换行分割
			String[] httpRequestArray = splitHttpRequestBuf(httpRequestBuf);

			// 解析数据
			HttpRequest request = parseHttpRequestArray(httpRequestArray);
			// 打印
			System.out.println(request.toString());

			buf.clear();
			bytesRead = sc.read(buf);
		}
		if (bytesRead == -1) {
			sc.close();
		}
	}

	private static String[] splitHttpRequestBuf(StringBuffer httpRequestBuf) {

		String delimeter = "\r\n";
		return httpRequestBuf.toString().split(delimeter);
	}

	private static HttpRequest parseHttpRequestArray(String[] httpRequestArray) {

		HttpRequest request = new HttpRequest();
		System.out.println(" HttpRequestArray length:" + httpRequestArray.length);

		if (httpRequestArray.length == 0) {
			return null;
		}
		// 解析 请求行
		String requestLine = httpRequestArray[0];
		String[] reLineArray = requestLine.split(" ");
		if (reLineArray.length < 3) {
			return null;
		}
		request.method = reLineArray[0];
		request.resource = reLineArray[1];
		request.httpVersion = extractVersion(reLineArray[2]);

		// 解析 请求头
		for (int i = 1; i < httpRequestArray.length; i++) {
			if (httpRequestArray[i] == "") {
				System.out.println(" 解析 请求头的for循环  在当前i:" + i + " 这里停下来了");
				break;
			}
			String sHeader = httpRequestArray[i];
			int index = sHeader.indexOf(":");

			if (index == -1) {
				continue;
			}
			request.headers.put(sHeader.substring(0, index), sHeader.substring(index + 1, sHeader.length()));

		}
		return request;
	}

	private static float extractVersion(String s) {

		// 如输入 HTTP/1.1 截取/后面的数字1.1 
		int index = s.indexOf("/");
		return Float.parseFloat(s.substring(index + 1, s.length()));
	}

	public static void handleWrite(SelectionKey key) throws IOException {

		ByteBuffer buf = (ByteBuffer) key.attachment();
		buf.flip();
		SocketChannel sc = (SocketChannel) key.channel();

		while (buf.hasRemaining()) {
			sc.write(buf);
		}
		// buf中暂未读完的数据先暂存
		buf.compact();
	}

	public static void selector(int port) {

		Selector selector = null;
		ServerSocketChannel ssc = null;

		try {
			// 创建选择器
			selector = Selector.open();
			// 打开服务器端 通道
			ssc = ServerSocketChannel.open();
			// 绑定端口
			ssc.socket().bind(new InetSocketAddress(port));
			// 设置非阻塞
			ssc.configureBlocking(false);
			// 注册选择器
			ssc.register(selector, SelectionKey.OP_ACCEPT);

			while (true) {
				if (selector.select(TIMEOUT) == 0) {
					System.out.println("server ==");
					continue;
				}

				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

				while (iter.hasNext()) {
					// 遍历selectionkey
					SelectionKey key = iter.next();
					// 根据key的状态（客户端的状态）去执行对应的操作
					if (key.isAcceptable()) {
						handleAccept(key);
					}
					if (key.isReadable()) {
						handleRead(key);
					}
					if (key.isWritable()) {
						handleWrite(key);
					}
					if (key.isConnectable()) {
						System.out.println("is Connecting ... ");
					}
					// 该方法移除的是iterator.next() 最后访问的元素.
					iter.remove();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (selector != null) {
					// 关闭 选择器
					selector.close();
				}
				if (ssc != null) {
					// 关闭 服务器通道
					ssc.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
