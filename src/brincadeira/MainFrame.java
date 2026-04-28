package brincadeira;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Janela principal da aplicação.
 *
 * Layout:
 *   NORTH  — painel de configuração (K, label do cesto, botão adicionar)
 *   CENTER — split horizontal:
 *              LEFT  = GamePanel (simulação visual, área principal)
 *              RIGHT = split vertical: tabela (cima) + log (baixo)
 *   SOUTH  — legenda de cores
 */
public class MainFrame extends JFrame {

    // Cesto compartilhado entre todas as crianças
    private BallBasket cesto;

    // Modelo da tabela e painel visual compartilham a mesma lista de crianças
    private final ChildTableModel modeloTabela = new ChildTableModel();
    private final GamePanel       gamePanel    = new GamePanel(modeloTabela);

    // Área de log de eventos
    private final JTextArea areaLog = new JTextArea();

    // Exibe o estado atual do cesto (X/K bolas) no painel superior
    private final JLabel labelCesto = new JLabel("Cesto: não criado");

    // Spinner para definir K antes de iniciar
    private final JSpinner spinnerCapacidade = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));

    // Gerenciador de áudio
    private final GameAudio audio = GameAudio.getInstancia();

    // Contador para IDs padrão ("Criança 1", "Criança 2" …)
    private int contadorCriancas = 0;

    // Quantas crianças já foram criadas com bola (M do enunciado; M≥1 evita deadlock)
    private int criancasComBola = 0;

    public MainFrame() {
        super("Brincadeira de Crianças — Semáforos");
        construirInterface();
        iniciarTimerDeAtualizacao();

        // System.exit(0) encerra todas as threads daemon ao fechar a janela
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                audio.dispose();
                System.exit(0);
            }
        });

        setSize(1100, 720);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
    }

    // ── Construção da interface ───────────────────────────────────────────────

    private void construirInterface() {
        setLayout(new BorderLayout(4, 4));
        add(criarPainelSuperior(), BorderLayout.NORTH);
        add(criarPainelCentral(),  BorderLayout.CENTER);
        add(criarLegenda(),        BorderLayout.SOUTH);
    }

    /** Painel superior: K, status do cesto, botão de adicionar criança. */
    private JPanel criarPainelSuperior() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Configuração"));

        painel.add(new JLabel("Capacidade do cesto (K):"));
        painel.add(spinnerCapacidade);
        painel.add(Box.createHorizontalStrut(15));
        painel.add(labelCesto);
        painel.add(Box.createHorizontalStrut(30));

        JButton btnAdicionar = new JButton("+ Adicionar Criança");
        btnAdicionar.setFont(btnAdicionar.getFont().deriveFont(Font.BOLD));
        btnAdicionar.addActionListener(e -> exibirDialogoCrianca());
        painel.add(btnAdicionar);

        return painel;
    }

    /**
     * Painel central: GamePanel à esquerda (simulação visual) e
     * tabela + log à direita, separados por um JSplitPane horizontal.
     */
    private JSplitPane criarPainelCentral() {
        gamePanel.setBorder(BorderFactory.createTitledBorder("Simulação Visual"));

        // --- Painel direito: tabela + log ---
        JTable tabela = new JTable(modeloTabela);
        tabela.setRowHeight(26);
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tabela.setDefaultRenderer(Object.class, new RenderizadorDeEstado());
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(200);

        JScrollPane scrollTabela = new JScrollPane(tabela);
        scrollTabela.setBorder(BorderFactory.createTitledBorder("Crianças"));

        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));

        JSplitPane direitoSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabela, scrollLog);
        direitoSplit.setResizeWeight(0.55);
        direitoSplit.setDividerLocation(300);

        // --- Split horizontal principal ---
        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gamePanel, direitoSplit);
        principal.setResizeWeight(0.62); // game panel recebe 62% da largura
        principal.setDividerLocation(680);
        return principal;
    }

    /** Barra de legenda de cores na parte inferior. */
    private JPanel criarLegenda() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        painel.setBorder(BorderFactory.createEtchedBorder());
        painel.add(new JLabel("Legenda:"));
        painel.add(amostrarCor(new Color(46,  204, 113), "Brincando"));
        painel.add(amostrarCor(new Color(241, 196,  15), "Aguardando bola"));
        painel.add(amostrarCor(new Color(230, 126,  34), "Aguardando espaço"));
        painel.add(amostrarCor(new Color(52,  152, 219), "Descansando"));
        return painel;
    }

    private JPanel amostrarCor(Color cor, String rotulo) {
        JPanel amostra = new JPanel();
        amostra.setBackground(cor);
        amostra.setPreferredSize(new Dimension(14, 14));
        amostra.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel w = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        w.add(amostra);
        w.add(new JLabel(rotulo));
        return w;
    }

    // ── Timer de atualização ─────────────────────────────────────────────────

    /** Atualiza tabela e label do cesto a cada 120 ms na EDT. */
    private void iniciarTimerDeAtualizacao() {
        new Timer(120, e -> {
            modeloTabela.refresh();
            if (cesto != null) {
                labelCesto.setText(String.format(
                    "Cesto: %d / %d bolas", cesto.getCount(), cesto.getCapacity()));
            }
        }).start();
    }

    // ── Diálogo de criação de criança ────────────────────────────────────────

    /** Exibe formulário para configurar e iniciar uma nova thread criança. */
    private void exibirDialogoCrianca() {
        contadorCriancas++;

        JTextField campId    = new JTextField("Criança " + contadorCriancas, 16);
        JCheckBox  checkBola = new JCheckBox("Com bola inicial", false);
        JSpinner   spinnerTb = new JSpinner(new SpinnerNumberModel(2000, 100, 60000, 100));
        JSpinner   spinnerTd = new JSpinner(new SpinnerNumberModel(1000, 100, 60000, 100));

        // A primeira criança DEVE ter bola: sem ela, todas as threads bloqueariam
        // em balls.acquire() com cesto vazio → deadlock garantido (M=0).
        if (criancasComBola == 0) {
            checkBola.setSelected(true);
            checkBola.setEnabled(false);
            checkBola.setToolTipText("A primeira criança deve ter bola (M≥1 evita deadlock)");
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("ID da criança:"), g);
        g.gridx = 1;               form.add(campId, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Status inicial:"), g);
        g.gridx = 1;               form.add(checkBola, g);
        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Tempo de brincadeira — Tb (ms):"), g);
        g.gridx = 1;               form.add(spinnerTb, g);
        g.gridx = 0; g.gridy = 3; form.add(new JLabel("Tempo de descanso — Td (ms):"), g);
        g.gridx = 1;               form.add(spinnerTd, g);

        int resposta = JOptionPane.showConfirmDialog(
                this, form, "Adicionar Nova Criança",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resposta != JOptionPane.OK_OPTION) {
            contadorCriancas--;
            return;
        }

        String  id      = campId.getText().trim().isEmpty() ? "Criança " + contadorCriancas : campId.getText().trim();
        boolean temBola = checkBola.isSelected();
        long    tb      = ((Number) spinnerTb.getValue()).longValue();
        long    td      = ((Number) spinnerTd.getValue()).longValue();

        // Cesto criado na primeira criança; K torna-se fixo a partir daí
        if (cesto == null) {
            int k = (Integer) spinnerCapacidade.getValue();
            cesto = new BallBasket(k);
            spinnerCapacidade.setEnabled(false);
            gamePanel.setCesto(cesto); // informa o painel visual
            audio.carregarEfeito("bola", "basketball.wav");
            log("[Sistema] Cesto criado com capacidade K=" + k);
        }

        if (temBola) criancasComBola++;

        Child crianca = new Child(id, temBola, tb, td, cesto, this::log,
                                  () -> audio.tocarEfeito("bola"));
        modeloTabela.addChild(crianca);
        log("[Sistema] '" + id + "' entrou na brincadeira" + (temBola ? " (com bola)" : " (sem bola)"));
        crianca.start();
    }

    // ── Log ──────────────────────────────────────────────────────────────────

    /** Acrescenta uma linha timestampada ao log; limita a ~3000 linhas. */
    private void log(String mensagem) {
        String hora = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        areaLog.append(hora + "  " + mensagem + "\n");
        if (areaLog.getLineCount() > 3500) {
            try {
                int fim = areaLog.getLineEndOffset(areaLog.getLineCount() - 3000);
                areaLog.replaceRange("", 0, fim);
            } catch (Exception ignorado) {}
        }
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // ── Renderizador de células da tabela ─────────────────────────────────────

    /**
     * Coloriza cada linha da tabela conforme o estado da criança.
     * Lê o estado da coluna 3 e aplica a cor em toda a linha.
     */
    private static class RenderizadorDeEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable tabela, Object valor,
                boolean selecionado, boolean foco, int linha, int coluna) {

            super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
            setHorizontalAlignment(coluna == 0 ? LEFT : CENTER);

            if (!selecionado) {
                Object estado = tabela.getModel().getValueAt(linha, 3);
                if (estado instanceof ChildState) {
                    switch ((ChildState) estado) {
                        case PLAYING:          setBackground(new Color(144, 238, 144)); break;
                        case WAITING_FOR_BALL: setBackground(new Color(255, 255, 153)); break;
                        case WAITING_TO_PUT:   setBackground(new Color(255, 178, 102)); break;
                        case RESTING:          setBackground(new Color(173, 216, 230)); break;
                        default:               setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }
    }
}
