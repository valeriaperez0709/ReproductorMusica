package reproduccion;

import Exceptions.EVacia;
import modelo.Cancion;

//Escoger el modo de reproducción
public abstract class ModoReproduccion {

    protected Cancion actual;
    public ModoReproduccion(){
        this.actual=null;
    }
    public abstract void agregarCancion(Cancion cancion);

    public abstract Cancion siguiente();

    public abstract Cancion anterior();

    public Cancion getActual(){
        return actual;
    }
    public abstract Cancion eliminarCancion(Cancion cancion) throws EVacia;

}
