package modelo;

import Exceptions.EAnioInvalido;
import Exceptions.ECalificacion;
import Exceptions.ENumeroNegativo;
import Exceptions.EVacia;

import java.util.Locale;

//Clase que representa la información de una canción
public class Cancion implements Comparable<Cancion> {
    private String nombre;
    private String artista;
    private String album;
    private int duracionEnSegundos;
    private String genero;
    private int anioLanzamiento;
    private int calificacion;

    //Atributos opcionales
    //Ruta a un archivo .wav en disco para reproduccion real (puede quedar vacia)
    private String rutaAudio = "";
    //Ruta a una imagen (jpg/png) elegida por el usuario como portada (puede quedar vacia)
    private String rutaPortada = "";
    //Marca si el usuario la guardo como favorita
    private boolean favorita = false;
    //Cuantas veces ha "sonado" completa (para las estadisticas)
    private int reproducciones = 0;

    public Cancion(){}

    public Cancion(String nombre, String artista, String album, int duracionEnSegundos, String genero, int anioLanzamiento) throws EVacia, ENumeroNegativo {

        if(nombre.isEmpty()||nombre.trim().isEmpty())throw new EVacia("El nombre de la cancion no puede estar vacio");
        if(artista.isEmpty()||artista.trim().isEmpty()) throw new EVacia("El nombre del artista no puede ser vacio");
        if(album.isEmpty()||album.trim().isEmpty()) throw new EVacia("La cancion no puede no pertenecer a algun album");
        if(duracionEnSegundos<0) throw new ENumeroNegativo("La duración no puede ser negativa el valor ingrsado fue: "+duracionEnSegundos);
        if(genero.isEmpty()||genero.trim().isEmpty()) throw new EVacia("El genro no puede ser vacio");
        if(anioLanzamiento<0) throw new ENumeroNegativo(" El anio no puede ser negativo, el anio ingresado fue: "+anioLanzamiento);

        this.nombre = nombre;
        this.artista = artista;
        this.album = album;
        this.duracionEnSegundos = duracionEnSegundos;
        this.genero = genero;
        this.anioLanzamiento = anioLanzamiento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre)throws EVacia {
        if(nombre==null || nombre.isEmpty()){
            throw new EVacia("El nombre no puede estar vacio");
        }
        this.nombre = nombre;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) throws EVacia {
        if(artista==null || artista.isEmpty()){
            throw new EVacia("El artista no puede estar en vacio");
        }
        this.artista = artista;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) throws EVacia {
        if(album==null || album.isEmpty()){
            throw new EVacia("El campo del album no puede estar vacio");
        }
        this.album = album;
    }

    public int getDuracionEnSegundos() {
        return duracionEnSegundos;
    }

    public void setDuracionEnSegundos(int duracionEnSegundos)throws ENumeroNegativo{
        if(duracionEnSegundos<0){
            throw new ENumeroNegativo("La duracion no puede ser negativa");
        }
        this.duracionEnSegundos = duracionEnSegundos;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) throws EVacia {
        if (genero == null || genero.isEmpty()) {
            throw new EVacia("El campo de genero no puede estar vacio");
        }

        this.genero = genero;
    }

    public int getAnioLanzamiento() {
        return anioLanzamiento;
    }

    public void setAnioLanzamiento(int anioLanzamiento) throws ENumeroNegativo {
        if(anioLanzamiento<0){
            throw new ENumeroNegativo("El anio de lanzamiento no puede ser negativo");
        }
        this.anioLanzamiento = anioLanzamiento;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(int calificacion) throws ECalificacion {
        if(calificacion<0 || calificacion>100){
            throw new ECalificacion(calificacion);
        }
        this.calificacion = calificacion;
    }

    //Getters y setters de los atributos opcionales
    //No lanzan excepciones porque son campos opcionales: "sin archivo" es un estado valido.
    public String getRutaAudio() {
        return rutaAudio;
    }

    public void setRutaAudio(String rutaAudio) {
        this.rutaAudio = (rutaAudio == null) ? "" : rutaAudio;
    }

    public boolean tieneAudioReal() {
        return rutaAudio != null && !rutaAudio.isBlank();
    }

    public String getRutaPortada() {
        return rutaPortada;
    }

    public void setRutaPortada(String rutaPortada) {
        this.rutaPortada = (rutaPortada == null) ? "" : rutaPortada;
    }

    public boolean tienePortadaPropia() {
        return rutaPortada != null && !rutaPortada.isBlank();
    }

    public boolean isFavorita() {
        return favorita;
    }

    public void setFavorita(boolean favorita) {
        this.favorita = favorita;
    }

    public int getReproducciones() {
        return reproducciones;
    }

    public void setReproducciones(int reproducciones) {
        this.reproducciones = Math.max(0, reproducciones);
    }

    //Se llama cada vez que la cancion termina de sonar completa (util para Estadisticas/Historial)
    public void registrarReproduccion() {
        this.reproducciones++;
    }

    @Override
    public String toString() {
        return "Cancion{" +
                "nombre='" + nombre + '\'' +
                ", artista='" + artista + '\'' +
                ", album='" + album + '\'' +
                ", duracionEnSegundos=" + duracionEnSegundos +
                ", genero='" + genero + '\'' +
                ", anioLanzamiento=" + anioLanzamiento +
                ", calificacion=" + calificacion +
                '}';
    }
    /*
        Ahora dos canciones se consideran iguales solo si comparten tanto nombre, como artista
    * */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Cancion otra = (Cancion) obj;
        return this.nombre.equalsIgnoreCase(otra.nombre) && this.artista.equalsIgnoreCase(otra.artista);
    }


    //Si sobreescribimos equals hay que sobreescribir hashCode, es el contrato de Java
    @Override
    public int hashCode() {
        return (nombre.toLowerCase()+ artista.toLowerCase()).hashCode();
    }

    //Para el arbol binario de busqueda
    @Override
    public int compareTo(Cancion o) {

        //tenemos que tener en cuenta que el nombre de la cancion se puede repetir
        //por lo que también compararemos por el artista

        int comparacionNombre=this.nombre.compareToIgnoreCase(o.nombre);

        if(comparacionNombre!=0){
            return comparacionNombre;
        }

        return this.artista.compareToIgnoreCase(o.artista);
    }
}
