package com.demo.tcp.java;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public String method;
    public String resource;
    public float httpVersion;
    public Map<String,String> headers;
    
    
    public HttpRequest(){
    	this.headers = new HashMap<String,String>();
    }
    
    
    @Override
    public String toString(){
    	
    	StringBuffer s = new StringBuffer();
    	s.append("------------------ HttpRequest Object OutPut -----------------\r\n");
    	s.append("http method:"+method +"\r\n");
    	s.append("http request resource:"+resource +"\r\n");
     	s.append("http version:"+ httpVersion +"\r\n");
     	s.append("http headers:\r\n");
     	for(String key:headers.keySet()){
     		String value = headers.get(key);
     		s.append(key+"&"+value+"\r\n");
     	}
      	s.append("-------------------------------------------------------------");
      	
      	return s.toString();
    }
}
