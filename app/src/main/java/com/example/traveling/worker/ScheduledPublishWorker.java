package com.example.traveling.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ScheduledPublishWorker extends Worker {

    private static final String TAG = "ScheduledPublishWorker";
    public static final String KEY_DOC_ID = "doc_id";
    public static final String KEY_COLLECTION = "collection";

    public ScheduledPublishWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String docId = getInputData().getString(KEY_DOC_ID);
        String collection = getInputData().getString(KEY_COLLECTION);

        if (docId == null || collection == null) return Result.failure();

        Map<String, Object> updates = new HashMap<>();
        updates.put("visibility", "public");
        updates.put("isPublic", true);
        updates.put("scheduledPublishDate", FieldValue.delete());

        try {
            Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection(collection)
                    .document(docId)
                    .update(updates),
                30, TimeUnit.SECONDS
            );
            Log.d(TAG, "Published " + collection + "/" + docId);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to publish " + collection + "/" + docId + ": " + e.getMessage());
            return Result.retry();
        }
    }
}
