package cn.gzsendi.stcp.server.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.gzsendi.stcp.utils.FlowCounter;
import cn.gzsendi.stcp.utils.MessageUtils;
import cn.gzsendi.system.exception.GzsendiException;
import cn.gzsendi.system.utils.JsonUtil;

import com.alibaba.fastjson.JSONObject;


public class ControlHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(ControlHandler.class);
	
	private ClientSocketThread clientSocketThread;
	private final static String characterSet = "UTF-8";
	
	public ControlHandler(ClientSocketThread clientSocketThread) {
		this.clientSocketThread = clientSocketThread;
	}
	
	public void handler() throws IOException{
		
		Map<String, ClientSocketThread> groupNames = clientSocketThread.getStcpServer().getGroupNames();
		String groupName = clientSocketThread.getGroupName();
		
		//控制器只有一个
		if(groupNames.containsKey(groupName)) {
			throw new GzsendiException("duplicate control, groupName: " + groupName);
		}
		
		try {
			
			//放入内存
			groupNames.put(groupName, clientSocketThread);
			
			while(true){
				
				JSONObject dataStr = readDataStr(clientSocketThread.getDin());
				String msgType = dataStr.getString("msgType");//消息类型，controlConnect,controlHeart,visitorConnect,visitorHeart,dataBindReq
				
				//心跳回应
				if("controlHeart".equals(msgType)) {
					
					logger.info(groupName + " -> "  + clientSocketThread + " receive heart pkg request.");
					
					DataOutputStream dout = clientSocketThread.getDout();
					dout.write(MessageUtils.bytessTart);
					String heartStr = "{\"msgType\":\"controlHeart\"}";
					dout.writeInt(heartStr.getBytes(characterSet).length);
					dout.write(heartStr.getBytes(characterSet));
					
				}else {
					
					throw new GzsendiException("unknown msgType.");
					
				}
				
			}
			
		} catch (Exception e) {
			throw e;
		}finally {
			
			groupNames.remove(groupName);
			
		}
		
	}
	
	//读取socket消息头 , 0x070x07+字符串长度+Json字符串
	private JSONObject readDataStr(DataInputStream dis) throws IOException{
		
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
		
		//记录流经StcpServer的流量
		FlowCounter.addReceivedSize(2 + resultlength);
		
		return JsonUtil.fromJson(headStr);
	}

}
