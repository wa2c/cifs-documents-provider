package com.wa2c.android.cifsdocumentsprovider

import android.app.Application
import com.wa2c.android.cifsdocumentsprovider.common.utils.initLog
import com.wa2c.android.cifsdocumentsprovider.data.BuildConfig
import com.wa2c.android.cifsdocumentsprovider.domain.repository.AppRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class App: Application() {

    @Inject
    lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()

        initLog(BuildConfig.DEBUG)
        runBlocking {
            repository.migrate()
        }
    }
}
