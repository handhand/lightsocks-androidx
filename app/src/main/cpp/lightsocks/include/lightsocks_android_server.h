//
// Created by Fox on 2021/7/30.
//

#pragma once

#include <string>
#include "lightsocks_android_encryptor.h"
#include "lightsocks_android_encryptor.h"
using namespace std;

/**
 * Start the proxy, listen on localPort, and transfer data to the serverIp:serverPort,
 * encrypt the data with lighsocksAndroidEncryptor before sending to server, and decrypt
 * the data when receive data and send back to client.
 *
 * This will start a libevent loop, and block the caller until stop_proxy_server() is called
 * to terminate the loop.
 *
 * @param serverIp
 * @param serverPort
 * @param localPort
 * @param lightsocksAndroidEncryptor
 * @param clientCallback
 * @return
 */
int start_proxy_server(string& serverIp,
                       int serverPort,
                       int localPort,
                       lightsocks_android_encryptor* lightsocksAndroidEncryptor,
                       void (*clientCallback)(int, string&));

int stop_proxy_server();
