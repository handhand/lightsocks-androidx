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
#include <csignal>
#include <memory.h>
#include <map>
#include <netinet/in.h>
#include <endian.h>
#include "android/log.h"

using namespace std;

static string server_ip;
static int server_port;
static int local_port;
static event_base* base = nullptr;
static lightsocks_android_encryptor* encryptor = nullptr;
static void (*client_callback)(int, string&) = nullptr;
static void proxy_listener_cb(evconnlistener *ev, int sock, sockaddr *client_addr,
                       int client_len, void *arg);
static bufferevent_filter_result dest_filter_in(evbuffer *src, evbuffer *dst, ssize_t dst_limit,
                                         bufferevent_flush_mode mode, void *ctx);
static bufferevent_filter_result dest_filter_out(evbuffer *src, evbuffer *dst, ev_ssize_t dst_limit,
                                          bufferevent_flush_mode mode, void *ctx);
static void src_read_cb(bufferevent *bev, void *arg);
static void proxy_event_cb(bufferevent *bev, short events, void *arg);
static void dst_read_cb(bufferevent *bev, void *arg);

int start_proxy_server(string& serverIp,
                       int serverPort,
                       int localPort,
                       lightsocks_android_encryptor* lightsocksAndroidEncryptor,
                       void (*clientCallback)(int, string&)) {
    server_ip = serverIp;
    server_port = serverPort;
    local_port = localPort;
    encryptor = lightsocksAndroidEncryptor;
    client_callback = clientCallback;

    //when send data to closed socket, error signal will send and app will crash
    //so ignore this error signal, don't crash the app
    if (signal(SIGPIPE, SIG_IGN) == SIG_ERR)
    {
        return -1;
    }

    //1. new a context
    base = event_base_new();
    if (!base)
    {
        return -2;
    }

    //2. new a listening socket
    sockaddr_in sin{};
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(localPort);

    //3. listene and bind
    evconnlistener *ev = evconnlistener_new_bind(base,
                                                 proxy_listener_cb,
                                                 base, //parameter to pass to cb
                                                 LEV_OPT_REUSEABLE | LEV_OPT_CLOSE_ON_FREE /*close socket when libevent is done*/,
                                                 10, //max pending incoming connections
                                                 (sockaddr *)&sin,
                                                 sizeof(sin));

    //start main loop
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "libevent entering loop");
    event_base_dispatch(base);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "libevent exiting loop");
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
void proxy_listener_cb(evconnlistener *ev, int sock, sockaddr *client_addr,
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

    //pass in src as arg, so that when read from dest we can transfer it to src
    // bufferevent_setcb(dst_bev, dst_read_cb, nullptr, proxy_event_cb, src_bev);
    bufferevent_setcb(dst_filter_bev, dst_read_cb, nullptr, proxy_event_cb, src_bev); //use filter

    //set source read callback
    bufferevent_enable(src_bev, EV_READ | EV_WRITE);
    bufferevent_setcb(src_bev, src_read_cb, nullptr, proxy_event_cb, dst_filter_bev /*dst_bev*/);

    //connent to dst
    sockaddr_in dst_addr{};
    memset(&dst_addr, 0, sizeof(dst_addr));
    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(server_port);
    evutil_inet_pton(AF_INET, server_ip.c_str(), &dst_addr.sin_addr.s_addr);
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
bufferevent_filter_result dest_filter_in(evbuffer *src, evbuffer *dst, ssize_t dst_limit,
                                          bufferevent_flush_mode mode, void *ctx) {
    char buffer[BUFFER_SIZE]{0};
    int len = 0;
    while (true)
    {
        len = evbuffer_remove(src, buffer, sizeof(buffer) - 1);
        if (len <= 0)
            break;
        for (int i = 0; i < len; i++)
        {
            buffer[i] = encryptor->decrypt(buffer[i]);
        }
        evbuffer_add(dst, buffer, len);
    }
    return BEV_OK;
}

/**
 * do encryption before sending to dst
 **/
bufferevent_filter_result dest_filter_out(evbuffer *src, evbuffer *dst, ev_ssize_t dst_limit,
                                          bufferevent_flush_mode mode, void *ctx)
{
    char buffer[BUFFER_SIZE]{0};
    int len = 0;
    while (true)
    {
        len = evbuffer_remove(src, buffer, sizeof(buffer) - 1);
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "dest filter out: %d", len);
        if (len <= 0)
            break;
        for (int i = 0; i < len; i++)
        {
            buffer[i] = encryptor->encrypt(buffer[i]);
        }
        evbuffer_add(dst, buffer, len);
    }
    return BEV_OK;
}

/**
 * read from source, and write to buffer to send to destination(before filter)
 * */
void src_read_cb(bufferevent *bev, void *arg)
{
    //forward data to destination
    auto *dst = (bufferevent *)arg;
    struct evbuffer* src_input_buffer = bufferevent_get_input(bev);
    struct evbuffer* dst_output_buffer = bufferevent_get_output(dst);
    evbuffer_add_buffer(dst_output_buffer, src_input_buffer);
}

/**
 * (after filter) read from destination, write buffer to send back to source
 **/
void dst_read_cb(bufferevent *bev, void *arg)
{
    //send data back to source
    auto *src = (bufferevent *)arg;
    struct evbuffer* src_output_buffer = bufferevent_get_output(src);
    struct evbuffer* dst_input_buffer = bufferevent_get_input(bev);
    evbuffer_add_buffer(src_output_buffer, dst_input_buffer);
}

/**
 * handle connection close event
 *
 * */
void proxy_event_cb(bufferevent *bev, short events, void *arg)
{

//    __android_log_print(ANDROID_LOG_DEBUG, "haha", "event callback: %hu", events);
    if ( events & BEV_EVENT_ERROR ){
        if (client_callback){
            string error_msg = "Connection error!";
            client_callback(-1, error_msg);
        }
        __android_log_print(ANDROID_LOG_ERROR, "haha", "BEV_EVENT_ERROR!!!!");
    }
    //connection is closed
    if ((events & BEV_EVENT_EOF) || (events & BEV_EVENT_ERROR))
    {
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "EOF: close connection");
        bufferevent_free(bev);
        bufferevent_free((bufferevent *)arg); //close the other end;
    }
}

/**
 * 停止
 * @return
 */
int stop_proxy_server() {
    auto result =  event_base_loopbreak(base);
    event_base_loopexit(base, NULL);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "end libevent");
    encryptor = nullptr;
    server_port = 0;
    client_callback = nullptr;
    return result;
}
