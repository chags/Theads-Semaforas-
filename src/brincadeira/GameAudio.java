package brincadeira;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de áudio da aplicação.
 * Usa javax.sound.sampled quando disponível; em WSL2 sem mixers ALSA,
 * cai automaticamente para PowerShell (SoundPlayer do Windows).
 */
public class GameAudio {

    private static GameAudio instancia;

    private Clip musicaFundo;
    private FloatControl controleVolumeMusica;
    private boolean musicaTocando = false;

    private final Map<String, Clip>   efeitos          = new HashMap<>();
    private final Map<String, String> caminhosPowerShell = new HashMap<>();

    // true quando Java não tem nenhum mixer de áudio disponível (ex.: WSL2 sem ALSA)
    private final boolean usarPowerShell;

    private GameAudio() {
        usarPowerShell = AudioSystem.getMixerInfo().length == 0;
        if (usarPowerShell) {
            System.out.println("[Audio] Nenhum mixer Java detectado — usando PowerShell (WSL2)");
        }
    }

    public static GameAudio getInstancia() {
        if (instancia == null) instancia = new GameAudio();
        return instancia;
    }

    // ── Efeitos sonoros ───────────────────────────────────────────────────────

    public boolean carregarEfeito(String nome, String caminho) {
        if (usarPowerShell) return carregarEfeitoPowerShell(nome, caminho);
        return carregarEfeitoJava(nome, caminho);
    }

    public void tocarEfeito(String nome) {
        if (usarPowerShell) { tocarEfeitoPowerShell(nome); return; }
        Clip efeito = efeitos.get(nome);
        if (efeito == null) return;
        if (efeito.isRunning()) efeito.stop();
        efeito.setFramePosition(0);
        efeito.start();
    }

    // ── Música de fundo ───────────────────────────────────────────────────────

    public boolean carregarMusicaFundo(String caminho) {
        if (usarPowerShell) return false; // loop contínuo não implementado via PS
        try {
            File arquivo = new File(caminho);
            if (!arquivo.exists()) { System.out.println("[Audio] Arquivo não encontrado: " + caminho); return false; }

            AudioInputStream stream = AudioSystem.getAudioInputStream(arquivo);
            AudioFormat fmt = stream.getFormat();
            if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || fmt.getSampleSizeInBits() != 16) {
                stream = AudioSystem.getAudioInputStream(formatoPadrao(fmt), stream);
            }
            musicaFundo = AudioSystem.getClip();
            musicaFundo.open(stream);
            musicaFundo.loop(Clip.LOOP_CONTINUOUSLY);
            if (musicaFundo.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                controleVolumeMusica = (FloatControl) musicaFundo.getControl(FloatControl.Type.MASTER_GAIN);
            }
            musicaTocando = true;
            System.out.println("[Audio] Música de fundo carregada: " + caminho);
            return true;
        } catch (Exception e) {
            System.out.println("[Audio] Erro ao carregar música: " + e.getMessage());
            return false;
        }
    }

    public void toggleMusica() {
        if (musicaFundo == null) return;
        if (musicaTocando) { musicaFundo.stop(); musicaTocando = false; }
        else               { musicaFundo.start(); musicaTocando = true; }
    }

    public void setVolumeMusica(float volume) {
        if (controleVolumeMusica == null) return;
        float db = (float) (20 * Math.log10(Math.max(volume, 0.0001f)));
        controleVolumeMusica.setValue(db);
    }

    public void pararMusica() {
        if (musicaFundo == null) return;
        musicaFundo.stop();
        musicaFundo.close();
        musicaFundo = null;
        musicaTocando = false;
    }

    public boolean isMusicaTocando() { return musicaTocando; }

    public void dispose() {
        pararMusica();
        for (Clip c : efeitos.values()) c.close();
        efeitos.clear();
        caminhosPowerShell.clear();
    }

    // ── Implementação Java (javax.sound.sampled) ──────────────────────────────

    private boolean carregarEfeitoJava(String nome, String caminho) {
        try {
            File arquivo = new File(caminho);
            if (!arquivo.exists()) { System.out.println("[Audio] Arquivo não encontrado: " + caminho); return false; }

            AudioInputStream stream = AudioSystem.getAudioInputStream(arquivo);
            AudioFormat fmt = stream.getFormat();
            if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || fmt.getSampleSizeInBits() != 16) {
                AudioInputStream convertido = AudioSystem.getAudioInputStream(formatoPadrao(fmt), stream);
                byte[] dados = convertido.readAllBytes();
                stream = new AudioInputStream(
                    new ByteArrayInputStream(dados), formatoPadrao(fmt),
                    dados.length / formatoPadrao(fmt).getFrameSize());
            }
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            efeitos.put(nome, clip);
            System.out.println("[Audio] Efeito carregado: " + nome);
            return true;
        } catch (Exception e) {
            System.out.println("[Audio] Erro ao carregar efeito " + nome + ": " + e.getMessage());
            return false;
        }
    }

    private AudioFormat formatoPadrao(AudioFormat fmt) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            fmt.getSampleRate(), 16,
            fmt.getChannels(), fmt.getChannels() * 2,
            fmt.getSampleRate(), false);
    }

    // ── Implementação PowerShell (fallback WSL2) ──────────────────────────────

    private boolean carregarEfeitoPowerShell(String nome, String caminho) {
        try {
            File arquivo = new File(caminho).getAbsoluteFile();
            if (!arquivo.exists()) { System.out.println("[Audio] Arquivo não encontrado: " + caminho); return false; }

            // Converte 24-bit → PCM 16-bit (SoundPlayer exige PCM padrão)
            AudioInputStream stream = AudioSystem.getAudioInputStream(arquivo);
            AudioFormat fmt = stream.getFormat();
            AudioFormat alvo = formatoPadrao(fmt);
            AudioInputStream convertido = AudioSystem.getAudioInputStream(alvo, stream);
            File temp = File.createTempFile("gameaudio_" + nome + "_", ".wav");
            temp.deleteOnExit();
            AudioSystem.write(convertido, AudioFileFormat.Type.WAVE, temp);

            // Caminho Windows (\\wsl.localhost\...)
            Process wslpath = new ProcessBuilder("wslpath", "-w", temp.getAbsolutePath()).start();
            String winPath = new String(wslpath.getInputStream().readAllBytes()).trim();
            caminhosPowerShell.put(nome, winPath);

            System.out.println("[Audio] Efeito carregado (PowerShell): " + nome + " -> " + winPath);
            return true;
        } catch (Exception e) {
            System.out.println("[Audio] Erro ao carregar efeito (PowerShell) " + nome + ": " + e.getMessage());
            return false;
        }
    }

    private void tocarEfeitoPowerShell(String nome) {
        String winPath = caminhosPowerShell.get(nome);
        if (winPath == null) return;
        // Executa em thread separada para não bloquear a thread da criança
        Thread t = new Thread(() -> {
            try {
                new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    "(New-Object Media.SoundPlayer '" + winPath + "').PlaySync()")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
            } catch (Exception e) { /* sem mixer, sem áudio — ignora silenciosamente */ }
        }, "audio-ps");
        t.setDaemon(true);
        t.start();
    }
}
