package com.example.traveling.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ScheduledPublishWorker extends Worker {

    public static final String KEY_DOC_ID = "doc_id";
    public static final String KEY_COLLECTION = "collection"; // "photos" or "travelPaths"

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
        updates.put("scheduledPublishDate", null);

        try {
            Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection(collection)
                    .document(docId)
                    .update(updates)
            );
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
