package costco;

/**
 * Producer: a customer shops for a while, then joins the checkout line.
 * (Replaces ex2.Producer, which slept and then put a number in the buffer.)
 */
public class Customer implements Runnable {
    private final int id;
    private final CheckoutQueue queue;
    private final double shoppingMinutes; // simulated minutes until checkout

    public Customer(int id, CheckoutQueue queue, double shoppingMinutes) {
        this.id = id;
        this.queue = queue;
        this.shoppingMinutes = shoppingMinutes;
    }

    @Override
    public void run() {
        try {
            Clock.sleepMinutes(shoppingMinutes);   // shopping
            Log.print("Customer %3d done shopping after %4.1f min", id, shoppingMinutes);
            queue.join(id);                        // blocks if the line is full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
