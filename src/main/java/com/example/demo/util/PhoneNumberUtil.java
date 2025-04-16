package com.example.demo.util;

public class PhoneNumberUtil {
    public static String convertPhoneToE164(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        if (cleaned.startsWith("+84")) {
            return cleaned;
        } else if (cleaned.startsWith("84")) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("0")) {
            return "+84" + cleaned.substring(1);
        }

        throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
    }
}
