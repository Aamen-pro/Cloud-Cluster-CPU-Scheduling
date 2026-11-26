import java.util.*;

public class ACS {

    public static void run(process[] input, int q, int cs) {
        if (q <= 0) throw new IllegalArgumentException("Quantum must be positive");
        if (cs < 0) throw new IllegalArgumentException("Context Switching cannot be negative");

        process[] arr = new process[input.length];
        for (int i = 0; i < input.length; i++) {
            arr[i] = input[i].copy();
        }

        process[] arrivals = arr.clone();
        Arrays.sort(arrivals, Comparator.comparingInt(process::getArrivalTime));

        Queue<process> t1 = new LinkedList<>();
        Tier2 t2 = new Tier2();

        int time = 0, nextArrival = 0, done = 0;
        process last = null;

        while (done < arrivals.length) {
            while (nextArrival < arrivals.length && arrivals[nextArrival].getArrivalTime() <= time) {
                process arrP = arrivals[nextArrival++];
                t1.add(arrP);
                System.out.println(arrP.getName() + " arrived at time " + arrP.getArrivalTime());
            }

            if (t1.isEmpty() && t2.isEmpty()) {
                System.out.println("Time " + time + ": CPU is IDLE");
                last = null;
                time++;
                continue;
            }

            boolean fromTier1 = !t1.isEmpty();
            int idx = -1;
            process p;
            if (fromTier1) {
                p = t1.peek();
            } else {
                idx = t2.pickIndex(time);
                p = t2.get(idx);
            }

            if (last != null && p != last && cs > 0) {
                for (int i = 0; i < cs; i++) {
                    while (nextArrival < arrivals.length && arrivals[nextArrival].getArrivalTime() <= time) {
                        process arrP = arrivals[nextArrival++];
                        t1.add(arrP);
                        System.out.println(arrP.getName() + " arrived at time " + arrP.getArrivalTime());
                    }
                    System.out.println("Time " + time + ": CONTEXT SWITCH");
                    System.out.println("Tier 1: " + Tier1.queueState(t1));
                    System.out.println("Tier 2: " + t2.state());
                    time++;
                }
                while (nextArrival < arrivals.length && arrivals[nextArrival].getArrivalTime() <= time) {
                    process arrP = arrivals[nextArrival++];
                    t1.add(arrP);
                    System.out.println(arrP.getName() + " arrived at time " + arrP.getArrivalTime());
                }
            }

            if (fromTier1) t1.poll();
            else t2.remove(idx);

            int runUnits = fromTier1 ? Math.min(q, p.getRemainingTime()) : p.getRemainingTime();

            System.out.println("\n" + p.getName() + " runs from " + time + " to " + (time + runUnits));
            for (int i = 0; i < runUnits; i++) {
                while (nextArrival < arrivals.length && arrivals[nextArrival].getArrivalTime() <= time) {
                    process arrP = arrivals[nextArrival++];
                    t1.add(arrP);
                    System.out.println(arrP.getName() + " arrived at time " + arrP.getArrivalTime());
                }
                System.out.println("Time " + time + " | Running: " + p.getName());
                p.runOneUnit(time);
                time++;
            }

            last = p;
            if (p.isFinished()) {
                p.setFinishTime(time);
                done++;
                System.out.println(p.getName() + " finished at " + time);
            } else {
                t2.add(p, time);
                System.out.println(p.getName() + " moved to Tier 2 | Remaining: " + p.getRemainingTime());
            }

            System.out.println("Tier 1: " + Tier1.queueState(t1));
            System.out.println("Tier 2: " + t2.state());
            System.out.println("--------------------------------");
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