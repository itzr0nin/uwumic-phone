package com.uwumic;

/**
 * Removes short, sharp, broadband transients (mouse clicks, keyboard clacks, mouth clicks) while
 * leaving speech alone.
 *
 * Voice has little energy above ~4 kHz, clicks do. The signal is split into a low band (voice) and a
 * high band (>4 kHz). A click is declared when the fast envelope of the high band jumps well above its
 * recent baseline (sustained highs such as an "s" raise the baseline within milliseconds and stop being
 * treated as clicks). On a click the high band is cut hard and the low band only a little for ~12 ms,
 * so speech under the click survives. A 2 ms delay line lets the gain drop before the click arrives.
 */
final class Declicker {
    private final int delay;
    private final float[] ringLow, ringHigh;
    private int pos;
    private float lp1, lp2;
    private float fast, base;
    private float gainH = 1f, gainL = 1f, lowEnv;
    private int hold;

    private final float lpCoef, fastRelease, baseGrow, baseRelease, attackStep, releaseStep, lowAttackStep, envGrow, envRelease;
    private final int holdLen;
    private static final float DUCK_HIGH = 0.04f; // about -28 dB on the high band
    private static final float DUCK_LOW = 0.40f;  // about -8 dB on the low band
    private static final float FLOOR = 0.010f;    // ignore anything quieter than this in the high band

    Declicker(int rate) {
        delay = rate / 500; // 2 ms look-ahead
        ringLow = new float[delay];
        ringHigh = new float[delay];
        lpCoef = (float) (1.0 - Math.exp(-2.0 * Math.PI * 4000.0 / rate));
        fastRelease = (float) Math.exp(-1.0 / (0.002 * rate));
        baseGrow = (float) (1.0 / (0.020 * rate));
        baseRelease = (float) Math.exp(-1.0 / (0.060 * rate));
        attackStep = (float) (1.0 / (0.0005 * rate));
        releaseStep = (float) (1.0 / (0.015 * rate));
        lowAttackStep = (float) (1.0 / (0.003 * rate));
        envGrow = (float) (1.0 / (0.020 * rate));
        envRelease = (float) Math.exp(-1.0 / (0.050 * rate));
        holdLen = (int) (0.012 * rate);
    }

    /** Processes n samples in place. strength 0 = off (audio only delayed), 100 = most aggressive. */
    void process(short[] s, int n, int strength) {
        final boolean on = strength > 0;
        final float ratio = 7f - 4.5f * strength / 100f; // 7x ... 2.5x above baseline
        for (int i = 0; i < n; i++) {
            float x = s[i] / 32768f;
            lp1 += lpCoef * (x - lp1);
            float h1 = x - lp1;
            lp2 += lpCoef * (h1 - lp2);
            float high = h1 - lp2;      // band above ~4 kHz
            float low = x - high;       // everything else (complementary, sums back to x)
            float a = Math.abs(high);

            fast = a > fast ? a : fast * fastRelease;
            if (on && fast > ratio * base + FLOOR) hold = holdLen;
            if (fast > base) base += (fast - base) * baseGrow; else base *= baseRelease;

            // how loud is the voice band? if it is quiet there is nothing to protect: duck everything
            float la = Math.abs(low);
            if (la > lowEnv) lowEnv += (la - lowEnv) * envGrow; else lowEnv *= envRelease;
            float quiet = Math.max(0f, Math.min(1f, 1f - lowEnv / 0.04f));
            float lowDuck = DUCK_LOW + (DUCK_HIGH - DUCK_LOW) * quiet;

            boolean ducking = hold > 0;
            if (ducking) hold--;
            float tH = ducking ? DUCK_HIGH : 1f, tL = ducking ? lowDuck : 1f;
            if (gainH > tH) gainH = Math.max(tH, gainH - attackStep); else gainH = Math.min(tH, gainH + releaseStep);
            if (gainL > tL) gainL = Math.max(tL, gainL - lowAttackStep); else gainL = Math.min(tL, gainL + releaseStep);

            float outLow = ringLow[pos], outHigh = ringHigh[pos];
            ringLow[pos] = low;
            ringHigh[pos] = high;
            pos = (pos + 1) % delay;
            int v = Math.round((outLow * gainL + outHigh * gainH) * 32768f);
            s[i] = (short) Math.max(-32768, Math.min(32767, v));
        }
    }
}
