@echo off

title _stcpserver.bat

java -cp "./lib/*;./remotestcpserver_sock5.jar" cn.gzsendi.stcp.server.StcpServerStart -ssl false -serverPort 7000 -token 123456

