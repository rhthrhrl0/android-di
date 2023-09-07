package woowacourse.shopping.di.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import woowacourse.shopping.di.AutoInjector
import woowacourse.shopping.di.application.DiApplication
import woowacourse.shopping.di.module.ActivityModule

abstract class DiEntryPointActivity<T : ActivityModule>(private val activityModuleClassType: Class<T>) :
    AppCompatActivity() {
    private val diApplication = application as DiApplication<*>

    lateinit var injector: AutoInjector
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previousHashCode = savedInstanceState?.getInt(ACTIVITY_INJECTOR_KEY)
        val activityModule = diApplication.diContainer.changeHashKeyForActivityModule(
            this.hashCode(),
            previousHashCode,
            activityModuleClassType,
        )
        injector = AutoInjector(activityModule)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            ACTIVITY_INJECTOR_KEY,
            this.hashCode(),
        ) // 객체가 사라지기 전에 해시코드값 저장. 강제 재생성시 인젝터 다시 불러오기 위해.
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            diApplication.diContainer.removeModule(this.hashCode())
        }
    }

    @MainThread
    inline fun <reified VM : ViewModel> ComponentActivity.viewModel(): Lazy<VM> {
        return ViewModelLazy(
            VM::class,
            { viewModelStore },
            {
                viewModelFactory {
                    initializer {
                        injector.inject(VM::class.java)
                    }
                }
            },
        )
    }

    companion object {
        private const val ACTIVITY_INJECTOR_KEY = "di_activity_injector_key"
    }
}