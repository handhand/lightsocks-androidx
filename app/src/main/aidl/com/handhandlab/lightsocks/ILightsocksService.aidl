// ILightsocksService.aidl
package com.handhandlab.lightsocks;
import com.handhandlab.lightsocks.ILightsocksServiceCallback;

interface ILightsocksService {

    void setCallback(ILightsocksServiceCallback callback);

    void stop();
}