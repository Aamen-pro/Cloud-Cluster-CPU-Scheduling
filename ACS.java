import java.util.*;

public class ACS {

    public static void run(process[] input, int q, int cs) {
        process[] arr = new process[input.length];
        for (int i = 0; i < input.length; i++) {
            arr[i] = input[i].copy();
        }

        // Tier 1
        List<process> demoted = Tier1.roundRobin(arr, q, cs);

        // when did Tier 1 finish, and who ran last
        int time = 0;
        process last = null;
        for (process p : arr) {
            int end;
            if (p.isFinished()) end = p.getFinishTime();
            else end = p.getStartTime() + q;

            if (end > time) {
                time = end;
                last = p;
            }
        }

        // Tier 2
        Tier2 t2 = new Tier2();
        for (process p : demoted) {
            t2.add(p, p.getStartTime() + q);
        }

        while (!t2.isEmpty()) {
            int idx = t2.pickIndex(time);
            process p = t2.get(idx);

            if (p != last && cs > 0) {
                for (int i = 0; i < cs; i++) {
                    System.out.println("Time " + time + ": CONTEXT SWITCH");
                    System.out.println("Tier 1: [Empty]");
                    System.out.println("Tier 2: " + t2.state());
                    time++;
                }
            }

            t2.remove(idx);
            System.out.println("\n" + p.getName() + " runs from " + time
                    + " to " + (time + p.getRemainingTime()));

            while (!p.isFinished()) {
                System.out.println("Time " + time + " | Running: " + p.getName());
                p.runOneUnit(time);
                time++;
            }
            p.setFinishTime(time);
            System.out.println(p.getName() + " finished at " + time);

            last = p;
            System.out.println("Tier 1: [Empty]");
            System.out.println("Tier 2: " + t2.state());
        }

        printResults(arr);
    }

    static void printResults(process[] arr) {
        double totalWait = 0, totalTat = 0;
        System.out.println("\nProcess Arrival Finish Turnaround Waiting Response");
        for (process p : arr) {
            System.out.println(p.getName() + "\t" + p.getArrivalTime() + "\t" + p.getFinishTime()
                    + "\t" + p.getTurnaroundTime() + "\t" + p.getWaitingTime()
                    + "\t" + p.getResponseTime());
            totalWait += p.getWaitingTime();
            totalTat += p.getTurnaroundTime();
        }
        System.out.println("Average Turnaround Time: " + totalTat / arr.length);
        System.out.println("Average Waiting Time: " + totalWait / arr.length);
    }

}