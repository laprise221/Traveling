package com.example.traveling.session;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Gère l'état de session de l'utilisateur.
 * S'appuie sur Firebase Auth : si aucun utilisateur n'est connecté,
 * on considère l'utilisateur comme anonyme.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    public boolean isAnonymous() {
        return FirebaseAuth.getInstance().getCurrentUser() == null;
    }
}
