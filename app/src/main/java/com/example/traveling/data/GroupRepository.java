package com.example.traveling.data;

import android.util.Log;

import com.example.traveling.model.Group;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupRepository {

    private static final String TAG = "GroupRepository";
    private static GroupRepository instance;
    private final FirebaseFirestore db;

    private GroupRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static GroupRepository get() {
        if (instance == null) instance = new GroupRepository();
        return instance;
    }

    public void searchPublicGroups(String theme, String query,
                                   FirestoreRepository.OnSuccessCallback<List<Group>> callback) {
        com.google.firebase.firestore.Query q = db.collection("groups")
                .whereEqualTo("isPublic", true);
        if (theme != null && !theme.isEmpty()) {
            q = q.whereEqualTo("theme", theme);
        }
        q.get().addOnSuccessListener(snapshots -> {
                    List<Group> groups = new ArrayList<>();
                    String lower = query != null ? query.toLowerCase().trim() : "";
                    for (DocumentSnapshot doc : snapshots) {
                        Group g = doc.toObject(Group.class);
                        if (g == null) continue;
                        g.setId(doc.getId());
                        if (lower.isEmpty() || (g.getName() != null && g.getName().toLowerCase().contains(lower))
                                || (g.getDescription() != null && g.getDescription().toLowerCase().contains(lower))) {
                            groups.add(g);
                        }
                    }
                    callback.onSuccess(groups);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching groups", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    public void createGroup(String name, String description, String theme, boolean isPublic,
                            FirestoreRepository.OnSuccessCallback<String> onSuccess,
                            FirestoreRepository.OnFailureCallback onFailure) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { if (onFailure != null) onFailure.onFailure(new Exception("Non connecté")); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("theme", theme != null ? theme : "");
        data.put("isPublic", isPublic);
        data.put("createdBy", user.getUid());
        data.put("createdByName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
        data.put("membersCount", 1);
        data.put("createdAt", FieldValue.serverTimestamp());

        data.put("memberIds", java.util.Collections.singletonList(user.getUid()));

        db.collection("groups").add(data)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("role", "owner");
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                    ref.collection("members").document(user.getUid()).set(memberData);
                    if (onSuccess != null) onSuccess.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating group", e);
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    public void loadUserGroups(FirestoreRepository.OnSuccessCallback<List<Group>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(new ArrayList<>()); return; }

        db.collection("groups")
                .whereArrayContains("memberIds", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Group> groups = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Group g = doc.toObject(Group.class);
                        if (g != null) { g.setId(doc.getId()); groups.add(g); }
                    }
                    callback.onSuccess(groups);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user groups", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    public void joinGroup(String groupId, FirestoreRepository.OnSuccessCallback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(false); return; }

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("role", "member");
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());

        db.runBatch(batch -> {
            batch.set(db.collection("groups").document(groupId)
                    .collection("members").document(user.getUid()), memberData);
            batch.update(db.collection("groups").document(groupId),
                    "membersCount", FieldValue.increment(1),
                    "memberIds", FieldValue.arrayUnion(user.getUid()));
        }).addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> {
              Log.e(TAG, "Error joining group", e);
              callback.onSuccess(false);
          });
    }

    public void joinGroupByName(String name, FirestoreRepository.OnSuccessCallback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(false); return; }

        db.collection("groups")
                .whereEqualTo("name", name)
                .whereEqualTo("isPublic", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) { callback.onSuccess(false); return; }
                    DocumentSnapshot doc = snapshots.getDocuments().get(0);
                    String groupId = doc.getId();
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("role", "member");
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                    db.runBatch(batch -> {
                        batch.set(db.collection("groups").document(groupId)
                                .collection("members").document(user.getUid()), memberData);
                        batch.update(db.collection("groups").document(groupId),
                                "membersCount", FieldValue.increment(1),
                                "memberIds", FieldValue.arrayUnion(user.getUid()));
                    }).addOnSuccessListener(v -> {
                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(e -> callback.onSuccess(false));
                })
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    public void addGroupPost(String groupId, String photoId, String message,
                             FirestoreRepository.OnSuccessCallback<String> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("photoId", photoId);
        data.put("message", message);
        data.put("authorId", user.getUid());
        data.put("authorName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups").document(groupId)
                .collection("posts").add(data)
                .addOnSuccessListener(ref -> {
                    if (photoId != null && !photoId.isEmpty()) {
                        Map<String, Object> photoUpdate = new HashMap<>();
                        photoUpdate.put("sharedToGroup", true);
                        db.collection("photos").document(photoId).update(photoUpdate);
                    }
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding post", e));
    }

    public void loadGroupPosts(String groupId, FirestoreRepository.OnSuccessCallback<QuerySnapshot> callback) {
        db.collection("groups").document(groupId)
                .collection("posts")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading group posts", e);
                    callback.onSuccess(null);
                });
    }

    public void loadGroupMembers(String groupId, FirestoreRepository.OnSuccessCallback<QuerySnapshot> callback) {
        db.collection("groups").document(groupId)
                .collection("members").get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    public void isUserMember(String groupId, FirestoreRepository.OnSuccessCallback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(false); return; }
        db.collection("groups").document(groupId)
                .collection("members").document(user.getUid()).get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> callback.onSuccess(false));
    }
}
