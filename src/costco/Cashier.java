package costco;

import java.security.SecureRandom;
import java.util.List;

/**
 * Consumer: a cashier repeatedly takes the next customer from the line and
 * rings them up. (Replaces ex2.Consumer, which took numbers and summed them.)
 *
 * Several Cashier threads share one CheckoutQueue — like Costco's single
 * serpentine line feeding many registers.
 */
public class Cashier implements Runnable {
    private static final SecureRandom generator = new SecureRandom();

    private final int id;
    private final CheckoutQueue queue;
    private final double minService;  // simulated minutes
    private final double maxService;
    private final List<Double> waitTimes; // shared, thread-safe list (minutes)

    private int served = 0;
    private double busyMinutes = 0;

    public Cashier(int id, CheckoutQueue queue, double minService, double maxService,
                   List<Double> waitTimes) {
        this.id = id;
        this.queue = queue;
        this.minService = minService;
        this.maxService = maxService;
        this.waitTimes = waitTimes;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int customerId = queue.next();          // blocks if line is empty
                if (customerId == CheckoutQueue.CLOSED) {
                    break;                              // store closed, go home
                }
                double waited = Clock.minutesSince(queue.readyTime(customerId));
                waitTimes.add(waited);

                double service = minService + generator.nextDouble() * (maxService - minService);
                Log.print("    Cashier %2d serving customer %3d (waited %4.1f min, service %3.1f min)",
                        id, customerId, waited, service);
                Clock.sleepMinutes(service);            // ringing them up
                served++;
                busyMinutes += service;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Log.print("    Cashier %2d closes register after serving %d customers", id, served);
    }

    public int getId() { return id; }
    public int getServed() { return served; }
    public double getBusyMinutes() { return busyMinutes; }
}
