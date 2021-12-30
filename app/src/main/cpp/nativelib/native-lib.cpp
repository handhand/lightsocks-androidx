#include <jni.h>
#include <string>
#include <locale>
#include <utility>
#include "fcntl.h"
#include "../badvpn-master/tun2socks/tun2socks.h"
#include <android/log.h>
#include "../lightsocks/include/lightsocks_android_server.h"
#include "../lightsocks/include/lightsocks_android_server.h"

static jmethodID callbackMethodID;
static jobject  jObjCallback; // callback to java
static JNIEnv *gEnv;

/**
 * callback func for proxy
 * it will call the jni callback
 *
 * @param state
 * @param msg
 */
void proxyCallbackFunc(int state, string& msg){
    if (jObjCallback) {
        __android_log_print(ANDROID_LOG_DEBUG, "haha", "proxy callback");
        jstring jStrMsg = gEnv->NewStringUTF(msg.c_str());
        gEnv->CallVoidMethod(jObjCallback, callbackMethodID, state, jStrMsg);
        gEnv->DeleteLocalRef(jStrMsg);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_startTunSocks(JNIEnv *env, jobject thiz,
                                                                          jint fd,
                                                                          jint tunmtu,
                                                                          jstring ifAddr,
                                                                          jstring ifAddrMask,
                                                                          jstring socksServerAddr,
                                                                          jstring udpServerAddr) {
    const char* if_addr = env->GetStringUTFChars(ifAddr, nullptr);
    const char*if_mask = env->GetStringUTFChars(ifAddrMask, nullptr);
    const char*socks_server_addr = env->GetStringUTFChars(socksServerAddr, nullptr);
    const char*udp_server_addr = env->GetStringUTFChars(udpServerAddr, nullptr);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "\n start %s\n", socks_server_addr);

    //启动tun2socks服务
    startTun2socks(fd, tunmtu,(char*) if_addr, (char*)if_mask, (char*)socks_server_addr, (char*)udp_server_addr);

    env->ReleaseStringUTFChars(ifAddr, if_addr);
    env->ReleaseStringUTFChars(ifAddrMask, if_mask);
    env->ReleaseStringUTFChars(socksServerAddr, socks_server_addr);
    env->ReleaseStringUTFChars(udpServerAddr, udp_server_addr);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "end");
}

/**
 * 启动转发
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_startProxy(JNIEnv *env, jobject thiz,
                                                                       jstring remote_socks_server_ip,
                                                                       jint remote_socks_server_port,
                                                                       jint local_listen_port,
                                                                       jstring encryptionKey,
                                                                       jobject proxyCallback) {
    const char* charArrayServerIp = env->GetStringUTFChars(remote_socks_server_ip, nullptr);
    const char* charArraySecret = env->GetStringUTFChars(encryptionKey, nullptr);
    string strServerIp{charArrayServerIp};
    string strSecret{charArraySecret};

    auto *encryptor = new lightsocks_android_encryptor{strSecret};

//    lightsocks_android_server::encryptor = encryptor;
//    lightsocks_android_server::start(strServerIp, remote_socks_server_port, local_listen_port);

    gEnv = env;
    // find callback object
    auto callbackClass = env->FindClass("com/handhandlab/lightsocks/NativeStatusCallback");
    callbackMethodID = env->GetMethodID(callbackClass, "onState", "(ILjava/lang/String;)V");
    jObjCallback = proxyCallback;

    start_proxy_server(
            strServerIp,
            remote_socks_server_port,
            local_listen_port,
            encryptor,
            proxyCallbackFunc
    );

    env->ReleaseStringUTFChars(remote_socks_server_ip, charArrayServerIp);
    env->ReleaseStringUTFChars(encryptionKey, charArraySecret);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "proxy end");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_stopTunSocks(JNIEnv *env, jobject thiz) {
    stop_proxy_server();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_handhandlab_lightsocks_LightsocksDroidNative_stopProxy(JNIEnv *env, jobject thiz) {
    stopTun2socks();
}