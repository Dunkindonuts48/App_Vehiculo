#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_car_1app_obd_ObdProcessor_processObdData(JNIEnv *env, jobject /* this */, jstring data) {
    const char *nativeData = env->GetStringUTFChars(data, nullptr);

    // Aquí podríamos procesar el dato, por ahora lo devolvemos igual.
    std::string processedData = "Procesado: " + std::string(nativeData);

    env->ReleaseStringUTFChars(data, nativeData);
    return env->NewStringUTF(processedData.c_str());
}
