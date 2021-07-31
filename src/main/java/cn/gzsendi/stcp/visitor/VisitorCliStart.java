package cn.gzsendi.stcp.visitor;

import java.util.Arrays;
import java.util.List;

public class VisitorCliStart {

	public static String token = "123456";
	public static String trunnelHost = "127.0.0.1";
	public static int trunnelPort = 7000;
	public static List<String> groups = Arrays.asList("stcp1");
	public static List<String> frontPorts = Arrays.asList("8899");
	public static boolean ssl = true;
	public static boolean needProxy = false;
	public static String proxyType = "socks";
	public static String proxyHost = "127.0.0.1";
	public static int proxyPort = 1080;
	public static String proxyUsername = "sendi";
	public static String proxyPassword = "sendi123";

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
