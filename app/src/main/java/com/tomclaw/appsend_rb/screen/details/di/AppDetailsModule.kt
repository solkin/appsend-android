package com.tomclaw.appsend_rb.screen.details.di

import android.annotation.SuppressLint
import android.content.Context
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider
import com.tomclaw.appsend_rb.screen.apps.PreferencesProviderImpl
import com.tomclaw.appsend_rb.screen.details.AppDetailsPresenter
import com.tomclaw.appsend_rb.screen.details.AppDetailsPresenterImpl
import com.tomclaw.appsend_rb.screen.details.AppDetailsResourceProvider
import com.tomclaw.appsend_rb.screen.details.AppDetailsResourceProviderImpl
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Module
import dagger.Provides
import java.text.DateFormat
import java.text.SimpleDateFormat

@Module
class AppDetailsModule(
    private val context: Context,
    private val entity: AppEntity
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        resourceProvider: AppDetailsResourceProvider
    ): AppDetailsPresenter = AppDetailsPresenterImpl(entity, resourceProvider)

    @Provides
    @PerActivity
    internal fun providePreferencesProvider(): PreferencesProvider {
        return PreferencesProviderImpl(context)
    }

    @Provides
    @PerActivity
    internal fun provideResourceProvider(dateFormat: DateFormat): AppDetailsResourceProvider {
        return AppDetailsResourceProviderImpl(context, dateFormat)
    }

    @SuppressLint("SimpleDateFormat")
    @Provides
    @PerActivity
    internal fun provideDateFormat(): DateFormat {
        return SimpleDateFormat("dd.MM.yy")
    }

}
