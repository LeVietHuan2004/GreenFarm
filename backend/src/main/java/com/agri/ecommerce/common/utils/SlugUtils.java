package com.agri.ecommerce.common.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace("đ", "d")
            .replace("Đ", "D")
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "item" : normalized;
    }
}
