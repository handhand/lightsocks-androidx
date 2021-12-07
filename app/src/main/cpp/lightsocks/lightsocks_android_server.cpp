//
// Created by Fox on 2021/7/30.
//
#include "include/lightsocks_android_server.h"
#include <string>
#include "event2/event.h"
#include "event2/event.h"
#include "event2/listener.h"
#include "event2/bufferevent.h"
#include "event2/buffer.h"
#include <iostream>
#include <signal.h>
#include <memory.h>
#include <map>
#include <netinet/in.h>
#include <endian.h>
#include "android/log.h"

using namespace std;

//lightsocks_android_server::lightsocks_android_server(lightsocks_android_encryptor& enc): encryptor{enc} {}

int lightsocks_android_server::start(){
    //when send data to closed socket, error signal will send and app will crash
    //so ignore this error signal, don't crash the app
    if (signal(SIGPIPE, SIG_IGN) == SIG_ERR)
    {
        return -1;
    }

    //1. new a context
    event_base *base = event_base_new();
    if (!base)
    {
        return -2;
    }

    //2. new a listening socket
    sockaddr_in sin{};
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(6666);

    //3. listene and bind
    evconnlistener *ev = evconnlistener_new_bind(base,
                                                 proxy_listener_cb,
                                                 base, //parameter to pass to cb
                                                 LEV_OPT_REUSEABLE | LEV_OPT_CLOSE_ON_FREE /*close socket when libevent is done*/,
                                                 10, //max pending incoming connections
                                                 (sockaddr *)&sin,
                                                 sizeof(sin));

    //start main loop
    event_base_dispatch(base);
    evconnlistener_free(ev);
    event_base_free(base);
    return 0;
}

/**
 * incoming connection callback
 *
 * accept a server socket to src
 * new a client socket to dst -> dst hard code to localhost 5001 which is a echo server
 * */
void lightsocks_android_server::proxy_listener_cb(evconnlistener *ev, int sock, sockaddr *client_addr,
                                              int client_len, void *arg) {
    bufferevent *src_bev;
    bufferevent *dst_bev;
    bufferevent *dst_filter_bev;
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "haha proxy listener cb");
    //accept socket to src as server
    src_bev = bufferevent_socket_new((event_base *)arg, sock, BEV_OPT_CLOSE_ON_FREE);

    //connect to dst as client
    dst_bev = bufferevent_socket_new((event_base *)arg, -1, BEV_OPT_CLOSE_ON_FREE);

    //new a filter for dst communication
    dst_filter_bev = bufferevent_filter_new(
            dst_bev,//pass in the dst event
            dest_filter_in,
            dest_filter_out,
            BEV_OPT_CLOSE_ON_FREE,
            nullptr,
            nullptr);

    //set destination read callback
    // bufferevent_enable(dst_bev, EV_READ | EV_WRITE);
    bufferevent_enable(dst_filter_bev, EV_READ | EV_WRITE); //use filter

    //pass in src, so that when read from dest we can transfer it to src
    // bufferevent_setcb(dst_bev, dst_read_cb, nullptr, proxy_event_cb, src_bev);
    bufferevent_setcb(dst_filter_bev, dst_read_cb, nullptr, proxy_event_cb, src_bev); //use filter

    //set source read callback
    bufferevent_enable(src_bev, EV_READ | EV_WRITE);
    bufferevent_setcb(src_bev, src_read_cb, nullptr, proxy_event_cb, dst_filter_bev /*dst_bev*/);

    //connent to dst
    sockaddr_in dst_addr{};
    memset(&dst_addr, 0, sizeof(dst_addr));
    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(5001);
    evutil_inet_pton(AF_INET, "192.168.31.76", &dst_addr.sin_addr.s_addr);
    cout << "try connecting to dst" << endl;
    int re = bufferevent_socket_connect(dst_bev, (sockaddr *)&dst_addr, sizeof(dst_addr));
    if (re == 0)
    {
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "connect to proxy successfully");
    }
    else
    {
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "connect to proxy failed");
    }
}

/**
 * do decryption after reading from dst, and before sending back to src
 */
bufferevent_filter_result
lightsocks_android_server::dest_filter_in(evbuffer *src, evbuffer *dst, ssize_t dst_limit,
                                          bufferevent_flush_mode mode, void *ctx) {
    char buffer[10]{0};
    int len = 0;
    while (true)
    {
        len = evbuffer_remove(src, buffer, sizeof(buffer) - 1);
        if (len <= 0)
            break;
        cout << "dest filter in:" << len << ":" << buffer << endl;
        for (int i = 0; i < len; i++)
        {
//            buffer[i] = decrypt(buffer[i]);//TODO:decrypt
        }
        evbuffer_add(dst, buffer, len);
    }
    return BEV_OK;
}

/**
 * do encryption before sending to dst
 **/
bufferevent_filter_result
lightsocks_android_server::dest_filter_out(evbuffer *src, evbuffer *dst, ev_ssize_t dst_limit,
                                          bufferevent_flush_mode mode, void *ctx)
{
    char buffer[10]{0};
    int len = 0;
    while (true)
    {
        len = evbuffer_remove(src, buffer, sizeof(buffer) - 1);
        if (len <= 0)
            break;
        cout << "out log c:";
        for (int i = 0; i < len; i++)
        {
//            buffer[i] = encrypt(buffer[i]);//TODO:encrypt
        }
        cout << endl;
        cout << "dest filter out:" << len << ":" << buffer << endl;
        evbuffer_add(dst, buffer, len);
    }
    return BEV_OK;
}

/**
 * read from source, and write to buffer to send to destination(before filter)
 * */
void lightsocks_android_server::src_read_cb(bufferevent *bev, void *arg)
{
    //forward data to destination
    auto *dst = (bufferevent *)arg;
    char buffer[10]{0};
    int len = 0;
    while (true)
    {
        len = bufferevent_read(bev, buffer, sizeof(buffer) - 1);
        if (len <= 0)
            break;
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "read from src: %s\n", buffer);
        bufferevent_write(dst, buffer, len);
    }
}

/**
 * (after filter) read from destination, write buffer to send back to source
 **/
void lightsocks_android_server::dst_read_cb(bufferevent *bev, void *arg)
{
    //send data back to source
    bufferevent *src = (bufferevent *)arg;
    char buffer[10]{0};
    int len = 0;
    while (true)
    {
        len = bufferevent_read(bev, buffer, sizeof(buffer) - 1);
        if (len <= 0)
            break;
        cout << "dest read cb:" << len << ":" << buffer << endl;
        bufferevent_write(src, buffer, len);
    }
}

/**
 * handle connection close event
 *
 * */
void lightsocks_android_server::proxy_event_cb(bufferevent *bev, short events, void *arg)
{

    __android_log_print(ANDROID_LOG_DEBUG, "haha", "event callback: %hu", events);
    //connection is closed
    if ((events & BEV_EVENT_EOF) || (events & BEV_EVENT_ERROR))
    {
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "EOF: close connection");
        bufferevent_free(bev);
        bufferevent_free((bufferevent *)arg); //close the other end;
    }
}
