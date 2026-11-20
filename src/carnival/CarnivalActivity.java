package carnival;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CarnivalActivity {

    private final String name;
    private final int capacity;
    private final long durationMillis;

    private final BlockingQueue<CarnivalAttendee> waitingLine =
            new LinkedBlockingQueue<>();

    private final Semaphore availableSpots;
    private final AtomicInteger completed = new AtomicInteger(0);

    public CarnivalActivity(String name, int capacity, long durationMillis) {
        if (capacity <= 0 || durationMillis < 0) {
            throw new IllegalArgumentException("Invalid capacity or duration");
        }

        this.name = name;
        this.capacity = capacity;
        this.durationMillis = durationMillis;
        this.availableSpots = new Semaphore(capacity);
    }

    // Producer: attendee joins the waiting line.
    public void joinLine(CarnivalAttendee attendee) {
        if (attendee == null || !attendee.isAdmitted()) {
            return;
        }

        waitingLine.offer(attendee);

        System.out.printf(
                "Attendee %d joined %s line (%d waiting)%n",
                attendee.getId(), name, waitingLine.size()
        );
    }

    // Consumer: activity worker takes the next attendee.
    // Returns the attendee after the activity is completed.
    public CarnivalAttendee startActivity()
            throws InterruptedException {

        availableSpots.acquire();

        try {
            CarnivalAttendee attendee = waitingLine.poll(
                    100, TimeUnit.MILLISECONDS
            );

            if (attendee == null) {
                return null;
            }

            System.out.printf(
                    "Attendee %d started %s%n",
                    attendee.getId(), name
            );

            Thread.sleep(durationMillis);

            completed.incrementAndGet();

            System.out.printf(
                    "Attendee %d finished %s%n",
                    attendee.getId(), name
            );

            return attendee;

        } finally {
            availableSpots.release();
        }
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getWaitingCount() {
        return waitingLine.size();
    }

    public int getCompletedCount() {
        return completed.get();
    }
}
