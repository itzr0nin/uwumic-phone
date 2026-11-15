package com.uwumic;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

/**
 * Loads the optional fonts from assets/fonts (display.ttf, mono.ttf, monobold.ttf).
 * Missing files simply fall back to system fonts, so the app always works.
 */
final class Fonts {
    private Fonts() { }

    static Typeface display(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Typeface t = new Typeface.Builder(c.getAssets(), "fonts/display.ttf")
                        .setFontVariationSettings("'wght' 800").build();
                if (t != null) return t;
            }
            return Typeface.createFromAsset(c.getAssets(), "fonts/display.ttf");
        } catch (Exception e) {
            return Typeface.create("sans-serif-black", Typeface.NORMAL);
        }
    }

    static Typeface mono(Context c) {
        try {
            return Typeface.createFromAsset(c.getAssets(), "fonts/mono.ttf");
        } catch (Exception e) {
            return Typeface.MONOSPACE;
        }
    }

    static Typeface monoBold(Context c) {
        try {
            return Typeface.createFromAsset(c.getAssets(), "fonts/monobold.ttf");
        } catch (Exception e) {
            return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }
    }
}
