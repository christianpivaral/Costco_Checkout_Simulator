package costco;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The shared checkout line (the "buffer" from ex2's BlockingBuffer).
 *
 * Holds customer IDs in an ArrayBlockingQueue. A bounded queue models the
 * physical space in front of the registers: when it is full, a customer who
 * finishes shopping has to wait (put() blocks) until a spot opens up; when it
 * is empty, a cashier waits (take() blocks) until someone arrives.
 *
 * Because ArrayBlockingQueue is thread-safe, no synchronized blocks are needed
 * here. The bookkeeping maps/counters use concurrent classes for the same reason.
 */
public class CheckoutQueue {
    /** Sent to each cashier at closing time so it knows to stop (a "poison pill"). */
    public static final int CLOSED = -1;

    private final ArrayBlockingQueue<Integer> line;
    private final int capacity;

    // when each customer *finished shopping* and tried to get in line (nanoTime)
    private final Map<Integer, Long> readyTimes = new ConcurrentHashMap<>();
    // how many customers got stuck because the line was full
    private final AtomicInteger blockedCustomers = new AtomicInteger();
    private final AtomicInteger longestLine = new AtomicInteger();

    public CheckoutQueue(int capacity) {
        this.capacity = capacity;
        this.line = new ArrayBlockingQueue<>(capacity);
    }

    /** Called by a Customer thread. Blocks while the line is full. */
    public void join(int customerId) throws InterruptedException {
        readyTimes.put(customerId, System.nanoTime());
        if (line.remainingCapacity() == 0) {
            blockedCustomers.incrementAndGet();
            Log.print("Customer %3d finds the line FULL (%d/%d) and waits for space",
                    customerId, line.size(), capacity);
        }
        Log.print("Customer %3d gets in line (%d ahead, cap %d)",
                customerId, line.size(), capacity);
        line.put(customerId); // blocks if full
        longestLine.accumulateAndGet(line.size(), Math::max);
    }

    /** Called by a Cashier thread. Blocks while the line is empty. */
    public int next() throws InterruptedException {
        return line.take(); // blocks if empty
    }

    /** Closing time: one CLOSED marker per cashier so every cashier stops. */
    public void close(int cashiers) throws InterruptedException {
        for (int i = 0; i < cashiers; i++) {
            line.put(CLOSED);
        }
    }

    /** Time (nanoTime) the customer finished shopping. */
    public long readyTime(int customerId) {
        return readyTimes.get(customerId);
    }

    public int size() { return line.size(); }
    public int capacity() { return capacity; }
    public int blockedCustomers() { return blockedCustomers.get(); }
    public int longestLine() { return longestLine.get(); }
}
