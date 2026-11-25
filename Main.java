import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<process> list = new ArrayList<>();

        System.out.print("Number of processes: ");
        int n = sc.nextInt();
        System.out.println("Enter for each process: name arrival burst priority");
        for (int i = 0; i < n; i++)
            list.add(new process(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt()));
        System.out.print("Context switch time: ");
        int cs = sc.nextInt();

        SJF sjf = new SJF(list, cs);
        sjf.run();
        sjf.printResults();
    }
}