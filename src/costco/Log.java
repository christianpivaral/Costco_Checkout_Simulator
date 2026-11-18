package costco;

/** Event printing that can be switched off when running many experiments. */
public final class Log {
    private static volatile boolean enabled = true;

    private Log() {}

    public static void setEnabled(boolean on) { enabled = on; }

    public static void print(String format, Object... args) {
        if (enabled) {
            System.out.printf(format + "%n", args);
        }
    }
}
