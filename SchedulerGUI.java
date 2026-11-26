import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class SchedulerGUI extends JFrame {

    static final Color BG1 = new Color(29, 33, 64), BG2 = new Color(11, 13, 26);
    static final Color CARD = new Color(23, 26, 46), CARD_LINE = new Color(42, 47, 77);
    static final Color SUB = new Color(31, 35, 64);
    static final Color TEXT = new Color(232, 234, 246), MUTED = new Color(154, 160, 192);
    static final Color FIELD = new Color(37, 42, 64);
    static final Color PURPLE = new Color(195, 63, 216), CYAN = new Color(0, 210, 255);
    static final Font F = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FB = new Font("Segoe UI", Font.BOLD, 14);

    static class Chip {
        final String name; final int rem, pri;
        Chip(String n, int r, int p) { name = n; rem = r; pri = p; }
    }
    static final int NONE = Integer.MIN_VALUE;

    static class Snap {
        int time; String event; int runningRem;
        List<Chip> tier1 = new ArrayList<>(), tier2 = new ArrayList<>();
    }

    static class Result {
        String name;
        List<Snap> snaps = new ArrayList<>();
        List<GanttPanel.Seg> segs = new ArrayList<>();
        List<process> procs;
        double avgW, avgT, avgR, util;
        int total;
    }

    private Result acsRes, sjfRes, current;
    private int idx = 0;

    private final DefaultTableModel inputModel = new DefaultTableModel(new Object[]{"Name", "Arrival", "Burst", "Priority"}, 0);
    private final JTable inputTable = new JTable(inputModel);
    private final JSpinner qSpin = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JSpinner csSpin = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
    private final GanttPanel gantt = new GanttPanel();
    private final Seek seek = new Seek();
    private final JLabel timeLbl = label("t = 0", FB, Color.WHITE);
    private final Pill playBtn = new Pill("Play", FIELD, FIELD, TEXT);
    private final Pill speedBtn = new Pill("speed 1x", FIELD, FIELD, MUTED);
    private final Pill acsBtn = new Pill("ACS", PURPLE, PURPLE, Color.WHITE);
    private final Pill sjfBtn = new Pill("SJF", FIELD, FIELD, MUTED);
    private final ChipsPanel cpuChips = new ChipsPanel("idle");
    private final ChipsPanel t1Chips = new ChipsPanel("empty");
    private final ChipsPanel t2Chips = new ChipsPanel("empty");
    private final JLabel t1Title = label("Tier 1 (Round Robin)", F, MUTED);
    private final JLabel t2Title = label("Tier 2 (Priority + aging)", F, MUTED);
    private final DefaultTableModel resModel = new DefaultTableModel(
            new Object[]{"Process", "Finish", "Turnaround", "Waiting", "Response"}, 0);
    private final JLabel avgLbl = label(" ", F, MUTED);
    private final Bars waitBars = new Bars("Avg waiting time", "");
    private final Bars utilBars = new Bars("CPU utilization", "%");
    private final JLabel chipQ = new JLabel(), chipCs = new JLabel(), chipTc = new JLabel();
    private final javax.swing.Timer player;
    private final double[] speeds = {0.5, 1, 2, 4};
    private int speedIdx = 1;
    private String testName = "Custom";

    public SchedulerGUI() {
        super("Cloud Cluster CPU Scheduler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        inputModel.addRow(new Object[]{"P1", 0, 4, 2});
        inputModel.addRow(new Object[]{"P2", 1, 7, 3});
        inputModel.addRow(new Object[]{"P3", 3, 2, 1});

        player = new javax.swing.Timer(500, e -> stepForward());

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, BG1, getWidth(), getHeight(), BG2));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.insets = new Insets(0, 0, 16, 16);
        c.gridx = 0; c.gridy = 0; c.weightx = 0; c.weighty = 0.55;
        body.add(buildInputCard(), c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 0, 16, 0);
        c.gridwidth = 3;
        body.add(buildGanttCard(), c);

        c.gridwidth = 1; c.gridy = 1; c.weighty = 0.45; c.insets = new Insets(0, 0, 0, 16);
        c.gridx = 0; c.weightx = 0;

        JPanel res = buildResultsCard();
        c.gridx = 0; c.gridwidth = 2; c.weightx = 1.4;
        body.add(res, c);
        c.gridx = 2; c.gridwidth = 1; c.weightx = 0.8;
        body.add(card(waitBars), c);
        c.gridx = 3; c.weightx = 0.8; c.insets = new Insets(0, 0, 0, 0);
        body.add(card(utilBars), c);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        runSimulation();
    }

    private JComponent buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 0.55f, 1f},
                        new Color[]{new Color(91, 43, 224), new Color(195, 63, 216), new Color(255, 106, 136)}));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        h.setBorder(new EmptyBorder(16, 28, 16, 28));
        JPanel left = new JPanel(); left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Cloud Cluster CPU Scheduler", new Font("Segoe UI", Font.BOLD, 24), Color.WHITE));
        left.add(label("Adaptive Cloud Scheduler (ACS) vs Preemptive SJF", new Font("Segoe UI", Font.PLAIN, 12),
                new Color(255, 255, 255, 220)));
        h.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8)); right.setOpaque(false);
        for (JLabel l : new JLabel[]{chipTc, chipQ, chipCs}) {
            l.setFont(new Font("Segoe UI", Font.PLAIN, 12)); l.setForeground(Color.WHITE);
            l.setBorder(new EmptyBorder(5, 14, 5, 14));
            l.setOpaque(false);
            right.add(new RoundBg(l, new Color(255, 255, 255, 55), 20));
        }
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildInputCard() {
        Card c = new Card(new BorderLayout(0, 10));
        c.setPreferredSize(new Dimension(340, 300));
        c.add(label("Processes", FB, TEXT), BorderLayout.NORTH);

        styleTable(inputTable, true);
        inputTable.setDefaultRenderer(Object.class, new DotRenderer(true));
        inputTable.setFillsViewportHeight(true);
        JScrollPane sp = new JScrollPane(inputTable);
        sp.setBorder(BorderFactory.createEmptyBorder()); sp.setViewportBorder(BorderFactory.createEmptyBorder()); sp.getViewport().setBackground(CARD); sp.setOpaque(false);
        c.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(); south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); btns.setOpaque(false);
        Pill add = new Pill("+ Add", FIELD, FIELD, TEXT), rem = new Pill("Remove", FIELD, FIELD, TEXT),
                load = new Pill("Load tcN.txt", FIELD, FIELD, TEXT), clr = new Pill("Clear", FIELD, FIELD, TEXT);
        add.addActionListener(e -> {
            stopEdit();
            inputModel.addRow(new Object[]{"P" + (inputModel.getRowCount() + 1), 0, 1, 1});
        });
        rem.addActionListener(e -> {
            stopEdit();
            int r = inputTable.getSelectedRow();
            if (r < 0) r = inputModel.getRowCount() - 1;
            if (r >= 0) inputModel.removeRow(r);
        });
        load.addActionListener(e -> loadFile());
        clr.addActionListener(e -> { stopEdit(); inputModel.setRowCount(0); });
        for (Pill p : new Pill[]{add, rem, load, clr}) { p.setFont(new Font("Segoe UI", Font.PLAIN, 11)); btns.add(p); }
        south.add(btns);
        south.add(Box.createVerticalStrut(10));

        JPanel spin = new JPanel(new GridLayout(2, 2, 8, 6)); spin.setOpaque(false);
        styleSpinner(qSpin); styleSpinner(csSpin);
        spin.add(label("Quantum (Q)", F, MUTED)); spin.add(qSpin);
        spin.add(label("Context switch (CS)", F, MUTED)); spin.add(csSpin);
        south.add(spin);
        south.add(Box.createVerticalStrut(12));

        Pill run = new Pill("RUN SIMULATION", new Color(0, 200, 83), new Color(0, 229, 160), new Color(6, 33, 15));
        run.setFont(new Font("Segoe UI", Font.BOLD, 14));
        run.setGlow(new Color(0, 229, 160));
        run.setPreferredSize(new Dimension(100, 42));
        run.addActionListener(e -> runSimulation());
        south.add(run);
        c.add(south, BorderLayout.SOUTH);
        return c;
    }

    private JPanel buildGanttCard() {
        Card c = new Card(new BorderLayout(0, 8));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(label("Gantt chart", FB, TEXT), BorderLayout.WEST);
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); tabs.setOpaque(false);
        acsBtn.addActionListener(e -> select(acsRes));
        sjfBtn.addActionListener(e -> select(sjfRes));
        tabs.add(acsBtn); tabs.add(sjfBtn);
        top.add(tabs, BorderLayout.EAST);

        JPanel mid = new JPanel(new BorderLayout(0, 8)); mid.setOpaque(false);
        mid.add(gantt, BorderLayout.CENTER);

        JPanel ctrl = new JPanel(new BorderLayout(10, 0)); ctrl.setOpaque(false);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); btns.setOpaque(false);
        Pill first = new Pill("|<", FIELD, FIELD, TEXT), prev = new Pill("<", FIELD, FIELD, TEXT),
                next = new Pill(">", FIELD, FIELD, TEXT), last = new Pill(">|", FIELD, FIELD, TEXT);
        playBtn.setColors(PURPLE, PURPLE, Color.WHITE);
        first.addActionListener(e -> { pause(); goTo(0); });
        prev.addActionListener(e -> { pause(); goTo(idx - 1); });
        next.addActionListener(e -> { pause(); goTo(idx + 1); });
        last.addActionListener(e -> { pause(); goTo(current == null ? 0 : current.snaps.size() - 1); });
        playBtn.addActionListener(e -> { if (player.isRunning()) pause(); else play(); });
        speedBtn.addActionListener(e -> {
            speedIdx = (speedIdx + 1) % speeds.length;
            speedBtn.setText("speed " + (speeds[speedIdx] == 0.5 ? "0.5" : String.valueOf((int) speeds[speedIdx])) + "x");
            player.setDelay((int) (500 / speeds[speedIdx]));
        });
        for (Pill p : new Pill[]{first, prev, playBtn, next, last}) btns.add(p);
        ctrl.add(btns, BorderLayout.WEST);
        seek.setListener(i -> { pause(); goTo(i); });
        ctrl.add(seek, BorderLayout.CENTER);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); east.setOpaque(false);
        east.add(timeLbl); east.add(speedBtn);
        ctrl.add(east, BorderLayout.EAST);
        mid.add(ctrl, BorderLayout.SOUTH);

        JPanel chips = new JPanel(new GridLayout(1, 3, 12, 0)); chips.setOpaque(false);
        chips.setPreferredSize(new Dimension(100, 120));
        chips.add(sub(label("On CPU now", F, MUTED), cpuChips));
        chips.add(sub(t1Title, t1Chips));
        chips.add(sub(t2Title, t2Chips));

        c.add(top, BorderLayout.NORTH);
        c.add(mid, BorderLayout.CENTER);
        c.add(chips, BorderLayout.SOUTH);
        return c;
    }

    private JPanel buildResultsCard() {
        Card c = new Card(new BorderLayout(0, 8));
        c.add(label("Results", FB, TEXT), BorderLayout.NORTH);
        JTable t = new JTable(resModel);
        styleTable(t, false);
        t.setDefaultRenderer(Object.class, new DotRenderer(false));
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder()); sp.setViewportBorder(BorderFactory.createEmptyBorder()); sp.getViewport().setBackground(CARD); sp.setOpaque(false);
        c.add(sp, BorderLayout.CENTER);
        c.add(avgLbl, BorderLayout.SOUTH);
        return c;
    }

    private JPanel card(JComponent inner) {
        Card c = new Card(new BorderLayout());
        c.add(inner, BorderLayout.CENTER);
        return c;
    }

    private JPanel sub(JLabel title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(SUB);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 12, 8, 12));
        title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(title, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private void runSimulation() {
        pause();
        stopEdit();
        List<process> list;
        try {
            list = readInput();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int q = (Integer) qSpin.getValue(), cs = (Integer) csSpin.getValue();

        List<String> names = new ArrayList<>();
        for (process p : list) names.add(p.getName());
        GanttPanel.registerProcesses(names);

        int totalBurst = 0;
        for (process p : list) totalBurst += p.getBurstTime();

        SJF sjf = new SJF(list, cs);
        sjf.run();
        sjfRes = new Result();
        sjfRes.name = "SJF";
        sjfRes.procs = sjf.getProcesses();
        sjfRes.avgW = sjf.getAverageWaitingTime();
        sjfRes.avgT = sjf.getAverageTurnaroundTime();
        sjfRes.avgR = sjf.getAverageResponseTime();
        sjfRes.total = sjf.getTotalTime();
        sjfRes.util = 100.0 * totalBurst / sjf.getTotalTime();
        Pattern sjfChip = Pattern.compile("([^\\s,\\[\\]()]+)\\(rem:(\\d+)\\)");
        for (SJF.Step s : sjf.getSteps()) {
            Snap sn = new Snap();
            sn.time = s.time; sn.event = s.event;
            Matcher m = sjfChip.matcher(s.readyQueue);
            while (m.find()) sn.tier1.add(new Chip(m.group(1), Integer.parseInt(m.group(2)), NONE));
            sjfRes.snaps.add(sn);
        }

        List<ACS.Step> trace = new ArrayList<>();
        process[] acsDone;
        PrintStream console = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            acsDone = ACS.run(list.toArray(new process[0]), q, cs, trace);
        } catch (RuntimeException ex) {
            System.setOut(console);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ACS error", JOptionPane.WARNING_MESSAGE);
            return;
        } finally {
            System.setOut(console);
        }
        acsRes = new Result();
        acsRes.name = "ACS";
        acsRes.procs = Arrays.asList(acsDone);
        double w = 0, t = 0, r = 0; int makespan = 0;
        for (process p : acsDone) {
            w += p.getWaitingTime(); t += p.getTurnaroundTime(); r += p.getResponseTime();
            makespan = Math.max(makespan, p.getFinishTime());
        }
        acsRes.avgW = w / acsDone.length; acsRes.avgT = t / acsDone.length; acsRes.avgR = r / acsDone.length;
        acsRes.total = makespan;
        acsRes.util = 100.0 * totalBurst / makespan;
        Pattern acsChip = Pattern.compile("([^\\s,\\[\\]()]+)\\(remaining: (\\d+), Priority: (-?\\d+)\\)");
        for (ACS.Step s : trace) {
            Snap sn = new Snap();
            sn.time = s.time; sn.event = s.event;
            Matcher m = acsChip.matcher(s.tier1);
            while (m.find()) sn.tier1.add(new Chip(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
            m = acsChip.matcher(s.tier2);
            while (m.find()) sn.tier2.add(new Chip(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
            acsRes.snaps.add(sn);
        }

        for (Result res : new Result[]{sjfRes, acsRes}) finishResult(res, list);

        chipTc.setText("Test case: " + testName);
        chipQ.setText("Q = " + q);
        chipCs.setText("CS = " + cs);
        waitBars.setData(new String[]{"SJF", "ACS"}, new double[]{sjfRes.avgW, acsRes.avgW},
                Math.max(sjfRes.avgW, acsRes.avgW) * 1.1 + 0.001,
                new Color[][]{{CYAN, new Color(58, 123, 213)}, {PURPLE, new Color(255, 106, 136)}}, 2);
        utilBars.setData(new String[]{"SJF", "ACS"}, new double[]{sjfRes.util, acsRes.util}, 100,
                new Color[][]{{new Color(0, 200, 83), new Color(0, 229, 160)}, {new Color(249, 212, 35), new Color(255, 138, 0)}}, 1);
        select(acsRes);
    }

    private void finishResult(Result res, List<process> input) {
        Map<String, Integer> done = new HashMap<>(), burst = new HashMap<>();
        for (process p : input) burst.put(p.getName(), p.getBurstTime());
        res.segs.clear();
        GanttPanel.Seg open = null;
        String curLabel = null; int start = 0;
        for (Snap s : res.snaps) {
            if (burst.containsKey(s.event)) {
                int d = done.getOrDefault(s.event, 0);
                s.runningRem = burst.get(s.event) - d;
                done.put(s.event, d + 1);
            }
            if (curLabel == null || !curLabel.equals(s.event)) {
                if (curLabel != null) res.segs.add(new GanttPanel.Seg(curLabel, start, s.time));
                curLabel = s.event; start = s.time;
            }
        }
        if (curLabel != null) res.segs.add(new GanttPanel.Seg(curLabel, start, res.total));
    }

    private List<process> readInput() {
        List<process> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (inputModel.getRowCount() == 0) throw new IllegalArgumentException("Add at least one process.");
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            String name = String.valueOf(inputModel.getValueAt(i, 0)).trim();
            if (name.isEmpty() || name.matches(".*[\\s,()\\[\\]].*"))
                throw new IllegalArgumentException("Row " + (i + 1) + ": name must be one word (no spaces, commas or brackets).");
            if (name.equals("CS") || name.equals("IDLE"))
                throw new IllegalArgumentException("Row " + (i + 1) + ": CS and IDLE are reserved names.");
            if (!seen.add(name)) throw new IllegalArgumentException("Duplicate process name: " + name);
            int a = num(i, 1, "Arrival"), b = num(i, 2, "Burst"), p = num(i, 3, "Priority");
            if (a < 0) throw new IllegalArgumentException("Row " + (i + 1) + ": arrival cannot be negative.");
            if (b <= 0) throw new IllegalArgumentException("Row " + (i + 1) + ": burst must be at least 1.");
            list.add(new process(name, a, b, p));
        }
        return list;
    }

    private int num(int row, int col, String what) {
        try {
            return Integer.parseInt(String.valueOf(inputModel.getValueAt(row, col)).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Row " + (row + 1) + ": " + what + " must be a whole number.");
        }
    }

    private void loadFile() {
        File dir = new File("Test Cases");
        JFileChooser fc = new JFileChooser(dir.isDirectory() ? dir : new File("."));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (Scanner sc = new Scanner(f)) {
            int n = sc.nextInt();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < n; i++)
                rows.add(new Object[]{sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt()});
            int cs = sc.hasNextInt() ? sc.nextInt() : (Integer) csSpin.getValue();
            int q = sc.hasNextInt() ? sc.nextInt() : (Integer) qSpin.getValue();
            stopEdit();
            inputModel.setRowCount(0);
            for (Object[] r : rows) inputModel.addRow(r);
            csSpin.setValue(cs);
            qSpin.setValue(Math.max(1, q));
            testName = f.getName().replace(".txt", "").toUpperCase();
            runSimulation();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not read " + f.getName() + ": " + ex.getMessage(),
                    "Load error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void stopEdit() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
    }

    private void select(Result r) {
        if (r == null) return;
        pause();
        current = r;
        boolean acs = r == acsRes;
        acsBtn.setColors(acs ? PURPLE : FIELD, acs ? PURPLE : FIELD, acs ? Color.WHITE : MUTED);
        sjfBtn.setColors(acs ? FIELD : PURPLE, acs ? FIELD : PURPLE, acs ? MUTED : Color.WHITE);
        t1Title.setText(acs ? "Tier 1 (Round Robin, Q=" + qSpin.getValue() + ")" : "Ready queue (SJF)");
        t2Title.setText(acs ? "Tier 2 (Priority + aging)" : "Tier 2 (not used by SJF)");

        resModel.setRowCount(0);
        for (process p : r.procs)
            resModel.addRow(new Object[]{p.getName(), p.getFinishTime(), p.getTurnaroundTime(),
                    p.getWaitingTime(), p.getResponseTime()});
        avgLbl.setText(String.format(Locale.US, "Average waiting %.2f   |   turnaround %.2f   |   response %.2f   |   CPU utilization %.1f%%",
                r.avgW, r.avgT, r.avgR, r.util));

        gantt.setSegments(r.segs, r.total);
        seek.setMax(Math.max(0, r.snaps.size() - 1));
        idx = 0;
        goTo(0);
    }

    private void goTo(int i) {
        if (current == null || current.snaps.isEmpty()) return;
        idx = Math.max(0, Math.min(i, current.snaps.size() - 1));
        Snap s = current.snaps.get(idx);
        gantt.setTime(s.time);
        seek.setValue(idx);
        timeLbl.setText("t = " + s.time);

        List<Chip> cpu = new ArrayList<>();
        if (s.event.equals("CS")) cpuChips.setText("context switch");
        else if (s.event.equals("IDLE")) cpuChips.setText("idle");
        else {
            cpu.add(new Chip(s.event, s.runningRem, NONE));
        }
        cpuChips.setChips(cpu);
        t1Chips.setChips(s.tier1);
        t2Chips.setChips(s.tier2);
    }

    private void stepForward() {
        if (current == null || idx >= current.snaps.size() - 1) { pause(); return; }
        goTo(idx + 1);
    }

    private void play() {
        if (current == null) return;
        if (idx >= current.snaps.size() - 1) goTo(0);
        player.start();
        playBtn.setText("Pause");
    }

    private void pause() {
        player.stop();
        playBtn.setText("Play");
    }

    static JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f); l.setForeground(c);
        return l;
    }

    static void styleSpinner(JSpinner sp) {
        sp.setFont(F);
        sp.setBorder(new LineBorder(CARD_LINE, 1, true));
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
            tf.setBackground(FIELD); tf.setForeground(TEXT); tf.setCaretColor(TEXT);
            tf.setBorder(new EmptyBorder(3, 6, 3, 6));
        }
        for (Component c : sp.getComponents()) {
            if (c instanceof JButton) { c.setBackground(FIELD); ((JButton) c).setBorder(new EmptyBorder(0, 4, 0, 4)); }
        }
    }

    static void styleTable(JTable t, boolean editable) {
        t.setBackground(CARD); t.setForeground(TEXT); t.setFont(F);
        t.setRowHeight(28); t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(60, 66, 110)); t.setSelectionForeground(Color.WHITE);
        t.setGridColor(CARD_LINE);
        t.setFocusable(editable);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setBackground(CARD); l.setForeground(new Color(124, 130, 160));
                l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                l.setBorder(new EmptyBorder(2, 6, 2, 6));
                return l;
            }
        });
        h.setBackground(CARD);
        if (editable) {
            JTextField tf = new JTextField();
            tf.setBackground(FIELD); tf.setForeground(TEXT); tf.setCaretColor(TEXT);
            tf.setBorder(new LineBorder(PURPLE));
            t.setDefaultEditor(Object.class, new DefaultCellEditor(tf));
        }
    }

    static class DotRenderer extends DefaultTableCellRenderer {
        private final boolean input;
        DotRenderer(boolean input) { this.input = input; }
        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, sel, false, r, c);
            l.setBackground(sel ? new Color(60, 66, 110) : CARD);
            l.setForeground(TEXT);
            l.setBorder(new EmptyBorder(0, 6, 0, 6));
            if (c == 0 && v != null) {
                final Color dot = GanttPanel.colorsFor(String.valueOf(v))[0];
                l.setIcon(new Icon() {
                    public void paintIcon(Component cc, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(dot); g2.fillOval(x, y, 10, 10); g2.dispose();
                    }
                    public int getIconWidth() { return 16; }
                    public int getIconHeight() { return 10; }
                });
            } else l.setIcon(null);
            return l;
        }
    }

    static class Card extends JPanel {
        Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 60));
            g.fillRoundRect(2, 5, getWidth() - 4, getHeight() - 5, 24, 24);
            g.setColor(CARD);
            g.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 24, 24);
            g.setColor(CARD_LINE);
            g.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 24, 24);
            g.dispose();
        }
    }

    static class RoundBg extends JPanel {
        private final Color bg; private final int arc;
        RoundBg(JComponent inner, Color bg, int arc) {
            super(new BorderLayout()); this.bg = bg; this.arc = arc;
            setOpaque(false); add(inner);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(bg); g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g.dispose();
        }
    }

    static class Pill extends JButton {
        private Color c1, c2, fg; private Color glow;
        Pill(String text, Color c1, Color c2, Color fg) {
            super(text);
            this.c1 = c1; this.c2 = c2; this.fg = fg;
            setFont(F); setForeground(fg);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
        }
        void setColors(Color a, Color b, Color f) { c1 = a; c2 = b; fg = f; setForeground(f); repaint(); }
        void setGlow(Color g) { glow = g; }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 12, Math.max(d.height, 30));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (glow != null) {
                for (int i = 4; i >= 1; i--) {
                    g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 14));
                    g.fillRoundRect(2 - i, 2 - i, w - 4 + 2 * i, h - 4 + 2 * i, 16 + i * 2, 16 + i * 2);
                }
            }
            Color a = c1, b = c2;
            if (getModel().isRollover()) { a = a.brighter(); b = b.brighter(); }
            if (getModel().isPressed()) { a = a.darker(); b = b.darker(); }
            g.setPaint(new GradientPaint(0, 0, a, w, 0, b));
            g.fillRoundRect(2, 2, w - 4, h - 4, 16, 16);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(fg);
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f);
            g.dispose();
        }
    }

    static class Seek extends JComponent {
        private int value = 0, max = 0;
        private Consumer<Integer> listener;
        Seek() {
            setPreferredSize(new Dimension(100, 26));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { pick(e.getX()); }
                @Override public void mouseDragged(MouseEvent e) { pick(e.getX()); }
            };
            addMouseListener(ma); addMouseMotionListener(ma);
        }
        void setListener(Consumer<Integer> l) { listener = l; }
        void setMax(int m) { max = m; repaint(); }
        void setValue(int v) { value = v; repaint(); }
        private void pick(int x) {
            if (max <= 0 || listener == null) return;
            int v = (int) Math.round((x - 8.0) / Math.max(1, getWidth() - 16) * max);
            listener.accept(Math.max(0, Math.min(max, v)));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 16, y = getHeight() / 2;
            g.setColor(FIELD); g.fillRoundRect(8, y - 3, w, 6, 6, 6);
            double f = max == 0 ? 0 : (double) value / max;
            int px = 8 + (int) (w * f);
            g.setPaint(new GradientPaint(8, 0, CYAN, 8 + w, 0, PURPLE));
            g.fillRoundRect(8, y - 3, Math.max(6, px - 8), 6, 6, 6);
            g.setColor(new Color(255, 255, 255, 60)); g.fillOval(px - 9, y - 9, 18, 18);
            g.setColor(Color.WHITE); g.fillOval(px - 6, y - 6, 12, 12);
            g.dispose();
        }
    }

    static class ChipsPanel extends JPanel {
        private List<Chip> chips = new ArrayList<>();
        private String emptyText;
        ChipsPanel(String emptyText) { this.emptyText = emptyText; setOpaque(false); }
        void setChips(List<Chip> c) { chips = c; if (c.isEmpty() && emptyText == null) emptyText = ""; repaint(); }
        void setText(String t) { emptyText = t; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (chips.isEmpty()) {
                g.setFont(F); g.setColor(new Color(93, 99, 133));
                g.drawString(emptyText == null ? "empty" : emptyText, 2, 24);
                g.dispose(); return;
            }
            Font nameF = new Font("Segoe UI", Font.BOLD, 14), infoF = new Font("Segoe UI", Font.PLAIN, 11);
            int x = 0, y = 4, h = 30, gap = 8;
            for (Chip c : chips) {
                String info = "rem " + c.rem + (c.pri == NONE ? "" : " | pri " + c.pri);
                g.setFont(nameF); int w1 = g.getFontMetrics().stringWidth(c.name);
                g.setFont(infoF); int w2 = g.getFontMetrics().stringWidth(info);
                int w = w1 + w2 + 32;
                if (x + w > getWidth() && x > 0) { x = 0; y += h + gap; }
                Color[] col = GanttPanel.colorsFor(c.name);
                g.setColor(new Color(0, 0, 0, 70)); g.fillRoundRect(x + 1, y + 3, w, h, 14, 14);
                g.setPaint(new GradientPaint(x, y, col[0], x + w, y + h, col[1]));
                g.fillRoundRect(x, y, w, h, 14, 14);
                g.setColor(Color.WHITE);
                g.setFont(nameF); g.drawString(c.name, x + 12, y + 20);
                g.setFont(infoF); g.drawString(info, x + 18 + w1, y + 20);
                x += w + gap;
            }
            g.dispose();
        }
    }

    static class Bars extends JPanel {
        private final String title, suffix;
        private String[] labels = {}; private double[] vals = {}; private double max = 1;
        private Color[][] cols = {}; private int decimals = 2;
        Bars(String title, String suffix) { this.title = title; this.suffix = suffix; setOpaque(false); }
        void setData(String[] l, double[] v, double mx, Color[][] c, int dec) {
            labels = l; vals = v; max = mx; cols = c; decimals = dec; repaint();
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(FB); g.setColor(TEXT); g.drawString(title, 0, 14);
            int y = 40, w = getWidth();
            for (int i = 0; i < labels.length; i++) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(new Color(201, 205, 228)); g.drawString(labels[i], 0, y);
                String v = String.format(Locale.US, "%." + decimals + "f", vals[i]) + suffix;
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.drawString(v, w - g.getFontMetrics().stringWidth(v), y);
                g.setColor(FIELD); g.fillRoundRect(0, y + 6, w, 9, 9, 9);
                int bw = (int) Math.max(4, w * Math.min(1, vals[i] / max));
                g.setPaint(new GradientPaint(0, 0, cols[i][0], w, 0, cols[i][1]));
                g.fillRoundRect(0, y + 6, bw, 9, 9, 9);
                y += 38;
            }
            g.dispose();
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new SchedulerGUI().setVisible(true));
    }
}
