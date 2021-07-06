package io.laplante.kmd_app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.laplante.kmd.BluetoothLeServiceWrapper
import javax.inject.Singleton


// TODO now that kable supports service UUID filtering this provider does nothing useful; remove it
//  and simplify the annotation processor

@Module
@InstallIn(SingletonComponent::class)
object EpcServiceProviderModule {
    @Provides
    @Singleton
    fun provideEpcServiceWrapper2(
        @ApplicationContext applicationContext: Context,
    ): BluetoothLeServiceWrapper {
        return BluetoothLeServiceWrapper(
            applicationContext
        )
    }
}
