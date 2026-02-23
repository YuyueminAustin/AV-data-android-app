cmake_minimum_required(VERSION 3.4.1)

add_library(PreProcessAudio SHARED
        PreProcessAudio.cpp)

find_library(log-lib log)

target_link_libraries(PreProcessAudio
        ${log-lib})