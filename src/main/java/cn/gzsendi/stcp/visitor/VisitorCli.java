package cn.gzsendi.stcp.visitor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.gzsendi.stcp.server.StcpServer;
import cn.gzsendi.stcp.utils.MessageUtils;
import cn.gzsendi.stcp.utils.SocketFactory;
import cn.gzsendi.system.exception.GzsendiException;
import cn.gzsendi.system.utils.JsonUtil;

import com.alibaba.fastjson.JSONObject;

public class VisitorCli extends Thread{
	private final static String characterSet = "UTF-8";
	private int bufferSize = 8092;
	private final static Logger logger = LoggerFactory.getLogger(VisitorCli.class);
	private int soTimeOut = 300000;//5分钟超时
	private FrontSocketThread frontSocketThread;
	private DataOutputStream dout = null;
	private Socket visitorCliSocket;
	private String groupName;//分组名称
	private String globalTraceId;
	
	String token = VisitorCliStart.token;
	String role = "visitorCli";
	String trunnelHost = VisitorCliStart.trunnelHost;
	int trunnelPort = VisitorCliStart.trunnelPort;
	
	private boolean needProxy = VisitorCliStart.needProxy;
	private String proxyType = VisitorCliStart.proxyType;
	private String proxyHost = VisitorCliStart.proxyHost;
	private int proxyPort = VisitorCliStart.proxyPort;
	private String proxyUsername = VisitorCliStart.proxyUsername;
	private String proxyPassword = VisitorCliStart.proxyPassword;
	
	public VisitorCli(FrontSocketThread frontSocketThread,String groupName,String globalTraceId){
		this.frontSocketThread = frontSocketThread;
		this.globalTraceId = globalTraceId;
		this.groupName = groupName;
	}
	
	private Proxy createProxy(String proxyAddr, int proxyPort) {
    	
    	// 设置认证
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
            }
        });
        
        // 设置Socks代理
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,new InetSocketAddress(proxyAddr, proxyPort));
        return proxy;
        
    }
	
	public void run() {
		
		InputStream in = null;
		OutputStream out = null;
		DataInputStream din = null;
		
		try {
			
			if(VisitorCliStart.ssl == false) {
				
				if(needProxy) {
					
					if("socks".equals(proxyType)){
			    		
						Proxy proxy = createProxy(proxyHost, proxyPort);
						visitorCliSocket = new Socket(proxy);
						visitorCliSocket.setSoTimeout(soTimeOut);//5分钟未收到数据，自动关闭资源
						visitorCliSocket.connect(new InetSocketAddress(trunnelHost, trunnelPort));
			    		
			    	}else {
			    		//"http".equals(proxyType))
			    		visitorCliSocket = SocketFactory.createHttpProxySocket(trunnelHost, trunnelPort, proxyHost, proxyPort,proxyUsername,proxyPassword);
			    	}
					
				}else {
					
					//无代理
					visitorCliSocket = new Socket();
					visitorCliSocket.setSoTimeout(soTimeOut);//5分钟未收到数据，自动关闭资源
					visitorCliSocket.connect(new InetSocketAddress(trunnelHost, trunnelPort));
					
				}
				
			}else {
				
				SSLContext ctx = SSLContext.getInstance("SSL");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		        KeyStore ks = KeyStore.getInstance("JKS");
		        KeyStore tks = KeyStore.getInstance("JKS");
		        
		        InputStream kclientIn = StcpServer.class.getClassLoader().getResourceAsStream("cert/kclient.ks");
		        InputStream tclientIn = StcpServer.class.getClassLoader().getResourceAsStream("cert/tclient.ks");
		        ks.load(kclientIn, "sendiclientpass".toCharArray());
		        tks.load(tclientIn, "sendiclientpublicpass".toCharArray());
		        kclientIn.close();
		        tclientIn.close();
		        
		        kmf.init(ks, "sendiclientpass".toCharArray());
		        tmf.init(tks);
		        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		        visitorCliSocket = (SSLSocket) ctx.getSocketFactory().createSocket(trunnelHost, trunnelPort);
		        visitorCliSocket.setSoTimeout(soTimeOut);//5分钟未收到数据，自动关闭资源
				
			}
			
			logger.info(groupName + " -> visitorCliSocket>>>> " + visitorCliSocket + "  connected" );
			
			in = visitorCliSocket.getInputStream();
			out = visitorCliSocket.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			
			//发送建立绑定请求
			//进行第一个数据包下发，告诉服务器当前是什么角色,
			dout.write(MessageUtils.bytessTart);
			String headStr = "{\"msgType\":\"visitorCliConnect\",\"token\":\""+token+"\",\"role\":\""+role+"\",\"groupName\":\""+groupName+"\",\"globalTraceId\":\""+globalTraceId+"\"} ";
			dout.writeInt(headStr.getBytes(characterSet).length);
			dout.write(headStr.getBytes(characterSet));
			
			//获取绑定成功返回包
			JSONObject returnPackageStr = readDataStr(din);
			String msgType = returnPackageStr.getString("msgType");//消息类型，controlConnect
			if("visitorCliConnectResp".equals(msgType)){
				
				//成功后设置绑定成功
				frontSocketThread.setVisitorCli(this);
				
			}else {
				throw new GzsendiException("visitorCliConnect error..");
			}
			
			//成功后进行数据转发
			byte[] data = new byte[bufferSize];
			int len = 0;
			while((len = din.read(data)) > 0){
				if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
					
					frontSocketThread.getDout().write(data);
					
				}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
					byte[] dest = new byte[len];
					System.arraycopy(data, 0, dest, 0, len);
					
					frontSocketThread.getDout().write(dest);
					
				}
			}
			
			
			
			
		} catch (Exception e) {
			logger.error("",e);
		}finally {
			close();	
			
			if(frontSocketThread != null) {
				frontSocketThread.close();
				frontSocketThread = null;
			}
			
		}
		
	}
	
	public void close(){
		
		if(visitorCliSocket !=null){
			
			try {
				visitorCliSocket.close();
				logger.info(groupName + " -> visitorCliSocket  >>>> " + visitorCliSocket +" socket closed ");
			} catch (Exception e1) {
				logger.error("",e1);
			} finally {
				visitorCliSocket = null;
			}
			
		}
			
	}
	
	public DataOutputStream getDout() {
		return dout;
	}
	
	//读取socket消息头 , 0x070x07+字符串长度+Json字符串
	private JSONObject readDataStr(DataInputStream dis) throws IOException{
		
		byte firstByte = dis.readByte();
		byte secondByte = dis.readByte();
		if( firstByte != 0x07 && secondByte != 0x07){
			throw new GzsendiException("unkown dataPackage.");
		}
		
		int resultlength = dis.readInt();
		byte[] datas = new byte[resultlength];
		int totalReadedSize = 0;
		while(totalReadedSize < resultlength) {
			int readedSize = dis.read(datas,totalReadedSize,resultlength-totalReadedSize);
			totalReadedSize += readedSize;
		}
		String headStr = new String(datas,characterSet);
		return JsonUtil.fromJson(headStr);
	}

}
