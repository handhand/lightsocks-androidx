## 你也可以做个Shadowsocks Android篇[WIP]

#### TODO:什么是tun

####1.Build libevent
直接用libevent的CMakeLists.txt，在gradle传入正确的cmake参数即可
"-DANDROID=TRUE", 
"-DEVENT__DISABLE_OPENSSL=TRUE", 跳过不使用的库
"-DEVENT__DISABLE_MBEDTLS=TRUE", 
"-DEVENT__DISABLE_BENCHMARK=TRUE", 
"-DEVENT__DISABLE_SAMPLES", 
"-DEVENT__LIBRARY_TYPE=STATIC" 编译成静态库


####2.Build tun2socks 

需要修改的地方：
https://www.brobwind.com/archives/824

ndk没有_nes, 用处为读取系统的dns，注释掉https://github.com/ambrop72/badvpn/issues/75，使用传入的dns

不需要显式链接pthread, 在bionic中自带，所以把CMakeLists.txt中的link_libraries(rt pthread)去掉
https://stackoverflow.com/questions/38666609/cant-find-lpthread-when-cross-compile-to-arm

把生成的可执行文件复制到asset目录：
https://stackoverflow.com/questions/44613889/building-executable-with-android-ndk
注意不可以放asset目录了，要放特定的目录和apk一起打包，会被释放到一个只读目录，另外在release包中命名必须为libxxx.so才会被释放
https://stackoverflow.com/questions/19218775/android-copy-assets-to-internal-storage
https://stackoverflow.com/questions/62391811/android-10-alternative-to-launching-executable-as-subproccess-stored-in-app-ho