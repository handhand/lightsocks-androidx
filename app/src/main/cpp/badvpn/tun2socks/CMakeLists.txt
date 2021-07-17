add_executable(libbadvpn-tun2socks.so#在android 10中只有这样命名安装apk时才会被放到可执行目录中
    tun2socks.c
    SocksUdpGwClient.c
)

target_link_libraries(libbadvpn-tun2socks.so system flow tuntap lwip socksclient udpgw_client socks_udp_client)

#install(
#    TARGETS badvpn-tun2socks
#    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
#)
#
#install(
#    FILES badvpn-tun2socks.8
#    DESTINATION ${CMAKE_INSTALL_MANDIR}/man8
#)
