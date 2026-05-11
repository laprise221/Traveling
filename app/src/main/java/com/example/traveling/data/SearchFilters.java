package com.example.traveling.data;

import java.util.ArrayList;
import java.util.List;

public class SearchFilters {
    public String query = "";
    public List<String> locationTypes = new ArrayList<>();
    public String authorName = "";
    public long dateFromMs = -1;  // -1 = non défini
    public long dateToMs   = -1;

    public boolean isEmpty() {
        return query.isEmpty() && locationTypes.isEmpty()
                && authorName.isEmpty() && dateFromMs < 0 && dateToMs < 0;
    }

    public static final SearchFilters EMPTY = new SearchFilters();
}
