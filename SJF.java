import java.util.*;


public class SJF {

    /** One row of the per-time-step output. */
    public static class Step {
        public final int time;
        public final String event;          // process name, "CS", or "IDLE"
        public final String readyQueue;     // snapshot at the start of this time unit
        Step(int time, String event, String readyQueue) {
            this.time = time; this.event = event; this.readyQueue = readyQueue;
        }
    }

    private final List<process> processes = new ArrayList<>();
    private final int contextSwitch;
    private final List<Step> steps = new ArrayList<>();
    private int contextSwitchCount = 0;
    private int totalTime = 0;

    public SJF(List<process> input, int contextSwitch) {
        for (process p : input) processes.add(p.copy());
        this.contextSwitch = contextSwitch;
    }

    public void run() {
        int time = 0, done = 0, n = processes.size();
        process last = null;                       // last process that executed

        while (done < n) {
            process next = pickNext(time, last);

            if (next == null) {                    // nothing ready -> CPU idle
                steps.add(new Step(time, "IDLE", "[Empty]"));
                time++;
                continue;
            }

            // context switch if we move to a different process
            if (last != null && next != last) {
                String q = queueSnapshot(time);
                for (int i = 0; i < contextSwitch; i++) {
                    steps.add(new Step(time, "CS", q));
                    time++;
                    q = queueSnapshot(time);
                }
                contextSwitchCount++;
            }

            // run the chosen process for one time unit
            steps.add(new Step(time, next.getName(), queueSnapshot(time)));
            next.runOneUnit(time);
            time++;
            last = next;

            if (next.isFinished()) {
                next.setFinishTime(time);
                done++;
            }
        }
        totalTime = time;
    }

    /** Shortest remaining time among arrived, unfinished processes. */
    private process pickNext(int time, process current) {
        process best = null;
        for (process p : processes) {
            if (p.isFinished() || !p.hasArrived(time)) continue;
            if (best == null
                    || p.getRemainingTime() < best.getRemainingTime()
                    || (p.getRemainingTime() == best.getRemainingTime()
                        && best != current
                        && (p == current || p.getArrivalTime() < best.getArrivalTime()))) {
                best = p;
            }
        }
        return best;
    }

    private String queueSnapshot(int time) {
        StringBuilder sb = new StringBuilder("[");
        for (process p : processes) {
            if (!p.isFinished() && p.hasArrived(time)) {
                if (sb.length() > 1) sb.append(", ");
                sb.append(p.getName()).append("(rem:").append(p.getRemainingTime()).append(")");
            }
        }
        return sb.length() == 1 ? "[Empty]" : sb.append("]").toString();
    }

    // ---------------- Results ----------------
    public List<process> getProcesses() { return processes; }
    public List<Step> getSteps()        { return steps; }
    public int getContextSwitchCount()  { return contextSwitchCount; }
    public int getTotalTime()           { return totalTime; }

    public double getAverageWaitingTime() {
        return processes.stream().mapToInt(process::getWaitingTime).average().orElse(0);
    }
    public double getAverageTurnaroundTime() {
        return processes.stream().mapToInt(process::getTurnaroundTime).average().orElse(0);
    }
    public double getAverageResponseTime() {
        return processes.stream().mapToInt(process::getResponseTime).average().orElse(0);
    }

    // ---------------- Printing ----------------
    public void printResults() {
        System.out.println("===== Preemptive SJF (CS = " + contextSwitch + ") =====\n");

        System.out.println("Time | Running | Ready queue (at start of time unit)");
        System.out.println("-----+---------+------------------------------------");
        for (Step s : steps)
            System.out.printf("%4d | %-7s | %s%n", s.time, s.event, s.readyQueue);

        System.out.println("\nGantt chart:");
        printGantt();

        System.out.println("\n" + String.format("%-8s%-9s%-7s%-9s%-12s%-9s%-9s",
                "Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"));
        for (process p : processes)
            System.out.println(String.format("%-8s%-9d%-7d%-9d%-12d%-9d%-9d",
                    p.getName(), p.getArrivalTime(), p.getBurstTime(), p.getFinishTime(),
                    p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime()));

        System.out.printf("%nAverage Waiting Time    : %.2f%n", getAverageWaitingTime());
        System.out.printf("Average Turnaround Time : %.2f%n", getAverageTurnaroundTime());
        System.out.printf("Average Response Time   : %.2f%n", getAverageResponseTime());
        System.out.println("Context switches        : " + contextSwitchCount);
    }

    private void printGantt() {
        StringBuilder bar = new StringBuilder("|"), times = new StringBuilder();
        int segStart = 0;
        String cur = steps.isEmpty() ? "" : steps.get(0).event;
        times.append(0);
        for (int i = 1; i <= steps.size(); i++) {
            String ev = i < steps.size() ? steps.get(i).event : null;
            if (!cur.equals(ev)) {
                int width = (i - segStart) * 2;
                bar.append(center(cur, Math.max(width, cur.length() + 2))).append("|");
                int len = Math.max(width, cur.length() + 2);
                times.append(" ".repeat(Math.max(1, len + 1 - String.valueOf(i).length()))).append(i);
                segStart = i;
                cur = ev;
            }
        }
        System.out.println(bar);
        System.out.println(times);
    }

    private static String center(String s, int w) {
        int left = (w - s.length()) / 2;
        return " ".repeat(left) + s + " ".repeat(w - s.length() - left);
    }
}