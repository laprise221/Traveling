package com.example.traveling.data;

public class FilterRegistry {
    private static SearchFilters current = new SearchFilters();

    public static SearchFilters get() { return current; }
    public static void set(SearchFilters f) { current = f; }
    public static void reset() { current = new SearchFilters(); }
}
