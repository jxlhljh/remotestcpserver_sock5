@echo off

title _visitor.bat

java -cp "./lib/*;./remotestcpserver.jar" cn.gzsendi.stcp.visitor.VisitorCliStart -ssl false -token 123456 -trunnelHost 127.0.0.1 -trunnelPort 7000 -groups stcp1 -frontPorts 13306 -needProxy true -proxyType http -proxyHost 127.0.0.1 -proxyPort 1080 -proxyUsername sendi -proxyPassword sendi123

