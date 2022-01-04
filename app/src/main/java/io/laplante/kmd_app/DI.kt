package io.laplante.kmd_app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


// TODO: Note that this provider isn't really necessary. It is an artifact of a previous version
//  of the code where I had to manually provide an additional parameter to the constructor of the
//  service wrapper. I am leaving it in place partially out of laziness, but also because it may be
//  instructive.

@Module
@InstallIn(SingletonComponent::class)
object EpcServiceProviderModule {
    @Provides
    @Singleton
    fun provideEpcServiceWrapper(
        @ApplicationContext applicationContext: Context,
    ): BluetoothLeServiceWrapper {
        return BluetoothLeServiceWrapper(
            applicationContext
        )
    }
}
