package costco;

/** Numbers collected from one run of the simulation. */
public record SimulationResult(
        int customers,
        int cashiers,
        int queueCapacity,
        double avgWait,        // minutes from "done shopping" to "cashier starts"
        double maxWait,
        double totalMinutes,   // store open time until the last customer is served
        double utilization,    // fraction of cashier time spent serving (0..1)
        int blockedCustomers,  // customers who found the line full
        int longestLine,
        double hourlyWage) {

    /** Labor cost: every cashier is paid for the whole time the store is running. */
    public double laborCost() {
        return cashiers * hourlyWage * (totalMinutes / 60.0);
    }

    public double costPerCustomer() {
        return laborCost() / customers;
    }
}
