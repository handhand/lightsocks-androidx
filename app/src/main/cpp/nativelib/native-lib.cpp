#include <jni.h>
#include <string>
#include <locale>
#include <utility>
#include "fcntl.h"
#include "../badvpn-master/tun2socks/tun2socks.h"
#include <android/log.h>
#include "../lightsocks/include/lightsocks_android_server.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_startTunSocks(JNIEnv *env, jobject thiz,
                                                                          jint fd,
                                                                          jint tunmtu,
                                                                          jstring ifAddr,
                                                                          jstring ifAddrMask,
                                                                          jstring socksServerAddr,
                                                                          jstring udpServerAddr) {
    char if_addr[50], if_mask[50], socks_server_addr[50], udp_server_addr[50];
    strcpy(if_addr, env->GetStringUTFChars(ifAddr, nullptr));
    strcpy(if_mask, env->GetStringUTFChars(ifAddrMask, nullptr));
    strcpy(socks_server_addr, env->GetStringUTFChars(socksServerAddr, nullptr));
    strcpy(udp_server_addr, env->GetStringUTFChars(udpServerAddr, nullptr));
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "\n start %s\n", socks_server_addr);

    //启动tun2socks服务
    start(fd, tunmtu, if_addr, if_mask, socks_server_addr, udp_server_addr);

    env->ReleaseStringUTFChars(ifAddr, if_addr);
    env->ReleaseStringUTFChars(ifAddrMask, if_mask);
    env->ReleaseStringUTFChars(socksServerAddr, socks_server_addr);
    env->ReleaseStringUTFChars(udpServerAddr, udp_server_addr);

}

/**
 * 启动转发
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_startProxy(JNIEnv *env, jobject thiz,
                                                                       jstring remote_socks_server_ip,
                                                                       jint remote_socks_server_port,
                                                                       jint local_listen_port) {
    const char* charArrayServerIp = env->GetStringUTFChars(remote_socks_server_ip, nullptr);
    string strServerIp{charArrayServerIp};
    lightsocks_android_server::start(strServerIp, remote_socks_server_port, local_listen_port);
    env->ReleaseStringUTFChars(remote_socks_server_ip, charArrayServerIp);
}