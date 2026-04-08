package com.example.traveling.session;

/**
 * Gère l'état de session de l'utilisateur.
 * Pour l'instant, tout le monde est en mode anonyme — à brancher sur le vrai
 * système d'authentification plus tard.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private boolean anonymous = true;

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }
}
