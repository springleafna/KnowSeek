package com.springleaf.knowseek.utils;

/**
 * 计算余弦距离工具类
 */
public class MathUtils {

    public static double cosineDistance(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vector dimensions mismatch");
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
