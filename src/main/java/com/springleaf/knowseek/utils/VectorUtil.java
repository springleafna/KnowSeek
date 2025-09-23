package com.springleaf.knowseek.utils;

import java.util.Arrays;

public class VectorUtil {

    // float[] → String for pgvector
    public static String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        return Arrays.toString(vector)
                .replace(" ", ""); // 去掉空格，pgvector 更严格
    }

    // String → float[]
    public static float[] stringToVector(String str) {
        if (str == null || str.trim().isEmpty() || "[]".equals(str.trim())) {
            return new float[0];
        }
        String clean = str.replaceAll("^\\[\\s*|\\s*]$", "").trim();
        if (clean.isEmpty()) return new float[0];

        // 先转成 double[]，再手动转 float[]
        double[] doubles = Arrays.stream(clean.split(","))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();

        float[] result = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            result[i] = (float) doubles[i];
        }
        return result;
    }
}
