/**
 * File generated by Jenny -- https://github.com/LanderlYoung/Jenny
 *
 * DO NOT EDIT THIS FILE.
 *
 * For bug report, please refer to github issue tracker https://github.com/LanderlYoung/Jenny/issues,
 * or contact author landerlyoung@gmail.com.
 */
#pragma once

#include <jni.h>
#include <assert.h>                        
#include <atomic>
#include <mutex>


class RequestListenerProxy {

public:
    static constexpr auto FULL_CLASS_NAME = "io/github/landerlyoung/jennysample/RequestListener";



private:
    // thread safe init
    static std::atomic_bool sInited;
    static std::mutex sInitLock;

    JNIEnv* mJniEnv;
    jobject mJavaObjectReference;

public:

    static bool initClazz(JNIEnv *env);
    
    static void releaseClazz(JNIEnv *env);

    static void assertInited(JNIEnv *env) {
        assert(initClazz(env));
    }

    RequestListenerProxy(JNIEnv *env, jobject javaObj)
            : mJniEnv(env), mJavaObjectReference(javaObj) {
        assertInited(env);
    }

    RequestListenerProxy(const RequestListenerProxy &from) = default;
    RequestListenerProxy &operator=(const RequestListenerProxy &) = default;

    RequestListenerProxy(RequestListenerProxy &&from)
           : mJniEnv(from.mJniEnv), mJavaObjectReference(from.mJavaObjectReference) {
        from.mJavaObjectReference = nullptr;
    }

    ~RequestListenerProxy() = default;
    
    // helper method to get underlay jobject reference
    jobject operator*() {
       return mJavaObjectReference;
    }
    
    // helper method to delete JNI local ref.
    // use only when you really understand JNIEnv::DeleteLocalRef.
    void deleteLocalRef() {
       if (mJavaObjectReference) {
           mJniEnv->DeleteLocalRef(mJavaObjectReference);
           mJavaObjectReference = nullptr;
       }
    }
    
    // === java methods below ===
    

    // method: public abstract void onResponse(boolean success, java.lang.String rsp)
    void onResponse(jboolean success, jstring rsp) const {
        mJniEnv->CallVoidMethod(mJavaObjectReference, sMethod_onResponse_0, success, rsp);
    }



private:
    static jclass sClazz;

    static jmethodID sMethod_onResponse_0;


};
