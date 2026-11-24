public class process {

    String name;

    int bt;             // Burst Time
    int remainingTime;  // Remaining Burst Time
    int at;             // Arrival Time
    int priority;

    int firstStartTime = -1;
    int finishTime = -1;

    process(String name, int bt, int at, int priority) {

        this.name = name;
        this.bt = bt;
        this.remainingTime = bt;
        this.at = at;
        this.priority = priority;
    }
}