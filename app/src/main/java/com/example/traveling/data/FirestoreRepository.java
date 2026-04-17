package com.example.traveling.data;

import android.util.Log;

import com.example.traveling.model.Comment;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central repository for all Firestore CRUD operations.
 * Follows the database structure defined in firestore_structure.puml.
 */
public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";
    private static FirestoreRepository instance;
    private final FirebaseFirestore db;

    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreRepository get() {
        if (instance == null) instance = new FirestoreRepository();
        return instance;
    }

    // ===================== USERS =====================

    /** Save or update user profile in Firestore */
    public void saveUser(String uid, String displayName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", displayName);
        user.put("email", email);
        user.put("createdAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid)
                .set(user, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    // ===================== PHOTOS =====================

    /** Save a new photo to Firestore */
    public void savePhoto(Photo photo, OnSuccessCallback<String> onSuccess, OnFailureCallback onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", photo.getTitle());
        data.put("description", photo.getDescription());
        data.put("authorId", photo.getAuthorId());
        data.put("authorName", photo.getAuthorName());
        data.put("locationName", photo.getLocationName());
        data.put("latitude", photo.getLatitude());
        data.put("longitude", photo.getLongitude());
        data.put("date", photo.getDate());
        data.put("locationType", photo.getLocationType());
        data.put("likeCount", 0);
        data.put("commentCount", 0);
        data.put("isPublic", photo.getIsPublic());
        data.put("groupId", photo.getGroupId());
        data.put("imageBase64", photo.getImageBase64());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("photos").add(data)
                .addOnSuccessListener(ref -> {
                    photo.setId(ref.getId());
                    if (onSuccess != null) onSuccess.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving photo", e);
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    /** Delete a photo from Firestore */
    public void deletePhoto(String photoId, OnSuccessCallback<Void> onSuccess) {
        db.collection("photos").document(photoId).delete()
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.onSuccess(null); })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting photo", e));
    }

    /** Load all public photos, ordered by newest first */
    public void loadPublicPhotos(OnSuccessCallback<List<Photo>> callback) {
        db.collection("photos")
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Photo> photos = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Photo photo = docToPhoto(doc);
                        if (photo != null) photos.add(photo);
                    }
                    callback.onSuccess(photos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading public photos", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    /** Load photos created by a specific user */
    public void loadUserPhotos(String uid, OnSuccessCallback<List<Photo>> callback) {
        db.collection("photos")
                .whereEqualTo("authorId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Photo> photos = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Photo photo = docToPhoto(doc);
                        if (photo != null) photos.add(photo);
                    }
                    callback.onSuccess(photos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user photos", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    // ===================== PHOTO LIKES =====================

    /** Toggle like on a photo (add or remove like sub-document + update likeCount) */
    public void toggleLikePhoto(String photoId, boolean like, OnSuccessCallback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DocumentReference likeRef = db.collection("photos").document(photoId)
                .collection("likes").document(user.getUid());
        DocumentReference photoRef = db.collection("photos").document(photoId);

        if (like) {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", user.getUid());
            likeData.put("likedAt", FieldValue.serverTimestamp());

            db.runBatch(batch -> {
                batch.set(likeRef, likeData);
                batch.update(photoRef, "likeCount", FieldValue.increment(1));
            }).addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
              .addOnFailureListener(e -> Log.e(TAG, "Error toggling photo like", e));
        } else {
            db.runBatch(batch -> {
                batch.delete(likeRef);
                batch.update(photoRef, "likeCount", FieldValue.increment(-1));
            }).addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
              .addOnFailureListener(e -> Log.e(TAG, "Error toggling photo like", e));
        }
    }

    /** Check if the current user has liked a specific photo */
    public void isPhotoLiked(String photoId, OnSuccessCallback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(false); return; }

        db.collection("photos").document(photoId)
                .collection("likes").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    /** Check liked status for a list of photos and set their liked flag */
    public void checkLikedPhotos(List<Photo> photos, Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || photos.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final int[] remaining = {photos.size()};
        for (Photo photo : photos) {
            if (photo.getId() == null) {
                if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                continue;
            }
            db.collection("photos").document(photo.getId())
                    .collection("likes").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) photo.setLikedSilent(true);
                        if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                    });
        }
    }

    // ===================== PHOTO COMMENTS =====================

    /** Add a comment to a photo */
    public void addPhotoComment(String photoId, Comment comment, OnSuccessCallback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("authorId", comment.getAuthorId());
        data.put("authorName", comment.getAuthor());
        data.put("text", comment.getText());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("photos").document(photoId)
                .collection("comments").add(data)
                .addOnSuccessListener(ref -> {
                    // Increment commentCount on parent document
                    db.collection("photos").document(photoId)
                            .update("commentCount", FieldValue.increment(1));
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding comment", e));
    }

    /** Load all comments for a photo */
    public void loadPhotoComments(String photoId, OnSuccessCallback<List<Comment>> callback) {
        db.collection("photos").document(photoId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);
                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String authorName = doc.getString("authorName");
                        String text = doc.getString("text");
                        Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? sdf.format(ts.toDate()) : "";
                        Comment c = new Comment(id, authorName != null ? authorName : "", text != null ? text : "", date);
                        c.setAuthorId(doc.getString("authorId"));
                        comments.add(c);
                    }
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading comments", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    // ===================== PATHS =====================

    /** Save a new path to Firestore */
    public void savePath(TravelPath path, OnSuccessCallback<String> onSuccess, OnFailureCallback onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", path.getTitle());
        data.put("city", path.getCity());
        data.put("description", path.getDescription());
        data.put("authorId", path.getAuthorId());
        data.put("authorName", path.getAuthorName());
        data.put("duration", path.getDuration());
        data.put("budget", path.getBudget());
        data.put("difficulty", path.getDifficulty());
        data.put("type", path.getType());
        data.put("likeCount", 0);
        data.put("commentCount", 0);
        data.put("isPublic", path.getIsPublic());
        data.put("imageBase64", path.getImageBase64());
        data.put("createdAt", FieldValue.serverTimestamp());

        // Convert steps to list of maps
        List<Map<String, Object>> stepMaps = new ArrayList<>();
        if (path.getSteps() != null) {
            for (com.example.traveling.model.PathStep step : path.getSteps()) {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("name", step.getName());
                stepMap.put("description", step.getDescription());
                stepMap.put("latitude", step.getLatitude());
                stepMap.put("longitude", step.getLongitude());
                stepMap.put("duration", step.getDuration());
                stepMap.put("timeSlot", step.getTimeSlot());
                stepMaps.add(stepMap);
            }
        }
        data.put("steps", stepMaps);

        db.collection("paths").add(data)
                .addOnSuccessListener(ref -> {
                    path.setId(ref.getId());
                    if (onSuccess != null) onSuccess.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving path", e);
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    /** Delete a path from Firestore */
    public void deletePath(String pathId, OnSuccessCallback<Void> onSuccess) {
        db.collection("paths").document(pathId).delete()
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.onSuccess(null); })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting path", e));
    }

    /** Load all public paths, ordered by newest first */
    public void loadPublicPaths(OnSuccessCallback<List<TravelPath>> callback) {
        db.collection("paths")
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<TravelPath> paths = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        TravelPath path = docToPath(doc);
                        if (path != null) paths.add(path);
                    }
                    callback.onSuccess(paths);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading public paths", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    /** Load paths created by a specific user */
    public void loadUserPaths(String uid, OnSuccessCallback<List<TravelPath>> callback) {
        db.collection("paths")
                .whereEqualTo("authorId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<TravelPath> paths = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        TravelPath path = docToPath(doc);
                        if (path != null) paths.add(path);
                    }
                    callback.onSuccess(paths);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user paths", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    // ===================== PATH LIKES =====================

    /** Toggle like on a path */
    public void toggleLikePath(String pathId, boolean like, OnSuccessCallback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DocumentReference likeRef = db.collection("paths").document(pathId)
                .collection("likes").document(user.getUid());
        DocumentReference pathRef = db.collection("paths").document(pathId);

        if (like) {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", user.getUid());
            likeData.put("likedAt", FieldValue.serverTimestamp());
            db.runBatch(batch -> {
                batch.set(likeRef, likeData);
                batch.update(pathRef, "likeCount", FieldValue.increment(1));
            }).addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
              .addOnFailureListener(e -> Log.e(TAG, "Error toggling path like", e));
        } else {
            db.runBatch(batch -> {
                batch.delete(likeRef);
                batch.update(pathRef, "likeCount", FieldValue.increment(-1));
            }).addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
              .addOnFailureListener(e -> Log.e(TAG, "Error toggling path like", e));
        }
    }

    /** Check liked status for a list of paths */
    public void checkLikedPaths(List<TravelPath> paths, Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || paths.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final int[] remaining = {paths.size()};
        for (TravelPath path : paths) {
            if (path.getId() == null) {
                if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                continue;
            }
            db.collection("paths").document(path.getId())
                    .collection("likes").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) path.setLikedSilent(true);
                        if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        if (--remaining[0] == 0 && onComplete != null) onComplete.run();
                    });
        }
    }

    // ===================== PATH COMMENTS =====================

    /** Add a comment to a path */
    public void addPathComment(String pathId, Comment comment, OnSuccessCallback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("authorId", comment.getAuthorId());
        data.put("authorName", comment.getAuthor());
        data.put("text", comment.getText());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("paths").document(pathId)
                .collection("comments").add(data)
                .addOnSuccessListener(ref -> {
                    db.collection("paths").document(pathId)
                            .update("commentCount", FieldValue.increment(1));
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding path comment", e));
    }

    /** Load all comments for a path */
    public void loadPathComments(String pathId, OnSuccessCallback<List<Comment>> callback) {
        db.collection("paths").document(pathId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);
                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String authorName = doc.getString("authorName");
                        String text = doc.getString("text");
                        Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? sdf.format(ts.toDate()) : "";
                        Comment c = new Comment(id, authorName != null ? authorName : "", text != null ? text : "", date);
                        c.setAuthorId(doc.getString("authorId"));
                        comments.add(c);
                    }
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading path comments", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    // ===================== FAVORITES (LIKED ITEMS) =====================

    /** Load all photo IDs liked by the current user via collection group query */
    public void loadLikedPhotoIds(OnSuccessCallback<List<String>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(new ArrayList<>()); return; }

        db.collectionGroup("likes")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> photoIds = new ArrayList<>();
                    List<String> pathIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        DocumentReference parent = doc.getReference().getParent().getParent();
                        if (parent != null) {
                            String collectionName = parent.getParent().getId();
                            if ("photos".equals(collectionName)) {
                                photoIds.add(parent.getId());
                            }
                        }
                    }
                    callback.onSuccess(photoIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading liked photo IDs", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    /** Load all path IDs liked by the current user via collection group query */
    public void loadLikedPathIds(OnSuccessCallback<List<String>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onSuccess(new ArrayList<>()); return; }

        db.collectionGroup("likes")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> pathIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        DocumentReference parent = doc.getReference().getParent().getParent();
                        if (parent != null) {
                            String collectionName = parent.getParent().getId();
                            if ("paths".equals(collectionName)) {
                                pathIds.add(parent.getId());
                            }
                        }
                    }
                    callback.onSuccess(pathIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading liked path IDs", e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    /** Load photos by a list of IDs */
    public void loadPhotosByIds(List<String> ids, OnSuccessCallback<List<Photo>> callback) {
        if (ids.isEmpty()) { callback.onSuccess(new ArrayList<>()); return; }

        List<Photo> photos = new ArrayList<>();
        final int[] remaining = {ids.size()};

        for (String id : ids) {
            db.collection("photos").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Photo photo = docToPhoto(doc);
                            if (photo != null) {
                                photo.setLikedSilent(true);
                                photos.add(photo);
                            }
                        }
                        if (--remaining[0] == 0) callback.onSuccess(photos);
                    })
                    .addOnFailureListener(e -> {
                        if (--remaining[0] == 0) callback.onSuccess(photos);
                    });
        }
    }

    /** Load paths by a list of IDs */
    public void loadPathsByIds(List<String> ids, OnSuccessCallback<List<TravelPath>> callback) {
        if (ids.isEmpty()) { callback.onSuccess(new ArrayList<>()); return; }

        List<TravelPath> paths = new ArrayList<>();
        final int[] remaining = {ids.size()};

        for (String id : ids) {
            db.collection("paths").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            TravelPath path = docToPath(doc);
                            if (path != null) {
                                path.setLikedSilent(true);
                                paths.add(path);
                            }
                        }
                        if (--remaining[0] == 0) callback.onSuccess(paths);
                    })
                    .addOnFailureListener(e -> {
                        if (--remaining[0] == 0) callback.onSuccess(paths);
                    });
        }
    }

    // ===================== HELPERS =====================

    private Photo docToPhoto(DocumentSnapshot doc) {
        Photo photo = doc.toObject(Photo.class);
        if (photo != null) {
            photo.setId(doc.getId());
            if (photo.getImageBase64() != null && !photo.getImageBase64().isEmpty()) {
                photo.setImageBitmap(ImageUtils.base64ToBitmap(photo.getImageBase64()));
            }
        }
        return photo;
    }

    private TravelPath docToPath(DocumentSnapshot doc) {
        TravelPath path = doc.toObject(TravelPath.class);
        if (path != null) {
            path.setId(doc.getId());
        }
        return path;
    }

    // ===================== CALLBACK INTERFACES =====================

    public interface OnSuccessCallback<T> {
        void onSuccess(T result);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }
}
