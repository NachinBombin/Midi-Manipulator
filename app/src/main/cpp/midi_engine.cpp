#include <jni.h>
#include <amidi/AMidi.h>
#include <android/log.h>
#include <thread>
#include <atomic>
#include <cstring>

#define LOG_TAG "MidiEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static AMidiDevice* g_inputDevice = nullptr;
static AMidiDevice* g_outputDevice = nullptr;
static AMidiInputPort* g_inputPort = nullptr;
static AMidiOutputPort* g_outputPort = nullptr;
static std::atomic<bool> g_running{false};
static std::thread g_midiThread;

// Callback to Kotlin — set via JNI
static JavaVM* g_jvm = nullptr;
static jobject g_callbackObj = nullptr;
static jmethodID g_onMidiMessageMethod = nullptr;

void midiReaderThread() {
    uint8_t buf[64];
    int32_t numMessages;
    while (g_running.load()) {
        if (g_outputPort == nullptr) { std::this_thread::sleep_for(std::chrono::milliseconds(5)); continue; }
        AMidi_poll(g_outputPort, nullptr, 0, buf, sizeof(buf), &numMessages);
        // Notify Kotlin on Note On/Off
        if (numMessages > 0 && g_jvm && g_callbackObj) {
            JNIEnv* env = nullptr;
            if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                uint8_t statusByte = buf[0];
                uint8_t noteNum   = (numMessages >= 2) ? buf[1] : 0;
                uint8_t velocity  = (numMessages >= 3) ? buf[2] : 0;
                env->CallVoidMethod(g_callbackObj, g_onMidiMessageMethod,
                                    (jint)statusByte, (jint)noteNum, (jint)velocity);
            }
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_startEngine(
        JNIEnv* env, jobject thiz) {
    if (!g_running.exchange(true)) {
        env->GetJavaVM(&g_jvm);
        g_callbackObj = env->NewGlobalRef(thiz);
        jclass clazz = env->GetObjectClass(thiz);
        g_onMidiMessageMethod = env->GetMethodID(clazz, "onMidiMessage", "(III)V");
        g_midiThread = std::thread(midiReaderThread);
        LOGI("MIDI Engine started");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_stopEngine(
        JNIEnv* env, jobject thiz) {
    g_running.store(false);
    if (g_midiThread.joinable()) g_midiThread.join();
    if (g_outputPort) { AMidiOutputPort_close(g_outputPort); g_outputPort = nullptr; }
    if (g_inputPort)  { AMidiInputPort_close(g_inputPort);  g_inputPort  = nullptr; }
    if (g_callbackObj) { env->DeleteGlobalRef(g_callbackObj); g_callbackObj = nullptr; }
    LOGI("MIDI Engine stopped");
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachinbombin_midimanipulator_midi_MidiEngineNative_sendMidiMessage(
        JNIEnv* env, jobject thiz, jbyteArray data, jint length) {
    if (!g_inputPort) return;
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    AMidiInputPort_send(g_inputPort, reinterpret_cast<uint8_t*>(buf), length);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
}
