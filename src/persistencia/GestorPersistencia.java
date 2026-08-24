package persistencia;

import modelo.Cancion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//Guarda y carga la biblioteca completa en un archivo de texto plano, para que
//la coleccion de canciones no se pierda al cerrar la aplicacion (bonificacion:
//"Persistencia de la biblioteca en archivos o base de datos").
//
//No se usa ninguna libreria externa de serializacion (JSON, XML, etc.) a proposito:
//el formato es un simple archivo de texto con un separador de campo poco comun
//(, el caracter de "separador de unidad") para que nombres de canciones con
//comas, puntos o pipes no rompan el formato.
public class GestorPersistencia {

    private static final String DELIMITADOR = "";
    private static final int CANTIDAD_CAMPOS = 11;

    private final String rutaArchivo;

    public GestorPersistencia() {
        this("biblioteca_aurea.txt");
    }

    public GestorPersistencia(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    //Escribe toda la biblioteca en disco, una cancion por linea.
    public void guardar(List<Cancion> canciones) {
        try (PrintWriter escritor = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            for (Cancion c : canciones) {
                escritor.println(String.join(DELIMITADOR,
                        limpiar(c.getNombre()),
                        limpiar(c.getArtista()),
                        limpiar(c.getAlbum()),
                        String.valueOf(c.getDuracionEnSegundos()),
                        limpiar(c.getGenero()),
                        String.valueOf(c.getAnioLanzamiento()),
                        String.valueOf(c.getCalificacion()),
                        String.valueOf(c.isFavorita()),
                        String.valueOf(c.getReproducciones()),
                        limpiar(c.getRutaAudio()),
                        limpiar(c.getRutaPortada())
                ));
            }
        } catch (IOException ex) {
            //Un fallo al guardar no debe tumbar la aplicacion: la biblioteca sigue
            //funcionando en memoria durante esta sesion, solo no persiste.
            System.err.println("No se pudo guardar la biblioteca en disco: " + ex.getMessage());
        }
    }

    //Reconstruye la biblioteca desde el archivo guardado. Si el archivo no existe
    //todavia (primera vez que se ejecuta el programa), devuelve una lista vacia.
    public ArrayList<Cancion> cargar() {
        ArrayList<Cancion> resultado = new ArrayList<>();
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            return resultado;
        }

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] campos = linea.split(DELIMITADOR, -1);
                if (campos.length < CANTIDAD_CAMPOS) {
                    continue; // linea incompleta o de un formato viejo: la ignoramos
                }

                try {
                    Cancion c = new Cancion(
                            campos[0], campos[1], campos[2],
                            Integer.parseInt(campos[3]), campos[4], Integer.parseInt(campos[5]));
                    c.setCalificacion(Integer.parseInt(campos[6]));
                    c.setFavorita(Boolean.parseBoolean(campos[7]));
                    c.setReproducciones(Integer.parseInt(campos[8]));
                    c.setRutaAudio(campos[9]);
                    c.setRutaPortada(campos[10]);
                    resultado.add(c);
                } catch (Exception filaInvalida) {
                    //Preferimos perder una sola fila corrupta antes que impedir que
                    //arranque toda la aplicacion.
                    System.err.println("Se omitio una fila invalida de la biblioteca: " + filaInvalida.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("No se pudo leer la biblioteca guardada: " + ex.getMessage());
        }
        return resultado;
    }

    //Evita que un salto de linea o el delimitador interno rompan el formato de archivo.
    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replace(DELIMITADOR, " ").replace("\n", " ").replace("\r", " ");
    }
}
