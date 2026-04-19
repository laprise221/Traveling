package com.example.traveling.data;

import com.example.traveling.model.TravelPath;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class PathRepository {

    private static final String COLLECTION = "paths";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static PathRepository instance;

    private PathRepository() {}

    public static PathRepository get() {
        if (instance == null) instance = new PathRepository();
        return instance;
    }

    public Task<DocumentReference> savePath(TravelPath path) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            path.setAuthorId(user.getUid());
            if (path.getAuthor() == null || path.getAuthor().equals("Moi")) {
                String name = user.getDisplayName();
                path.setAuthor(name != null ? name : user.getEmail());
            }
        }
        return db.collection(COLLECTION).add(path);
    }

    public Task<QuerySnapshot> getPublicPaths() {
        return db.collection(COLLECTION)
                .whereEqualTo("public", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getUserPaths(String userId) {
        return db.collection(COLLECTION)
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> deletePath(String documentId) {
        return db.collection(COLLECTION).document(documentId).delete();
    }
}
