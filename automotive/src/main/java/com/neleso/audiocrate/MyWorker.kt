package com.neleso.audiocrate

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d(TAG, "Default worker for performing long running task in scheduled job")
        // TODO: abstract Worker, which you can use as template
        return Result.success()
    }

    companion object {
        private val TAG = "MyWorker"
    }
}