package carnival;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CarnivalAttendee implements Runnable {
    private final long id;
    private final int preferredGate;
    private final CarnivalSimulation simulation;

    private final CountDownLatch processedLatch = new CountDownLatch(1);
    private final AtomicBoolean admitted = new AtomicBoolean(false);
    private final AtomicBoolean receivedCottonCandy = new AtomicBoolean(false);
    private final AtomicInteger ticketNumber = new AtomicInteger(-1);
    private final AtomicReference<String> status = new AtomicReference<>("Created");

    private volatile long arrivedAtNanos;
    private volatile long queuedAtNanos;

    public CarnivalAttendee(long id, int preferredGate, CarnivalSimulation simulation) {
        this.id = id;
        this.preferredGate = (preferredGate == 2) ? 2 : 1;
        this.simulation = simulation;
    }

    @Override
    public void run() {
        arrivedAtNanos = System.nanoTime();
        simulation.logEvent(String.format(
                "Attendee %4d arrived at carnival (%.2fs)",
                id, simulation.elapsedSeconds(arrivedAtNanos)
        ));

        queuedAtNanos = System.nanoTime();
        int aheadInLine = simulation.estimateLineSize(preferredGate);
        boolean queued = simulation.joinGateQueue(this);

        if (!queued) {
            status.set("Could not join Gate " + preferredGate + " queue");
            simulation.logEvent(String.format(
                    "Attendee %4d could not join Gate %d line",
                    id, preferredGate
            ));
            processedLatch.countDown();
            return;
        }

        status.set("Waiting at Gate " + preferredGate);
        simulation.logEvent(String.format(
                "Attendee %4d gets in line at Gate %d (%d ahead)",
                id, preferredGate, aheadInLine
        ));

        try {
            if (!processedLatch.await(180, TimeUnit.SECONDS)) {
                status.set("Timed out waiting at Gate " + preferredGate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status.set("Interrupted at Gate " + preferredGate);
        }
    }

    public void onTicketPurchased(int gateNumber, int assignedTicketNumber, boolean getsCottonCandy, double serviceSeconds) {
        admitted.set(true);
        ticketNumber.set(assignedTicketNumber);
        receivedCottonCandy.set(getsCottonCandy);

        double waitedSeconds = Math.max(0.0, (System.nanoTime() - queuedAtNanos) / 1_000_000_000.0);
        String candyTag = getsCottonCandy ? " [FREE COTTON CANDY!]" : "";

        status.set("Admitted (Ticket #" + assignedTicketNumber + candyTag + ")");
        simulation.logEvent(String.format(
                "  Gate %d serving attendee %4d (waited %.2fs, service %.2fs) -> ENTERED with Ticket #%4d / %d%s",
                gateNumber, id, waitedSeconds, serviceSeconds,
                assignedTicketNumber, CarnivalSimulation.MAX_TICKETS, candyTag
        ));
        processedLatch.countDown();
    }

    public void onSoldOut(int gateNumber, double serviceSeconds) {
        admitted.set(false);
        ticketNumber.set(-1);
        receivedCottonCandy.set(false);
        status.set("Turned away - 5,000 ticket limit reached");

        double waitedSeconds = Math.max(0.0, (System.nanoTime() - queuedAtNanos) / 1_000_000_000.0);
        simulation.logEvent(String.format(
                "  Gate %d serving attendee %4d (waited %.2fs, service %.2fs) -> SOLD OUT (5,000 limit reached)",
                gateNumber, id, waitedSeconds, serviceSeconds
        ));
        processedLatch.countDown();
    }

    public long getId() {
        return id;
    }

    public int getPreferredGate() {
        return preferredGate;
    }

    public boolean isAdmitted() {
        return admitted.get();
    }

    public boolean hasReceivedCottonCandy() {
        return receivedCottonCandy.get();
    }

    public int getTicketNumber() {
        return ticketNumber.get();
    }

    public String getStatus() {
        return status.get();
    }
}