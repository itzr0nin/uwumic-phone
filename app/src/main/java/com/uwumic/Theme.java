package com.uwumic;

/** The two looks of the app: "dark" (night rack) and "light" (cream paper with thick outlines). */
final class Theme {
    String name;
    boolean light;
    int bg, grid, card, edge, shadow, txt, txt2, dim, teal, tealText, coral, amber, amberBg, amberEdge,
            chipBg, chipEdge, tagBg, tagEdge, tagText, lcdBg, lcdEdge, lcdIdle, lcdLive, lcdScan, lcdTag,
            startFace, startHi, startText, stopFace, stopHi, stopText, iconBg, iconEdge,
            trackFace, trackEdge, cap, capEdge, outBg, outEdge, muteBg, muteEdge, muteTint,
            selBg, selEdge, switchBg, switchEdge, ledLive, liveText, brand;

    static final Theme DARK = dark();
    static final Theme LIGHT = light();

    static Theme byName(String n) { return "light".equals(n) ? LIGHT : DARK; }

    private static Theme dark() {
        Theme t = new Theme();
        t.name = "dark"; t.light = false;
        t.bg = 0xFF11151E; t.grid = 0; t.card = 0xFF181D29; t.edge = 0xFF2C3548; t.shadow = 0xFF080B10;
        t.txt = 0xFFE6EAF2; t.txt2 = 0xFF98A3B5; t.dim = 0xFF626C7E;
        t.teal = 0xFF00C9B7; t.tealText = 0xFF2DD4BF; t.coral = 0xFFFF2E63;
        t.amber = 0xFFFBBF24; t.amberBg = 0xFF2A2108; t.amberEdge = 0xFF6B4F0E;
        t.chipBg = 0xFF0C2A2C; t.chipEdge = 0xFF0F9F92;
        t.tagBg = 0xFF202737; t.tagEdge = 0xFF384259; t.tagText = 0xFFC4CCDA;
        t.lcdBg = 0xFF050E0C; t.lcdEdge = 0xFF174536; t.lcdIdle = 0xFF1FD8A4; t.lcdLive = 0xFF3DF58A;
        t.lcdScan = 0xFF030A09; t.lcdTag = 0xFF1FB88A;
        t.startFace = 0xFF00C9B7; t.startHi = 0xFF5EEAD4; t.startText = 0xFF08201F;
        t.stopFace = 0xFFFF2E63; t.stopHi = 0xFFFF8FB0; t.stopText = 0xFFFFFFFF;
        t.iconBg = 0xFFFFFFFF; t.iconEdge = 0xFF0A0D13;
        t.trackFace = 0xFFDDE2E9; t.trackEdge = 0xFF8893A5; t.cap = 0xFF00C9B7; t.capEdge = 0xFF08201F;
        t.outBg = 0xFF0F131B; t.outEdge = 0xFF3A445A;
        t.muteBg = 0xFF1C2232; t.muteEdge = 0xFF2F3A50; t.muteTint = 0xFF3A1824;
        t.selBg = 0xFF0F2C2E; t.selEdge = 0xFF0F9F92; t.switchBg = 0xFF0D1118; t.switchEdge = 0xFF2C3548;
        t.ledLive = 0xFF14D6A1; t.liveText = 0xFF19C8B0; t.brand = 0xFFFFFFFF;
        return t;
    }

    private static Theme light() {
        Theme t = new Theme();
        t.name = "light"; t.light = true;
        t.bg = 0xFFF2F1E8; t.grid = 0xFFE7E5D8; t.card = 0xFFFBFBF8; t.edge = 0xFF1C2635; t.shadow = 0xFF1C2635;
        t.txt = 0xFF111827; t.txt2 = 0xFF4B5563; t.dim = 0xFF6B7280;
        t.teal = 0xFF00B3A0; t.tealText = 0xFF007F73; t.coral = 0xFFFF2E63;
        t.amber = 0xFFB45309; t.amberBg = 0xFFFEF3C7; t.amberEdge = 0xFFB45309;
        t.chipBg = 0xFFCCFBF1; t.chipEdge = 0xFF0F766E;
        t.tagBg = 0xFFF1F1EC; t.tagEdge = 0xFF1C2635; t.tagText = 0xFF374151;
        t.lcdBg = 0xFFDFEADF; t.lcdEdge = 0xFF1C2635; t.lcdIdle = 0xFF17382E; t.lcdLive = 0xFF0B3B2E;
        t.lcdScan = 0; t.lcdTag = 0xFF2F5D4C;
        t.startFace = 0xFF00D2B4; t.startHi = 0xFF6FF0DC; t.startText = 0xFF07211E;
        t.stopFace = 0xFFFF2E63; t.stopHi = 0xFFFF86A5; t.stopText = 0xFFFFFFFF;
        t.iconBg = 0xFFFFFFFF; t.iconEdge = 0xFF1C2635;
        t.trackFace = 0xFFE2E6EC; t.trackEdge = 0xFF1C2635; t.cap = 0xFF00D2B4; t.capEdge = 0xFF1C2635;
        t.outBg = 0xFFFFFFFF; t.outEdge = 0xFF1C2635;
        t.muteBg = 0xFFFFFFFF; t.muteEdge = 0xFF1C2635; t.muteTint = 0xFFFFE4EA;
        t.selBg = 0xFFCCFBF1; t.selEdge = 0xFF0F766E; t.switchBg = 0xFFFFFFFF; t.switchEdge = 0xFF1C2635;
        t.ledLive = 0xFF00A892; t.liveText = 0xFF008476; t.brand = 0xFF111827;
        return t;
    }
}
