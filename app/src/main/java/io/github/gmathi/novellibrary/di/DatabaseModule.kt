package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDBHelper(@ApplicationContext context: Context): DBHelper {
        return DBHelper.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDataCenter(@ApplicationContext context: Context): DataCenter {
        return DataCenter(context)
    }
}