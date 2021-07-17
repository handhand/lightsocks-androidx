#include <jni.h>
#include <string>
#include <locale>
#include "testlib.h"
#include "event2/event.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_handhandlab_lightsocks_ui_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    event_base* base = event_base_new();
    std::string test = "no base";
    if(base){
        test = "has base";
    }
    std::string hello = "Hello from C++3 " + test;
    return env->NewStringUTF(hello.c_str());
}
