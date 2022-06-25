package cn.gzsendi.stcp.server.handler;

import java.io.IOException;
import java.util.Map;

import cn.gzsendi.stcp.utils.FlowCounter;
import cn.gzsendi.stcp.utils.MessageUtils;
import cn.gzsendi.system.exception.GzsendiException;
import cn.gzsendi.system.utils.JsonUtil;

public class VisitorCliHandler {
	
	private int bufferSize = 8092;
	private ControlCliHandler controlCliHandler;
	private ClientSocketThread clientSocketThread;
	private Map<String,Object> headStr;
	private final static String characterSet = "UTF-8";
	
	public VisitorCliHandler(ClientSocketThread clientSocketThread,Map<String,Object> headStr) {
		this.clientSocketThread = clientSocketThread;
		this.headStr = headStr;
	}
	
	public void handler() throws Exception{
		
		Map<String,VisitorCliHandler> visitorHandlers = clientSocketThread.getStcpServer().getVisitorHandlers();
		String globalTraceId = JsonUtil.getString(headStr, "globalTraceId"); 
		visitorHandlers.put(globalTraceId, this); //放入映射表

		try {
			
			Map<String, ClientSocketThread> groupNames = clientSocketThread.getStcpServer().getGroupNames();
			
			String groupName = clientSocketThread.getGroupName();
			String groupNameKey = groupName;
			
			//控制器如果没有连接
			if(!groupNames.containsKey(groupNameKey)) {
				throw new GzsendiException("no control role connected, groupName: " + groupName);
			}
			
			//下发连接绑定请求给control角色
			ClientSocketThread control = groupNames.get(groupNameKey);
			synchronized (control) {
				String bindRequestStr = "{\"msgType\":\"dataBindReq\",\"groupName\":\""+groupName+"\",\"globalTraceId\":\""+globalTraceId+"\"}";
				control.getDout().write(MessageUtils.bytessTart);
				control.getDout().writeInt(bindRequestStr.getBytes(characterSet).length);
				control.getDout().write(bindRequestStr.getBytes(characterSet));
				
				//记录流经StcpServer的流量
				FlowCounter.addSendSize(2 + 4 + bindRequestStr.getBytes(characterSet).length);
			}
			
			//10秒未绑定成功关闭
			//等10秒未绑定，则直接关闭
			long start = System.currentTimeMillis();
			while(getControlCliHandler() == null) {
				long currentTime = System.currentTimeMillis();
				if((currentTime - start ) > 10000l) { //如果超过30秒未绑定成功
					throw new GzsendiException("binded controlCli failed over 10s...");
				}
				Thread.sleep(10l);//
			}
			
			//绑定成功后进行数据转发
			
			//成功后进行数据转发
			byte[] data = new byte[bufferSize];
			int len = 0;
			while((len = this.clientSocketThread.getDin().read(data)) > 0){
				if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
					
					//记录流经StcpServer的流量
					FlowCounter.addReceivedSize(data.length);
					
					controlCliHandler.getClientSocketThread().getDout().write(data);
					
				}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
					byte[] dest = new byte[len];
					System.arraycopy(data, 0, dest, 0, len);
					
					//记录流经StcpServer的流量
					FlowCounter.addReceivedSize(dest.length);
					
					controlCliHandler.getClientSocketThread().getDout().write(dest);
					
				}
			}
			
			
		} catch (Exception e) {
			
			throw e;
			
		}finally {
			
			visitorHandlers.remove(globalTraceId);//移除映射表
			
			if(controlCliHandler != null) {
				controlCliHandler.getClientSocketThread().close();
				controlCliHandler = null;
			}
			
		}
		
	}
	
	public ControlCliHandler getControlCliHandler() {
		return controlCliHandler;
	}

	public void setControlCliHandler(ControlCliHandler controlCliHandler) {
		this.controlCliHandler = controlCliHandler;
	}
	
	public void returnBindSuccessMessage() throws IOException{
		String bindSuccessStr = "{\"msgType\":\"visitorCliConnectResp\"}";
		clientSocketThread.getDout().write(MessageUtils.bytessTart);
		clientSocketThread.getDout().writeInt(bindSuccessStr.getBytes(characterSet).length);
		clientSocketThread.getDout().write(bindSuccessStr.getBytes(characterSet));
	}
	
	public ClientSocketThread getClientSocketThread() {
		return clientSocketThread;
	}

	public void setClientSocketThread(ClientSocketThread clientSocketThread) {
		this.clientSocketThread = clientSocketThread;
	}
	

}
