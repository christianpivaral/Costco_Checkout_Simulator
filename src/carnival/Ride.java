package carnival;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Ride extends CarnivalActivity {

    private final BlockingQueue<CarnivalAttendee> rideLine =
            new LinkedBlockingQueue<>();

    private final long rideDurationMillis;
    private final AtomicInteger completedRides = new AtomicInteger(0);
    private final AtomicInteger completedPassengers = new AtomicInteger(0);

    public Ride(String name, int capacity, long durationMillis) {
        super(name, capacity, durationMillis);
        this.rideDurationMillis = durationMillis;
    }

    @Override
    public void joinLine(CarnivalAttendee attendee) {
        if (attendee == null || !attendee.isAdmitted()) {
            return;
        }

        rideLine.offer(attendee);

        System.out.printf(
                "Attendee %d joined %s ride line (%d waiting)%n",
                attendee.getId(), getName(), rideLine.size()
        );
    }

    // Load a group of attendees and run one ride cycle.
    public List<CarnivalAttendee> runRide()
            throws InterruptedException {

        List<CarnivalAttendee> passengers = new ArrayList<>();

        CarnivalAttendee firstPassenger =
                rideLine.poll(100, TimeUnit.MILLISECONDS);

        if (firstPassenger == null) {
            return passengers;
        }

        passengers.add(firstPassenger);

        // Fill remaining seats without exceeding capacity.
        rideLine.drainTo(passengers, getCapacity() - 1);

        System.out.printf(
                "%s starting with %d passengers%n",
                getName(), passengers.size()
        );

        try {
            Thread.sleep(rideDurationMillis);
        } catch (InterruptedException e) {
            // Return passengers to the queue if the ride is interrupted.
            for (CarnivalAttendee attendee : passengers) {
                rideLine.offer(attendee);
            }
            throw e;
        }

        completedRides.incrementAndGet();
        completedPassengers.addAndGet(passengers.size());

        System.out.printf(
                "%s finished! %d passengers completed the ride%n",
                getName(), passengers.size()
        );

        return passengers;
    }

    @Override
    public int getWaitingCount() {
        return rideLine.size();
    }

    public int getCompletedRides() {
        return completedRides.get();
    }
    public int getCompletedCount(){
        return completedPassengers.get();
    }
}

