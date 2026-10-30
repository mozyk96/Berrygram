package org.telegram.messenger.fork.auth;

/** Whitelisted error categories for privacy-preserving diagnostics. */
public final class AuthDiagnostics {
    private AuthDiagnostics() {}
    public static String errorCategory(String error) {
        if (error == null) return "UNKNOWN";
        if (error.matches("FLOOD_WAIT_[0-9]+")) return "FLOOD_WAIT";
        if (error.matches("(PHONE|NETWORK|USER)_MIGRATE_[0-9]+")) return "DC_MIGRATE";
        switch (error) {
            case "API_ID_INVALID": case "API_ID_PUBLISHED_FLOOD":
            case "PHONE_NUMBER_INVALID": case "PHONE_NUMBER_APP_SIGNUP_FORBIDDEN":
            case "PHONE_NUMBER_BANNED": case "PHONE_NUMBER_FLOOD":
            case "PHONE_CODE_INVALID": case "PHONE_CODE_EXPIRED":
            case "SESSION_PASSWORD_NEEDED": case "UPDATE_APP_TO_LOGIN":
            case "AUTH_RESTART": return error;
            default: return "OTHER";
        }
    }
}
