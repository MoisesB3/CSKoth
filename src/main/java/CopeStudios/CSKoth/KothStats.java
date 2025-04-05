package CopeStudios.CSKoth;

public class KothStats {
    private int captures;
    private int contests;
    private int points;

    public KothStats(int captures, int contests, int points) {
        this.captures = captures;
        this.contests = contests;
        this.points = points;
    }

    public int getCaptures() {
        return captures;
    }

    public int getContests() {
        return contests;
    }

    public int getPoints() {
        return points;
    }

    public void incrementCaptures() {
        captures++;
    }

    public void incrementContests() {
        contests++;
    }

    public void addPoints(int amount) {
        points += amount;
    }
}