package com.example.traveling.data;

import com.example.traveling.model.Photo;

/** Passerelle simple pour transmettre une Photo entre fragments sans Parcelable. */
public class PhotoRegistry {
    private static Photo current;
    public static void set(Photo p) { current = p; }
    public static Photo get() { return current; }
}
