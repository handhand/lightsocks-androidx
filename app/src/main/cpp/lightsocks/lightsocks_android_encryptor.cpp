//
// Created by Fox on 2021/7/30.
//
#include "include/lightsocks_android_encryptor.h"
#include <string>
#include <vector>
#include "include/base64.h"
using namespace std;

lightsocks_android_encryptor::lightsocks_android_encryptor(string &base64secret): enc_mapping(MAPPING_SIZE), dec_mapping(MAPPING_SIZE) {
    vector<BYTE> bytes = base64_decode(base64secret);
    //init mapping
    for(int i=0;i<bytes.size();i++){
        enc_mapping[i] = bytes[i];
        dec_mapping[(int)bytes[i]] = i;
    }
}

void lightsocks_android_encryptor::encrypt(char input, char &output) {
    output = enc_mapping[(int)input];
}

void lightsocks_android_encryptor::decrypt(char input, char &output) {
    output = dec_mapping[(int)input];
}