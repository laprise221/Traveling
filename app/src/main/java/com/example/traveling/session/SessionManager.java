package com.example.traveling.session;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    /** True si aucune session Firebase n'existe (même pas anonyme). */
    public boolean isAnonymous() {
        return FirebaseAuth.getInstance().getCurrentUser() == null;
    }

    /** True si l'utilisateur n'a pas de vrai compte (pas connecté ou session Firebase anonyme). */
    public boolean isGuest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null || user.isAnonymous();
    }

    /** Garantit qu'une session Firebase existe. Se connecte anonymement si besoin, puis appelle onReady. */
    public void ensureFirebaseSession(Runnable onReady) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            onReady.run();
            return;
        }
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(result -> onReady.run())
                .addOnFailureListener(e -> onReady.run());
    }
}
