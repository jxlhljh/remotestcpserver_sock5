package cn.gzsendi.stcp.server.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.gzsendi.stcp.server.StcpServer;
import cn.gzsendi.stcp.server.StcpServerStart;
import cn.gzsendi.stcp.utils.FlowCounter;
import cn.gzsendi.system.exception.GzsendiException;
import cn.gzsendi.system.utils.JsonUtil;

public class ClientSocketThread extends Thread{
	
	private final static Logger logger = LoggerFactory.getLogger(ClientSocketThread.class);
	private final static String characterSet = "UTF-8";
	private static String token = StcpServerStart.token;
	private StcpServer stcpServer;
	private Socket clientSocket;
	private InputStream in;
	private OutputStream out;
	private DataInputStream din;
	private DataOutputStream dout;

	private String groupName;
	private String role;
	
	public ClientSocketThread(StcpServer stcpServer,Socket cliSocket){
		this.stcpServer = stcpServer;
		this.clientSocket = cliSocket;
	}
	
	public DataInputStream getDin() {
		return din;
	}
	
	public DataOutputStream getDout() {
		return dout;
	}
	
	public void run() {
		
		try {
			
			in = clientSocket.getInputStream();
			out = clientSocket.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			
			//解析头部信息
			Map<String,Object> headStr = readDataStr(din);
			
			String msgType = JsonUtil.getString(headStr, "msgType");//消息类型，controlConnect
			String tokenStr = JsonUtil.getString(headStr, "token");//gzsendi
			String groupName = JsonUtil.getString(headStr, "groupName");//stcp
			String role = JsonUtil.getString(headStr, "role");//control,visitor,controlCli,visitorCli
			if(StringUtils.isEmpty(msgType)) {
				throw new GzsendiException("msgType is null.");
			}
			if(StringUtils.isEmpty(tokenStr)) {
				throw new GzsendiException(groupName + " -> token is null.");
			}
			if(StringUtils.isEmpty(groupName)) {
				throw new GzsendiException(groupName + " -> groupName is null.");
			}
			if(StringUtils.isEmpty(role)) {
				throw new GzsendiException(groupName + " -> role is null.");
			}
			if(!tokenStr.equals(token)) {
				throw new GzsendiException(groupName + " -> token is error..");
			}
			this.groupName = groupName;
			this.role = role;
			
			if("control".equals(role)){
				
				new ControlHandler(this).handler();
				
			}else if("visitor".equals(role)){
				
				throw new GzsendiException(groupName + " -> visitor role not support...");
				//System.in.read();
				
			}else if("controlCli".equals(role)){
				
				new ControlCliHandler(this,headStr).handler();
				
			}else if("visitorCli".equals(role)){
				
				new VisitorCliHandler(this,headStr).handler();
				
			}else {
				
				throw new GzsendiException(groupName + " -> role not support...");
				
			}
			
			
		}catch (Exception e) {
			logger.error("",e);
		}finally {
			close();
		}
		
	}
	
	public synchronized void close(){
		
		try {
			
			if(clientSocket !=null && !clientSocket.isClosed()) {
				clientSocket.close();
				logger.info(groupName + " -> clientSocket  >>>> " + clientSocket +" socket closed ");
			}
			
		} catch (Exception e1) {
			logger.error("",e1);
		} finally {
			clientSocket = null;
		}
		
	}
	
	//读取socket消息头 , 0x070x07+字符串长度+Json字符串
	private Map<String,Object> readDataStr(DataInputStream dis) throws IOException{
		
		byte firstByte = dis.readByte();
		byte secondByte = dis.readByte();
		if( firstByte != 0x07 && secondByte != 0x07){
			throw new GzsendiException("unkown clientSocket.");
		}
		
		int resultlength = dis.readInt();
		byte[] datas = new byte[resultlength];
		int totalReadedSize = 0;
		while(totalReadedSize < resultlength) {
			int readedSize = dis.read(datas,totalReadedSize,resultlength-totalReadedSize);
			totalReadedSize += readedSize;
		}
		String headStr = new String(datas,characterSet);
		
		//记录进入StcpServer的流量
		FlowCounter.addReceivedSize(2 + resultlength);
		
		return JsonUtil.castToObject(headStr);
	}
	
	public StcpServer getStcpServer() {
		return stcpServer;
	}
	
	public String getGroupName() {
		return groupName;
	}

	public String getRole() {
		return role;
	}
	
	public Socket getClientSocket() {
		return clientSocket;
	}

}
