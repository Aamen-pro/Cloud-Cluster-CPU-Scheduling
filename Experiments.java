import java.io.*;
import java.util.*;

public class Experiments {

    static final int[] QUANTA = {1, 2, 3, 4, 5, 6, 7, 8};
    static final int[] SWITCH_TIMES = {0, 1, 2, 3};

    public static void main(String[] args) throws Exception {
        String folder = args.length > 0 ? args[0] : "Test Cases";

        File[] files = new File(folder).listFiles((dir, name) -> name.matches("tc\\d+\\.txt"));
        if (files == null || files.length == 0) {
            System.out.println("No test case files (tc1.txt, tc2.txt, ...) found in: " + folder);
            return;
        }
        Arrays.sort(files, Comparator.comparingInt(f -> number(f.getName())));

        int rows = 0;
        try (PrintWriter csv = new PrintWriter("results.csv")) {
            csv.println("testcase,algorithm,Q,CS,avgWaiting,avgTurnaround,avgResponse,cpuUtilization,demoted");

            for (File f : files) {
                String name = f.getName().replace(".txt", "").toUpperCase();
                List<process> input = readTestCase(f);

                for (int cs : SWITCH_TIMES) {
                    rows += runSJF(csv, name, input, cs);
                    for (int q : QUANTA) {
                        rows += runACS(csv, name, input, q, cs);
                    }
                }
            }
        }
        System.out.println("Done: " + files.length + " test cases, " + rows + " rows written to results.csv");
    }

    static int runSJF(PrintWriter csv, String name, List<process> input, int cs) {
        SJF sjf = new SJF(input, cs);
        sjf.run();

        int totalBurst = 0;
        for (process p : input) totalBurst += p.getBurstTime();
        double utilization = (double) totalBurst / sjf.getTotalTime();

        csv.println(row(name, "SJF", "-", cs,
                sjf.getAverageWaitingTime(), sjf.getAverageTurnaroundTime(),
                sjf.getAverageResponseTime(), utilization, "-"));
        return 1;
    }

    static int runACS(PrintWriter csv, String name, List<process> input, int q, int cs) {
        PrintStream console = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        process[] done;
        try {
            done = ACS.run(input.toArray(new process[0]), q, cs);
        } finally {
            System.setOut(console);
        }

        double wait = 0, tat = 0, resp = 0;
        int totalBurst = 0, makespan = 0, demoted = 0;
        for (process p : done) {
            wait += p.getWaitingTime();
            tat += p.getTurnaroundTime();
            resp += p.getResponseTime();
            totalBurst += p.getBurstTime();
            makespan = Math.max(makespan, p.getFinishTime());
            if (p.getBurstTime() > q) demoted++;   // used its whole quantum without finishing
        }
        int n = done.length;

        csv.println(row(name, "ACS", String.valueOf(q), cs,
                wait / n, tat / n, resp / n, (double) totalBurst / makespan, String.valueOf(demoted)));
        return 1;
    }

    static String row(String name, String algo, String q, int cs,
                      double wait, double tat, double resp, double util, String demoted) {
        return String.format(Locale.US, "%s,%s,%s,%d,%.2f,%.2f,%.2f,%.4f,%s",
                name, algo, q, cs, wait, tat, resp, util, demoted);
    }

    static List<process> readTestCase(File f) throws FileNotFoundException {
        List<process> list = new ArrayList<>();
        try (Scanner sc = new Scanner(f)) {
            int n = sc.nextInt();
            for (int i = 0; i < n; i++) {
                list.add(new process(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt()));
            }
        }
        return list;
    }

    static int number(String fileName) {
        return Integer.parseInt(fileName.replaceAll("\\D", ""));
    }
}