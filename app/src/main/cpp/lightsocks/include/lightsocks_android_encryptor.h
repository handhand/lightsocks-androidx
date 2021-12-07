//
// Created by Fox on 2021/7/18.
//

#ifndef LIGHTSOCKSDROID_LSDENCRYPTOR_H
#define LIGHTSOCKSDROID_LSDENCRYPTOR_H
#include <vector>
#define MAPPING_SIZE  255
using namespace std;
class lightsocks_android_encryptor{

public:

    lightsocks_android_encryptor(string& base64secret);

    void encrypt(char input, char& output);

    void decrypt(char input, char& output);

private:

    std::vector<unsigned char> enc_mapping;

    std::vector<unsigned char> dec_mapping;

};
#endif //LIGHTSOCKSDROID_LSDENCRYPTOR_H
