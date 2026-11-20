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

    private final ConcurrentLinkedQueue<CarnivalAttendee> gate1Queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CarnivalAttendee> gate2Queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger ticketsSold = new AtomicInteger(0);
    private final AtomicInteger cottonCandyCount = new AtomicInteger(0);
    private final AtomicBoolean gatesOpen = new AtomicBoolean(true);
    private final List<CarnivalAttendee> attendees = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        CarnivalSimulation sim = new CarnivalSimulation();
        sim.run(120, 16);
    }

    public double elapsedSeconds(long nanoTime) {
        return Math.max(0.0, (nanoTime - simStartNanos) / 1_000_000_000.0);
    }

    public int estimateLineSize(int gateNumber) {
        return (gateNumber == 2) ? gate2Queue.size() : gate1Queue.size();
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

    public boolean tryBuyTicket(int gateNumber, CarnivalAttendee attendee, double serviceSeconds) {
        while (true) {
            int current = ticketsSold.get();
            if (current >= MAX_TICKETS) {
                attendee.onSoldOut(gateNumber, serviceSeconds);
                return false;
            }
            int nextTicketNumber = current + 1;
            if (ticketsSold.compareAndSet(current, nextTicketNumber)) {
                boolean getsCottonCandy = nextTicketNumber <= FREE_COTTON_CANDY_LIMIT;
                if (getsCottonCandy) {
                    cottonCandyCount.incrementAndGet();
                }
                attendee.onTicketPurchased(gateNumber, nextTicketNumber, getsCottonCandy, serviceSeconds);
                return true;
            }
        }
    }

    public void run(int totalAttendees, int attendeePoolSize) throws InterruptedException {
        ExecutorService gatePool = Executors.newFixedThreadPool(2);
        ExecutorService attendeePool = Executors.newFixedThreadPool(attendeePoolSize);

        gatePool.submit(() -> processGate(1, gate1Queue));
        gatePool.submit(() -> processGate(2, gate2Queue));

        for (long i = 1; i <= totalAttendees; i++) {
            int gate = ThreadLocalRandom.current().nextInt(1, 3);
            CarnivalAttendee attendee = new CarnivalAttendee(i, gate, this);
            attendees.add(attendee);
            attendeePool.submit(attendee);
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));
        }

        attendeePool.shutdown();
        if (!attendeePool.awaitTermination(30, TimeUnit.SECONDS)) {
            attendeePool.shutdownNow();
        }

        gatesOpen.set(false);
        gatePool.shutdown();
        if (!gatePool.awaitTermination(30, TimeUnit.SECONDS)) {
            gatePool.shutdownNow();
        }

        printSummary();
    }

    private void processGate(int gateNumber, ConcurrentLinkedQueue<CarnivalAttendee> queue) {
        while (!Thread.currentThread().isInterrupted()) {
            CarnivalAttendee next = queue.poll();
            if (next != null) {
                int serviceMillis = ThreadLocalRandom.current().nextInt(10, 45);
                try {
                    Thread.sleep(serviceMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                tryBuyTicket(gateNumber, next, serviceMillis / 1000.0);
            } else if (!gatesOpen.get() && queue.isEmpty()) {
                break;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void printSummary() {
        long admitted = attendees.stream().filter(CarnivalAttendee::isAdmitted).count();
        long cottonCandy = attendees.stream().filter(CarnivalAttendee::hasReceivedCottonCandy).count();

        synchronized (printLock) {
            System.out.println("\n=== Carnival Simulation Summary ===");
            System.out.println("Total attendees arrived: " + attendees.size());
            System.out.println("Tickets sold: " + ticketsSold.get() + " / " + MAX_TICKETS);
            System.out.println("Admitted attendees: " + admitted);
            System.out.println("Cotton candy awarded: " + cottonCandyCount.get() + " (flags: " + cottonCandy + ")");
        }
    }
}