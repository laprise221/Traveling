package com.example.traveling.worker;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SchedulePublishHelper {

    public static final String COLLECTION_PHOTOS = "photos";
    public static final String COLLECTION_PATHS = "travelPaths";

    public static void schedule(Context context, String docId, String collection, Date publishAt) {
        long delayMs = publishAt.getTime() - System.currentTimeMillis();
        if (delayMs <= 0) delayMs = 0;

        Data inputData = new Data.Builder()
                .putString(ScheduledPublishWorker.KEY_DOC_ID, docId)
                .putString(ScheduledPublishWorker.KEY_COLLECTION, collection)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ScheduledPublishWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(tagFor(docId))
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    public static void cancel(Context context, String docId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(docId));
    }

    private static String tagFor(String docId) {
        return "scheduled_publish_" + docId;
    }
}
