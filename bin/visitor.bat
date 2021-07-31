@echo off

title _visitor.bat

java -cp "./lib/*;./remotestcpserver_sock5.jar" cn.gzsendi.stcp.visitor.VisitorCliStart -ssl true -token gzsendi -trunnelHost 127.0.0.1 -trunnelPort 7000 -groups stcp1,stcp2 -frontPorts 1080,55314

