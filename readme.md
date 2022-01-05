## 你也可以做个Shadowsocks Android篇[WIP]

### 基础：什么是tun

TUN是操作系统内核中的虚拟网络设备。不同于普通靠硬件网络适配器实现的设备，这些虚拟的网络设备全部用软件实现，并向运行于操作系统上的软件提供与硬件的网络设备完全相同的功能。

TAP等同于一个以太网设备，它操作第二层数据包如以太网数据帧。TUN模拟了网络层设备，操作第三层数据包比如IP数据包。

操作系统通过TUN/TAP设备向绑定该设备的用户空间的程序发送数据，反之，用户空间的程序也可以像操作硬件网络设备那样，通过TUN/TAP设备发送数据。

### ToyVPN解析：

拓扑参考：
https://github.com/daBisNewBee/Notes/blob/master/ToyVpn.md

App -> VpnService -> DatagramChannel -> ToyVpnServer -> tun0 -> iptables route to eth0

（1）ifconfig tun0 10.0.0.1 dstaddr 10.0.0.2 up的作用：

参考https://stackoverflow.com/questions/36375530/what-is-the-destination-address-for-a-tap-tun-device, What is the destination (peer) address?

一般为接口如eth0设置ip地址时，如ifconfig eth0 192.168.1.123，则表示192.168.1.0/24这个子网是可以通过这个接口直连的，内核会增加相应的一条路由；
当使用dstaddr时，则表示这个接口点对点链接的另一端是10.0.0.2，则内核会增加一条路由，凡是发往10.0.0.2这个地址的数据，都会走这个接口；
所以外部当有数据需要发往到10.0.0.2(即客户端的ip)时，会通过tun这个接口发送出去，这时ToyVpnServer就可以通过读取tun的文件描述符得到数据，再通过channel转发到客户端；

（2）iptables -t nat -A POSTROUTING -s 10.0.0.0/8 -o eth0 -j MASQUERADE的作用

ToyVpnServer会把tunnel接收到的客户端数据，写入到tun0这个虚拟设备，这条命令配置了tun0的转发规则，客户端的源地址为10.0.0.2，则其数据包会通过eth0这个设备发送出去；

（3）IP in UDP隧道

所谓隧道，就是用一种网络协议，将另一种协议的数据包，封装在负载部分（相当于一般数据）进行发送。

Android的VpnService中可以获取到系统转发过来的IP协议的数据包，然后将这些数据包当作一般的数据，通过DatagramChannel（使用UDP协议）发送到ToyVpnServer；ToyVpnServer通过socket读取到数据，即原始的数据包，再将这些数据包写入到tun0中，对tun0来说就好像直接收到了源地址为10.0.0.2的客户端（即VpnService.Builder#address设置的值) 发来的数据包一样；

 
### tun2socks

Tun2socks是badvpn的一部分，作用是读取系统中发送到虚拟网卡tun的数据包（注意tun设备工作在IP层的，tun2socks读取到的是IP协议的数据包），然后通过协议栈的解析，转换为第5层的socks5协议。

tun2socks 官方例子解析：

需要手动新建一个tun设备，并为这个接口设置10.0.0.1的IP；

在Android中，tun设备由系统建立并返回一个文件描述符给我们，IP也是在建立VpnService时设置即可；

启动tun2socks：
badvpn-tun2socks --tundev <tun_spec> --netif-ipaddr 10.0.0.2 --netif-netmask 255.255.255.0 --socks-server-addr 127.0.0.1:1080

--netif-ipaddr 10.0.0.2的作用应该是类似ToyVpn，设置tun0连接的网关/另一端为10.0.0.2；

配合 route add 0.0.0.0 mask 0.0.0.0 10.0.0.2 metric 6 这条命令，把所有流量转发到10.0.0.2，所以系统会把流量转发到tun0中，再由tun2socks读取(相当于tun2socks就是一个ip为10.0.0.2的路由器)，然后转为socks5协议发送出去；

App -> iptables route to 10.0.0.2 -> tun0 -> tun2socks -> socks5 server

在Android中，系统默认为我们添加了转发规则，所有流量都会发送到tun0中，所以不需要自己添加防火墙规则，不过netif-ipaddr还是和tun0在同一子网比较靠谱。

#### ndk编译tun2socks 

需要修改的地方：
https://www.brobwind.com/archives/824

### 关于转发udp和dns:

在PC的浏览器上设置socks代理，浏览器会把域名也放到socks数据包中，由服务器做解析并返回数据，所以只需要用到tcp的协议。但是对于VpnService这种透明代理来说，域名的解析需要由客户端发起，所以代理需要同时支持udp协议（特别是科学上网的情况下）。

socks5协议是支持udp的，简单的过程是客户端连接socks5服务后，发送udp associate命令，服务端另外打开一个udp端口并告诉客户端，客户端然后就将udp数据包发向该端口，由服务端转发。

流程见https://stackoverflow.com/a/47079318/4376839

注意这是一个UDP over UDP的隧道 https://stackoverflow.com/questions/41967217/why-does-socks5-require-to-relay-udp-over-udp

Tun2socks没有用这种方式，而是自己实现了一个UDP over TCP的隧道，由于这不是协议标准，所以需要在服务端额外打开一个程序，用来解析数据。

#### udpgw

https://github.com/ambrop72/badvpn/issues/15

The udpgw mechanism works such that tun2socks establishes a TCP connection to udpgw, through the SOCKS server. Udpgw sends and receives UDP packets using normal OS network access.

在服务端运行：badvpn-udpgw --listen-addr 127.0.0.1:7300

整个流程是，当客户端有udp包需要发送时，首先通过一般的socks5协议，经过socks5代理服务器和badvpn-udpgw建立tcp连接，然后再在该连接上发送需要转发的udp包（这时整个udp包作为数据），由badvpn-udpgw解析并发送实际的udp包。

在tun2socks.c中device_read_handler_send是一个callback，当读取到tun设备时会被回调，同时获得从tun中读取的数据包，然后会调用process_device_udp_packet ，处理udp的数据包；最终会调用到udpgw_client/UdpGwGlient.c中的connection_send()，在connect_send()中可以看到将flag标记（如该包是否是dns包，如果是dns包，udpgw应该是直接用服务端设置的dns服务器，而忽略原包的目标地址），数据包原地址和目标地址，以及整个原udp包（包括header）一起作为payload写入缓存中进行发送。

#### udpgw的编译

badvpn里已经包含了编译udpgw的脚本，在badvpn上一级目录执行以下命令即可在当前目录生成udpgw可执行文件。（如果在badvpn里执行，输出的udpgw文件会和原有目录同名冲突）

    env CC=gcc SRCDIR=badvpn-master ENDIAN=little ./badvpn-master/compile-udpgw.sh

#### docker

可以使用docker目录下的dockerfile建立一个docker image，里边已经包含了lightsocks和udpgw。

运行：

    docker run -p 12315:12315 -p 7300:7300 -d --name lightsocks ${image名称}
    
查看自动生成的配置：

    docker exec -t lightsocks cat /root/.lightsocks.json
    
### ndk编译libevent

直接用libevent的CMakeLists.txt，在gradle传入正确的cmake参数即可
"-DANDROID=TRUE", 
"-DEVENT__DISABLE_OPENSSL=TRUE", 跳过不使用的库
"-DEVENT__DISABLE_MBEDTLS=TRUE", 
"-DEVENT__DISABLE_BENCHMARK=TRUE", 
"-DEVENT__DISABLE_SAMPLES", 
"-DEVENT__LIBRARY_TYPE=STATIC" 编译成静态库