/**
 * Copyright 2016 landerlyoung@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.landerlyoung.jenny

import java.io.IOException
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.StandardLocation
import kotlin.collections.HashSet

/**
 * Author: landerlyoung@gmail.com
 * Date:   2016-06-05
 * Time:   00:30
 * Life with Passion, Code with Creativity.
 */
class NativeProxyCodeGenerator(env: Environment, clazz: TypeElement) : AbsCodeGenerator(env, clazz) {
    //what we need to generate includes
    //---------- id ----------
    //constructor
    //method
    //field
    //-------- getXxxId -------
    //constructor
    //method
    //field
    //------- newInstance ------
    //constructor
    //------- callXxxMethod -----
    //method
    //------ get/setXxxField ----
    //field

    private val mConstructors = mutableListOf<MethodOverloadResolver.MethodRecord>()
    private val mMethodSimpleName = mutableSetOf<String>()
    private val mMethods = mutableListOf<MethodOverloadResolver.MethodRecord>()
    private val mFields = mutableListOf<Element>()
    private val mConsts: MutableSet<String> = HashSet()
    private val mNativeProxyAnnotation: NativeProxy
    private val mHeaderName: String
    private val mSourceName: String

    private val cppClassName: String
        get() {
            val fileName = mNativeProxyAnnotation.fileName
            return if (fileName.length > 0) {
                fileName
            } else {
                (if (mNativeProxyAnnotation.simpleName)
                    mSimpleClassName
                else
                    mJNIClassName) + "Proxy"
            }
        }


    init {
        var annotation: NativeProxy? = clazz.getAnnotation(NativeProxy::class.java)
        if (annotation == null) {
            annotation = AnnotationResolver.getDefaultImplementation(NativeProxy::class.java)
        }
        mNativeProxyAnnotation = annotation

        mHeaderName = "${cppClassName}.h"
        mSourceName = "${cppClassName}.cpp"
    }

    private fun init() {
        findConstructors()
        findMethods()
        findFields()
    }

    override fun doGenerate() {
        init()

        generatorHeader()
        generateSource()
    }

    private fun generatorHeader() {
        val fileObject = mEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, PKG_NAME, mHeaderName)
        fileObject.openOutputStream().use { out ->
            try {
                log("write native proxy file [" + fileObject.name + "]")
                buildString {
                    append(Constants.AUTO_GENERATE_NOTICE)
                    append("""
                        |#pragma once
                        |
                        |#include <jni.h>
                        |#include <assert.h>                        
                        |""".trimMargin())
                    if (mEnv.configurations.threadSafe) {
                        append("""
                        |#include <atomic>
                        |#include <mutex>
                        |
                        |""".trimMargin())
                    }

                    append("""
                        |class $cppClassName {
                        |
                        |public:
                        |    static constexpr auto FULL_CLASS_NAME = "$mSlashClassName";
                        |    
                        |private:
                        |
                    """.trimMargin())

                    if (mEnv.configurations.threadSafe) {
                        append("""
                        |    // thread safe init
                        |    static std::atomic_bool sInited;
                        |    static std::mutex sInitLock;
                        |""".trimMargin())
                    } else {
                        append("    static bool sInited;\n")
                    }

                    append("""
                        |
                        |    JNIEnv* mJniEnv;
                        |    jobject mJavaObjectReference;
                        |
                        |public:
                        |
                        |    static bool initClazz(JNIEnv *env);
                        |    
                        |    static void releaseClazz(JNIEnv *env);
                        |
                        |    static void assertInited(JNIEnv *env) {
                        |        assert(initClazz(env));
                        |    }
                        |
                        |    ${cppClassName}(JNIEnv *env, jobject javaObj)
                        |            : mJniEnv(env), mJavaObjectReference(javaObj) {
                        |        assertInited(env);
                        |    }
                        |
                        |    ${cppClassName}(const $cppClassName &from) = default;
                        |    $cppClassName &operator=(const $cppClassName &) = default;
                        |
                        |    ${cppClassName}($cppClassName &&from)
                        |           : mJniEnv(from.mJniEnv), mJavaObjectReference(from.mJavaObjectReference) {
                        |        from.mJavaObjectReference = nullptr;
                        |    }
                        |
                        |    ~${cppClassName}() = default;
                        |    
                        |    // helper method to get underlay jobject reference
                        |    jobject operator*() {
                        |       return mJavaObjectReference;
                        |    }
                        |    
                        |    // helper method to delete JNI local ref.
                        |    // use only when you really understand JNIEnv::DeleteLocalRef.
                        |    void releaseLocalRef() {
                        |       mJniEnv->DeleteLocalRef(mJavaObjectReference);
                        |       mJavaObjectReference = nullptr;
                        |    }
                        |    
                        |    // === java methods below ===
                        |    
                        |""".trimMargin())

                    buildConstructorDefines()
                    buildMethodDefines()
                    buildFieldDefines()

                    append("""
                        |
                        |private:
                        |    static jclass sClazz;
                        |
                    """.trimMargin())

                    buildConstantsIdDeclare()
                    buildConstructorIdDeclare()
                    buildMethodIdDeclare()
                    buildFieldIdDeclare()

                    append("};")

                }.let { content ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (e: IOException) {
                warn("generate header file $mHeaderName failed!")
            }
        }
    }

    private fun generateSource() {
        val fileObject = mEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, PKG_NAME, mSourceName)
        fileObject.openOutputStream().use { out ->
            try {
                log("write native proxy file [" + fileObject.name + "]")
                buildString {
                    append(Constants.AUTO_GENERATE_NOTICE)
                    append("""
                        |#include "$mHeaderName"
                        |
                        |jclass ${cppClassName}::sClazz = nullptr;
                        |
                        |""".trimMargin())

                    if (mEnv.configurations.threadSafe) {
                        append("""
                            |// thread safe init
                            |std::mutex $cppClassName::sInitLock;
                            |std::atomic_bool $cppClassName::sInited;
                            |
                            |""".trimMargin())
                    }

                    buildNativeInitClass()

                    mConstructors.forEach { r ->
                        append("jmethodID ${cppClassName}::${getConstructorName(r.method, r.index)};\n")
                    }
                    append("\n")
                    mMethods.forEach { r ->
                        append("jmethodID ${cppClassName}::${getMethodName(r.method, r.index)};\n")
                    }
                    append("\n")
                    mFields.forEachIndexed { index, f ->
                        append("jfieldID ${cppClassName}::${getFieldName(f, index)};\n")
                    }
                    append("\n")
                }.let { content ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (e: IOException) {
                warn("generate header file $mHeaderName failed!")
            }
        }
    }

    private fun StringBuilder.buildConstantsIdDeclare() {
        mClazz.enclosedElements
                .stream()
                .filter { e -> e.kind == ElementKind.FIELD }
                .map { e -> e as VariableElement }
                .filter { ve -> ve.constantValue != null }
                .forEach { ve ->
                    // if this field is a compile-time constant value it's
                    // value will be returned, otherwise null will be returned.
                    val constValue = ve.constantValue!!

                    mConsts.add(ve.simpleName.toString())
                    val type = mHelper.toNativeType(ve.asType(), true)
                    val name = ve.simpleName
                    val value = HandyHelper.getJNIHeaderConstantValue(constValue)
                    append("    static constexpr const $type $name = ${value};\n")
                }
        append('\n')
    }

    private fun StringBuilder.buildConstructorIdDeclare() {
        mConstructors.forEach { r ->
            append("    static jmethodID ${getConstructorName(r.method, r.index)};\n")
        }
        append('\n')
    }

    private fun StringBuilder.buildMethodIdDeclare() {
        mMethods.forEach { r ->
            append("    static jmethodID ${getMethodName(r.method, r.index)};\n")
        }
        append('\n')

    }

    private fun StringBuilder.buildFieldIdDeclare() {
        mFields.forEachIndexed { index, f ->
            val f = f as VariableElement
            if (f.constantValue != null) {
                warn("you are trying to add getter/setter to a compile-time constant "
                        + mClassName + "." + f.simpleName.toString())
            }
            append("    static jfieldID ${getFieldName(f, index)};\n")
        }
        append('\n')
    }

    private fun StringBuilder.buildConstructorDefines() {
        mConstructors.forEach { r ->
            var param = getJniMethodParam(r.method)
            if (param.isNotEmpty()) {
                param = ", $param"
            }
            append("""
                |    // construct: ${mHelper.getModifiers(r.method)} ${mSimpleClassName}(${mHelper.getJavaMethodParam(r.method)})
                |    static $cppClassName newInstance${r.resolvedPostFix}(JNIEnv* env${param}) noexcept {
                |       assertInited(env);
                |       return ${cppClassName}(env, env->NewObject(sClazz, ${getConstructorName(r.method, r.index)}${getJniMethodParamVal(r.method)}));
                |    } 
                |    
                |""".trimMargin())
        }
        append('\n')
    }

    private fun StringBuilder.buildMethodDefines() {
        mMethods.forEach { r ->
            val m = r.method
            val isStatic = m.modifiers.contains(Modifier.STATIC)
            val returnType = mHelper.toJNIType(m.returnType)

            var jniParam = getJniMethodParam(m)
            if (isStatic) {
                jniParam = if (jniParam.isNotEmpty()) {
                    "JNIEnv* env, $jniParam"
                } else {
                    "JNIEnv* env"
                }
            }

            val staticMethod = if (isStatic) "static " else ""
            val env = if (isStatic) "env" else "mJniEnv"
            val constMod = if (isStatic) "" else "const "

            append("""
                |    // method: ${mHelper.getModifiers(m)} ${m.returnType} ${m.simpleName}(${mHelper.getJavaMethodParam(m)})
                |    ${staticMethod}$returnType ${m.simpleName}${r.resolvedPostFix}(${jniParam}) ${constMod}{
                |""".trimMargin())
            if (isStatic) {
                append("        assertInited(env);\n")
            }

            if (m.returnType.kind !== TypeKind.VOID) {
                append("        return ")
            } else {
                append("        ")
            }
            if (returnTypeNeedCast(returnType)) {
                append("reinterpret_cast<${returnType}>(")
            }

            val static = if (isStatic) "Static" else ""
            val classOrObj = if (isStatic) "sClazz" else "mJavaObjectReference"
            append("${env}->Call${static}${getTypeForJniCall(m.returnType)}Method(${classOrObj}, ${getMethodName(m, r.index)}${getJniMethodParamVal(m)})")
            if (returnTypeNeedCast(returnType)) {
                append(")")
            }
            append(";\n")
            append("    }\n\n")
        }
        append('\n')
    }

    private fun StringBuilder.buildFieldDefines() {
        mFields.forEachIndexed { index, f ->
            val isStatic = f.modifiers.contains(Modifier.STATIC)
            val camelCaseName = f.simpleName.toString().capitalize()
            val returnType = mHelper.toJNIType(f.asType())
            val getterSetters = hasGetterSetter(f)
            val fieldId = getFieldName(f, index)
            val typeForJniCall = getTypeForJniCall(f.asType())
            val jniType = mHelper.toJNIType(f.asType())


            val static = if (isStatic) "Static" else ""
            val classOrObj = if (isStatic) "sClazz" else "mJavaObjectReference"

            val comment = "// field: ${mHelper.getModifiers(f)} ${f.asType()} ${f.simpleName}"

            if (getterSetters.contains(GetterSetter.GETTER)) {
                append("""
                    |    $comment
                    |    $returnType get${camelCaseName}() const {
                    |       return """.trimMargin())

                if (returnTypeNeedCast(returnType)) {
                    append("reinterpret_cast<${returnType}>(")
                }

                append("mJniEnv->Get${static}${typeForJniCall}Field(${classOrObj}, $fieldId)")

                if (returnTypeNeedCast(returnType)) {
                    append(")")
                }

                append(""";
                    |
                    |   }
                    |""".trimMargin())
            }

            if (getterSetters.contains(GetterSetter.SETTER)) {
                append("""
                    |    $comment
                    |    void set${camelCaseName}(${jniType} ${f.simpleName}) const {
                    |        mJniEnv->Set${static}${typeForJniCall}Field(${classOrObj}, ${fieldId}, ${f.simpleName});
                    |    }
                    |""".trimMargin())
            }
            append('\n')
        }
    }

    private fun StringBuilder.buildNativeInitClass() {

        append("""
            |/*static*/ bool $cppClassName::initClazz(JNIEnv *env) {
            |#define JENNY_CHECK_NULL(val)                      \
            |       do {                                        \
            |           if ((val) == nullptr) {                 \
            |               return false;                       \
            |           }                                       \
            |       } while(false)
            |
            |""".trimMargin())

        if (mEnv.configurations.threadSafe) {
            append("""
                |    if (!sInited) {
                |        std::lock_guard<std::mutex> lg(sInitLock);
                |""".trimMargin())
        }
        append("""
                |        if (!sInited) {
                |            auto clazz = env->FindClass(FULL_CLASS_NAME);
                |            JENNY_CHECK_NULL(clazz);
                |            sClazz = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
                |            env->DeleteLocalRef(clazz);
                |            JENNY_CHECK_NULL(sClazz);
                |""".trimMargin())

        buildConstructorIdInit()
        buildMethodIdInit()
        buildFieldIdInit()

        append("""
                |            sInited = true;
                |        }
                |""".trimMargin())
        if (mEnv.configurations.threadSafe) {
            append("    }\n")
        }

        append("""
            |#undef JENNY_CHECK_NULL
            |   return true;
            |}
            |
            |""".trimMargin())

        if (mEnv.configurations.threadSafe) {
            append("""
                |/*static*/ void $cppClassName::releaseClazz(JNIEnv *env) {
                |    if (sInited) {
                |        std::lock_guard<std::mutex> lg(sInitLock);
                |        if (sInited) {
                |            env->DeleteLocalRef(sClazz);
                |            sInited = false;
                |        }
                |    }
                |}
                |
                |""".trimMargin())
        } else {
            append("""
                |/*static*/ void $cppClassName::releaseClazz(JNIEnv *env) {
                |    if (sInited) {
                |        env->DeleteLocalRef(sClazz);
                |        sInited = false;
                |    }
                |}
                |
                |""".trimMargin())
        }
    }

    private fun StringBuilder.buildConstructorIdInit() {
        mConstructors.forEach { r ->
            val c = r.method
            val name = getConstructorName(c, r.index)
            val signature = mHelper.getBinaryMethodSignature(c)

            append("""
            |            $name = env->GetMethodID(sClazz, "<init>", "$signature");
            |            JENNY_CHECK_NULL(${name});
            |
            |""".trimMargin())
        }
        append('\n')
    }

    private fun StringBuilder.buildMethodIdInit() {
        mMethods.forEach { r ->
            val m = r.method
            val name = getMethodName(m, r.index)
            val static = if (m.modifiers.contains(Modifier.STATIC)) "Static" else ""
            val methodName = m.simpleName
            val signature = mHelper.getBinaryMethodSignature(m)

            append("""
            |            $name = env->Get${static}MethodID(sClazz, "$methodName", "$signature");
            |            JENNY_CHECK_NULL(${name});
            |
            |""".trimMargin())
        }
        append('\n')
    }

    private fun StringBuilder.buildFieldIdInit() {
        mFields.forEachIndexed { index, f ->
            val name = getFieldName(f, index)
            val static = if (f.modifiers.contains(Modifier.STATIC)) "Static" else ""
            val fieldName = f.simpleName
            val signature = mHelper.getBinaryTypeSignature(f.asType())

            append("""
            |            $name = env->Get${static}FieldID(sClazz, "$fieldName", "$signature");
            |            JENNY_CHECK_NULL(${name});
            |
            |""".trimMargin())
        }
        append('\n')
    }

    private fun shouldGenerateMethod(m: ExecutableElement): Boolean {
        val annotation = m.getAnnotation(NativeMethodProxy::class.java)
        return annotation?.enabled ?: mNativeProxyAnnotation.allMethods
    }

    private fun shouldGenerateField(f: Element): Boolean {
        return !hasGetterSetter(f).isEmpty()
    }

    private enum class GetterSetter {
        GETTER, SETTER
    }

    private fun hasGetterSetter(field: Element): EnumSet<GetterSetter> {
        var getter = false
        var setter = false

        var auto = mNativeProxyAnnotation.allFields
        val annotation = field.getAnnotation(NativeFieldProxy::class.java)
        if (annotation != null) {
            auto = false
            getter = annotation.getter
            setter = annotation.setter
        } else {
            if (mConsts.contains(field.simpleName.toString())) {
                auto = false
                //don't generate
                getter = false
                setter = false
            }
        }

        if (auto) {
            val camelCaseName = field.simpleName.toString().capitalize()
            setter = !mMethodSimpleName.contains("set$camelCaseName")

            val type = mHelper.toJNIType(field.asType())
            getter = !mMethodSimpleName.contains("get$camelCaseName")
            if ("jboolean" == type) {
                getter = getter and !mMethodSimpleName.contains("is$camelCaseName")
            }
        }

        return if (getter && setter) {
            EnumSet.of(GetterSetter.GETTER, GetterSetter.SETTER)
        } else if (getter) {
            EnumSet.of(GetterSetter.GETTER)
        } else if (setter) {
            EnumSet.of(GetterSetter.SETTER)
        } else {
            EnumSet.noneOf(GetterSetter::class.java)
        }
    }


    private fun returnTypeNeedCast(returnType: String): Boolean {
        return when (returnType) {
            "jclass", "jstring", "jarray", "jobjectArray", "jbooleanArray", "jbyteArray", "jcharArray", "jshortArray", "jintArray", "jlongArray", "jfloatArray", "jdoubleArray", "jthrowable", "jweak" -> true
            else ->
                //primitive type or jobject or void
                false
        }
    }

    private fun getConstructorName(e: ExecutableElement, index: Int): String {
        return "sConstruct_$index"
    }

    private fun getMethodName(e: ExecutableElement, index: Int): String {
        return "sMethod_" + e.simpleName + "_" + index
    }

    private fun getFieldName(e: Element, index: Int): String {
        return "sField_" + e.simpleName + "_" + index
    }

    private fun findConstructors() {
        mClazz.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.CONSTRUCTOR }
                .map { it as ExecutableElement }
                .filter { shouldGenerateMethod(it) }
                .toList()
                .let {
                    MethodOverloadResolver(mHelper, this::getJniMethodParamTypes).resolve(it)
                            .let { mConstructors.addAll(it) }
                }
    }

    private fun findMethods() {
        mClazz.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .filter { shouldGenerateMethod(it) }
                .groupBy { it.simpleName.toString() }
                .forEach { (simpleName, methodList) ->
                    mMethodSimpleName.add(simpleName)
                    MethodOverloadResolver(mHelper, this::getJniMethodParamTypes).resolve(methodList).let {
                        mMethods.addAll(it)
                    }
                }
    }

    private fun findFields() {
        mClazz.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.FIELD }
                .filter { shouldGenerateField(it) }
                .forEach { mFields.add(it) }
    }

    private fun getJniMethodParamTypes(m: ExecutableElement) = buildString {
        var needComma = false
        if (mHelper.isNestedClass(mClazz)) {
            val enclosingElement = mClazz.enclosingElement
            //nested class has an this$0 in its constructor
            append(mHelper.toJNIType(enclosingElement.asType()))
            needComma = true
        }
        m.parameters.forEach { p ->
            if (needComma) append(", ")
            append(mHelper.toJNIType(p.asType()))
            needComma = true
        }
    }

    private fun getJniMethodParam(m: ExecutableElement) = buildString {
        var needComma = false
        if (mHelper.isNestedClass(mClazz)) {
            val enclosingElement = mClazz.enclosingElement
            //nested class has an this$0 in its constructor
            append(mHelper.toJNIType(enclosingElement.asType()))
                    .append(" ")
                    .append("enclosingClass")
            needComma = true
        }
        m.parameters.forEach { p ->
            if (needComma) append(", ")
            append(mHelper.toJNIType(p.asType()))
                    .append(" ")
                    .append(p.simpleName)
            needComma = true
        }
    }

    private fun getJniMethodParamVal(m: ExecutableElement): String {
        val sb = StringBuilder(64)
        if (mHelper.isNestedClass(mClazz)) {
            //nested class has an this$0 in its constructor
            sb.append(", ")
                    .append("enclosingClass")
        }
        m.parameters.forEach { p ->
            sb.append(", ")
                    .append(p.simpleName)
        }
        return sb.toString()
    }

    private fun getTypeForJniCall(type: TypeMirror): String {
        val result: String
        val k = type.kind
        result = if (k.isPrimitive || k == TypeKind.VOID) {
            k.name.toLowerCase(Locale.US)
        } else {
            "object"
        }
        return result.capitalize()
    }
}