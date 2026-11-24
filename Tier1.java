
import java.util.*;

public class Tier1{

    static List<process> roundRobin( process[] arrivals, int quantum, int contextSwitch) {

        if (quantum <= 0) {
            throw new IllegalArgumentException(
                "Quantum must be positive"
            );
        }

        if (contextSwitch < 0) {
            throw new IllegalArgumentException(
                "Context Switching cannot be negative"
            );
        }

        Arrays.sort( arrivals, Comparator.comparingInt(p -> p.at) );

        Queue<process> tier1 = new LinkedList<>();
        List<process> tier2 = new ArrayList<>();

        int currentTime = 0;
        int nextArrival = 0;

        boolean previousExecution = false;

        while (nextArrival < arrivals.length || !tier1.isEmpty()) {

            while (nextArrival < arrivals.length && arrivals[nextArrival].at <= currentTime) {

                process p = arrivals[nextArrival];
                tier1.add(p);

                System.out.println(p.name + " arrived at time " + p.at);

                nextArrival++;
            }

            if (tier1.isEmpty()) {
                System.out.println( "Time " + currentTime + ": CPU is IDLE" );
                currentTime++;
                previousExecution = false;

                continue;
            }
            if (previousExecution && contextSwitch > 0) {
                for (int i = 0; i < contextSwitch; i++) {
                    while (nextArrival < arrivals.length && arrivals[nextArrival].at <= currentTime) {
                        process p = arrivals[nextArrival];
                        tier1.add(p);
                        System.out.println(p.name + " arrived at time " + p.at);
                        nextArrival++;
                    }

                    System.out.println(
                        "Time " + currentTime + ": CONTEXT SWITCH"
                    );

                    System.out.println(
                        "Tier 1: " + queueState(tier1)
                    );

                    System.out.println(
                        "Tier 2: " + listState(tier2)
                    );

                    currentTime++;
                }
            }
            while (nextArrival < arrivals.length && arrivals[nextArrival].at <= currentTime) {
                process p = arrivals[nextArrival];

                tier1.add(p);

                System.out.println(
                    p.name + " arrived at time " + p.at
                );

                nextArrival++;
            }

            process curr = tier1.poll();

            int executionTime = Math.min( quantum,curr.remainingTime );

            if (curr.firstStartTime == -1) {
                curr.firstStartTime = currentTime;
            }

            System.out.println( "\n" + curr.name + " runs from " + currentTime + " to " + (currentTime + executionTime));

            for (int i = 0; i < executionTime; i++) {

                while (nextArrival < arrivals.length && arrivals[nextArrival].at <= currentTime) {
                    process p = arrivals[nextArrival];
                    tier1.add(p);
                    System.out.println(
                        p.name + " arrived at time " + p.at
                    );

                    nextArrival++;
                }

                System.out.println( "Time " + currentTime + " | Running: " + curr.name );

                curr.remainingTime--;
                currentTime++;
            }

            previousExecution = true;

            if (curr.remainingTime == 0) {

                curr.finishTime = currentTime;

                System.out.println( curr.name + " finished at " + currentTime );

            } else {

                tier2.add(curr); 
                System.out.println( curr.name + " moved to Tier 2" + " | Remaining: " + curr.remainingTime );
            }

            System.out.println( "Tier 1: " + queueState(tier1));

            System.out.println( "Tier 2: " + listState(tier2));

            System.out.println("--------------------------------");
        }

        return tier2;
    }


    // Print Tier 1 Queue
    static String queueState(Queue<process> queue) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (process p : queue) {
            result.add(p.name + "(remaining: " + p.remainingTime + ", Priority: " + p.priority + ")");
        }

        return result.toString();
    }


    // Print Tier 2 List
    static String listState(List<process> processes) {

        StringJoiner result = new StringJoiner(", ", "[", "]");

        for (process p : processes) {

            result.add( p.name + "(remaining: " + p.remainingTime + ", Priority: " + p.priority + ")" );
        }

        return result.toString();
    }


    public static void main(String[] args) {

        process[] processes = {

            new process("P1", 4, 0, 2),

            new process("P2", 7, 1, 3),

            new process("P3", 2, 3, 1)
        };

        List<process> demoted = roundRobin(
            processes,
            3,
            0
        );

        System.out.println("\nFinal Tier 2 processes:");

        for (process p : demoted) {

            System.out.println(
                p.name
                + " | Remaining: " + p.remainingTime
                + " | Priority: " + p.priority
            );
        }
    }
}
