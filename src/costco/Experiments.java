package costco;

/**
 * Runs the five required experiments plus the staffing analysis
 * (60 customers/hour, $18/hour per cashier) without event printing.
 * Each scenario is run several times and averaged, because thread timing
 * and random shopping/service times make single runs noisy.
 */
public class Experiments {
    private static final int TRIALS = 5;
    private static final double WAGE = 18.0;

    public static void main(String[] args) throws InterruptedException {
        Log.setEnabled(false);

        System.out.println("PART 1 - Five experiments (20 customers at 60/hour, averaged over "
                + TRIALS + " runs)");
        header();
        row("1. Understaffed (1 cashier)", 20, 1, 10);
        row("2. Optimal (3 cashiers)", 20, 3, 10);
        row("3. Overstaffed (10 cashiers)", 20, 10, 10);
        row("4. Small line (3 cashiers, cap 2)", 20, 3, 2);
        row("5. Large line (3 cashiers, cap 50)", 20, 3, 50);

        System.out.println();
        System.out.println("PART 2 - Staffing for 60 customers/hour at $18/hour (line cap 20)");
        header();
        for (int c = 1; c <= 6; c++) {
            row(c + " cashier" + (c == 1 ? "" : "s"), 60, c, 20);
        }
    }

    private static void header() {
        System.out.printf("%-36s %8s %8s %8s %6s %6s %9s %8s%n",
                "Scenario", "AvgWait", "MaxWait", "Total", "Util", "Full", "Labor$", "$/cust");
    }

    private static void row(String label, int customers, int cashiers, int cap)
            throws InterruptedException {
        double avg = 0, max = 0, total = 0, util = 0, blocked = 0, cost = 0;
        for (int t = 0; t < TRIALS; t++) {
            SimulationResult r = CostcoSimulation.run(customers, cashiers, cap, 60, WAGE);
            avg += r.avgWait();
            max += r.maxWait();
            total += r.totalMinutes();
            util += r.utilization();
            blocked += r.blockedCustomers();
            cost += r.laborCost();
        }
        System.out.printf("%-36s %7.1fm %7.1fm %7.1fm %5.0f%% %6.1f %9.2f %8.2f%n",
                label, avg / TRIALS, max / TRIALS, total / TRIALS, util / TRIALS * 100,
                blocked / TRIALS, cost / TRIALS, cost / TRIALS / customers);
    }
}
