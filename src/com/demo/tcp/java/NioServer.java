package com.demo.tcp.java;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

		InputStream input = Server.class.getResourceAsStream("../assets/config.xml");

		JacksonXmlModule module = new JacksonXmlModule();
		// 核心对象
		XmlMapper mapper = new XmlMapper(module);
		// 读取字节流并返回一个 JavaBean
		Config config = (Config) mapper.readValue(input, Config.class);

		selector(config);

	}

	/**
	 * accept操作
	 * 
	 * @param key
	 * @throws IOException
	 */
	public static void handleAccept(SelectionKey key) throws IOException {

		ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssChannel.accept();
		sc.configureBlocking(false);
		sc.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(BUF_SIZE));
		System.out.println("client address:" + sc.getRemoteAddress());
	}

	/**
	 * 读取数据操作
	 * 
	 * @param key
	 * @throws IOException
	 */
	public static void handleRead(SelectionKey key, String webAssetsPath) throws IOException {

		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer buf = (ByteBuffer) key.attachment();
		// 从通道里面读取数据至buffer中
		long bytesRead = sc.read(buf);

		while (bytesRead > 0) {
			// 进入读取状态
			buf.flip();
			StringBuffer httpRequestStrBuf = new StringBuffer();
			// 循环 遍历 一个一个 byte去读
			while (buf.hasRemaining()) {
				// System.out.println((char)buf.get());
				httpRequestStrBuf.append((char) buf.get());
			}
			// System.out.println("string buffer :" + httpRequestStrBuf.toString());
			// 对这个字符串进行换行分割
			String[] httpRequestArray = splitHttpRequestBuf(httpRequestStrBuf);
			// 解析数据
			HttpRequest request = parseHttpRequestArray(httpRequestArray);
			// 打印
			System.out.println(request.toString());
			// TODO: 待写入
			writeHtml(sc, request.resource, webAssetsPath);

			buf.clear();
			bytesRead = sc.read(buf);
		}
		if (bytesRead == -1) {
//			key.cancel();
//			sc.close();
		}
	}

	/**
	 * 依据换行符号分割 web浏览器的请求String， 分割成数组
	 * 
	 * @param httpRequestBuf
	 * @return
	 */
	private static String[] splitHttpRequestBuf(StringBuffer httpRequestBuf) {

		String delimeter = "\r\n";
		return httpRequestBuf.toString().split(delimeter);
	}

	/**
	 * 对 请求数组进行解析，转成请求行、请求头相关信息 TODO: 没考虑有request body的情况，后续看看怎么解析
	 * 
	 * @param httpRequestArray
	 * @return
	 */
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
		// 提取版本信息
		request.httpVersion = extractVersion(reLineArray[2]);
		// 解析 请求头
		for (int i = 1; i < httpRequestArray.length; i++) {
			if (httpRequestArray[i] == "") {
				System.out.println("parse request header for loop i stop:" + i);
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

	/**
	 * 从类似HTTP/1.1 这样字符串中提取数字
	 * 
	 * @param s
	 * @return
	 */
	private static float extractVersion(String s) {

		// 如输入 HTTP/1.1 截取/后面的数字1.1
		int index = s.indexOf("/");
		return Float.parseFloat(s.substring(index + 1, s.length()));
	}

	/**
	 * 写数据操作
	 * 
	 * @param key
	 * @throws IOException
	 */
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

	/**
	 * 根据解析的请求头，看到请求什么资源，从静态文件根目录里面取
	 * @param sc socket 通道 
	 * @param resource 浏览器请求的资源
	 * @param webAssertsPath 静态文件根目录 
	 */
	public static void writeHtml(SocketChannel sc, String resource, String webAssertsPath) {

		System.out.println("resource:" + resource);
		String filePath = webAssertsPath;

		if (resource == null) {
			return;
		}
		if (resource.equals( "/" )|| resource .equals( "/default.html")) {
			filePath += "/default.html";
		} else if (resource .equals( "/page.html")) {
			filePath += "/page.html";
		} else {
			filePath += "/notFound.html";
		}
		System.out.println("filePath:" + filePath);
		// 用相对路径 读取文件
		try {

			BufferedReader fileReader = new BufferedReader(new FileReader(filePath));

			String fileContent = "";
			String fileLine;

			try {

				// 读取文件
				while ((fileLine = fileReader.readLine()) != null) {
					fileContent += (fileLine + "\r\n");
				}
				int utf8FileContentLength = fileContent.getBytes("utf-8").length;
				ByteBuffer buf = ByteBuffer.allocate(utf8FileContentLength+200);
				buf.clear();
				// 写入缓冲区
				buf.put("HTTP/1.1 200 OK \r\n".getBytes());
				buf.put("Content-Type: text/html;charset=utf-8 \r\n".getBytes());
				buf.put(("Content-Length: " + utf8FileContentLength + "\r\n").getBytes());
				System.out.println("fileContent.length: "+fileContent.length()+"	fileContent.getBytes(utf-8).length:"+utf8FileContentLength);
				buf.put("\r\n".getBytes());
				buf.put(fileContent.getBytes());
				// 变化缓冲区的limit指针，方便写入
				buf.flip();
				while (buf.hasRemaining()) {
					// 写入数据至通道
					sc.write(buf);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 根据selector判断哪个channel需要做什么操作
	 * 
	 * @param port
	 */
	public static void selector(Config config) {

		Selector selector = null;
		ServerSocketChannel ssc = null;
		int port = config.port;

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
					
					//  避免  Exception in thread "main" java.nio.channels.CancelledKeyException
					// 加了好像跑得贼快 server ==
					if ( !key.isValid() ){ 
						continue;  
					}
					// 根据key的状态（客户端的状态）去执行对应的操作
					if (key.isAcceptable()) {
						handleAccept(key);
					}
					if (key.isReadable()) {
						handleRead(key, config.webAssetsPath);
					}
					if (key.isWritable()) {
						handleWrite(key);
					}
					if (key.isConnectable()) {
						System.out.println("is Connecting ...  ");
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
