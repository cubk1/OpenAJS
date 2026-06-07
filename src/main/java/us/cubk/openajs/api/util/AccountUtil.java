package us.cubk.openajs.api.util;

public final class AccountUtil {

    private AccountUtil() {
    }

    public static boolean isMobile(String input) {
        return input != null && input.matches("^1\\d{10}$");
    }

    public static String mode(String input) {
        return isMobile(input) ? "mobile" : "email";
    }

    public static String accountName(String input) {
        return isMobile(input) ? "+86-" + input : input;
    }
}
