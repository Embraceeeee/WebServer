package com.demo.tcp.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class NioServer {
	
	
	private static final int BUF_SIZE=1024;
	private static final int TIMEOUT= 3000;

	public static void main(String[] args)  throws Exception  {
		// TODO Auto-generated method stub
		
		InputStream input = Server.class.getResourceAsStream("../config/config.xml");
		JacksonXmlModule module = new JacksonXmlModule();
		// 核心对象
		XmlMapper mapper = new XmlMapper(module);
		// 读取字节流并返回一个 JavaBean
		Config config = (Config)mapper.readValue(input, Config.class);
		
		selector(config.port);

	}
	
	
	
	public static void  handleAccept(SelectionKey key) throws  IOException {
		
		
		ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssChannel.accept();
		sc.configureBlocking(false);
		sc.register(key.selector(), SelectionKey.OP_READ,ByteBuffer.allocateDirect(BUF_SIZE));
		
	}
	
	
public static void  handleRead(SelectionKey key) throws  IOException {
		
		
		
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer buf = (ByteBuffer) key.attachment();
		// 从通道里面读取数据至buffer中
		long bytesRead = sc.read(buf);
		
		while(bytesRead>0) {
			buf.flip();
			while(bytesRead>0){
				
				buf.flip();
				while(buf.hasRemaining()){
					System.out.println((char)buf.get());
				}
				System.out.println();
				buf.clear();
				bytesRead = sc.read(buf);
				
			}
			
			if(bytesRead == -1){
				sc.close();
			}
			
		}
		
	
	}

public static void  handleWrite (SelectionKey key) throws  IOException {
	
	
	ByteBuffer buf = (ByteBuffer) key.attachment();
	buf.flip();
	SocketChannel sc  = (SocketChannel) key.channel();
	
	while(buf.hasRemaining()){
		sc.write(buf);
	}
	// buf中暂未读完的数据先暂存 
	buf.compact();
	
}



 public static void selector(int port){
	 
	 Selector selector = null;
	 ServerSocketChannel ssc = null;
	 
	 try{
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
		 
		 while(true){
			 if(selector.select(TIMEOUT)  == 0 ){
				 System.out.println("==");
				 continue;
			 }
			 
			 Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			 
			 while(iter.hasNext() ){
				 // 遍历selectionkey
				 SelectionKey key = iter.next();
				 // 根据key的状态（客户端的状态）去执行对应的操作
				 if(key.isAcceptable() ){
					 handleAccept(key);
				 }
				 if(key.isReadable() ){
					 handleRead(key);
				 }if(key.isWritable() ){
					 handleWrite(key);
				 }
				 if(key.isConnectable()  ){ 
					 System.out.println("is Connecting ... ");
				 }
				// 该方法移除的是iterator.next() 最后访问的元素.
				 iter.remove();
			 }
		 }
		 
		 
	 }catch(IOException e){
		 e.printStackTrace();
	 }finally{
		 try {
				if (selector != null) {
					// 关闭  选择器 
					selector.close();
				}
				if (ssc != null) {
					// 关闭  服务器通道 
					ssc.close();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
	 }
	 
	 
	 
 }

}
