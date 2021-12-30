//
// Created by Fox on 2021/7/30.
//
#include "include/lightsocks_android_encryptor.h"
#include <string>
#include <vector>
#include <android/log.h>
#include "include/base64.h"
using namespace std;

lightsocks_android_encryptor::lightsocks_android_encryptor(string &base64secret): encrypt_vec(MAPPING_SIZE), decrypt_vec(MAPPING_SIZE) {
    encrypt_vec = base64_decode(base64secret);
    __android_log_print(ANDROID_LOG_DEBUG, "haha", "\n secret size %d\n", encrypt_vec.size());
    //init vectors
    for(int i=0;i<encrypt_vec.size();i++){
        decrypt_vec[static_cast<int>(encrypt_vec[i])] = i;
    }
}

char lightsocks_android_encryptor::encrypt(char input) {
    return encrypt_vec[static_cast<int>(input)];
}

char lightsocks_android_encryptor::decrypt(char input) {
    return decrypt_vec[static_cast<int>(input)];
}