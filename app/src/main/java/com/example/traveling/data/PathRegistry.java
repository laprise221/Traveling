package com.example.traveling.data;

import com.example.traveling.model.TravelPath;

public class PathRegistry {
    private static TravelPath current;
    public static void set(TravelPath p) { current = p; }
    public static TravelPath get() { return current; }
}
