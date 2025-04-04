package com.example.xiangqi.util;

public class DeepClone {
    public static String[][] clone(String[][] original) {
        String[][] copy = new String[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone(); // Clone each inner array
        }
        return copy;
    }
}
