//
// Created by Fox on 2021/7/10.
//

#ifndef LIGHTSOCKSDROID_TESTLIB_H
#define LIGHTSOCKSDROID_TESTLIB_H
bool test();
#endif //LIGHTSOCKSDROID_TESTLIB_H

int start(int tunfd,
          int tunmtu,
          const char* if_addr,
          const char* if_netmask,
          const char* socks5_server_addr,
          const char* udp_server_addr);