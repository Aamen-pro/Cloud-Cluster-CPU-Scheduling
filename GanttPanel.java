import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class GanttPanel extends JPanel {

    public static class Seg {
        public final String label;
        public final int start, end;
        public Seg(String label, int start, int end) {
            this.label = label; this.start = start; this.end = end;
        }
    }

    private static final Color[][] PALETTE = {
            {new Color(0, 210, 255), new Color(58, 123, 213)},
            {new Color(248, 87, 166), new Color(255, 88, 88)},
            {new Color(249, 212, 35), new Color(255, 138, 0)},
            {new Color(0, 230, 160), new Color(0, 160, 90)},
            {new Color(179, 136, 255), new Color(98, 70, 234)},
            {new Color(255, 171, 145), new Color(255, 112, 67)},
            {new Color(128, 222, 234), new Color(0, 151, 167)},
            {new Color(244, 143, 177), new Color(173, 20, 87)}
    };
    private static final Map<String, Integer> COLOR_INDEX = new HashMap<>();

    public static void registerProcesses(List<String> names) {
        COLOR_INDEX.clear();
        for (int i = 0; i < names.size(); i++) COLOR_INDEX.put(names.get(i), i);
    }

    public static Color[] colorsFor(String name) {
        Integer i = COLOR_INDEX.get(name);
        int idx = i == null ? Math.abs(name.hashCode()) : i;
        return PALETTE[idx % PALETTE.length];
    }

    private List<Seg> segs = new ArrayList<>();
    private int total = 1;
    private double playhead = 0;
    private double target = 0;
    private double reveal = 1;
    private final javax.swing.Timer timer;

    public GanttPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(500, 120));
        timer = new javax.swing.Timer(16, e -> tick());
    }

    public void setSegments(List<Seg> segments, int totalTime) {
        this.segs = new ArrayList<>(segments);
        this.total = Math.max(1, totalTime);
        this.reveal = 0;
        this.playhead = 0;
        this.target = 0;
        timer.start();
        repaint();
    }

    public void setTime(double t) {
        target = t;
        if (!timer.isRunning()) timer.start();
    }

    private void tick() {
        boolean moving = false;
        if (reveal < 1) { reveal = Math.min(1, reveal + 0.05); moving = true; }
        double diff = target - playhead;
        if (Math.abs(diff) > 0.002) { playhead += diff * 0.22; moving = true; }
        else playhead = target;
        repaint();
        if (!moving) timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padL = 6, padR = 6, top = 18, barH = 64;
        double w = getWidth() - padL - padR;
        double unit = w / total;

        for (Seg s : segs) {
            double x = padL + s.start * unit;
            double bw = (s.end - s.start) * unit - 3;
            double shown = bw * ease(reveal);
            if (shown < 1) continue;
            RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, top, shown, barH, 12, 12);
            boolean future = s.start >= playhead + 0.001;
            Composite old = g.getComposite();
            if (future) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));

            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new RoundRectangle2D.Double(x + 1, top + 4, shown, barH, 12, 12));

            if (s.label.equals("CS")) {
                g.setColor(new Color(42, 46, 66));
                g.fill(r);
                Shape oldClip = g.getClip();
                g.clip(r);
                g.setColor(new Color(64, 70, 96));
                g.setStroke(new BasicStroke(4f));
                for (double k = x - barH; k < x + shown + barH; k += 12)
                    g.draw(new Line2D.Double(k, top + barH, k + barH, top));
                g.setClip(oldClip);
            } else if (s.label.equals("IDLE")) {
                g.setColor(new Color(30, 34, 54));
                g.fill(r);
                g.setColor(new Color(110, 116, 150));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                        new float[]{5f, 4f}, 0f));
                g.draw(r);
            } else {
                Color[] c = colorsFor(s.label);
                g.setPaint(new GradientPaint((float) x, top, c[0], (float) (x + shown), top + barH, c[1]));
                g.fill(r);
                g.setPaint(new GradientPaint(0, top, new Color(255, 255, 255, 60), 0, top + barH / 2f,
                        new Color(255, 255, 255, 0)));
                g.fill(new RoundRectangle2D.Double(x, top, shown, barH / 2.0, 12, 12));
            }

            if (reveal > 0.85) {
                g.setFont(new Font("Segoe UI", Font.BOLD, s.label.equals("CS") ? 12 : 15));
                FontMetrics fm = g.getFontMetrics();
                if (fm.stringWidth(s.label) < bw - 4) {
                    g.setColor(Color.WHITE);
                    g.drawString(s.label, (float) (x + (bw - fm.stringWidth(s.label)) / 2),
                            top + (barH + fm.getAscent() - fm.getDescent()) / 2f);
                }
            }
            g.setComposite(old);
        }

        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.setColor(new Color(124, 130, 160));
        FontMetrics fm = g.getFontMetrics();
        int step = Math.max(1, (int) Math.ceil(total / (w / 28.0)));
        for (int t = 0; t <= total; t += step) {
            String txt = String.valueOf(t);
            g.drawString(txt, (float) (padL + t * unit - fm.stringWidth(txt) / 2.0), top + barH + 16);
        }

        double px = padL + playhead * unit;
        for (int i = 4; i >= 1; i--) {
            g.setColor(new Color(255, 255, 255, 18 * (5 - i)));
            g.setStroke(new BasicStroke(1 + i * 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(px, top - 8, px, top + barH + 6));
        }
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(px, top - 8, px, top + barH + 6));
        g.dispose();
    }

    private static double ease(double t) { return 1 - Math.pow(1 - t, 3); }
}
