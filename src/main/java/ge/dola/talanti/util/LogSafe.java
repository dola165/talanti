package ge.dola.talanti.util;

public final class LogSafe {
    private LogSafe() {}

    private static String stripControl(String s) {
        if (s == null) return "null";
        // Remove ASCII/Unicode control chars, CR/LF, and Unicode line/paragraph separators
        // \p{Cntrl} covers 0x00–0x1F and 0x7F; we also drop U+2028/U+2029 which some encoders treat as newlines
        return s.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", "");
    }

    /** Sanitize arbitrary user-controlled strings for logging */
    public static String safe(Object o) {
        if (o == null) return "null";
        String s = stripControl(String.valueOf(o)).trim();
        if (s.length() > 500) s = s.substring(0, 500) + "...";
        return s;
    }

    /** Keep domain, partially mask local part; also strips control chars */
    public static String safeEmail(String email) {
        if (email == null) return "null";
        String clean = stripControl(email).trim();
        int at = clean.indexOf('@');
        if (at <= 1) return "***@" + (at == -1 ? "unknown" : clean.substring(at + 1));
        String user = clean.substring(0, at);
        String domain = clean.substring(at + 1);
        String head = user.length() <= 2 ? user : user.substring(0, 2);
        return head + "***@" + domain;
    }

    /** If you ever need to log tokens for DEBUG in dev only, mask them */
    public static String maskToken(String token) {
        if (token == null) return "null";
        String clean = stripControl(token);
        int len = clean.length();
        return (len <= 8) ? "***" : clean.substring(0, 4) + "..." + clean.substring(len - 4);
    }
}