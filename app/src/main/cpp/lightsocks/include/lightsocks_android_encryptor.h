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

    char encrypt(char input);

    char decrypt(char input);

private:

    std::vector<unsigned char> encrypt_vec;

    std::vector<unsigned char> decrypt_vec;
};
#endif //LIGHTSOCKSDROID_LSDENCRYPTOR_H
