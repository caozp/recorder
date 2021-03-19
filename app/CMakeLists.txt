cmake_minimum_required(VERSION 2.8.12)

add_subdirectory(src/main/cpp/ogg)

add_subdirectory(src/main/cpp/opus)

add_library(lib_ogg SHARED IMPORTED)
set_target_properties(lib_ogg PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/build/intermediates/cmake/debug/obj/${ANDROID_ABI}/libogg.so)

add_library(xiphogg
        SHARED
        src/main/cpp/OggLoader.cpp)

target_link_libraries(xiphogg
        log
        ogg)

add_library(lib_opus SHARED IMPORTED)


set_target_properties(lib_opus PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/build/intermediates/cmake/debug/obj/${ANDROID_ABI}/libopus.so)

add_library(xiphopus
        SHARED
        src/main/cpp/OpusLoader.cpp)

target_link_libraries(xiphopus
        log
        opus)