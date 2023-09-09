package com.example.di.module

import android.content.Context
import com.example.di.annotation.FieldInject
import com.example.di.annotation.Qualifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

abstract class Module(var context: Context?, private val parentModule: Module? = null) {
    protected val cache = mutableMapOf<String, Any>()

    fun <T : Any> provideInstance(clazz: Class<T>, qualifier: KClass<out Annotation>? = null): T {
        if (qualifier != null && qualifier.hasAnnotation<Qualifier>().not()) {
            throw IllegalArgumentException("qualifier 인자는 Qualifier를 메타 애노테이션으로 가져야 합니다")
        }
        val injectableFunctionWithModuleMap = searchInjectableFunctions(clazz, qualifier)
        return when (injectableFunctionWithModuleMap.size) {
            0 -> createWithPrimaryConstructor(clazz)
            1 -> {
                val entry = injectableFunctionWithModuleMap.entries.first()
                createWithModuleFunc(entry.value, entry.key)
            }

            else -> throw IllegalStateException("실행할 함수를 선택할 수 없습니다.")
        }
    }

    private fun <T : Any> searchInjectableFunctions(
        clazz: Class<T>,
        qualifier: KClass<out Annotation>? = null,
    ): Map<KFunction<*>, Module> {
        return getPublicMethodMap()
            .filter { (func, _) ->
                val returnKClass = func.returnType.classifier as KClass<*>
                clazz.kotlin.isSubclassOf(returnKClass)
            }.filter { (func, _) ->
                qualifier?.let { hasQualifierAtFunc(func, it) } ?: true
            }.takeUnless { it.isEmpty() }
            ?: parentModule?.searchInjectableFunctions(clazz, qualifier) ?: mapOf()
    }

    private fun hasQualifierAtFunc(func: KFunction<*>, qualifier: KClass<out Annotation>): Boolean {
        val funcQualifiers =
            func.annotations
                .filter { it.annotationClass.hasAnnotation<Qualifier>() }
                .map { it.annotationClass }
        if (funcQualifiers.contains(qualifier)) return true
        return false
    }

    private fun <T : Any> createWithModuleFunc(module: Module, func: KFunction<*>): T {
        @Suppress("UNCHECKED_CAST")
        return func.call(module, *getArguments(module, func).toTypedArray()) as T
    }

    private fun <T : Any> createWithPrimaryConstructor(clazz: Class<T>): T {
        val primaryConstructor =
            clazz.kotlin.primaryConstructor
                ?: throw NullPointerException("모듈에 특정 클래스를 주 생성자로 인스턴스화 하는데 필요한 인자를 제공하는 함수를 정의하지 않았습니다")
        val args = getArguments(this, primaryConstructor)
        return provideInjectField(primaryConstructor.call(*args.toTypedArray()))
    }

    private fun getArguments(baseModule: Module, func: KFunction<*>): List<Any> {
        return func.valueParameters.map { param ->
            val paramKClass = param.type.classifier as KClass<*>
            val qualifier =
                param.annotations.firstOrNull { it.annotationClass.hasAnnotation<Qualifier>() }
            baseModule.provideInstance(paramKClass.java, qualifier?.annotationClass)
        }
    }

    private fun <T : Any> provideInjectField(instance: T): T {
        instance::class.declaredMemberProperties
            .filterIsInstance<KMutableProperty<*>>()
            .filter { it.hasAnnotation<FieldInject>() }
            .forEach { field ->
                if (field.visibility != KVisibility.PUBLIC) throw IllegalStateException("필드 주입을 받으려는 ${field.name}의 가시성이 공개되어 있지 않습니다.")
                val qualifier =
                    field.annotations.firstOrNull { it.annotationClass.hasAnnotation<Qualifier>() }
                val fieldKClass = field.returnType.classifier as KClass<*>
                field.setter.call(
                    instance,
                    provideInstance(fieldKClass.java, qualifier?.annotationClass),
                )
            }
        return instance
    }

    protected inline fun <reified T : Any> getOrCreateInstance(crossinline create: () -> T): T {
        val name = T::class.qualifiedName ?: throw NullPointerException("클래스 이름 없음")
        if (cache[name] == null) cache[name] = create()
        return cache[name] as T
    }

    private fun getPublicMethodMap(): Map<KFunction<*>, Module> {
        return this@Module::class.declaredMemberFunctions.filter {
            it.visibility == KVisibility.PUBLIC
        }.associateWith { this@Module }
    }
}