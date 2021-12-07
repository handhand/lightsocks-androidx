#include <jni.h>
#include <string>
#include <locale>
#include <utility>
//#include "event2/event.h"
#include "fcntl.h"
#include "../badvpn-master/tun2socks/tun2socks.h"
#include <android/log.h>
#include "../lightsocks/include/lightsocks_android_server.h"

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_handhandlab_lightsocks_ui_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    event_base* base = event_base_new();
//    std::string test = "no base";
//    if(base){
//        test = "has base";
//    }
//    std::string hello = "Hello from C++3 " + test;
//    return env->NewStringUTF(hello.c_str());
//}

extern "C"
JNIEXPORT int JNICALL
Java_com_handhandlab_lightsocks_utils_LightsocksDroid_test(JNIEnv *env, jobject thiz, jint fd) {
    int retv = fcntl(fd, F_SETFL, O_NONBLOCK);
    printf("hahahaha %d", retv);
    return retv;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_utils_LightsocksDroid_start(JNIEnv *env, jobject thiz,
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
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "\n start %s\n", udp_server_addr);

    //启动tun2socks服务
//    start(fd, tunmtu, if_addr, if_mask, socks_server_addr, udp_server_addr);

    //TODO:启动lightsocks服务
    lightsocks_android_server las;
    las.start();

    env->ReleaseStringUTFChars(ifAddr, if_addr);
    env->ReleaseStringUTFChars(ifAddrMask, if_mask);
    env->ReleaseStringUTFChars(socksServerAddr, socks_server_addr);
    env->ReleaseStringUTFChars(udpServerAddr, udp_server_addr);
}