## 2.1需求背景2分析
1. ##阿里云服务器启动服务端程序，内网1和内网2具有上网权限的2台机器启动客户端程序，主动建立连接1，2，并保持，如图
![在这里插入图片描述](https://img-blog.csdnimg.cn/ba42ad562b3743efb4563621aaa30e87.png)
2. ##内网1机器1主动发起访问内网2机器2的访问请求时，过程如下
>1).建立`连接3`，数据由内网1机器1发送请求给内网1机器2
>2).内网2机器2建立与阿里云的`连接4`,并通过连接2转发绑定请求给阿里云
>3).阿里云机器通过`连接1`将步骤2的转发请求转给内网2机器2
>4).内网2机器1建立与内网2机器2的`连接5`。
>5).连接5建立成功后，内网2机器2主动建立`连接6`,作为数据传输用通道
>6).最后在阿里云服务器进行 3->4>6>5的连接通道绑定，进行数据传输
![在这里插入图片描述](https://img-blog.csdnimg.cn/b743510053ea4c6386501f883651a86a.png)
3. ##通信结束后，关闭所有的资源，留下2条控制`连接1`和`连接2`
![在这里插入图片描述](https://img-blog.csdnimg.cn/ba42ad562b3743efb4563621aaa30e87.png)
## 2.2需求背景2的java实现部署使用步骤
1. ##拉取代码
```c
git clone https://github.com/jxlhljh/remotestcpserver_sock5.git

git clone https://gitee.com/jxlhljh/remotestcpserver_sock5.git

```
2. ##编绎
```c
maven clean package
```
3. ##启动示例

3.1 ####场景1：通过内网192.168.56.1的18899端口穿透访问172.168.201.20的8899端口如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/b743510053ea4c6386501f883651a86a.png)
```c
##服务端，在阿里云服务器103.212.12.74上部署，开启端7000监听
cd remotestcpserver_sock5
java -cp "./lib/*:./remotestcpserver_sock5.jar" cn.gzsendi.stcp.server.StcpServerStart -ssl false -serverPort 7000 -token 123456

##控制端，在内网2机器1（172.168.201.11）上部署
cd remotestcpserver_sock5
java -server -Xmx256m -Xms256m -Xmn128m -cp "./lib/*:./remotestcpserver_sock5.jar" cn.gzsendi.stcp.control.ControlClientStart -ssl true -token gzsendi -trunnelHost 103.212.12.74 -trunnelPort 7000 -groups stcp1 -types tcp -remoteHosts 172.168.201.20 -remotePorts 8899

##访问端，在内网1机器2（192.168.56.1）上部署
cd remotestcpserver_sock5
java -server -Xmx128m -Xms64m -Xmn64m -cp "./lib/*;./remotestcpserver_sock5.jar" cn.gzsendi.stcp.visitor.VisitorCliStart -ssl true -token gzsendi -trunnelHost 103.212.12.74 -trunnelPort 7000 -groups stcp1 -frontPorts 18899
```
`程序启动后，在内网1访问192.168.56.1的18899相当于访问内网2的172.168.201.20的8899端口`