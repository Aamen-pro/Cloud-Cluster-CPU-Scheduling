import java.util.*;

public class Tier1 {
    private final Queue<process> queue = new LinkedList<>();
    private final int quantum;

    public Tier1(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be positive");
        }
        this.quantum = quantum;
    }

    public void add(process p) {
        queue.add(p);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public process peek() {
        return queue.peek();
    }

    public process poll() {
        return queue.poll();
    }

    /** Round Robin execution slice for a process in Tier 1. */
    public int getRunUnits(process p) {
        return Math.min(quantum, p.getRemainingTime());
    }

    public String state() {
        return queueState(queue);
    }

    // Print Tier 1 Queue
    static String queueState(Queue<process> queue) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (process p : queue) {
            result.add(p.getName() + "(remaining: " + p.getRemainingTime() + ", Priority: " + p.getPriority() + ")");
        }
        return result.toString();
    }

    // Print Tier 2 List
    static String listState(List<process> processes) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (process p : processes) {
            result.add(p.getName() + "(remaining: " + p.getRemainingTime() + ", Priority: " + p.getPriority() + ")");
        }
        return result.toString();
    }
}