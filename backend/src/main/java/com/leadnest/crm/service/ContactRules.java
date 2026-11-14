package com.leadnest.crm.service;

import com.leadnest.crm.exception.ApiException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ContactRules {

    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_CHARS = Pattern.compile("^[0-9+()\\-.\\s]{7,30}$");

    private ContactRules() {
    }

    public record Parsed(String email, String phone) {
    }

    public static Parsed parse(String rawEmail, String rawPhone) {
        String email = blankToNull(rawEmail);
        String phone = blankToNull(rawPhone);
        Map<String, String> errors = new LinkedHashMap<>();
        if (email == null && phone == null) {
            errors.put("email", "Provide an email address or a phone number");
            errors.put("phone", "Provide an email address or a phone number");
        }
        if (email != null) {
            email = email.toLowerCase();
            if (!EMAIL.matcher(email).matches()) {
                errors.put("email", "Enter a valid email address");
            }
        }
        if (phone != null) {
            if (!PHONE_CHARS.matcher(phone).matches() || digitCount(phone) < 7 || digitCount(phone) > 15) {
                errors.put("phone", "Enter a valid phone number");
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return new Parsed(email, phone);
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int digitCount(String phone) {
        int count = 0;
        for (int i = 0; i < phone.length(); i++) {
            if (Character.isDigit(phone.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}
