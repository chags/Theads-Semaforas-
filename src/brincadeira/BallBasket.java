package brincadeira;

import javax.sound.sampled.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.IOException;

/**
 * Cesto de bolas com capacidade limitada K.
 *
 * Dois semáforos controlam o acesso concorrente:
 *   - emptySlots: conta os espaços livres no cesto (inicia em K).
 *                 Uma criança que quer COLOCAR uma bola precisa adquiri-lo;
 *                 se o cesto estiver cheio (valor 0), ela fica bloqueada até
 *                 que outra criança retire uma bola e libere um espaço.
 *
 *   - balls:      conta as bolas presentes no cesto (inicia em 0).
 *                 Uma criança que quer PEGAR uma bola precisa adquiri-lo;
 *                 se o cesto estiver vazio (valor 0), ela fica bloqueada até
 *                 que outra criança coloque uma bola.
 */
public class BallBasket {
    private final int capacity;

    // Semáforo que controla os espaços livres no cesto
    private final Semaphore emptySlots;

    // Semáforo que controla a quantidade de bolas no cesto
    private final Semaphore balls;

    // Contador atômico para leitura na interface gráfica (thread-safe)
    private final AtomicInteger count = new AtomicInteger(0);

    // Efeito sonoro tocar ao trocar bola
    private Clip somTroca;

    public BallBasket(int capacity) {
        this.capacity   = capacity;
        this.emptySlots = new Semaphore(capacity); // cesto começa vazio → K espaços livres
        this.balls      = new Semaphore(0);        // nenhuma bola no início
        
        // Carrega som usando caminho absoluto
        String caminho = System.getProperty("user.dir") + "/basketball.wav";
        carregarSom(caminho);
    }

    /** Carrega o efeito sonoro do arquivo. */
    private void carregarSom(String caminho) {
        System.out.println("[Audio] ====== Iniciando carregamento ======");
        try {
            File arquivo = new File(caminho);
            System.out.println("[Audio] Caminho: " + arquivo.getAbsolutePath());
            System.out.println("[Audio] Existe: " + arquivo.exists());
            
            if (!arquivo.exists()) {
                System.out.println("[Audio] Arquivo não encontrado: " + caminho);
                return;
            }
            
            AudioInputStream ais = AudioSystem.getAudioInputStream(arquivo);
            AudioFormat baseFormat = ais.getFormat();
            System.out.println("[Audio] Formato base: " + baseFormat);
            
            // Usar formato PCM padrão mantendo endianness original
            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                baseFormat.isBigEndian());
            
            System.out.println("[Audio] Formato destino: " + decodedFormat);
            
            // Converter para formato suportado
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(decodedFormat, ais);
            
            somTroca = AudioSystem.getClip();
            somTroca.open(convertedStream);
            System.out.println("[Audio] Som carregado com sucesso!");
        } catch (Exception e) {
            System.out.println("[Audio] ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Toca o som de troca de bola. */
    private void tocarSom() {
        if (somTroca != null) {
            if (somTroca.isRunning()) {
                somTroca.stop();
            }
            somTroca.setFramePosition(0);
            somTroca.start();
        }
    }

    /**
     * Coloca uma bola no cesto.
     * Bloqueia se o cesto estiver cheio, aguardando um espaço livre.
     */
    public void put() throws InterruptedException {
        emptySlots.acquire(); // aguarda espaço livre (bloqueia se cesto cheio)
        count.incrementAndGet();
        balls.release();    // sinaliza que há mais uma bola disponível
        tocarSom();
    }

    /**
     * Retira uma bola do cesto.
     * Bloqueia se o cesto estiver vazio, aguardando uma bola ser colocada.
     */
    public void get() throws InterruptedException {
        balls.acquire();      // aguarda uma bola (bloqueia se cesto vazio)
        count.decrementAndGet();
        emptySlots.release(); // sinaliza que há mais um espaço livre
        tocarSom();
    }

    public int getCount()    { return count.get(); }
    public int getCapacity() { return capacity; }
}
