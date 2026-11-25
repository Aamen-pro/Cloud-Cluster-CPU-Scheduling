import java.util.*;

public class Tier2 {
    static final int AGING = 4;
    private ArrayList<process> list = new ArrayList<>();
    private ArrayList<Integer> entryTime = new ArrayList<>();
    private ArrayList<Integer> basePriority = new ArrayList<>();

    public void add(process p, int time) {
        list.add(p);
        entryTime.add(time);
        basePriority.add(p.getPriority());
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public process get(int i) {
        return list.get(i);
    }

    public void remove(int i) {
        list.remove(i);
        entryTime.remove(i);
        basePriority.remove(i);
    }

    // update priorities (aging) then return index of the best process
    public int pickIndex(int now) {
        int best = 0;
        for (int i = 0; i < list.size(); i++) {
            int waited = now - entryTime.get(i);
            list.get(i).setPriority(basePriority.get(i) - waited / AGING);

            if (list.get(i).getPriority() < list.get(best).getPriority()) {
                best = i;
            }
        }
        return best;
    }

    public String state() {
        return Tier1.listState(list);
    }
}