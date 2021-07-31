package cn.gzsendi.stcp.visitor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.gzsendi.system.exception.GzsendiException;
import cn.gzsendi.system.utils.SnowflakeIdWorker;

public class FrontSocketThread extends Thread{
	
	private final static Logger logger = LoggerFactory.getLogger(FrontSocketThread.class);
	private int bufferSize = 8092;
	private String gloalTraceId = SnowflakeIdWorker.generateId().toString(); //
	private VisitorCli visitorCli;//绑定的visitorCli
	private Socket frontSocket;
	private String groupName;//分组名称
	
	private InputStream in;
	private OutputStream out;
	private DataInputStream din;
	private DataOutputStream dout;
	
	public FrontSocketThread(Socket frontSocket, String groupName){
		this.frontSocket = frontSocket;
		this.groupName = groupName;
	}
	
	public void run() {
		
		try {
			
			logger.info(groupName + " -> frontSocket>>>> " + frontSocket + "  connected" );
			
			in = frontSocket.getInputStream();
			out = frontSocket.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			
			//尝试进行后端绑定
			new VisitorCli(this,groupName,gloalTraceId).start();
			
			//等10秒未绑定，则直接关闭
			long start = System.currentTimeMillis();
			while(getVisitorCli() == null && getFrontSocket() != null) {
				long currentTime = System.currentTimeMillis();
				if((currentTime - start ) > 10000l) { //如果超过10秒未绑定成功
					throw new GzsendiException("binded visitorCli failed over 10s...");
				}
				Thread.sleep(10l);//
			}
			
			//成功后进行数据转发
			byte[] data = new byte[bufferSize];
			int len = 0;
			while((len = din.read(data)) > 0){
				if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
					
					this.visitorCli.getDout().write(data);
					
				}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
					byte[] dest = new byte[len];
					System.arraycopy(data, 0, dest, 0, len);
					
					this.visitorCli.getDout().write(dest);
					
				}
			}
			
			
		}catch (Exception e) {
			logger.error("",e);
		}finally {
			close();
			
			if(visitorCli !=null){
				
				try {
					visitorCli.close();
				} catch (Exception e1) {
					logger.error("",e1);
				} finally {
					visitorCli = null;
				}
				
			}
		}
		
	}
	
	public void close(){
		
		if(frontSocket !=null){
			
			try {
				frontSocket.close();
				logger.info(groupName + " -> frontSocket  >>>> " + frontSocket +" socket closed ");
			} catch (Exception e1) {
				logger.error("",e1);
			} finally {
				frontSocket = null;
			}
		}
		
			
	}
	
	public VisitorCli getVisitorCli() {
		return visitorCli;
	}

	public void setVisitorCli(VisitorCli visitorCli) {
		this.visitorCli = visitorCli;
	}
	
	public DataOutputStream getDout() {
		return dout;
	}
	
	public Socket getFrontSocket() {
		return frontSocket;
	}


}
