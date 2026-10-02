// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

final class LanguageManager {

    static final String CODE_VI_VN = "vi-VN";
    static final String CODE_EN_US = "en-US";
    static final String CODE_ID_ID = "id-ID";
    static final String CODE_TH_TH = "th-TH";

    private static final String PREFS = "home_hold_cts";
    private static final String PREF_LANGUAGE = "ui_language";

    private static final String[] CODES = {
            CODE_VI_VN,
            CODE_EN_US,
            CODE_ID_ID,
            CODE_TH_TH
    };

    private static final String[] LABELS = {
            "🇻🇳 Tiếng Việt",
            "\uD83C\uDDFA\uD83C\uDDF8 English",
            "🇮🇩 Bahasa Indonesia",
            "🇹🇭 ไทย"
    };

    private LanguageManager() {}

    static Context wrap(Context base) {
        String tag = get(base);
        Locale locale = Locale.forLanguageTag(tag);
        Locale.setDefault(locale);

        Configuration config =
                new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLocales(new LocaleList(locale));
        return base.createConfigurationContext(config);
    }

    static String get(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                LocaleManager manager =
                        context.getSystemService(LocaleManager.class);
                if (manager != null) {
                    LocaleList locales = manager.getApplicationLocales();
                    if (locales != null && !locales.isEmpty()) {
                        return normalize(locales.get(0).toLanguageTag());
                    }
                }
            } catch (Throwable ignored) {}
        }

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = prefs.getString(PREF_LANGUAGE, null);
        if (saved != null) return normalize(saved);

        // Fresh installations use English; explicit app/system language choices above remain intact.
        return CODE_EN_US;
    }

    static void set(Context context, String code) {
        String normalized = normalize(code);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_LANGUAGE, normalized)
                .apply();

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                LocaleManager manager =
                        context.getSystemService(LocaleManager.class);
                if (manager != null) {
                    manager.setApplicationLocales(
                            LocaleList.forLanguageTags(normalized));
                }
            } catch (Throwable ignored) {}
        }

        Locale.setDefault(Locale.forLanguageTag(normalized));
    }

    static int count() {
        return CODES.length;
    }

    static int indexOfCurrent(Context context) {
        String current = get(context);
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(current)) return i;
        }
        return 1;
    }

    static String codeAt(int index) {
        if (index < 0 || index >= CODES.length) return CODE_EN_US;
        return CODES[index];
    }

    static String[] displayNames(Context context) {
        return LABELS.clone();
    }

    static String displayName(Context context, String code) {
        String normalized = normalize(code);
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(normalized)) return LABELS[i];
        }
        return LABELS[1];
    }

    private static String normalize(String code) {
        if (code == null) return CODE_EN_US;

        String lower = code.toLowerCase(Locale.ROOT);

        if (lower.startsWith("vi")) return CODE_VI_VN;
        if (lower.startsWith("id")) return CODE_ID_ID;
        if (lower.startsWith("th")) return CODE_TH_TH;
        if (lower.startsWith("en")) return CODE_EN_US;

        return CODE_EN_US;
    }
}
