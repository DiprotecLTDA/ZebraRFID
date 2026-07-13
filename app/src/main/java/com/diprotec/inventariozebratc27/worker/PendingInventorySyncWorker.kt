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
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class PendingInventorySyncWorker @AssistedInject constructor(
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
            val summary =
                NetworkUsageContext.runWith(
                    source =
                        NetworkUsageClassifier.SOURCE_WORKER,
                    operation =
                        "Worker sincronizar inventarios pendientes"
                ) {
                    sync.syncAllInventarioPendiente()
                }

            Log.d(
                TAG,
                "Pending inventory sync OK. " +
                        "Capturas=${summary.capturas}, " +
                        "Finalizados=${summary.finalizados}"
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
                            "No fue posible sincronizar los inventarios pendientes."
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
            ?: "No fue posible sincronizar los inventarios pendientes."
    }

    companion object {
        const val KEY_ERROR_MESSAGE =
            "error_message"

        private const val TAG =
            "PENDING_INV_SYNC"

        private const val UNIQUE_PERIODIC =
            "pendingInventorySync_periodic"

        private const val UNIQUE_ONCE =
            "pendingInventorySync_once"

        fun schedulePeriodic(
            context: Context
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                PeriodicWorkRequestBuilder<PendingInventorySyncWorker>(
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
        ): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                OneTimeWorkRequestBuilder<PendingInventorySyncWorker>()
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
                    ExistingWorkPolicy.REPLACE,
                    request
                )

            return request.id
        }
    }
}