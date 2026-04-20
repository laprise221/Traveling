package com.example.traveling.data;

import android.util.Log;

import com.example.traveling.model.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static NotificationRepository instance;
    private final FirebaseFirestore db;

    private NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static NotificationRepository get() {
        if (instance == null) instance = new NotificationRepository();
        return instance;
    }

    /** Send a notification to a specific user (fire-and-forget). */
    public void sendNotification(String recipientUid, String type,
                                  String senderId, String senderName,
                                  String contentId, String contentType,
                                  String contentTitle, String message) {
        if (recipientUid == null || recipientUid.equals(senderId)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("senderId", senderId);
        data.put("senderName", senderName != null ? senderName : "Quelqu'un");
        data.put("contentId", contentId != null ? contentId : "");
        data.put("contentType", contentType != null ? contentType : "");
        data.put("contentTitle", contentTitle != null ? contentTitle : "");
        data.put("message", message != null ? message : "");
        data.put("read", false);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(recipientUid)
                .collection("notifications").add(data)
                .addOnFailureListener(e -> Log.w(TAG, "Error sending notification", e));
    }

    /** Load all notifications for the current user, newest first. */
    public void loadNotifications(FirestoreRepository.OnSuccessCallback<List<AppNotification>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(new ArrayList<>()); return; }

        db.collection("users").document(user.getUid())
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<AppNotification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        if (n != null) { n.setId(doc.getId()); list.add(n); }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    /** Mark a single notification as read. */
    public void markAsRead(String notifId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || notifId == null) return;
        db.collection("users").document(user.getUid())
                .collection("notifications").document(notifId)
                .update("read", true)
                .addOnFailureListener(e -> Log.w(TAG, "markAsRead failed", e));
    }

    /** Mark all notifications as read. */
    public void markAllAsRead(Runnable onDone) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { if (onDone != null) onDone.run(); return; }

        db.collection("users").document(user.getUid())
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) { if (onDone != null) onDone.run(); return; }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
                            .addOnFailureListener(e -> { if (onDone != null) onDone.run(); });
                })
                .addOnFailureListener(e -> { if (onDone != null) onDone.run(); });
    }

    /** Count unread notifications for the current user. */
    public void getUnreadCount(FirestoreRepository.OnSuccessCallback<Integer> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(0); return; }

        db.collection("users").document(user.getUid())
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshots -> callback.onSuccess(snapshots.size()))
                .addOnFailureListener(e -> callback.onSuccess(0));
    }
}
