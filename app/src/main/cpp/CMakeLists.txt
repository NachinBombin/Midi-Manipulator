cmake_minimum_required(VERSION 3.22.1)
project(midi_engine)

find_library(log-lib log)
find_library(amidi-lib amidi)

add_library(
    midi_engine
    SHARED
    midi_engine.cpp
)

target_link_libraries(
    midi_engine
    ${log-lib}
    ${amidi-lib}
)
