package cn.gzsendi.stcp.server;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.gzsendi.stcp.server.handler.ClientSocketThread;
import cn.gzsendi.stcp.server.handler.VisitorCliHandler;
import cn.gzsendi.stcp.utils.FlowCounter;

/**
 * ------------------------------------------->>>
协议约定：
String controlConnect --->>> {\"msgType\":\"cotrolConnect\",\"token\":\"gzsendi\",\"role\":\"control\",\"groupName\":\"stcp\"} 
String controlHeart   --->>> {\"msgType\":\"controlHeart\"} 
String visitorCliConnect --->>> {\"msgType\":\"visitorCliConnect\",\"token\":\"gzsendi\",\"role\":\"visitorCli\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
String visitorCliConnectResp --->>> {\"msgType\":\"visitorCliConnectResp\"} 
String dataBindReq --->>> {\"msgType\":\"dataBindReq\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
String controlCliConnect --->>> {\"msgType\":\"controlCliConnect\",\"token\":\"gzsendi\",\"role\":\"controlCli\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
 * @author liujh
 *
 */
public class StcpServer {
	
	private final static Logger logger = LoggerFactory.getLogger(StcpServer.class);
	private int serverPort;
	private int soTimeOut = 300000;//5分钟超时
	private Map<String,ClientSocketThread> groupNames = new ConcurrentHashMap<String,ClientSocketThread>();
	private Map<String,VisitorCliHandler> visitorHandlers = new ConcurrentHashMap<String,VisitorCliHandler>();
	
	public Map<String, VisitorCliHandler> getVisitorHandlers() {
		return visitorHandlers;
	}

	public Map<String, ClientSocketThread> getGroupNames() {
		return groupNames;
	}

	public StcpServer(int serverPort){

		this.serverPort = serverPort;
		
	}
	
	public void startServer(){
		
		//启动检查线程
		StcpServerCheckCheck check = new StcpServerCheckCheck();
		check.setDaemon(true);
		check.start();
		
		ServerSocket serverSocket = null;
		try {
			
			if(StcpServerStart.ssl == false) {
				serverSocket = new ServerSocket(serverPort);
			}else {
				
				SSLContext ctx = SSLContext.getInstance("SSL");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		        KeyStore ks = KeyStore.getInstance("JKS");
		        KeyStore tks = KeyStore.getInstance("JKS");
		    
		        InputStream kserverIn = StcpServer.class.getClassLoader().getResourceAsStream("cert/kserver.ks");
		        InputStream tserverIn = StcpServer.class.getClassLoader().getResourceAsStream("cert/tserver.ks");
		        ks.load(kserverIn, "sendiserverpass".toCharArray());
		        tks.load(tserverIn, "sendiserverpublicpass".toCharArray());
		        kserverIn.close();
		        tserverIn.close();
		        
		        kmf.init(ks, "sendiserverpass".toCharArray());
		        tmf.init(tks);
		        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		        serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(serverPort);
		        ((SSLServerSocket)serverSocket).setNeedClientAuth(true);				
				
			}
			
			logger.info("stcpServer started , listen on " +serverSocket.getInetAddress().getHostAddress()+":"+ serverPort );

			// 一直监听，接收到新连接，则开启新线程去处理
			while (true) {
				
				Socket clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(soTimeOut);
				new ClientSocketThread(this,clientSocket).start();
				
			}

		} catch (Exception e) {
			logger.error("",e);
		}
		
	}
	
	//心跳包发送
	private class StcpServerCheckCheck extends Thread {
		
		public void run() {
			
			while(true) {
				
				try {
					Thread.sleep(30000l);
				} catch (InterruptedException e) {
					logger.error("",e);
				}
				
				logger.info("groupNames.size ->>>> {}", groupNames.size());
				for(ClientSocketThread clientSocketThread : groupNames.values()) {
					logger.info("groupName: " + clientSocketThread.getGroupName() + " id:" + clientSocketThread.hashCode() +  " ->>>> ");
				}
				
				logger.info("visitorHandlers.size ->>>> {}", visitorHandlers.size());
				for(String globalTraceId : visitorHandlers.keySet()) {
					logger.info("groupName: " + visitorHandlers.get(globalTraceId).getClientSocketThread().getGroupName() + " globalTraceId: " + globalTraceId+ " ->>>> ");
				}
				
				logger.info("Stcpserver total transfer flow size ->>>> {}", formatSize(FlowCounter.totalReceivedSize));
				logger.info("");
				
			}
			
		}
		
	}
	
	private String formatSize(long totalSize){
		
		if(totalSize < 1024) {
			return totalSize + "Byte";
		}
		
		if(totalSize < 1024 * 1024) {
			return totalSize /(1024) + "KB";
		}
		
		return totalSize /(1024*1024) + "MB";
		
	}

}
