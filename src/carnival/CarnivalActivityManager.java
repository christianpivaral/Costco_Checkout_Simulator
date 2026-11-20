package carnival;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CarnivalActivityManager {

    private final List<CarnivalActivity> activities = new ArrayList<>();
    private static final int MAX_ACTIVITIES_PER_ATTENDEE = 3;

    private final java.util.concurrent.ConcurrentHashMap<Long, Integer>
            activityCounts = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.concurrent.atomic.AtomicInteger attendeesExited =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private ExecutorService workers;
    private volatile boolean running = false;

    public CarnivalActivityManager() {

        // Game booths: players participate independently.
        activities.add(new Booth(
                "Ring Toss", 3, 2000, "Ring Toss Game"
        ));

        activities.add(new Booth(
                "Balloon Darts", 2, 3000, "Dart Game"
        ));

        // Rides: passengers board and finish in groups.
        activities.add(new Ride(
                "Ferris Wheel", 8, 5000
        ));

        activities.add(new Ride(
                "Roller Coaster", 12, 4000
        ));
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;

        int totalWorkers = 0;

        for (CarnivalActivity activity : activities) {
            if (activity instanceof Ride) {
                totalWorkers++;
            } else {
                totalWorkers += activity.getCapacity();
            }
        }

        workers = Executors.newFixedThreadPool(totalWorkers);

        for (CarnivalActivity activity : activities) {

            if (activity instanceof Ride) {
                Ride ride = (Ride) activity;

                // One worker processes one ride group at a time.
                workers.submit(() -> {
                    while (running &&
                            !Thread.currentThread().isInterrupted()) {

                        try {
                            List<CarnivalAttendee> finished =
                                    ride.runRide();

                            for (CarnivalAttendee attendee : finished) {
                                sendToNextActivity(attendee, ride);
                                }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });

            } else {

                // Each booth has one worker per available spot.
                for (int i = 0; i < activity.getCapacity(); i++) {

                    workers.submit(() -> {
                        while (running &&
                                !Thread.currentThread().isInterrupted()) {

                            try {
                                CarnivalAttendee finished =
                                        activity.startActivity();

                                if (finished != null) {
                                    sendToNextActivity(finished, activity);
                                }

                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    });
                }
            }
        }

        System.out.println("Carnival booths and rides are OPEN!");
    }

    // Send an admitted attendee to a random activity.
    public void sendToActivity(CarnivalAttendee attendee) {

        if (!running || attendee == null || !attendee.isAdmitted()) {
            return;
        }

        int choice = ThreadLocalRandom.current()
                .nextInt(activities.size());

        activities.get(choice).joinLine(attendee);
    }

    private void sendToNextActivity(
            CarnivalAttendee attendee,
            CarnivalActivity previousActivity) {

        if (attendee == null || !attendee.isAdmitted()) {
            return;
        }

        int completed = activityCounts.merge(
                attendee.getId(), 1, Integer::sum
        );

        System.out.printf(
                "Attendee %d has completed %d/%d activities%n",
                attendee.getId(),
                completed,
                MAX_ACTIVITIES_PER_ATTENDEE
        );

        if (completed >= MAX_ACTIVITIES_PER_ATTENDEE) {
            attendeesExited.incrementAndGet();

            System.out.printf(
                    "Attendee %d completed all activities and EXITED the carnival!%n",
                    attendee.getId()
            );

            return;
        }

        if (!running) {
            return;
        }

        List<CarnivalActivity> choices =
                new ArrayList<>(activities);

        choices.remove(previousActivity);

        if (choices.isEmpty()) {
            return;
        }

        int choice = ThreadLocalRandom.current()
                .nextInt(choices.size());

        CarnivalActivity nextActivity = choices.get(choice);

        System.out.printf(
                "Attendee %d is moving from %s to %s%n",
                attendee.getId(),
                previousActivity.getName(),
                nextActivity.getName()
        );

        nextActivity.joinLine(attendee);
    }

    public boolean haveAllAttendeesExited(int admittedCount) {
        return attendeesExited.get() >= admittedCount;
    }

    // Stop workers and display results.
    public synchronized void stop() throws InterruptedException {

        running = false;

        if (workers != null) {
            workers.shutdown();

            if (!workers.awaitTermination(10, TimeUnit.SECONDS)) {
                workers.shutdownNow();
                workers.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        printSummary();
    }

    public void printSummary() {
        System.out.println("\n=== Carnival Activities Summary ===");

        for (CarnivalActivity activity : activities) {
            System.out.printf(
                    "%s: %d completed, %d waiting%n",
                    activity.getName(),
                    activity.getCompletedCount(),
                    activity.getWaitingCount()
            );

            if (activity instanceof Ride) {
                Ride ride = (Ride) activity;

                System.out.printf(
                        "  Ride cycles completed: %d%n",
                        ride.getCompletedRides()
                );
            }
        }

        System.out.println("\n=== Attendee Exit Summary ===");

        System.out.println(
                "Attendees who completed 3 activities: "
                        + attendeesExited.get()
        );

        System.out.println(
                "Total completed activity visits: "
                        + activityCounts.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum()
        );
    }
}
