# 🎡 Carnival Simulation — Extra Credit Project

**Course:** CSC 311 — Java Programming  
**Project:** Carnival Simulation (Producer–Consumer and Multithreading)  
**Language:** Java  
**Repository:** [Costco Checkout Simulator](https://github.com/christianpivaral/Costco_Checkout_Simulator)

---

## 1. Project Overview

The Carnival Simulation is a multithreaded Java application that models ticket admission and carnival activities using concurrent programming concepts.

Attendees arrive at a carnival, select one of two entrance gates, purchase tickets, and participate in carnival games and rides.

The simulation demonstrates thread management, concurrent queues, synchronization, atomic operations, and producer-consumer behavior.

The project supports two execution modes:

- **Full Carnival Simulation:** 120 attendees participate in ticket admission, booths, and rides.
- **Ticket-Limit Test:** 5,005 attendees attempt to purchase tickets to verify the 5,000-ticket maximum.

## 2. Carnival Features

### Ticket Admission

- Two independently operating entrance gates.
- Each gate maintains a concurrent attendee queue.
- A maximum of 5,000 tickets can be sold.
- Ticket numbers are assigned through thread-safe atomic operations.
- Attendees who arrive after tickets sell out are denied admission.
- The first 50 successful ticket buyers receive free cotton candy.

### Carnival Game Booths

Two game booths are available:

| Booth | Concurrent Players | Game Duration |
|---|---:|---:|
| Ring Toss | 3 | 2 seconds |
| Balloon Darts | 2 | 3 seconds |

Each booth maintains a waiting queue and allows multiple attendees to participate concurrently, subject to its capacity.

### Carnival Rides

Two carnival rides are available:

| Ride | Maximum Passengers | Ride Duration |
|---|---:|---:|
| Ferris Wheel | 8 | 5 seconds |
| Roller Coaster | 12 | 4 seconds |

Rides board attendees in batches, run for a fixed duration, and process subsequent batches from their waiting queues.

A ride may depart with fewer passengers than its maximum capacity.

### Attendee Activity Tracking

- Admitted attendees are assigned to a carnival activity.
- After completing an activity, an attendee moves to another available activity.
- Each attendee completes three activities before exiting.
- Completed visits and attendee exits are tracked.
- The simulation waits for admitted attendees to finish, with a safety timeout to prevent indefinite execution.

## 3. Java Classes

| Java File | Responsibility |
|---|---|
| `CarnivalAttendee.java` | Represents attendees, their gate preferences, ticket purchases, admission status, and related processing |
| `CarnivalSimulation.java` | Coordinates ticket gates, attendee generation, ticket sales, activity integration, and final statistics |
| `CarnivalActivity.java` | Provides shared activity behavior, waiting queues, capacity controls, and completion tracking |
| `Booth.java` | Represents carnival game booths with configurable player capacity and game duration |
| `Ride.java` | Implements batch-based ride processing and tracks completed ride cycles and passengers |
| `CarnivalActivityManager.java` | Manages activity workers, attendee transitions, completion tracking, and shutdown |
| `CarnivalTicketLimitTest.java` | Runs a separate admission-only test with 5,005 attendees |

## 4. Multithreading and Synchronization

The project uses several Java concurrency mechanisms.

**ExecutorService:** Manages worker threads for entrance gates, attendees, booths, and rides.

**ConcurrentLinkedQueue:** Supports thread-safe ticket-gate queues.

**LinkedBlockingQueue:** Maintains waiting queues for carnival activities.

**AtomicInteger:** Safely tracks ticket sales, cotton candy distribution, and activity statistics.

**AtomicBoolean:** Tracks whether ticket gates remain open.

**Semaphore:** Controls the number of attendees participating in a booth activity simultaneously.

**CountDownLatch:** Coordinates attendee ticket-processing completion.

**Synchronization:** Protects shared console output and selected activity-management operations.

These mechanisms help coordinate multiple attendees and workers without exceeding the ticket limit or activity capacities.

## 5. How to Run the Project

### Requirements

- Java JDK
- IntelliJ IDEA or another Java-compatible IDE

### Option A: Full Carnival Simulation

1. Open the project in IntelliJ IDEA.
2. Navigate to `src/carnival/CarnivalSimulation.java`.
3. Run the `main()` method.

The default configuration is:

```text
sim.run(120, 16);
```

This launches the full carnival simulation with 120 attendees and 16 attendee worker threads.

The program processes ticket admission, carnival activities, attendee exits, and final statistics.

### Option B: 5,000-Ticket Limit Test

1. Open `src/carnival/CarnivalTicketLimitTest.java`.
2. Run its `main()` method.

The test uses:

```text
simulation.run(5005, 100, false);
```

The arguments represent:

- `5005`: Total attendees
- `100`: Attendee worker threads
- `false`: Disable carnival activities for admission-only testing

This mode tests ticket sales and admission capacity without running booths and rides.

## 6. Testing and Results

### Test 1: Full Carnival Simulation

The complete carnival was tested with 120 attendees.

| Metric | Observed Result |
|---|---:|
| Total attendees | 120 |
| Tickets sold | 120 |
| Admitted attendees | 120 |
| Free cotton candy awarded | 50 |
| Attendees completing three activities | 120 |
| Total completed activity visits | 360 |
| Attendees waiting at shutdown | 0 |
| Attendees denied tickets | 0 |
| Process exit code | 0 |

All 120 attendees successfully completed their required activities and exited.

The simulation completed with no attendees remaining in the activity queues.

### Test 2: Maximum Ticket Capacity

The admission-only test was executed with 5,005 attendees.

| Metric | Observed Result |
|---|---:|
| Total attendees | 5,005 |
| Maximum tickets | 5,000 |
| Tickets sold | 5,000 |
| Admitted attendees | 5,000 |
| Attendees denied tickets | 5 |
| Free cotton candy awarded | 50 |
| Process exit code | 0 |

The test confirmed that the ticket limit was enforced and no more than 5,000 tickets were sold.

### Test Conclusion

Both execution modes completed successfully during testing.

The results demonstrate correct ticket-limit enforcement, cotton candy distribution, activity completion tracking, and orderly shutdown in the tested configurations.

## 7. Team Contributions

### Logan — Original Carnival Implementation

- Developed the original `CarnivalAttendee.java` and `CarnivalSimulation.java` implementation.
- Implemented attendee arrival and entrance-gate processing.
- Developed the ticket-purchasing logic.
- Implemented the 5,000-ticket limit and first-50 cotton candy distribution.
- Established the original concurrent admission system.

### Sahithi Attada — Carnival Activities and Integration

**New Java classes added:**

- `CarnivalActivity.java`
- `Booth.java`
- `Ride.java`
- `CarnivalActivityManager.java`
- `CarnivalTicketLimitTest.java`

**Existing Java classes modified:**

- `CarnivalAttendee.java`
- `CarnivalSimulation.java`

**Implementation contributions:**

- Added concurrent carnival game booths.
- Implemented batch-based carnival rides.
- Developed activity queue and worker management.
- Integrated carnival activities with the existing ticket-admission system.
- Added attendee activity completion and exit tracking.
- Improved the activity shutdown process.
- Added a separate admission-only test mode.
- Tested both the 120-attendee simulation and 5,005-attendee ticket-limit scenario.

### Christian Pivaral — Repository Owner and Video Demonstration

- Created and maintains the shared GitHub repository.
- Recorded the video walkthrough of the carnival's threading design: ExecutorService thread pools, the AtomicInteger `compareAndSet` ticket limit, Semaphore-controlled booths, batch-boarding rides, and both test runs.
- Video: [CSC 311 Extra Credit – Multithreaded Carnival Simulation](https://youtu.be/WNuLlisIJHU)

## 8. Future Improvements

Potential enhancements include:

- More carnival booths and rides.
- Configurable ride boarding delays.
- Additional attendee activity choices.
- Performance comparisons across different attendee counts.
- More detailed activity and waiting-time statistics.

---

## 9. Project Summary

This extra-credit project extends an existing concurrent ticket-admission simulation into a functioning carnival environment with booths, rides, attendee activity tracking, and separate ticket-capacity testing.

The implementation demonstrates practical Java multithreading and coordination of shared resources.

**Status:** Implementation, both simulation tests, and video demonstration completed.