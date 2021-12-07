add_library( # Sets the name of the library.
        tun2socks

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        tun2socks.h
        tun2socks.c
        SocksUdpGwClient.c
        SocksUdpGwClient.h
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )
# Links the top-level CMake build output against lib_gmath.
target_link_libraries(tun2socks ${log-lib})

target_link_libraries(tun2socks system flow tuntap lwip socksclient udpgw_client socks_udp_client)
