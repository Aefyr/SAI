package com.aefyr.sai.utils;

public class MathUtils {

    public static int clamp(int a, int min, int max) {
        if (a < min)
            return min;

        if (a > max)
            return max;

        return a;
    }

    public static int closest(int x, int a, int b) {
        int distanceToA = Math.abs(x - a);
        int distanceToB = Math.abs(x - b);

        if (distanceToA > distanceToB)
            return b;

        return a;
    }

}
