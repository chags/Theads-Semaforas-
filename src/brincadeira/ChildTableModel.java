package brincadeira;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo de dados para a JTable que exibe as crianças.
 * Armazena a lista de threads e expõe seus atributos como colunas.
 */
public class ChildTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {"ID", "Tb (ms)", "Td (ms)", "Status"};

    // Lista de threads criança cadastradas
    private final List<Child> criancas = new ArrayList<>();

    /** Adiciona uma nova criança à tabela e notifica a JTable. */
    public void addChild(Child crianca) {
        criancas.add(crianca);
        int linha = criancas.size() - 1;
        fireTableRowsInserted(linha, linha);
    }

    /**
     * Atualiza as linhas exibidas sem resetar seleção nem cabeçalho.
     * Chamado periodicamente pelo Timer da MainFrame.
     */
    public void refresh() {
        if (!criancas.isEmpty()) {
            fireTableRowsUpdated(0, criancas.size() - 1);
        }
    }

    /** Retorna a lista somente-leitura de crianças (usado pelo GamePanel). */
    public List<Child> getCriancas() { return Collections.unmodifiableList(criancas); }

    @Override public int    getRowCount()           { return criancas.size(); }
    @Override public int    getColumnCount()         { return COLUNAS.length; }
    @Override public String getColumnName(int col)   { return COLUNAS[col]; }

    @Override
    public Object getValueAt(int linha, int coluna) {
        Child c = criancas.get(linha);
        switch (coluna) {
            case 0: return c.getChildId();    // identificador
            case 1: return c.getPlayTime();   // Tb
            case 2: return c.getRestTime();   // Td
            case 3: return c.getChildState(); // estado atual
            default: return null;
        }
    }
}
