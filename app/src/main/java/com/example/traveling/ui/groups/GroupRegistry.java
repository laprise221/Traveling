package com.example.traveling.ui.groups;

import com.example.traveling.model.Group;

public class GroupRegistry {
    private static Group current;
    public static void set(Group g) { current = g; }
    public static Group get() { return current; }
}
