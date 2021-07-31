package cn.gzsendi.stcp.server.handler;

import java.io.DataInputStream;
import java.util.Map;

import cn.gzsendi.stcp.utils.FlowCounter;
import cn.gzsendi.system.exception.GzsendiException;

import com.alibaba.fastjson.JSONObject;

public class ControlCliHandler {
	
	private int bufferSize = 8092;
	private VisitorCliHandler visitorCliHandler;
	private ClientSocketThread clientSocketThread;
	private JSONObject headStr;
	
	public ControlCliHandler(ClientSocketThread clientSocketThread,JSONObject headStr){
		this.clientSocketThread = clientSocketThread;
		this.headStr = headStr;
	}
	
	public void handler() throws Exception{

		try {
			
			Map<String,VisitorCliHandler> visitorHandlers = clientSocketThread.getStcpServer().getVisitorHandlers();
			String globalTraceId = headStr.getString("globalTraceId"); 
			VisitorCliHandler visitorCliHandler = visitorHandlers.get(globalTraceId);
			
			//visitorCliHandler没有连接
			if(visitorCliHandler == null) {
				throw new GzsendiException("no visitorCliHandler connected, globalTraceId: " + globalTraceId);
			}
			
			//绑定成功
			this.visitorCliHandler = visitorCliHandler;
			this.visitorCliHandler.setControlCliHandler(this);
			this.visitorCliHandler.returnBindSuccessMessage();
			
			//绑定成功后进行数据转发
			byte[] data = new byte[bufferSize];
			int len = 0;
			DataInputStream din = this.getClientSocketThread().getDin();
			while((len = din.read(data)) > 0){
				if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
					
					//记录进入StcpServer的流量
					FlowCounter.addReceivedSize(data.length);
					
					visitorCliHandler.getClientSocketThread().getDout().write(data);
					
				}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
					byte[] dest = new byte[len];
					System.arraycopy(data, 0, dest, 0, len);
					
					//记录进入StcpServer的流量
					FlowCounter.addReceivedSize(dest.length);
					
					visitorCliHandler.getClientSocketThread().getDout().write(dest);
					
				}
			}
			
			
		} catch (Exception e) {
			
			throw e;
			
		}finally {
			
			if(visitorCliHandler != null) {
				
				visitorCliHandler.getClientSocketThread().close();
				
			}
			
		}
		
	}
	
	public ClientSocketThread getClientSocketThread() {
		return clientSocketThread;
	}

	public void setClientSocketThread(ClientSocketThread clientSocketThread) {
		this.clientSocketThread = clientSocketThread;
	}

}
