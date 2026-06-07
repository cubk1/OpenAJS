package us.cubk.openajs.api.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

    private RandomUtil() {
    }

    public static String hex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(ThreadLocalRandom.current().nextInt(16)));
        }
        return sb.toString();
    }

    public static int nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }
}
