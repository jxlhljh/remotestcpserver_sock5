package cn.gzsendi.stcp.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.gzsendi.system.utils.JsonUtil;
import cn.gzsendi.system.utils.YmlUtils;

public class VisitorCliConfig {
	
	private static final String fileName = "visitorCli.yml";
	
	public static final String token = YmlUtils.getValue(fileName, "token", "123456").toString();
	public static final boolean ssl = (boolean)YmlUtils.getValue(fileName, "ssl", false);
	
	public static final String trunnelHost = YmlUtils.getValue(fileName, "trunnelHost", "127.0.0.1").toString();
	public static final int trunnelPort = Integer.parseInt(YmlUtils.getValue(fileName, "trunnelPort", 7000).toString());

	public static final boolean needProxy = (boolean)YmlUtils.getValue(fileName, "needProxy", false);
	public static final String proxyType = YmlUtils.getValue(fileName, "proxyType", "tcp").toString();//tcp, sock5
	public static final String proxyHost = YmlUtils.getValue(fileName, "proxyHost", "127.0.0.1").toString();//
	public static final int proxyPort = Integer.parseInt(YmlUtils.getValue(fileName, "proxyPort", 1080).toString());
	public static final String proxyUsername = YmlUtils.getValue(fileName, "proxyUsername", "sendi").toString();//
	public static final String proxyPassword = YmlUtils.getValue(fileName, "proxyPassword", "sendi123").toString();//
	
	public static List<String> groups = new ArrayList<String>();
	public static List<String> frontPorts = new ArrayList<String>();
	
	static {
		
		Object configs = YmlUtils.getValue(fileName, "configs");
		if(configs != null) {
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Map<String,Object>> list = (List)configs;
			for(Map<String,Object> aConfig : list) {
				
				String groupStr = JsonUtil.getString(aConfig, "group");
				String frontPort = JsonUtil.getInteger(aConfig, "frontPort").toString();
				
				groups.add(groupStr);
				frontPorts.add(frontPort);
				
			}
			
		}
		
	}
	
}
