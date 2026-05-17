package com.eshop.app.location.shared.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map Indian State names to their ISO 3166-2:IN codes.
 */
public final class IndianStateMapper {

    private static final Map<String, String> STATE_CODE_MAP = new HashMap<>();

    static {
        STATE_CODE_MAP.put("andhra pradesh", "AP");
        STATE_CODE_MAP.put("arunachal pradesh", "AR");
        STATE_CODE_MAP.put("assam", "AS");
        STATE_CODE_MAP.put("bihar", "BR");
        STATE_CODE_MAP.put("chhattisgarh", "CT");
        STATE_CODE_MAP.put("goa", "GA");
        STATE_CODE_MAP.put("gujarat", "GJ");
        STATE_CODE_MAP.put("haryana", "HR");
        STATE_CODE_MAP.put("himachal pradesh", "HP");
        STATE_CODE_MAP.put("jharkhand", "JH");
        STATE_CODE_MAP.put("karnataka", "KA");
        STATE_CODE_MAP.put("kerala", "KL");
        STATE_CODE_MAP.put("madhya pradesh", "MP");
        STATE_CODE_MAP.put("maharashtra", "MH");
        STATE_CODE_MAP.put("manipur", "MN");
        STATE_CODE_MAP.put("meghalaya", "ML");
        STATE_CODE_MAP.put("mizoram", "MZ");
        STATE_CODE_MAP.put("nagaland", "NL");
        STATE_CODE_MAP.put("odisha", "OR");
        STATE_CODE_MAP.put("punjab", "PB");
        STATE_CODE_MAP.put("rajasthan", "RJ");
        STATE_CODE_MAP.put("sikkim", "SK");
        STATE_CODE_MAP.put("tamil nadu", "TN");
        STATE_CODE_MAP.put("telangana", "TG");
        STATE_CODE_MAP.put("tripura", "TR");
        STATE_CODE_MAP.put("uttar pradesh", "UP");
        STATE_CODE_MAP.put("uttarakhand", "UT");
        STATE_CODE_MAP.put("uttaranchal", "UL");
        STATE_CODE_MAP.put("west bengal", "WB");
        STATE_CODE_MAP.put("andaman and nicobar islands", "AN");
        STATE_CODE_MAP.put("chandigarh", "CH");
        STATE_CODE_MAP.put("dadra and nagar haveli and daman and diu", "DH");
        STATE_CODE_MAP.put("delhi", "DL");
        STATE_CODE_MAP.put("jammu and kashmir", "JK");
        STATE_CODE_MAP.put("ladakh", "LA");
        STATE_CODE_MAP.put("lakshadweep", "LD");
        STATE_CODE_MAP.put("puducherry", "PY");
    }

    private IndianStateMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets the state code for a given state name.
     * Returns "XX" if the state name is unknown.
     *
     * @param stateName The name of the state (e.g., "Karnataka")
     * @return The 2-character ISO code (e.g., "KA")
     */
    public static String getStateCode(String stateName) {
        if (stateName == null)
            return "XX";
        return STATE_CODE_MAP.getOrDefault(stateName.trim().toLowerCase(), "XX");
    }
}
