package com.pawsandhomes.petadoption.service;

import java.math.BigDecimal;
import java.util.Locale;

public final class AdoptionPricing {

    private static final BigDecimal CAT_FEE = BigDecimal.valueOf(2000);
    private static final BigDecimal SMALL_DOG_FEE = BigDecimal.valueOf(3000);
    private static final BigDecimal MEDIUM_DOG_FEE = BigDecimal.valueOf(5000);
    private static final BigDecimal LARGE_DOG_FEE = BigDecimal.valueOf(5000);

    private AdoptionPricing() {
    }

    public static BigDecimal contributionForBreed(String breed) {
        String normalizedBreed = breed == null ? "" : breed.toLowerCase(Locale.ROOT);
        if (matches(normalizedBreed,
                "cat", "siamese", "persian", "maine coon", "bengal", "ragdoll",
                "calico", "tabby", "sphynx", "russian blue", "british shorthair")) {
            return CAT_FEE;
        }
        if (matches(normalizedBreed,
                "chihuahua", "pomeranian", "dachshund", "yorkshire", "shih tzu",
                "maltese", "pug", "papillon", "miniature pinscher", "toy poodle")) {
            return SMALL_DOG_FEE;
        }
        if (matches(normalizedBreed,
                "golden retriever", "labrador", "husky", "german shepherd", "great dane",
                "rottweiler", "saint bernard", "mastiff", "doberman", "bernese mountain")) {
            return LARGE_DOG_FEE;
        }
        return MEDIUM_DOG_FEE;
    }

    private static boolean matches(String breed, String... indicators) {
        for (String indicator : indicators) {
            if (breed.contains(indicator)) {
                return true;
            }
        }
        return false;
    }
}
