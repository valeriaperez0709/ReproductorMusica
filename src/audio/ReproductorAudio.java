package audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.File;

//Motor de audio real para archivos .wav, usando SOLO javax.sound.sampled
//(viene incluido en el JDK estandar, por eso el proyecto no necesita ninguna
//libreria externa ni gestor de dependencias como Maven/Gradle).
//
//Esta clase pertenece a la capa de logica: no importa nada de Swing ni conoce
//la interfaz grafica. Se comunica hacia afuera unicamente mediante el callback
//"alTerminar", que la interfaz (VentanaPrincipal) usa para reaccionar cuando
//una cancion real termina de sonar sola.
public class ReproductorAudio {

    private Clip clip;
    private Runnable alTerminar;

    //Carga un archivo .wav nuevo, liberando el anterior si existia.
    //Lanza excepcion si el archivo no existe o no es un formato de audio soportado.
    public void cargar(String rutaArchivo) throws Exception {
        liberar();

        File archivo = new File(rutaArchivo);
        try (AudioInputStream flujo = AudioSystem.getAudioInputStream(archivo)) {
            clip = AudioSystem.getClip();
            clip.open(flujo);
        }

        clip.addLineListener(evento -> {
            //El evento STOP se dispara tanto cuando pausamos manualmente como cuando
            //la cancion llega a su fin sola; los distinguimos comparando la posicion
            //actual contra la duracion total del clip.
            if (evento.getType() == LineEvent.Type.STOP
                    && clip.getFramePosition() >= clip.getFrameLength()
                    && alTerminar != null) {
                alTerminar.run();
            }
        });
    }

    public void reproducir() {
        if (clip != null) clip.start();
    }

    //Pausa SIN perder la posicion (a diferencia de stop(), que aqui usamos internamente,
    //clip.start() luego de esto continua exactamente donde se quedo).
    public void pausar() {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    public void reiniciarDesdeCero() {
        if (clip != null) clip.setFramePosition(0);
    }

    public void posicionarEnSegundo(double segundos) {
        if (clip != null) {
            long micros = Math.max(0, (long) (segundos * 1_000_000));
            clip.setMicrosecondPosition(Math.min(micros, clip.getMicrosecondLength()));
        }
    }

    public boolean estaCargado() {
        return clip != null;
    }

    public boolean estaSonando() {
        return clip != null && clip.isRunning();
    }

    public double getPosicionSegundos() {
        return clip == null ? 0 : clip.getMicrosecondPosition() / 1_000_000.0;
    }

    public double getDuracionSegundos() {
        return clip == null ? 0 : clip.getMicrosecondLength() / 1_000_000.0;
    }

    public void setAlTerminar(Runnable callback) {
        this.alTerminar = callback;
    }

    //Libera los recursos del clip actual (se debe llamar antes de cargar otro archivo
    //y al cerrar la aplicacion, ya que los Clip de Java usan recursos del sistema operativo).
    public void liberar() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}
