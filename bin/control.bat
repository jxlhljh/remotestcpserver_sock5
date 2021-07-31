@echo off

title _control.bat

::java -cp "./lib/*;./remotestcpserver_sock5.jar" cn.gzsendi.stcp.control.ControlClientStart -ssl true -token gzsendi -trunnelHost 127.0.0.1 -trunnelPort 7000 -groups stcp1 -types tcp -remoteHosts 192.168.60.133 -remotePorts 8899

::where types is sock5, then remoteHosts and remotePorts value will be ignored, but must input a value to aviod nullpoint exception.
java -cp "./lib/*;./remotestcpserver_sock5.jar" cn.gzsendi.stcp.control.ControlClientStart -ssl true -token gzsendi -trunnelHost 127.0.0.1 -trunnelPort 7000 -groups stcp1,stcp2 -types sock5,tcp -remoteHosts 192.168.0.103,192.168.0.103 -remotePorts 55314,55314

