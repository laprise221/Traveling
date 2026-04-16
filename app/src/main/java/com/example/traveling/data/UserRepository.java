package com.example.traveling.data;

import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton en mémoire (remplacé par Firestore quand Firebase sera configuré).
 * Centralise : favoris (photos likées, parcours likés) + publications de l'utilisateur.
 */
public class UserRepository {

    private static UserRepository instance;

    private final List<Photo> likedPhotos = new ArrayList<>();
    private final List<TravelPath> likedPaths = new ArrayList<>();
    private final List<Photo> myPhotos = new ArrayList<>();
    private final List<TravelPath> myPaths = new ArrayList<>();

    private UserRepository() {}

    public static UserRepository get() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    // ---- Favoris photos ----

    public void likePhoto(Photo photo) {
        if (!likedPhotos.contains(photo)) likedPhotos.add(photo);
    }

    public void unlikePhoto(Photo photo) {
        likedPhotos.remove(photo);
    }

    public List<Photo> getLikedPhotos() {
        return new ArrayList<>(likedPhotos);
    }

    // ---- Favoris parcours ----

    public void likePath(TravelPath path) {
        if (!likedPaths.contains(path)) likedPaths.add(path);
    }

    public void unlikePath(TravelPath path) {
        likedPaths.remove(path);
    }

    public List<TravelPath> getLikedPaths() {
        return new ArrayList<>(likedPaths);
    }

    // ---- Publications ----

    public void addPhoto(Photo photo) {
        myPhotos.add(0, photo); // plus récent en premier
    }

    public void addPath(TravelPath path) {
        myPaths.add(0, path);
    }

    public void removePhoto(Photo photo) { myPhotos.remove(photo); }
    public void removePath(TravelPath path) { myPaths.remove(path); }

    public List<Photo> getMyPhotos() {
        return new ArrayList<>(myPhotos);
    }

    public List<TravelPath> getMyPaths() {
        return new ArrayList<>(myPaths);
    }

    public int getTotalPublications() {
        return myPhotos.size() + myPaths.size();
    }

    public void clear() {
        likedPhotos.clear();
        likedPaths.clear();
        myPhotos.clear();
        myPaths.clear();
    }
}
