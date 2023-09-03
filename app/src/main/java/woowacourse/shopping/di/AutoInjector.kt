package woowacourse.shopping.di

import woowacourse.shopping.di.module.NormalModule
import woowacourse.shopping.di.module.SingletonModule
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

class AutoInjector(
    private val singletonModule: SingletonModule,
    private val normalModule: NormalModule,
) : Injector {
    private val moduleFunctions =
        singletonModule::class.memberFunctions.filter { it.visibility == KVisibility.PUBLIC } +
            normalModule::class.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }

    override fun <T : Any> inject(modelClass: Class<T>): T {
        val primaryConstructor = modelClass.kotlin.primaryConstructor

        val injectableFunctions = mutableListOf<KFunction<*>>()
        for (function in moduleFunctions) {
            val returnKClass = function.returnType.classifier as KClass<*>
            if (modelClass.kotlin.isSubclassOf(returnKClass)) {
                injectableFunctions.add(function)
            }
        }

        // 예시로 함수 목록 출력
        println("Injectable Functions for T:")
        for (function in injectableFunctions) {
            println("Function Name: ${function.name}")
            println("Return Type: ${function.returnType}")
        }

        // 해당 객체를 만들기 위한 필요 구성 요소들을 얻는 과정.
        val params = mutableListOf<Any>()
        if (injectableFunctions.size == 1) {
            injectableFunctions.first().valueParameters.forEach { param ->
                val kcalss = param.type.classifier as KClass<*>
                params.add(inject(kcalss.java))
            }
            normalModule::class.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }
                .forEach {
                    if (it.name == injectableFunctions.first().name) {
                        params.add(0, normalModule)
                        return it.call(*params.toTypedArray()) as T
                    }
                }

            singletonModule::class.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }
                .forEach {
                    if (it.name == injectableFunctions.first().name) {
                        params.add(0, singletonModule)
                        return it.call(*params.toTypedArray()) as T
                    }
                }

            throw RuntimeException("일치 없음")
        } else if (injectableFunctions.size >= 2) {
            // 이 메소드들 중 어노테이션으로 판단해서 일치하는 메소드를 실행시킬 예정.
        } else { // 모듈에 지정하지 않았다는 것은 그냥 주생성자로 바로 객체를 얻을 수 있다는 의미
            if (primaryConstructor == null) throw RuntimeException("주생성자 없음")
            primaryConstructor.valueParameters.forEach { param ->
                val kcalss = param.type.classifier as KClass<*>
                params.add(inject(kcalss.java))
            }
            return primaryConstructor.call(*params.toTypedArray())
        }

        throw RuntimeException("여기까지 옴?")
    }
}
