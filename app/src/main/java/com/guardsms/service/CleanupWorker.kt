package com.guardsms.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guardsms.data.repository.GuardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: GuardRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.deleteExpiredMessages()
                .onSuccess { Timber.d("Expired messages cleaned up") }
                .onFailure { Timber.e(it, "Cleanup failed") }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CleanupWorker error")
            Result.retry()
        }
    }
}
