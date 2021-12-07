//
// Created by Fox on 2021/7/30.
//

#pragma once
#include "lightsocks_android_encryptor.h"
#include "event2/event.h"
#include "event2/event.h"
#include "event2/event.h"
#include "event2/listener.h"
#include "event2/bufferevent.h"
#include "event2/buffer.h"
using namespace std;
class lightsocks_android_server{

public:

    lightsocks_android_server(){};

//    lightsocks_android_server(lightsocks_android_encryptor& encryptor);

    int start();


private:

    static void proxy_listener_cb(evconnlistener *ev, evutil_socket_t sock, sockaddr *client_addr, int client_len, void *arg);

    static bufferevent_filter_result dest_filter_in(evbuffer *src, evbuffer *dst, ev_ssize_t dst_limit,
                                                    bufferevent_flush_mode mode, void *ctx);

    static bufferevent_filter_result dest_filter_out(evbuffer *src, evbuffer *dst, ev_ssize_t dst_limit,
                                               bufferevent_flush_mode mode, void *ctx);

    static void src_read_cb(bufferevent *bev, void *arg);

    static void dst_read_cb(bufferevent *bev, void *arg);

    static void proxy_event_cb(bufferevent *bev, short events, void *arg);
};
