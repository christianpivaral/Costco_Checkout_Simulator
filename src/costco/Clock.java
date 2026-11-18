package costco;

/**
 * Speeds up time so a simulated hour runs in a few seconds.
 * 1 simulated minute = MS_PER_MINUTE real milliseconds.
 */
public final class Clock {
    public static final long MS_PER_MINUTE = 50; // 60 sim minutes = 3 real seconds

    private Clock() {}

    public static void sleepMinutes(double minutes) throws InterruptedException {
        Thread.sleep(Math.round(minutes * MS_PER_MINUTE));
    }

    public static double minutesSince(long startNanos) {
        double ms = (System.nanoTime() - startNanos) / 1_000_000.0;
        return ms / MS_PER_MINUTE;
    }
}
