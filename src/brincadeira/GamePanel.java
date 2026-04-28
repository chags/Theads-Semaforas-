package brincadeira;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Painel de simulação visual.
 *
 * Renderiza a 30 fps usando Java2D:
 *   - Cesto no centro com os slots de bola visíveis
 *   - Cada criança como sprite animada (menino_sprite.png) ao redor do cesto
 *   - Quando PLAYING: cicla os 3 frames de drible (~8 fps)
 *   - Quando aguardando/descansando: frame estático
 */
public class GamePanel extends JPanel {

    private final ChildTableModel modelo;
    private BallBasket cesto;

    // Cores dos estados (para halo de fundo do sprite)
    private static final Color COR_BRINCANDO     = new Color(46,  204, 113);
    private static final Color COR_AGUARD_BOLA   = new Color(241, 196,  15);
    private static final Color COR_AGUARD_ESPACO = new Color(230, 126,  34);
    private static final Color COR_DESCANSANDO   = new Color(52,  152, 219);

    // Sprite sheet: 3 frames do menino driblando
    private BufferedImage[] spriteFrames = null;

    // Sprite sheet: 2 frames do menino descansando
    private BufferedImage[] spriteDescansando = null;

    // Sprite sheet: 3 frames do menino aguardando bola
    private BufferedImage[] spriteAguardandoBola = null;

    // Contador de frame global para drible (3 frames) e descanso (2 frames)
    private int frameAtual   = 0;
    private int frameCounter = 0;
    private static final int FRAMES_POR_UPDATE = 4; // ~8 fps a 30 fps de repaint

    public GamePanel(ChildTableModel modelo) {
        this.modelo = modelo;
        carregarSprite();
        carregarSpriteDescansando();
        carregarSpriteAguardandoBola();

        new Timer(33, e -> {
            frameCounter++;
            if (frameCounter >= FRAMES_POR_UPDATE) {
                frameCounter = 0;
                frameAtual++;  // cicla livre; cada animação usa % do seu total de frames
            }
            repaint();
        }).start();
    }

    /** Carrega e divide o sprite sheet de aguardar bola em 3 frames (cortes em col 487 e 1039). */
    private void carregarSpriteAguardandoBola() {
        try {
            BufferedImage sheet = ImageIO.read(new File("aguardando_bola.png"));
            int h = sheet.getHeight();
            int[] cortes = {0, 487, 1039, sheet.getWidth()};
            spriteAguardandoBola = new BufferedImage[3];
            for (int i = 0; i < 3; i++)
                spriteAguardandoBola[i] = sheet.getSubimage(cortes[i], 0, cortes[i+1] - cortes[i], h);
            System.out.println("[Sprite] AguardandoBola carregado: 3 frames");
        } catch (Exception e) {
            System.out.println("[Sprite] AguardandoBola falhou: " + e.getMessage());
            spriteAguardandoBola = null;
        }
    }

    /** Carrega e divide o sprite sheet de descanso em 2 frames. */
    private void carregarSpriteDescansando() {
        try {
            BufferedImage sheet = ImageIO.read(new File("decansando.png"));
            int fw = sheet.getWidth() / 2;
            int fh = sheet.getHeight();
            spriteDescansando = new BufferedImage[2];
            spriteDescansando[0] = sheet.getSubimage(0,  0, fw, fh);
            spriteDescansando[1] = sheet.getSubimage(fw, 0, sheet.getWidth() - fw, fh);
            System.out.println("[Sprite] Descansando carregado: " + fw + "x" + fh + " por frame (2 frames)");
        } catch (Exception e) {
            System.out.println("[Sprite] Descansando falhou: " + e.getMessage());
            spriteDescansando = null;
        }
    }

    /** Carrega e divide o sprite sheet em 3 frames. */
    private void carregarSprite() {
        try {
            BufferedImage sheet = ImageIO.read(new File("menino_sprite.png"));
            int fw = sheet.getWidth() / 3;
            int fh = sheet.getHeight();
            spriteFrames = new BufferedImage[3];
            for (int i = 0; i < 3; i++) {
                int x = i * fw;
                int w = (i == 2) ? sheet.getWidth() - x : fw; // último frame pega o restante
                spriteFrames[i] = sheet.getSubimage(x, 0, w, fh);
            }
            System.out.println("[Sprite] Carregado: " + fw + "x" + fh + " por frame");
        } catch (Exception e) {
            System.out.println("[Sprite] Falhou (" + e.getMessage() + ") — usando personagem geométrico");
            spriteFrames = null;
        }
    }

    /** Chamado pela MainFrame quando o cesto é criado (na primeira criança adicionada). */
    public void setCesto(BallBasket cesto) { this.cesto = cesto; }

    // ── Pintura principal ─────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth(), h = getHeight();
        desenharFundo(g2, w, h);
        desenharCesto(g2, w, h);
        desenharCriancas(g2, w, h);
    }

    // ── Fundo ─────────────────────────────────────────────────────────────────

    private void desenharFundo(Graphics2D g2, int w, int h) {
        GradientPaint ceu = new GradientPaint(
            0, 0,        new Color(135, 206, 235),
            0, h * 0.7f, new Color(210, 240, 255));
        g2.setPaint(ceu);
        g2.fillRect(0, 0, w, (int)(h * 0.72));

        g2.setColor(new Color(86, 180, 84));
        g2.fillRect(0, (int)(h * 0.72), w, h);

        g2.setColor(new Color(60, 150, 58));
        g2.fillRect(0, (int)(h * 0.72), w, 6);
    }

    // ── Cesto ─────────────────────────────────────────────────────────────────

    private void desenharCesto(Graphics2D g2, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;

        if (cesto == null) {
            g2.setColor(new Color(80, 80, 80, 180));
            g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 15));
            String msg = "Adicione uma criança para iniciar a simulação";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy);
            return;
        }

        int k     = cesto.getCapacity();
        int bolas = cesto.getCount();

        int cestoW = Math.max(110, Math.min(k * 28 + 20, w / 3));
        int cestoH = 80;

        RoundRectangle2D corpo = new RoundRectangle2D.Double(
            cx - cestoW / 2.0, cy - cestoH / 2.0, cestoW, cestoH, 18, 18);

        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new RoundRectangle2D.Double(
            cx - cestoW / 2.0 + 4, cy - cestoH / 2.0 + 6, cestoW, cestoH, 18, 18));

        g2.setColor(new Color(180, 120, 50));
        g2.fill(corpo);
        g2.setColor(new Color(120, 70, 20));
        g2.setStroke(new BasicStroke(3));
        g2.draw(corpo);

        int slotR   = Math.max(10, Math.min(20, (cestoW - 20) / (2 * k) - 2));
        int spacing = slotR * 2 + 6;
        int totalW  = k * spacing - 6;
        int startX  = cx - totalW / 2 + slotR;

        for (int i = 0; i < k; i++) {
            int sx = startX + i * spacing;
            int sy = cy;

            if (i < bolas) {
                g2.setColor(new Color(255, 220, 50));
                g2.fillOval(sx - slotR, sy - slotR, slotR * 2, slotR * 2);
                g2.setColor(new Color(255, 255, 180, 120));
                g2.fillOval(sx - slotR + 3, sy - slotR + 3, slotR - 2, slotR - 2);
                g2.setColor(new Color(200, 160, 20));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(sx - slotR, sy - slotR, slotR * 2, slotR * 2);
            } else {
                g2.setColor(new Color(100, 60, 20, 150));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{4, 3}, 0));
                g2.drawOval(sx - slotR, sy - slotR, slotR * 2, slotR * 2);
                g2.setStroke(new BasicStroke(1.5f));
            }
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        String label = String.format("Cesto  %d / %d", bolas, k);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + cestoH / 2 + 22);
    }

    // ── Crianças ──────────────────────────────────────────────────────────────

    private void desenharCriancas(Graphics2D g2, int w, int h) {
        List<Child> criancas = modelo.getCriancas();
        if (criancas.isEmpty() || cesto == null) return;

        int cx = w / 2;
        int cy = h / 2;

        double rBase = Math.min(w, h) * 0.36;
        int n = criancas.size();
        double escala = Math.max(0.55, 1.0 - n * 0.03);

        for (int i = 0; i < n; i++) {
            Child c      = criancas.get(i);
            ChildState e = c.getChildState();

            double angulo = 2 * Math.PI * i / n - Math.PI / 2;
            double r = (e == ChildState.WAITING_FOR_BALL || e == ChildState.WAITING_TO_PUT)
                       ? rBase * 0.55 : rBase;

            int px = cx + (int)(r * Math.cos(angulo));
            int py = cy + (int)(r * Math.sin(angulo));

            if (e == ChildState.WAITING_FOR_BALL || e == ChildState.WAITING_TO_PUT) {
                g2.setColor(new Color(120, 120, 120, 120));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6, 5}, 0));
                g2.drawLine(px, py, cx, cy);
                g2.setStroke(new BasicStroke(1.5f));
            }

            desenharPersonagem(g2, c, i, px, py, e, escala);
        }
    }

    /**
     * Desenha o personagem usando sprite animada (se carregada) ou fallback geométrico.
     * Frame de animação:
     *   PLAYING          → cicla os 3 frames (desfasado por índice para não ficarem sincronizados)
     *   WAITING_TO_PUT   → frame 2 (de pé com bola)
     *   WAITING_FOR_BALL → frame 0 (abaixado procurando)
     *   RESTING          → frame 0 semitransparente
     */
    private void desenharPersonagem(Graphics2D g2, Child c, int indice,
                                    int px, int py, ChildState estado, double escala) {
        if (spriteFrames != null) {
            desenharSprite(g2, c, indice, px, py, estado, escala);
        } else {
            desenharGeometrico(g2, c, px, py, estado, escala);
        }
    }

    // ── Sprite ────────────────────────────────────────────────────────────────

    private void desenharSprite(Graphics2D g2, Child c, int indice,
                                int px, int py, ChildState estado, double escala) {

        boolean descansando = estado == ChildState.RESTING && spriteDescansando != null;

        // Escolhe imagem e frame conforme estado
        BufferedImage imgAtual;
        if (descansando) {
            int frame = (frameAtual + indice) % spriteDescansando.length;
            imgAtual = spriteDescansando[frame];
        } else if (estado == ChildState.WAITING_FOR_BALL && spriteAguardandoBola != null) {
            int frame = (frameAtual + indice) % spriteAguardandoBola.length;
            imgAtual = spriteAguardandoBola[frame];
        } else {
            int frame;
            switch (estado) {
                case PLAYING:        frame = (frameAtual + indice) % spriteFrames.length; break;
                case WAITING_TO_PUT: frame = 2; break;
                default:             frame = 0;
            }
            imgAtual = spriteFrames[frame];
        }

        int renderH = (int)(140 * escala);
        int renderW = (int)(renderH * (imgAtual.getWidth() / (double) imgAtual.getHeight()));

        Color cor = corDoEstado(estado);

        // Halo colorido atrás do sprite (indica estado)
        g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 70));
        g2.fillOval(px - renderW / 2 - 6, py - renderH / 2 - 4,
                    renderW + 12, renderH + 8);

        g2.drawImage(imgAtual,
                     px - renderW / 2, py - renderH / 2,
                     renderW, renderH, this);

        // Nome e estado abaixo do sprite
        int baseY = py + renderH / 2 + 2;
        int fontSize = Math.max(9, (int)(12 * escala));
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(20, 20, 20));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String nome = c.getChildId();
        if (fm.stringWidth(nome) > renderW * 2) nome = nome.substring(0, 6) + "…";
        g2.drawString(nome, px - fm.stringWidth(nome) / 2, baseY + fontSize);

        int fSize2 = Math.max(8, (int)(10 * escala));
        g2.setColor(cor.darker());
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fSize2));
        fm = g2.getFontMetrics();
        String estadoStr = estadoCurto(estado);
        g2.drawString(estadoStr, px - fm.stringWidth(estadoStr) / 2, baseY + fontSize + fSize2 + 2);
    }

    // ── Fallback geométrico (caso sprite não carregue) ────────────────────────

    private void desenharGeometrico(Graphics2D g2, Child c,
                                    int px, int py, ChildState estado, double escala) {
        Color cor = corDoEstado(estado);

        int cabecaR = (int)(16 * escala);
        int corpoW  = (int)(22 * escala);
        int corpoH  = (int)(28 * escala);
        int bolaR   = (int)(10 * escala);
        int cabecaY = py - corpoH / 2 - cabecaR * 2 + 4;

        g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 55));
        g2.fillOval(px - cabecaR - corpoW, cabecaY - 4,
                    cabecaR * 2 + corpoW * 2, cabecaR * 2 + corpoH + 14);

        g2.setColor(cor);
        g2.fillOval(px - corpoW / 2, py - corpoH / 2, corpoW, corpoH);
        g2.setColor(cor.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(px - corpoW / 2, py - corpoH / 2, corpoW, corpoH);

        g2.setColor(new Color(255, 218, 168));
        g2.fillOval(px - cabecaR, cabecaY, cabecaR * 2, cabecaR * 2);
        g2.setColor(cor.darker());
        g2.drawOval(px - cabecaR, cabecaY, cabecaR * 2, cabecaR * 2);

        g2.setColor(Color.BLACK);
        int olhoY = cabecaY + cabecaR - 3;
        if (estado == ChildState.RESTING) {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(px - cabecaR / 2 - 1, olhoY, px - cabecaR / 5, olhoY);
            g2.drawLine(px + cabecaR / 5,     olhoY, px + cabecaR / 2 + 1, olhoY);
        } else {
            int olhoRaio = Math.max(2, (int)(3 * escala));
            g2.fillOval(px - cabecaR / 2 - olhoRaio, olhoY - olhoRaio, olhoRaio * 2, olhoRaio * 2);
            g2.fillOval(px + cabecaR / 5,             olhoY - olhoRaio, olhoRaio * 2, olhoRaio * 2);
        }

        if (estado == ChildState.PLAYING || estado == ChildState.WAITING_TO_PUT) {
            int bx = px + corpoW / 2 + 4;
            int by = py - bolaR;
            g2.setColor(new Color(255, 220, 50));
            g2.fillOval(bx, by, bolaR * 2, bolaR * 2);
            g2.setColor(new Color(255, 255, 200, 140));
            g2.fillOval(bx + 2, by + 2, bolaR - 2, bolaR - 2);
            g2.setColor(new Color(200, 160, 20));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(bx, by, bolaR * 2, bolaR * 2);
        }

        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(30, 30, 30));
        int fontSize = Math.max(9, (int)(12 * escala));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String nome = c.getChildId();
        if (fm.stringWidth(nome) > corpoW * 3) nome = nome.substring(0, 6) + "…";
        g2.drawString(nome, px - fm.stringWidth(nome) / 2, py + corpoH / 2 + fontSize + 2);

        int fSize2 = Math.max(8, (int)(10 * escala));
        g2.setColor(cor.darker().darker());
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fSize2));
        fm = g2.getFontMetrics();
        String estadoStr = estadoCurto(estado);
        g2.drawString(estadoStr, px - fm.stringWidth(estadoStr) / 2, py + corpoH / 2 + fontSize + fSize2 + 4);
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private Color corDoEstado(ChildState estado) {
        switch (estado) {
            case PLAYING:          return COR_BRINCANDO;
            case WAITING_FOR_BALL: return COR_AGUARD_BOLA;
            case WAITING_TO_PUT:   return COR_AGUARD_ESPACO;
            case RESTING:          return COR_DESCANSANDO;
            default:               return Color.GRAY;
        }
    }

    private String estadoCurto(ChildState estado) {
        switch (estado) {
            case PLAYING:          return "brincando";
            case WAITING_FOR_BALL: return "aguard. bola";
            case WAITING_TO_PUT:   return "aguard. espaço";
            case RESTING:          return "descansando";
            default:               return "";
        }
    }
}
