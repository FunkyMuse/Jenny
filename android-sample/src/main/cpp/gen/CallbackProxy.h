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

class CallbackProxy {

public:
    static constexpr auto FULL_CLASS_NAME = "io/github/landerlyoung/jennysampleapp/Callback";
    
private:
    static jclass sClazz;
    static constexpr const jint COMPILE_CONSTANT_INT = 15;
    static constexpr const jint ANOTHER_COMPILE_CONSTANT_INT = 16;

    static jmethodID sConstruct_0;
    static jmethodID sConstruct_1;
    static jmethodID sConstruct_2;

    static jmethodID sMethod_onJobDone_0;
    static jmethodID sMethod_onJobProgress_0;
    static jmethodID sMethod_onJobStart_0;
    static jmethodID sMethod_onJobStart_1;

    static jfieldID sField_lock_0;
    static jfieldID sField_COMPILE_CONSTANT_INT_1;
    static jfieldID sField_ANOTHER_COMPILE_CONSTANT_INT_2;
    static jfieldID sField_count_3;
    static jfieldID sField_staticCount_4;
    static jfieldID sField_name_5;
    static jfieldID sField_staticName_6;
    static jfieldID sField_aStaticField_7;

private:
    static std::atomic_bool sInited;
    static std::mutex sInitLock;

private:
    JNIEnv* mJniEnv;
    jobject mJavaObjectReference;

public:

    static bool initClazz(JNIEnv *env);
    
    static void releaseClazz(JNIEnv *env);

    static void assertInited(JNIEnv *env) {
        assert(initClazz(env));
    }

    CallbackProxy(JNIEnv *env, jobject javaObj)
            : mJniEnv(env), mJavaObjectReference(javaObj) {
        assertInited(env);
    }

    CallbackProxy(const CallbackProxy &from) = default;
    CallbackProxy &operator=(const CallbackProxy &) = default;

    CallbackProxy(CallbackProxy &&from)
           : mJniEnv(from.mJniEnv), mJavaObjectReference(from.mJavaObjectReference) {
        from.mJavaObjectReference = nullptr;
    }

    ~CallbackProxy() = default;
    
    // helper method to get underlay jobject reference
    jobject operator*() {
       return mJavaObjectReference;
    }
    
    // helper method to delete JNI local ref, use with caution!
    void releaseLocalRef() {
       mJniEnv->DeleteLocalRef(mJavaObjectReference);
       mJavaObjectReference = nullptr;
    }
    
    // construct: public Callback()
    static CallbackProxy newInstance(JNIEnv* env) noexcept {
       assertInited(env);
       return CallbackProxy(env, env->NewObject(sClazz, sConstruct_0));
    } 
    
    // construct: public Callback(int a)
    static CallbackProxy newInstance(JNIEnv* env, jint a) noexcept {
       assertInited(env);
       return CallbackProxy(env, env->NewObject(sClazz, sConstruct_1, a));
    } 
    
    // construct: public Callback(java.util.HashMap<?,?> sth)
    static CallbackProxy newInstance(JNIEnv* env, jobject sth) noexcept {
       assertInited(env);
       return CallbackProxy(env, env->NewObject(sClazz, sConstruct_2, sth));
    } 
    

    // method:  void onJobDone(boolean success, java.lang.String result)
    void onJobDone(jboolean success, jstring result) const {
        mJniEnv->CallVoidMethod(mJavaObjectReference, sMethod_onJobDone_0, success, result);
    }

    // method:  void onJobProgress(long progress)
    void onJobProgress(jlong progress) const {
        mJniEnv->CallVoidMethod(mJavaObjectReference, sMethod_onJobProgress_0, progress);
    }

    // method: public void onJobStart()
    void onJobStart() const {
        mJniEnv->CallVoidMethod(mJavaObjectReference, sMethod_onJobStart_0);
    }

    // method:  void onJobStart(io.github.landerlyoung.jennysampleapp.Callback.NestedClass overrloadedMethod)
    void onJobStart(jobject overrloadedMethod) const {
        mJniEnv->CallVoidMethod(mJavaObjectReference, sMethod_onJobStart_1, overrloadedMethod);
    }


    // field: protected java.lang.Object lock
    jobject getLock() const {
       return mJniEnv->GetObjectField(mJavaObjectReference, sField_lock_0);

   }
    // field: protected java.lang.Object lock
    void setLock(jobject lock) const {
        mJniEnv->SetObjectField(mJavaObjectReference, sField_lock_0, lock);
    }


    // field: public final int ANOTHER_COMPILE_CONSTANT_INT
    void setANOTHER_COMPILE_CONSTANT_INT(jint ANOTHER_COMPILE_CONSTANT_INT) const {
        mJniEnv->SetIntField(mJavaObjectReference, sField_ANOTHER_COMPILE_CONSTANT_INT_2, ANOTHER_COMPILE_CONSTANT_INT);
    }

    // field: public int count
    jint getCount() const {
       return mJniEnv->GetIntField(mJavaObjectReference, sField_count_3);

   }
    // field: public int count
    void setCount(jint count) const {
        mJniEnv->SetIntField(mJavaObjectReference, sField_count_3, count);
    }

    // field: public static int staticCount
    jint getStaticCount() const {
       return mJniEnv->GetStaticIntField(sClazz, sField_staticCount_4);

   }
    // field: public static int staticCount
    void setStaticCount(jint staticCount) const {
        mJniEnv->SetStaticIntField(sClazz, sField_staticCount_4, staticCount);
    }

    // field: public java.lang.String name
    jstring getName() const {
       return reinterpret_cast<jstring>(mJniEnv->GetObjectField(mJavaObjectReference, sField_name_5));

   }
    // field: public java.lang.String name
    void setName(jstring name) const {
        mJniEnv->SetObjectField(mJavaObjectReference, sField_name_5, name);
    }

    // field: public static java.lang.String staticName
    jstring getStaticName() const {
       return reinterpret_cast<jstring>(mJniEnv->GetStaticObjectField(sClazz, sField_staticName_6));

   }
    // field: public static java.lang.String staticName
    void setStaticName(jstring staticName) const {
        mJniEnv->SetStaticObjectField(sClazz, sField_staticName_6, staticName);
    }

    // field: public static java.util.List<java.lang.String> aStaticField
    jobject getAStaticField() const {
       return mJniEnv->GetStaticObjectField(sClazz, sField_aStaticField_7);

   }
    // field: public static java.util.List<java.lang.String> aStaticField
    void setAStaticField(jobject aStaticField) const {
        mJniEnv->SetStaticObjectField(sClazz, sField_aStaticField_7, aStaticField);
    }

};