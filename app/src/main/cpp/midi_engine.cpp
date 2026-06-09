#include <jni.h>
#include <amidi/AMidi.h>
#include <android/log.h>
#include <thread>
#include <atomic>
#include <cstring>

#define LOG_TAG "MidiEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static AMidiOutputPort* g_outputPort = nullptr;
static AMidiInputPort*  g_inputPort  = nullptr;
static std::atomic<bool> g_running{false};
static std::thread g_midiThread;

// JNI callback into Kotlin
static JavaVM*    g_jvm               = nullptr;
static jobject    g_callbackObj       = nullptr;
static jmethodID  g_onMidiMessageMethod = nullptr;

// ---------------------------------------------------------------------------
// Reader thread — uses AMidiOutputPort_receive (correct AMidi API)
// ---------------------------------------------------------------------------
void midiReaderThread() {
    uint8_t  buf[64];
    int32_t  opcode           = 0;
    int64_t  frameTimestamp   = 0;
    size_t   numBytesReceived = 0;

    while (g_running.load()) {
        if (g_outputPort == nullptr) {
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
            continue;
        }

        // FIX: AMidi_poll does not exist.  Correct call is AMidiOutputPort_receive.
        // Returns 1 when a message was read, 0 when the queue is empty, <0 on error.
        int32_t result = AMidiOutputPort_receive(
            g_outputPort,
            &opcode,
            buf, sizeof(buf),
            &numBytesReceived,
            &frameTimestamp
        );

        if (result > 0
                && opcode == AMIDI_OPCODE_DATA   // skip SysEx / flush opcodes
                && numBytesReceived >= 3          // FIX: guard on actual byte count, not message count
                && g_jvm && g_callbackObj) {

            JNIEnv* env = nullptr;
            if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                uint8_t statusByte = buf[0];
                uint8_t noteNum    = buf[1];
                uint8_t velocity   = buf[2];
                env->CallVoidMethod(
                    g_callbackObj,
                    g_onMidiMessageMethod,
                    (jint)statusByte,
                    (jint)noteNum,
                    (jint)velocity
                );
                // Do NOT detach — AttachCurrentThread on an already-attached thread
                // returns the existing env; detaching it would break the thread.
            }
        } else if (result == 0) {
            // Queue empty — yield briefly to avoid busy-spin
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        } else if (result < 0) {
            LOGE("AMidiOutputPort_receive error: %d", result);
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }
    }
}

// ---------------------------------------------------------------------------
// startEngine / stopEngine
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_startEngine(
        JNIEnv* env, jobject thiz) {
    if (!g_running.exchange(true)) {
        env->GetJavaVM(&g_jvm);
        g_callbackObj         = env->NewGlobalRef(thiz);
        jclass clazz          = env->GetObjectClass(thiz);
        g_onMidiMessageMethod = env->GetMethodID(clazz, "onMidiMessage", "(III)V");
        g_midiThread          = std::thread(midiReaderThread);
        LOGI("MIDI Engine started");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_stopEngine(
        JNIEnv* env, jobject thiz) {
    g_running.store(false);
    if (g_midiThread.joinable()) g_midiThread.join();
    if (g_outputPort) { AMidiOutputPort_close(g_outputPort); g_outputPort = nullptr; }
    if (g_inputPort)  { AMidiInputPort_close(g_inputPort);   g_inputPort  = nullptr; }
    if (g_callbackObj) { env->DeleteGlobalRef(g_callbackObj); g_callbackObj = nullptr; }
    LOGI("MIDI Engine stopped");
}

// ---------------------------------------------------------------------------
// connectOutputPort — called from Kotlin MidiManager after opening a device
// output port so the reader thread can receive MIDI bytes from the device.
//
// Usage (Kotlin):
//   val nativePort = AMidiOutputPort.open(device, portIndex)  // long = native ptr
//   MidiEngineNative.connectOutputPort(nativePort)
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_connectOutputPort(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong nativePortPtr) {
    if (g_outputPort) {
        AMidiOutputPort_close(g_outputPort);
        g_outputPort = nullptr;
    }
    g_outputPort = reinterpret_cast<AMidiOutputPort*>(nativePortPtr);
    LOGI("AMidiOutputPort connected: %p", (void*)g_outputPort);
}

// ---------------------------------------------------------------------------
// connectInputPort — called from Kotlin MidiManager after opening a device
// input port so sendMidiMessage can write MIDI bytes to the device.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_connectInputPort(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong nativePortPtr) {
    if (g_inputPort) {
        AMidiInputPort_close(g_inputPort);
        g_inputPort = nullptr;
    }
    g_inputPort = reinterpret_cast<AMidiInputPort*>(nativePortPtr);
    LOGI("AMidiInputPort connected: %p", (void*)g_inputPort);
}

// ---------------------------------------------------------------------------
// disconnectPorts — safely nulls both port pointers without stopping the
// engine thread.  Call this before reconnecting to a different device.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_disconnectPorts(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_outputPort) { AMidiOutputPort_close(g_outputPort); g_outputPort = nullptr; }
    if (g_inputPort)  { AMidiInputPort_close(g_inputPort);   g_inputPort  = nullptr; }
    LOGI("Ports disconnected");
}

// ---------------------------------------------------------------------------
// sendMidiMessage — write bytes to the connected device input port
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_sendMidiMessage(
        JNIEnv* env, jobject /*thiz*/, jbyteArray data, jint length) {
    if (!g_inputPort) return;
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    AMidiInputPort_send(g_inputPort, reinterpret_cast<uint8_t*>(bytes), (size_t)length);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
}
