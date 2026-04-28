package brincadeira;

/**
 * Estados possíveis de uma thread criança durante a simulação.
 */
public enum ChildState {
    PLAYING("Brincando"),
    WAITING_FOR_BALL("Aguardando bola no cesto"),
    WAITING_TO_PUT("Aguardando espaço no cesto"),
    RESTING("Descansando");

    private final String label;

    ChildState(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
