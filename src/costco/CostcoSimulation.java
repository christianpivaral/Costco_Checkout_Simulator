package costco;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class: prompts for the parameters, starts Customer (producer) and
 * Cashier (consumer) threads that share one CheckoutQueue, then reports
 * wait times and cost.
 *
 * Time is simulated: 1 minute = Clock.MS_PER_MINUTE real milliseconds.
 */
public class CostcoSimulation {
    private static final SecureRandom generator = new SecureRandom();

    public static final double MIN_SERVICE = 1.0; // minutes per customer at the register
    public static final double MAX_SERVICE = 4.0; // average 2.5 minutes

    public static void main(String[] args) throws InterruptedException {
        Scanner in = new Scanner(System.in);
        System.out.println("=== Costco Checkout Simulator ===");
        int customers = askInt(in, "Number of customers", 20);
        int cashiers = askInt(in, "Number of cashiers", 3);
        int capacity = askInt(in, "Checkout line capacity", 10);
        int perHour = askInt(in, "Customer arrival rate (customers/hour)", 60);
        double wage = askDouble(in, "Cashier wage ($/hour)", 18.0);
        System.out.println();

        SimulationResult r = run(customers, cashiers, capacity, perHour, wage);
        printReport(r);
    }

    /** Runs one simulation and returns its statistics. */
    public static SimulationResult run(int customers, int cashiers, int capacity,
                                       int perHour, double wage) throws InterruptedException {
        CheckoutQueue queue = new CheckoutQueue(capacity);
        List<Double> waitTimes = Collections.synchronizedList(new ArrayList<>());

        // Customers finish shopping at random times spread over the arrival window,
        // e.g. 60 customers at 60/hour arrive over 60 minutes (about 1 per minute).
        double windowMinutes = customers * 60.0 / perHour;

        ExecutorService cashierPool = Executors.newFixedThreadPool(cashiers);
        List<Cashier> cashierList = new ArrayList<>();
        for (int i = 1; i <= cashiers; i++) {
            Cashier c = new Cashier(i, queue, MIN_SERVICE, MAX_SERVICE, waitTimes);
            cashierList.add(c);
            cashierPool.execute(c);
        }

        long start = System.nanoTime();
        ExecutorService customerPool = Executors.newCachedThreadPool();
        for (int i = 1; i <= customers; i++) {
            double shopping = generator.nextDouble() * windowMinutes;
            customerPool.execute(new Customer(i, queue, shopping));
        }

        // wait until every customer is in line, then close the store
        customerPool.shutdown();
        customerPool.awaitTermination(10, TimeUnit.MINUTES);
        queue.close(cashiers);

        // wait for cashiers to finish everyone still in line
        cashierPool.shutdown();
        cashierPool.awaitTermination(10, TimeUnit.MINUTES);
        double totalMinutes = Clock.minutesSince(start);

        double avg, max;
        synchronized (waitTimes) {
            avg = waitTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            max = waitTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }
        double busy = cashierList.stream().mapToDouble(Cashier::getBusyMinutes).sum();
        double utilization = busy / (cashiers * totalMinutes);

        return new SimulationResult(customers, cashiers, capacity, avg, max, totalMinutes,
                utilization, queue.blockedCustomers(), queue.longestLine(), wage);
    }

    public static void printReport(SimulationResult r) {
        System.out.println();
        System.out.println("=========== RESULTS ===========");
        System.out.printf("Customers / cashiers / line cap : %d / %d / %d%n",
                r.customers(), r.cashiers(), r.queueCapacity());
        System.out.printf("Average wait                    : %.1f min%n", r.avgWait());
        System.out.printf("Longest wait                    : %.1f min%n", r.maxWait());
        System.out.printf("Total time to serve everyone    : %.1f min%n", r.totalMinutes());
        System.out.printf("Cashier utilization             : %.0f%%%n", r.utilization() * 100);
        System.out.printf("Longest line                    : %d%n", r.longestLine());
        System.out.printf("Customers who found line full   : %d%n", r.blockedCustomers());
        System.out.printf("Labor cost                      : $%.2f ($%.2f per customer)%n",
                r.laborCost(), r.costPerCustomer());
    }

    private static int askInt(Scanner in, String prompt, int def) {
        while (true) {
            System.out.printf("%s [%d]: ", prompt, def);
            String line = in.hasNextLine() ? in.nextLine().trim() : "";
            if (line.isEmpty()) return def;
            try {
                int v = Integer.parseInt(line);
                if (v > 0) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Please enter a positive whole number.");
        }
    }

    private static double askDouble(Scanner in, String prompt, double def) {
        while (true) {
            System.out.printf("%s [%.2f]: ", prompt, def);
            String line = in.hasNextLine() ? in.nextLine().trim() : "";
            if (line.isEmpty()) return def;
            try {
                double v = Double.parseDouble(line);
                if (v > 0) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Please enter a positive number.");
        }
    }
}
