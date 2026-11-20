package carnival;

public class CarnivalTicketLimitTest {

    public static void main(String[] args)
            throws InterruptedException {

        System.out.println("=== 5,000 Ticket Limit Test ===");

        CarnivalSimulation simulation = new CarnivalSimulation();

        simulation.run(5005, 100, false);
    }
}

