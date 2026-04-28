package brincadeira;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/**
 * Thread que representa uma criança na simulação.
 *
 * Ciclo de vida (eterno):
 *   - Se iniciou COM bola: brinca → coloca bola → descansa → pega bola → repete
 *   - Se iniciou SEM bola: pega bola → brinca → coloca bola → descansa → repete
 *
 * As esperas por recurso (pegar/colocar bola) são feitas via semáforo.
 * Os tempos de brincadeira (Tb) e descanso (Td) usam espera ativa (busy wait).
 */
public class Child extends Thread {
    private final String childId;
    private final boolean startedWithBall;
    private final long playTime; // Tb em milissegundos
    private final long restTime; // Td em milissegundos
    private final BallBasket basket;
    private final Consumer<String> logCallback;
    private final Runnable audioCallback;

    // Lido pela interface gráfica; volatile garante visibilidade entre threads
    private volatile ChildState state;

    public Child(String childId, boolean startedWithBall, long playTime, long restTime,
                 BallBasket basket, Consumer<String> logCallback, Runnable audioCallback) {
        this.childId         = childId;
        this.startedWithBall = startedWithBall;
        this.playTime        = playTime;
        this.restTime        = restTime;
        this.basket          = basket;
        this.logCallback     = logCallback;
        this.audioCallback   = audioCallback;
        this.state           = startedWithBall ? ChildState.PLAYING : ChildState.WAITING_FOR_BALL;

        setDaemon(true);                    // morre junto com a JVM ao fechar a janela
        setPriority(Thread.MIN_PRIORITY);   // cede CPU à Event Dispatch Thread do Swing
        setName("Child-" + childId);
    }

    @Override
    public void run() {
        try {
            // Fase inicial: quem já tem bola começa brincando
            if (startedWithBall) {
                brincar();
                colocarBola();
                descansar();
            }

            // Loop eterno: pega bola → brinca → devolve → descansa
            while (!isInterrupted()) {
                pegarBola();
                brincar();
                colocarBola();
                descansar();
            }
        } catch (InterruptedException e) {
            // Thread interrompida (ex: encerramento da aplicação)
            Thread.currentThread().interrupt();
        }
    }

    // ── Ações da criança ─────────────────────────────────────────────────────

    private void brincar() throws InterruptedException {
        setState(ChildState.PLAYING);
        log("começou a brincar");
        esperaAtiva(playTime); // tempo Tb de brincadeira
        log("terminou de brincar");
    }

    /** Bloqueia no semáforo se o cesto estiver cheio. */
    private void colocarBola() throws InterruptedException {
        setState(ChildState.WAITING_TO_PUT);
        log("quer colocar a bola no cesto");
        basket.put();          // ← bloqueia aqui se cesto cheio
        audioCallback.run();
        log("colocou a bola no cesto");
    }

    private void descansar() throws InterruptedException {
        setState(ChildState.RESTING);
        log("está descansando");
        esperaAtiva(restTime); // tempo Td de descanso
        log("terminou de descansar");
    }

    /** Bloqueia no semáforo se o cesto estiver vazio. */
    private void pegarBola() throws InterruptedException {
        setState(ChildState.WAITING_FOR_BALL);
        log("aguardando bola no cesto");
        basket.get();          // ← bloqueia aqui se cesto vazio
        log("pegou uma bola do cesto");
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    private void setState(ChildState novoEstado) {
        this.state = novoEstado;
    }

    /** Envia mensagem de log para a interface gráfica via EDT. */
    private void log(String mensagem) {
        final String msg = "[" + childId + "] " + mensagem;
        SwingUtilities.invokeLater(() -> logCallback.accept(msg));
    }

    /**
     * Espera ativa pelo tempo indicado — a thread permanece no estado RUNNABLE
     * (não bloqueia em nenhum semáforo nem dorme).
     *
     * Thread.onSpinWait() emite a instrução PAUSE na CPU (x86/ARM), reduzindo
     * o consumo de energia e a contenção de cache durante o spin sem bloquear
     * a thread. Combinado com prioridade mínima, preserva a responsividade da UI.
     */
    private void esperaAtiva(long ms) throws InterruptedException {
        long fim = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < fim) {
            if (Thread.interrupted()) throw new InterruptedException();
            Thread.onSpinWait(); // dica de spin para a CPU; não é sleep
        }
    }

    // ── Getters para a tabela ────────────────────────────────────────────────

    public String    getChildId()    { return childId; }
    public long      getPlayTime()   { return playTime; }
    public long      getRestTime()   { return restTime; }
    public ChildState getChildState() { return state; }
}
