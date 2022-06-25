package cn.gzsendi.stcp.visitor;

import java.util.Arrays;
import java.util.List;

import cn.gzsendi.stcp.config.VisitorCliConfig;

public class VisitorCliStart {

	public static String token = VisitorCliConfig.token;
	public static String trunnelHost = VisitorCliConfig.trunnelHost;
	public static int trunnelPort = VisitorCliConfig.trunnelPort;
	public static List<String> groups = VisitorCliConfig.groups;
	public static List<String> frontPorts = VisitorCliConfig.frontPorts;
	public static boolean ssl = VisitorCliConfig.ssl;
	public static boolean needProxy = VisitorCliConfig.needProxy;
	public static String proxyType = VisitorCliConfig.proxyType;
	public static String proxyHost = VisitorCliConfig.proxyHost;
	public static int proxyPort = VisitorCliConfig.proxyPort;
	public static String proxyUsername = VisitorCliConfig.proxyUsername;
	public static String proxyPassword = VisitorCliConfig.proxyPassword;

	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if ("-trunnelHost".equals(args[i])) {
				trunnelHost = args[i + 1];
				i++;
			} else if ("-trunnelPort".equals(args[i])) {
				trunnelPort = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-token".equals(args[i])) {
				token = args[i + 1];
				i++;
			} else if ("-groups".equals(args[i])) {
				groups = Arrays.asList(args[i + 1].split(","));
				i++;
			} else if ("-frontPorts".equals(args[i])) {
				frontPorts = Arrays.asList(args[i + 1].split(","));
				i++;

			} else if ("-proxyType".equals(args[i])) {
				proxyType = args[i + 1];
				i++;
			} else if ("-needProxy".equals(args[i])) {
				needProxy = Boolean.parseBoolean(args[i + 1]);
				i++;
			} else if ("-proxyHost".equals(args[i])) {
				proxyHost = args[i + 1];
				i++;
			} else if ("-proxyPort".equals(args[i])) {
				proxyPort = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-proxyUsername".equals(args[i])) {
				proxyUsername = args[i + 1];
				i++;
			} else if ("-proxyPassword".equals(args[i])) {
				proxyPassword = args[i + 1];
				i++;
			} else if ("-ssl".equals(args[i])) {
		    	ssl = Boolean.parseBoolean(args[i+1]);
		    	i++;
		    }

		}

		for (int i = 0; i < groups.size(); i++) {

			String groupName = groups.get(i);
			int port = Integer.parseInt(frontPorts.get(i));
			new FrontServer(port, groupName).start();

		}

	}

}
