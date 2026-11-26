public class process {


    // ---- Input data ----
    private final String name;
    private final int arrivalTime;
    private final int burstTime;
    private int priority;          // used by ACS (Tier 2 / aging); ignored by SJF

    // ---- Runtime state ----
    private int remainingTime;
    private int startTime = -1;    // first time the process got the CPU
    private int finishTime = -1;

    public process(String name, int arrivalTime, int burstTime, int priority) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }

    public process(String name, int arrivalTime, int burstTime) {
        this(name, arrivalTime, burstTime, 0);
    }

    // ---- Execution helpers ----
    /** Run this process for one time unit. */
    public void runOneUnit(int currentTime) {
        if (startTime == -1) startTime = currentTime;
        remainingTime--;
    }

    public boolean isFinished()            { return remainingTime == 0; }
    public boolean hasArrived(int time)    { return arrivalTime <= time; }

    // ---- Metrics ----
    public int getTurnaroundTime() { return finishTime - arrivalTime; }
    public int getWaitingTime()    { return getTurnaroundTime() - burstTime; }
    public int getResponseTime()   { return startTime - arrivalTime; }

    // ---- Getters / setters ----
    public String getName()        { return name; }
    public int getArrivalTime()    { return arrivalTime; }
    public int getBurstTime()      { return burstTime; }
    public int getPriority()       { return priority; }
    public void setPriority(int p) { this.priority = p; }
    public int getRemainingTime()  { return remainingTime; }
    public int getStartTime()      { return startTime; }
    public int getFinishTime()     { return finishTime; }
    public void setFinishTime(int t) { this.finishTime = t; }
    public void setRemainingTime(int r) { this.remainingTime = r; }
    public void setStartTime(int t)     { this.startTime = t; }

    /** Fresh copy with the original input values (so one test case can be run on several schedulers). */
    public process copy() {
        return new process(name, arrivalTime, burstTime, priority);
    }

    @Override
    public String toString() {
        return name + " (remaining: " + remainingTime + ", Priority: " + priority + ")";
    }
}
