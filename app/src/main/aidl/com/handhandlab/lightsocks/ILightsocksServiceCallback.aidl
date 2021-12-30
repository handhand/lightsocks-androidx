// ILightsocksServiceCallback.aidl
package com.handhandlab.lightsocks;

interface ILightsocksServiceCallback {

    void onState(int status, String msg);
}