package com.example.traveling.session;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class SessionManager {

    private static final String TAG = "SessionManager";

    private static final SessionManager INSTANCE = new SessionManager();

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    /** True si l'utilisateur n'a pas de vrai compte (pas connecté ou session Firebase anonyme). */
    public boolean isAnonymous() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null || user.isAnonymous();
    }

    /** Alias de isAnonymous() pour la lisibilité. */
    public boolean isGuest() {
        return isAnonymous();
    }

    /** Garantit qu'une session Firebase existe. Se connecte anonymement si besoin, puis appelle onReady. */
    public void ensureFirebaseSession(Runnable onReady) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            onReady.run();
            return;
        }
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "signInAnonymously SUCCESS uid=" + result.getUser().getUid());
                    onReady.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "signInAnonymously FAILED: " + e.getMessage(), e);
                    onReady.run();
                });
    }
}
