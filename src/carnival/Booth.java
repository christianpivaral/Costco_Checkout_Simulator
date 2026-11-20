package carnival;

public class Booth extends CarnivalActivity {

    private final String gameType;

    public Booth(String name, int capacity,
                 long durationMillis, String gameType) {

        super(name, capacity, durationMillis);
        this.gameType = gameType;
    }

    public String getGameType() {
        return gameType;
    }

    public void displayBoothInfo() {
        System.out.println("\n=== Carnival Game Booth ===");
        System.out.println("Booth Name: " + getName());
        System.out.println("Game Type: " + gameType);
        System.out.println("Maximum Players: " + getCapacity());
        System.out.println("People Waiting: " + getWaitingCount());
        System.out.println("Games Completed: " + getCompletedCount());
    }
}

