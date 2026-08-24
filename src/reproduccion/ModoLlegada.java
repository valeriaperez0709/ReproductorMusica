package reproduccion;

import Estructuras.Cola;
import Exceptions.EVacia;
import modelo.Cancion;

import java.util.ArrayList;

//controla el modo de reproduccion por orden de llegada
//sigue logica de cola simple
public class ModoLlegada extends ModoReproduccion {

    private Cola<Cancion> cola;

    //indica si la cancion actual ya fue confirmada como escuchada mediante siguiente()
    //false = actual solo esta al frente de la cola en espera (aun no se ha llamado siguiente())
    //true  = actual ya sono y la proxima llamada a siguiente() debe sacarla de la cola
    private boolean reproduccionIniciada;

    //const
    public ModoLlegada() {
        cola = new Cola<>();
        actual = null;
        reproduccionIniciada = false;
    }

    //Agregar una cancion
    @Override
    public void agregarCancion(Cancion cancion) {
        cola.encolar(cancion);

        //Si era la primera cancion se  la mostramos como la actual
        //(todavia no ha sonado solo esta al frente de la cola)
        if (actual == null) {
            actual = cola.cabeza();
        }
    }
    /*
    obtener la siguiente cancion
    Segun la especificacion: una vez una cancion fue reproducida, sale de la cola
    La PRMERA llamada a este metodo no saca nada: solo confirma que la cancion
    al frente de la cola comenzo a sonar recien la llamada siguiente saca esa
    cancion (porque ya termino su turno) y muestra la nueva cabeza*/
    @Override
    public Cancion siguiente() {
        if (cola.estaVacia()) {
            actual = null;
            reproduccionIniciada = false;
            return null;
        }

        if (!reproduccionIniciada) {
            //Aun no habiamos confirmado ninguna cancion: mostramos la cabeza sin sacarla
            reproduccionIniciada = true;
            actual = cola.cabeza();
            return actual;
        }

        //La cancion actual ya sonoy por eso sale de la cola
        cola.desencolar();

        if (cola.estaVacia()) {
            actual = null;
            reproduccionIniciada = false;
        } else {
            actual = cola.cabeza();
        }

        return actual;
    }
    /*
    Este modo no permite regresar a canciones anteriores (segun la tarea
    por lo que no tiene sentido implementarlo como en los otros modos*/
    @Override
    public Cancion anterior() {
        throw new UnsupportedOperationException(
                "El modo de reproduccion por orden de llegada no permite regresar a canciones anteriores");
    }
    /*
    Eliminar una cancion de la cola
    Como una cola simple solo permite operar sobre sus extremos, para eliminar
    cualquier cancion (no solo la cabeza) reconstruimos la cola sin ella.
    Esto cuesta O(n) en vez del O(1) que tendria eliminar solo la cabeza,
    pero es necesario porque la interfaz debe poder eliminar cualquier cancion de la biblioteca*/
    @Override
    public Cancion eliminarCancion(Cancion c) throws EVacia {
        if (cola.estaVacia()) {
            throw new EVacia("La cola esta vacia, no hay canciones para eliminar");
        }

        ArrayList<Cancion> todas = cola.recorrer();

        if (!todas.contains(c)) {
            throw new EVacia("La cancion con el nombre de: " + c.getNombre() + " no se encuentra en la cola");
        }

        //comprobamos si la cancion que vamos a eliminar es la cancion actual
        boolean esActual = actual != null && actual.equals(c);

        //se reconstuye la cola respetando el orden original, sin la cancion eliminada
        Cola<Cancion> nueva = new Cola<>();
        for (Cancion cancion : todas) {
            if (!cancion.equals(c)) {
                nueva.encolar(cancion);
            }
        }
        cola = nueva;

        //Si eliminamos la cancion actual, la nueva cabeza pasa a ser la cancion actual,
        //pero como todavia no ha sido "confirmada" por siguiente(), reiniciamos la bandera
        if (esActual) {
            actual = cola.estaVacia() ? null : cola.cabeza();
            reproduccionIniciada = false;
        }

        return c;
    }

    //exponer el contenido de la cola para que la interfaz grafica
    //pueda mostrar "toda la biblioteca" en este modo
    public ArrayList<Cancion> recorrer() {
        return cola.recorrer();
    }

    //Obtener el tamano de la cola (util para la interfaz grafica)
    public int getTamano() {
        return cola.getTamano();
    }
}