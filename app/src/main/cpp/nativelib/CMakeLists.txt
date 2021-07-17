# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("LightsocksDroidLib")

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp
        lightsocks_droid.cpp)
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )
# Links the top-level CMake build output against lib_gmath.
#target_link_libraries( native-lib testlib ${log-lib})

target_link_libraries (native-lib LINK_PUBLIC testlib)

target_link_libraries (native-lib LINK_PUBLIC event_core_static)