// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

final class ThemeManager {

    static final String MODE_LIGHT = "light";
    static final String MODE_SYSTEM = "system";
    static final String MODE_DARK = "dark";
    static final String MODE_NIGHT = "night";

    static final String COLOR_BLUE = "blue";
    static final String COLOR_PURPLE = "purple";
    static final String COLOR_GREEN = "green";
    static final String COLOR_TEAL = "teal";
    static final String COLOR_CORAL = "coral";
    static final String COLOR_SAGE = "sage";
    static final String COLOR_YELLOW = "yellow";
    static final String COLOR_ORANGE = "orange";
    static final String COLOR_PINK = "pink";
    static final String COLOR_LAVENDER = "lavender";

    private static final String PREFS = "home_hold_cts";
    private static final String PREF_MODE = "appearance_mode";
    private static final String PREF_COLOR = "theme_color";

    static final class Palette {
        final int primary;
        final int onPrimary;
        final int primaryContainer;
        final int onPrimaryContainer;
        final int surface;
        final int surfaceContainer;
        final int surfaceContainerHigh;
        final int onSurface;
        final int onSurfaceVariant;
        final int outline;
        final int success;
        final int warning;
        final int error;

        Palette(
                int primary,
                int onPrimary,
                int primaryContainer,
                int onPrimaryContainer,
                int surface,
                int surfaceContainer,
                int surfaceContainerHigh,
                int onSurface,
                int onSurfaceVariant,
                int outline,
                int success,
                int warning,
                int error) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.primaryContainer = primaryContainer;
            this.onPrimaryContainer = onPrimaryContainer;
            this.surface = surface;
            this.surfaceContainer = surfaceContainer;
            this.surfaceContainerHigh = surfaceContainerHigh;
            this.onSurface = onSurface;
            this.onSurfaceVariant = onSurfaceVariant;
            this.outline = outline;
            this.success = success;
            this.warning = warning;
            this.error = error;
        }
    }

    private ThemeManager() {}

    static Context wrap(Context base) {
        String mode = getMode(base);
        if (MODE_SYSTEM.equals(mode)) {
            return base;
        }

        Configuration config =
                new Configuration(base.getResources().getConfiguration());

        int nightMask = MODE_DARK.equals(mode) || MODE_NIGHT.equals(mode)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;

        config.uiMode =
                (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                        | nightMask;
        return base.createConfigurationContext(config);
    }

    static String getMode(Context context) {
        String saved = prefs(context).getString(PREF_MODE, MODE_LIGHT);
        // Migrate the old "Default follows system" value to the new white-first default.
        if ("default".equals(saved)) {
            return MODE_LIGHT;
        }
        if (MODE_SYSTEM.equals(saved)
                || MODE_DARK.equals(saved)
                || MODE_NIGHT.equals(saved)) {
            return saved;
        }
        return MODE_LIGHT;
    }

    static void setMode(Context context, String mode) {
        if (!MODE_LIGHT.equals(mode)
                && !MODE_SYSTEM.equals(mode)
                && !MODE_DARK.equals(mode)
                && !MODE_NIGHT.equals(mode)) {
            mode = MODE_LIGHT;
        }
        prefs(context).edit().putString(PREF_MODE, mode).apply();
    }

    static String getColor(Context context) {
        String value = prefs(context).getString(PREF_COLOR, COLOR_BLUE);
        if (COLOR_PURPLE.equals(value)
                || COLOR_GREEN.equals(value)
                || COLOR_TEAL.equals(value)
                || COLOR_CORAL.equals(value)
                || COLOR_SAGE.equals(value)
                || COLOR_YELLOW.equals(value)
                || COLOR_ORANGE.equals(value)
                || COLOR_PINK.equals(value)
                || COLOR_LAVENDER.equals(value)) {
            return value;
        }
        return COLOR_BLUE;
    }

    static void setColor(Context context, String color) {
        if (!COLOR_PURPLE.equals(color)
                && !COLOR_GREEN.equals(color)
                && !COLOR_TEAL.equals(color)
                && !COLOR_CORAL.equals(color)
                && !COLOR_SAGE.equals(color)
                && !COLOR_YELLOW.equals(color)
                && !COLOR_ORANGE.equals(color)
                && !COLOR_PINK.equals(color)
                && !COLOR_LAVENDER.equals(color)) {
            color = COLOR_BLUE;
        }
        prefs(context).edit().putString(PREF_COLOR, color).apply();
    }

    static boolean isDark(Context context) {
        String mode = getMode(context);
        if (MODE_LIGHT.equals(mode)) {
            return false;
        }
        if (MODE_DARK.equals(mode) || MODE_NIGHT.equals(mode)) {
            return true;
        }
        int uiMode =
                context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    static boolean isNight(Context context) {
        return MODE_NIGHT.equals(getMode(context));
    }

    static Palette palette(Context context) {
        boolean dark = isDark(context);
        boolean night = isNight(context);
        String accent = getColor(context);

        int primary;
        int onPrimary;
        int primaryContainer;
        int onPrimaryContainer;

        if (COLOR_PURPLE.equals(accent)) {
            if (dark) {
                primary = c("#D0BCFF");
                onPrimary = c("#381E72");
                primaryContainer = c("#4F378B");
                onPrimaryContainer = c("#EADDFF");
            } else {
                primary = c("#6750A4");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#EADDFF");
                onPrimaryContainer = c("#21005D");
            }
        } else if (COLOR_GREEN.equals(accent)) {
            if (dark) {
                primary = c("#7DD991");
                onPrimary = c("#003914");
                primaryContainer = c("#0B5228");
                onPrimaryContainer = c("#99F6AB");
            } else {
                primary = c("#146C2E");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#A6F4B4");
                onPrimaryContainer = c("#002108");
            }
        } else if (COLOR_TEAL.equals(accent)) {
            if (dark) {
                primary = c("#80D5D1");
                onPrimary = c("#003735");
                primaryContainer = c("#00504D");
                onPrimaryContainer = c("#9CF2ED");
            } else {
                primary = c("#006A67");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#9CF2ED");
                onPrimaryContainer = c("#00201F");
            }
        } else if (COLOR_SAGE.equals(accent)) {
            if (dark) {
                primary = c("#B9CC9D");
                onPrimary = c("#253313");
                primaryContainer = c("#3B4B28");
                onPrimaryContainer = c("#D5E8B8");
            } else {
                primary = c("#53643E");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#D7E8BC");
                onPrimaryContainer = c("#111F04");
            }
        } else if (COLOR_YELLOW.equals(accent)) {
            if (dark) {
                primary = c("#E9C349");
                onPrimary = c("#3D2F00");
                primaryContainer = c("#584500");
                onPrimaryContainer = c("#FFE16B");
            } else {
                primary = c("#725C00");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#FFE170");
                onPrimaryContainer = c("#231B00");
            }
        } else if (COLOR_ORANGE.equals(accent)) {
            if (dark) {
                primary = c("#FFB77B");
                onPrimary = c("#4C2700");
                primaryContainer = c("#6D3900");
                onPrimaryContainer = c("#FFDCBE");
            } else {
                primary = c("#8C4F00");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#FFDCBE");
                onPrimaryContainer = c("#2D1600");
            }
        } else if (COLOR_PINK.equals(accent)) {
            if (dark) {
                primary = c("#FFB0C8");
                onPrimary = c("#5F1130");
                primaryContainer = c("#7D2946");
                onPrimaryContainer = c("#FFD9E2");
            } else {
                primary = c("#9B405E");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#FFD9E2");
                onPrimaryContainer = c("#3E001D");
            }
        } else if (COLOR_LAVENDER.equals(accent)) {
            if (dark) {
                primary = c("#CDBDFF");
                onPrimary = c("#36255E");
                primaryContainer = c("#4D3C75");
                onPrimaryContainer = c("#E9DDFF");
            } else {
                primary = c("#66558E");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#E9DDFF");
                onPrimaryContainer = c("#211047");
            }
        } else if (COLOR_CORAL.equals(accent)) {
            if (dark) {
                primary = c("#FFB4A6");
                onPrimary = c("#561E14");
                primaryContainer = c("#733428");
                onPrimaryContainer = c("#FFDAD2");
            } else {
                primary = c("#8C4A3C");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#FFDAD2");
                onPrimaryContainer = c("#3B0904");
            }
        } else {
            // Google-blue inspired default.
            if (dark) {
                primary = c("#A8C7FA");
                onPrimary = c("#062E6F");
                primaryContainer = c("#0842A0");
                onPrimaryContainer = c("#D7E3FF");
            } else {
                primary = c("#0B57D0");
                onPrimary = c("#FFFFFF");
                primaryContainer = c("#D7E3FF");
                onPrimaryContainer = c("#041E49");
            }
        }

        int surface;
        int surfaceContainer;
        int surfaceContainerHigh;
        int onSurface;
        int onSurfaceVariant;
        int outline;
        int success;
        int warning;
        int error;

        if (night) {
            surface = c("#050609");
            surfaceContainer = c("#0B0D12");
            surfaceContainerHigh = c("#12151B");
            onSurface = c("#F1F3F4");
            onSurfaceVariant = c("#BDC1C6");
            outline = c("#636A73");
            success = c("#7ADFA7");
            warning = c("#FFC46B");
            error = c("#FFB4AB");
        } else if (dark) {
            surface = c("#111318");
            surfaceContainer = c("#1B1D22");
            surfaceContainerHigh = c("#24262C");
            onSurface = c("#E3E2E9");
            onSurfaceVariant = c("#C7C5D0");
            outline = c("#91909A");
            success = c("#6EDBAA");
            warning = c("#FFB95F");
            error = c("#FFB4AB");
        } else {
            surface = c("#FFFFFF");
            surfaceContainer = c("#F8F9FA");
            surfaceContainerHigh = c("#F1F3F4");
            onSurface = c("#1F1F1F");
            onSurfaceVariant = c("#444746");
            outline = c("#747775");
            success = c("#167D55");
            warning = c("#8A5100");
            error = c("#BA1A1A");
        }

        return new Palette(
                primary,
                onPrimary,
                primaryContainer,
                onPrimaryContainer,
                surface,
                surfaceContainer,
                surfaceContainerHigh,
                onSurface,
                onSurfaceVariant,
                outline,
                success,
                warning,
                error);
    }

    static int previewPrimary(Context context, String color) {
        String old = getColor(context);
        if (old.equals(color)) {
            return palette(context).primary;
        }

        boolean dark = isDark(context);
        if (COLOR_PURPLE.equals(color)) {
            return c(dark ? "#D0BCFF" : "#6750A4");
        }
        if (COLOR_GREEN.equals(color)) {
            return c(dark ? "#7DD991" : "#146C2E");
        }
        if (COLOR_TEAL.equals(color)) {
            return c(dark ? "#80D5D1" : "#006A67");
        }
        if (COLOR_CORAL.equals(color)) {
            return c(dark ? "#FFB4A6" : "#8C4A3C");
        }
        if (COLOR_SAGE.equals(color)) {
            return c(dark ? "#B9CC9D" : "#53643E");
        }
        if (COLOR_YELLOW.equals(color)) {
            return c(dark ? "#E9C349" : "#725C00");
        }
        if (COLOR_ORANGE.equals(color)) {
            return c(dark ? "#FFB77B" : "#8C4F00");
        }
        if (COLOR_PINK.equals(color)) {
            return c(dark ? "#FFB0C8" : "#9B405E");
        }
        if (COLOR_LAVENDER.equals(color)) {
            return c(dark ? "#CDBDFF" : "#66558E");
        }
        return c(dark ? "#A8C7FA" : "#0B57D0");
    }

    static int previewOnPrimary(Context context, String color) {
        if (!isDark(context)) {
            return Color.WHITE;
        }
        if (COLOR_PURPLE.equals(color)) return c("#381E72");
        if (COLOR_GREEN.equals(color)) return c("#003914");
        if (COLOR_TEAL.equals(color)) return c("#003735");
        if (COLOR_CORAL.equals(color)) return c("#561E14");
        if (COLOR_SAGE.equals(color)) return c("#253313");
        if (COLOR_YELLOW.equals(color)) return c("#3D2F00");
        if (COLOR_ORANGE.equals(color)) return c("#4C2700");
        if (COLOR_PINK.equals(color)) return c("#5F1130");
        if (COLOR_LAVENDER.equals(color)) return c("#36255E");
        return c("#062E6F");
    }

    static int previewContainer(Context context, String color) {
        boolean dark = isDark(context);
        if (COLOR_PURPLE.equals(color)) return c(dark ? "#4F378B" : "#EADDFF");
        if (COLOR_GREEN.equals(color)) return c(dark ? "#0B5228" : "#A6F4B4");
        if (COLOR_TEAL.equals(color)) return c(dark ? "#00504D" : "#9CF2ED");
        if (COLOR_CORAL.equals(color)) return c(dark ? "#733428" : "#FFDAD2");
        if (COLOR_SAGE.equals(color)) return c(dark ? "#3B4B28" : "#D7E8BC");
        if (COLOR_YELLOW.equals(color)) return c(dark ? "#584500" : "#FFE170");
        if (COLOR_ORANGE.equals(color)) return c(dark ? "#6D3900" : "#FFDCBE");
        if (COLOR_PINK.equals(color)) return c(dark ? "#7D2946" : "#FFD9E2");
        if (COLOR_LAVENDER.equals(color)) return c(dark ? "#4D3C75" : "#E9DDFF");
        return c(dark ? "#0842A0" : "#D7E3FF");
    }


    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int c(String value) {
        return Color.parseColor(value);
    }
}
