package com.diprotec.inventariozebratc27.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.diprotec.inventariozebratc27.core.network.ApiException
import com.diprotec.inventariozebratc27.core.network.NetworkUsageClassifier
import com.diprotec.inventariozebratc27.core.network.NetworkUsageContext
import com.diprotec.inventariozebratc27.service.SyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class CatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(
            TAG,
            "doWork id=$id attempt=$runAttemptCount"
        )

        return try {
            val summary = NetworkUsageContext.runWith(
                source = NetworkUsageClassifier.SOURCE_WORKER,
                operation = "Worker sincronizar catálogos"
            ) {
                sync.syncAllCatalogs()
            }

            Log.d(
                TAG,
                "Catalog sync OK: $summary"
            )

            Result.success()
        } catch (exception: ApiException) {
            Log.e(
                TAG,
                "Error API: ${exception.message}",
                exception
            )

            if (exception.isRetryable()) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_ERROR_MESSAGE to
                                exception.safeMessage()
                    )
                )
            }
        } catch (exception: IOException) {
            Log.w(
                TAG,
                "Error de red. Se reintentará.",
                exception
            )

            Result.retry()
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "Error inesperado",
                throwable
            )

            Result.failure(
                workDataOf(
                    KEY_ERROR_MESSAGE to
                            "No fue posible sincronizar los catálogos."
                )
            )
        }
    }

    private fun ApiException.isRetryable(): Boolean {
        return httpCode in 500..599 ||
                estado in 500..599 ||
                cause is IOException
    }

    private fun ApiException.safeMessage(): String {
        return message
            .trim()
            .takeIf { it.isNotBlank() }
            ?: "No fue posible sincronizar los catálogos."
    }

    companion object {
        const val KEY_ERROR_MESSAGE =
            "error_message"

        private const val TAG =
            "CATALOG_SYNC"

        private const val UNIQUE_PERIODIC =
            "catalogSync_periodic"

        private const val UNIQUE_ONCE =
            "catalogSync_once"

        fun schedulePeriodic(
            context: Context
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                PeriodicWorkRequestBuilder<CatalogSyncWorker>(
                    15,
                    TimeUnit.MINUTES
                )
                    .setInitialDelay(
                        15,
                        TimeUnit.MINUTES
                    )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun runOnce(
            context: Context
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_ONCE,
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}