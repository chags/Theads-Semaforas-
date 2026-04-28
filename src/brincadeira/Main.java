package brincadeira;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ponto de entrada da aplicação.
 * Aplica o look-and-feel do sistema operacional e abre a janela principal
 * na Event Dispatch Thread (EDT), como exige o Swing.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("[1] main iniciado");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("[2] L&F aplicado: " + UIManager.getLookAndFeel().getName());
        } catch (Exception e) {
            System.out.println("[2] L&F falhou: " + e.getMessage());
        }

        System.out.println("[3] agendando MainFrame na EDT...");
        SwingUtilities.invokeLater(() -> {
            System.out.println("[4] EDT: criando MainFrame");
            MainFrame frame = new MainFrame();
            System.out.println("[5] EDT: setVisible(true)");
            frame.setVisible(true);
            System.out.println("[6] EDT: janela em " + frame.getBounds());
        });
        System.out.println("[7] invokeLater retornou (EDT separada)");
    }
}
