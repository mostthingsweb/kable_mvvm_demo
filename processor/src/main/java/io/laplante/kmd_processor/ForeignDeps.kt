package io.laplante.kmd_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

object ForeignDeps {
    object android {
        object content {
            val Context = ClassName("android.content", "Context")
            val Intent = ClassName("android.content", "Intent")
            val ServiceConnection = ClassName("android.content", "ServiceConnection")
            val ComponentName = ClassName("android.content", "ComponentName")
        }

        object os {
            val IBinder = ClassName("android.os", "IBinder")
        }
    }

    object androidx {
        object lifecycle {
            val LifecycleObserver = ClassName("androidx.lifecycle", "LifecycleObserver")
            val OnLifecycleEvent = ClassName("androidx.lifecycle", "OnLifecycleEvent")
            val Lifecycle = ClassName("androidx.lifecycle", "Lifecycle")
            val LifecycleScope = MemberName("androidx.lifecycle", "lifecycleScope")
        }
    }

    object javax {
        object inject {
            val Singleton = ClassName("javax.inject", "Singleton")
        }
    }

    object kotlinx {
        object coroutines {
            val launch = MemberName("kotlinx.coroutines", "launch")

            object flow {
                val StateFlow = ClassName("kotlinx.coroutines.flow", "StateFlow")
                val Flow = ClassName("kotlinx.coroutines.flow", "Flow")
                val MutableStateFlow = ClassName("kotlinx.coroutines.flow", "MutableStateFlow")
                val asStateFlow = MemberName("kotlinx.coroutines.flow", "asStateFlow")
                val emitAll = MemberName("kotlinx.coroutines.flow", "emitAll")
            }
        }
    }
}