package com.neleso.audiocrate

import android.car.Car
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.net.URL

class CmdPushCarData(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // TODO(Sebastian): add task for sending car data to server


        Log.d(TAG, "Here you have to push your car data to backend server")

        MyCar.fireAndForget()
        return Result.success()

    }

    companion object {
        private val TAG = "CmdPushCarData"
    }
}