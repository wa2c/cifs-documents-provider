package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import com.wa2c.android.cifsdocumentsprovider.common.values.ThumbnailType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileDescriptorManager @Inject internal constructor(
    @ApplicationContext private val context: Context,
) {

    private val fileHandler = lazy {
        Handler(
            HandlerThread(this.javaClass.simpleName)
                .apply { start() }
                .looper
        )
    }

    private val storageManager: StorageManager by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }


    fun provideFileDescriptor(
        accessMode: AccessMode,
        callback: ProxyFileDescriptorCallback
    ): ParcelFileDescriptor {
        return storageManager.openProxyFileDescriptor(
            ParcelFileDescriptor.parseMode(accessMode.safMode),
            callback,
            fileHandler.value
        )
    }

    private val thumbnailJobs = mutableSetOf<Job>()

    /**
     * Get thumbnail descriptor
     */
    suspend fun getThumbnailDescriptor(
        thumbnailType: ThumbnailType?,
        getFileDescriptor: suspend () -> ParcelFileDescriptor?,
    ): ParcelFileDescriptor? {
        return try {
            when (thumbnailType) {
                ThumbnailType.IMAGE -> {
                    getFileDescriptor()
                }
                ThumbnailType.AUDIO,
                ThumbnailType.VIDEO, -> {
                    val pipe = ParcelFileDescriptor.createReliablePipe()
                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                            try {
                                MediaMetadataRetriever().use { mmr ->
                                    getFileDescriptor()?.let {fd ->
                                        mmr.setDataSource(fd.fileDescriptor)
                                        mmr.embeddedPicture
                                    }
                                }?.let { imageBytes ->
                                    imageBytes.inputStream().use { input ->
                                        val buffer = ByteArray(BUFFER_SIZE)
                                        var bytes = input.read(buffer)
                                        while (bytes >= 0) {
                                            if (isActive.not()) break
                                            output.write(buffer, 0, bytes)
                                            bytes = input.read(buffer)
                                        }
                                    }
                                }
                            } catch (e: IOException) {
                                logE(e)
                            }
                        }
                        thumbnailJobs.remove(this.coroutineContext.job)
                    }.also {
                        thumbnailJobs.add(it)
                    }
                    pipe[0]
                }
                else -> null
            }
        } catch (e: IOException) {
            logE(e)
            throw e
        }
    }

    fun cancelThumbnailLoading() {
        thumbnailJobs.forEach { it.cancel() }
        thumbnailJobs.clear()
    }

    fun close() {
        cancelThumbnailLoading()
        if (fileHandler.isInitialized()) fileHandler.value.looper.quit()
    }
}
