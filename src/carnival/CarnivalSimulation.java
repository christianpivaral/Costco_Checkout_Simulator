package carnival;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CarnivalSimulation {

    public static final int MAX_TICKETS = 5000;
    public static final int FREE_COTTON_CANDY_LIMIT = 50;

    private final Object printLock = new Object();
    private final long simStartNanos = System.nanoTime();

    private final ConcurrentLinkedQueue<CarnivalAttendee> gate1Queue =
            new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<CarnivalAttendee> gate2Queue =
            new ConcurrentLinkedQueue<>();

    private final AtomicInteger ticketsSold = new AtomicInteger(0);
    private final AtomicInteger cottonCandyCount = new AtomicInteger(0);
    private final AtomicBoolean gatesOpen = new AtomicBoolean(true);

    private final List<CarnivalAttendee> attendees = new ArrayList<>();

    private CarnivalActivityManager activityManager;

    public static void main(String[] args) throws InterruptedException {
        CarnivalSimulation sim = new CarnivalSimulation();

        // Default: complete carnival simulation with 120 attendees.
        sim.run(120, 16);
    }

    public double elapsedSeconds(long nanoTime) {
        return (nanoTime - simStartNanos) / 1_000_000_000.0;
    }

    public int estimateLineSize(int gateNumber) {
        if (gateNumber == 2) {
            return gate2Queue.size();
        }
        return gate1Queue.size();
    }

    public void logEvent(String message) {
        synchronized (printLock) {
            System.out.println(message);
        }
    }

    public boolean joinGateQueue(CarnivalAttendee attendee) {
        if (!gatesOpen.get()) {
            return false;
        }

        if (attendee.getPreferredGate() == 2) {
            return gate2Queue.offer(attendee);
        }

        return gate1Queue.offer(attendee);
    }

    public boolean tryBuyTicket(
            int gateNumber,
            CarnivalAttendee attendee,
            double serviceSeconds) {

        while (true) {
            int current = ticketsSold.get();

            if (current >= MAX_TICKETS) {
                attendee.onSoldOut(gateNumber, serviceSeconds);
                return false;
            }

            int nextTicketNumber = current + 1;

            if (ticketsSold.compareAndSet(current, nextTicketNumber)) {

                boolean getsCottonCandy =
                        nextTicketNumber <= FREE_COTTON_CANDY_LIMIT;

                if (getsCottonCandy) {
                    cottonCandyCount.incrementAndGet();
                }

                attendee.onTicketPurchased(
                        gateNumber,
                        nextTicketNumber,
                        getsCottonCandy,
                        serviceSeconds
                );

                // Only send attendees to activities when enabled.
                if (activityManager != null) {
                    activityManager.sendToActivity(attendee);
                }

                return true;
            }
        }
    }

    // Default mode: run the complete carnival.
    public void run(int totalAttendees, int attendeePoolSize)
            throws InterruptedException {

        run(totalAttendees, attendeePoolSize, true);
    }

    // Supports both the full carnival and ticket-limit testing.
    public void run(
            int totalAttendees,
            int attendeePoolSize,
            boolean enableActivities)
            throws InterruptedException {

        ExecutorService gatePool = Executors.newFixedThreadPool(2);

        ExecutorService attendeePool =
                Executors.newFixedThreadPool(attendeePoolSize);

        // Start booths and rides only for the full simulation.
        if (enableActivities) {
            activityManager = new CarnivalActivityManager();
            activityManager.start();
        } else {
            activityManager = null;
            System.out.println(
                    "Ticket-limit test mode: activities disabled."
            );
        }

        // Start both ticket gates.
        gatePool.submit(() -> processGate(1, gate1Queue));
        gatePool.submit(() -> processGate(2, gate2Queue));

        // Generate attendees.
        for (long i = 1; i <= totalAttendees; i++) {

            int gate = ThreadLocalRandom.current().nextInt(1, 3);

            CarnivalAttendee attendee =
                    new CarnivalAttendee(i, gate, this);

            attendees.add(attendee);
            attendeePool.submit(attendee);

            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));
        }

        // Wait for attendee ticket processing.
        attendeePool.shutdown();

        if (!attendeePool.awaitTermination(180, TimeUnit.SECONDS)) {
            System.out.println(
                    "WARNING: Some attendees did not finish processing!"
            );
            attendeePool.shutdownNow();
        }

        // Close ticket gates after attendees have arrived.
        gatesOpen.set(false);

        gatePool.shutdown();

        if (!gatePool.awaitTermination(180, TimeUnit.SECONDS)) {
            System.out.println(
                    "WARNING: Gate processing timed out!"
            );
            gatePool.shutdownNow();
        }

        // Only wait for activities in the full carnival mode.
        if (enableActivities) {

            System.out.println(
                    "\nWaiting for attendees to finish their activities..."
            );

            int admittedCount = ticketsSold.get();

            // Safety timeout of 3 minutes.
            long deadline = System.nanoTime()
                    + TimeUnit.MINUTES.toNanos(3);

            while (!activityManager.haveAllAttendeesExited(admittedCount)
                    && System.nanoTime() < deadline) {

                Thread.sleep(500);
            }

            if (activityManager.haveAllAttendeesExited(admittedCount)) {
                System.out.println(
                        "\nAll admitted attendees completed their activities!"
                );
            } else {
                System.out.println(
                        "\nSafety timeout reached. Closing carnival."
                );
            }

            activityManager.stop();

        } else {
            System.out.println(
                    "\nTicket-limit test completed. No activities were run."
            );
        }

        printSummary();
    }

    private void processGate(
            int gateNumber,
            ConcurrentLinkedQueue<CarnivalAttendee> queue) {

        while (!Thread.currentThread().isInterrupted()) {

            CarnivalAttendee next = queue.poll();

            if (next != null) {

                int serviceMillis =
                        ThreadLocalRandom.current().nextInt(10, 45);

                try {
                    Thread.sleep(serviceMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                tryBuyTicket(
                        gateNumber,
                        next,
                        serviceMillis / 1000.0
                );

            } else if (!gatesOpen.get() && queue.isEmpty()) {
                break;

            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void printSummary() {

        long admitted = attendees.stream()
                .filter(CarnivalAttendee::isAdmitted)
                .count();

        long cottonCandy = attendees.stream()
                .filter(CarnivalAttendee::hasReceivedCottonCandy)
                .count();

        synchronized (printLock) {

            System.out.println(
                    "\n=== Carnival Simulation Summary ==="
            );

            System.out.println(
                    "Total attendees arrived: " + attendees.size()
            );

            System.out.println(
                    "Tickets sold: " + ticketsSold.get()
                            + " / " + MAX_TICKETS
            );

            System.out.println(
                    "Admitted attendees: " + admitted
            );

            System.out.println(
                    "Cotton candy awarded: "
                            + cottonCandyCount.get()
                            + " (flags: " + cottonCandy + ")"
            );

            System.out.println(
                    "Attendees denied tickets: "
                            + (attendees.size() - admitted)
            );
        }
    }
}
