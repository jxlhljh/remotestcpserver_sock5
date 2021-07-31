package cn.gzsendi.stcp.visitor;

import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrontServer extends Thread{
	
	private final static Logger logger = LoggerFactory.getLogger(FrontServer.class);
	
	private int frontPort;
	private String groupName;//分组名称
	private int soTimeOut = 300000;//5分钟超时
	
	public FrontServer(int frontPort,String groupName){

		this.frontPort = frontPort;
		this.groupName = groupName;
		
	}
	
	public void run() {
		
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(frontPort);
			
			logger.info(groupName + " -> frontServer started , listen on " +serverSocket.getInetAddress().getHostAddress()+":"+ frontPort );

			// 一直监听，接收到新连接，则开启新线程去处理
			while (true) {
				
				Socket visitroCliSocket = serverSocket.accept();
				visitroCliSocket.setSoTimeout(soTimeOut);
				new FrontSocketThread(visitroCliSocket,groupName).start();
				
			}

		} catch (Exception e) {
			logger.error("",e);
		}
		
	}
	

}
