package co.eci.snake.ui.legacy;

import co.eci.snake.concurrency.SnakeRunner;
import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class SnakeApp extends JFrame {

    private final Board board;
    private final GamePanel gamePanel;
    private final JButton actionButton;
    private final GameClock clock;
    private final List<Snake> snakes = new java.util.ArrayList<>();

    public SnakeApp() {
        super("The Snake Race");
        this.board = new Board(35, 28);

        int N = Integer.getInteger("snakes", 2);
        for (int i = 0; i < N; i++) {
            int x = 2 + (i * 3) % board.width();
            int y = 2 + (i * 2) % board.height();
            var dir = Direction.values()[i % Direction.values().length];
            snakes.add(Snake.of(x, y, dir));
        }

        this.gamePanel = new GamePanel(board, () -> snakes);
        this.actionButton = new JButton("Action");

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        this.clock = new GameClock(60, () -> SwingUtilities.invokeLater(gamePanel::repaint));

        var exec = Executors.newVirtualThreadPerTaskExecutor();
        snakes.forEach(s -> exec.submit(new SnakeRunner(s, board, clock)));

        actionButton.addActionListener((ActionEvent e) -> togglePause());

        gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("SPACE"), "pause");
        gamePanel.getActionMap().put("pause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });

        setVisible(true);
        clock.start();
    }

  /* =======================
     MÉTODOS DEL LABORATORIO
     ======================= */

    private void togglePause() {
        if ("Action".equals(actionButton.getText())) {
            actionButton.setText("Resume");
            clock.pause();

            // Leer estado solo cuando el reloj está detenido
            showStats();
        } else {
            actionButton.setText("Action");
            clock.resume();
        }
    }

    private void showStats() {
        synchronized (snakes) {
            Snake best = snakes.stream()
                    .max((a, b) -> Integer.compare(a.length(), b.length()))
                    .orElse(null);

            Snake worst = snakes.stream()
                    .min((a, b) -> Integer.compare(a.length(), b.length()))
                    .orElse(null);

            System.out.println("===== STATS =====");
            if (best != null) {
                System.out.println("Mejor serpiente (más larga): " + best.length());
            }
            if (worst != null) {
                System.out.println("Peor serpiente (más corta): " + worst.length());
            }
            System.out.println("=================");
        }
    }

  /* =======================
           GAME PANEL
     ======================= */

    public static final class GamePanel extends JPanel {
        private final Board board;
        private final Supplier snakesSupplier;
        private final int cell = 20;

        @FunctionalInterface
        public interface Supplier {
            List<Snake> get();
        }

        public GamePanel(Board board, Supplier snakesSupplier) {
            this.board = board;
            this.snakesSupplier = snakesSupplier;
            setPreferredSize(new Dimension(board.width() * cell + 1,
                    board.height() * cell + 40));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(220, 220, 220));
            for (int x = 0; x <= board.width(); x++)
                g2.drawLine(x * cell, 0, x * cell, board.height() * cell);
            for (int y = 0; y <= board.height(); y++)
                g2.drawLine(0, y * cell, board.width() * cell, y * cell);

            // Serpientes
            var snakes = snakesSupplier.get();
            int idx = 0;
            for (Snake s : snakes) {
                var body = s.snapshot().toArray(new Position[0]);
                for (int i = 0; i < body.length; i++) {
                    var p = body[i];
                    Color base = (idx == 0)
                            ? new Color(0, 170, 0)
                            : new Color(0, 160, 180);
                    int shade = Math.max(0, 40 - i * 4);
                    g2.setColor(new Color(
                            Math.min(255, base.getRed() + shade),
                            Math.min(255, base.getGreen() + shade),
                            Math.min(255, base.getBlue() + shade)));
                    g2.fillRect(p.x() * cell + 2, p.y() * cell + 2,
                            cell - 4, cell - 4);
                }
                idx++;
            }
            g2.dispose();
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(SnakeApp::new);
    }
}
