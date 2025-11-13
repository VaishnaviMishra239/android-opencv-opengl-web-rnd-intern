cmake_minimum_required(VERSION 3.10.2)
project("edgedetector")

# Set OpenCV_DIR to opencv sdk path relative to this CMake file if present
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv/sdk/native/jni)
find_package(OpenCV REQUIRED)

add_library(native-lib SHARED native-lib.cpp)

include_directories(${OpenCV_INCLUDE_DIRS})
target_link_libraries(native-lib ${OpenCV_LIBS} log)
