@echo off

title _control.bat

java -cp "./lib/*;./remotestcpserver.jar" cn.gzsendi.stcp.control.ControlClientStart -ssl false -token gzsendi -trunnelHost 127.0.0.1 -trunnelPort 7000 -groups stcp1 -types tcp -remoteHosts 127.0.0.1 -remotePorts 3306 -needProxy true -proxyType http -proxyHost 127.0.0.1 -proxyPort 1080 -proxyUsername sendi -proxyPassword sendi123

