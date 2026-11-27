import java.util.*;

public class ACS {

    public static class Step {
        public final int time;
        public final String event;      // process name, "CS" or "IDLE"
        public final String tier1;
        public final String tier2;
        Step(int time, String event, String tier1, String tier2) {
            this.time = time; this.event = event; this.tier1 = tier1; this.tier2 = tier2;
        }
    }

    public static process[] run(process[] input, int q, int cs) {
        return run(input, q, cs, null);
    }

    public static process[] run(process[] input, int q, int cs, List<Step> trace) {
        if (cs < 0) throw new IllegalArgumentException("Context Switching cannot be negative");

        process[] arr = new process[input.length];
        for (int i = 0; i < input.length; i++) {
            arr[i] = input[i].copy();
        }

        process[] arrivals = arr.clone();
        Arrays.sort(arrivals, Comparator.comparingInt(process::getArrivalTime));

        Tier1 t1 = new Tier1(q);
        Tier2 t2 = new Tier2();

        int time = 0, nextArrival = 0, done = 0;
        process last = null;

        while (done < arrivals.length) {
            nextArrival = enqueueArrivals(arrivals, nextArrival, time, t1);

            if (t1.isEmpty() && t2.isEmpty()) {
                System.out.println("Time " + time + ": CPU is IDLE");
                if (trace != null) trace.add(new Step(time, "IDLE", t1.state(), t2.state()));
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
                    nextArrival = enqueueArrivals(arrivals, nextArrival, time, t1);
                    System.out.println("Time " + time + ": CONTEXT SWITCH");
                    if (trace != null) trace.add(new Step(time, "CS", t1.state(), t2.state()));
                    System.out.println("Tier 1: " + t1.state());
                    System.out.println("Tier 2: " + t2.state());
                    time++;
                }
                nextArrival = enqueueArrivals(arrivals, nextArrival, time, t1);

                fromTier1 = !t1.isEmpty();
                if (fromTier1) {
                    idx = -1;
                    p = t1.peek();
                } else {
                    idx = t2.pickIndex(time);
                    p = t2.get(idx);
                }
            }

            if (fromTier1) t1.poll();
            else t2.remove(idx);

            int runUnits = fromTier1 ? t1.getRunUnits(p) : p.getRemainingTime();

            System.out.println("\n" + p.getName() + " runs from " + time + " to " + (time + runUnits));
            for (int i = 0; i < runUnits; i++) {
                nextArrival = enqueueArrivals(arrivals, nextArrival, time, t1);
                System.out.println("Time " + time + " | Running: " + p.getName());
                if (trace != null) trace.add(new Step(time, p.getName(), t1.state(), t2.state()));
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

            System.out.println("Tier 1: " + t1.state());
            System.out.println("Tier 2: " + t2.state());
            System.out.println("--------------------------------");
        }

        printResults(arr);
        return arr;
    }

    private static int enqueueArrivals(process[] arrivals, int nextArrival, int time, Tier1 t1) {
        while (nextArrival < arrivals.length && arrivals[nextArrival].getArrivalTime() <= time) {
            process arrP = arrivals[nextArrival++];
            t1.add(arrP);
            System.out.println(arrP.getName() + " arrived at time " + arrP.getArrivalTime());
        }
        return nextArrival;
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