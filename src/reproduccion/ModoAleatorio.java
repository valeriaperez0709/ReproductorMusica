package reproduccion;

import Estructuras.ListaCircularDoble;
import Estructuras.Nodo;
import Exceptions.EVacia;
import modelo.Cancion;

import java.util.Random;

//Responsable de controlar el comportamiento del modo aleatorio
// con la ayuda de la clase de ListaDobleCircular
public class ModoAleatorio extends ModoReproduccion {

    private ListaCircularDoble<Cancion> lista;
    private Nodo<Cancion> nodoActual; //el cursor que marca en que parte de la lista vamos
    private Random random;

    //Constructor
    public ModoAleatorio() {
        lista = new ListaCircularDoble<>();
        random = new Random();
        nodoActual = null;
        actual = null;
    }

    //Agregar una cancion
    @Override
    public void agregarCancion(Cancion cancion) {
        //Insertamos en una posicion al azar para que la lista quede desordenada desde que se arma
        int posicion = random.nextInt(lista.getTamano() + 1);
        lista.insertarEnPosicion(posicion, cancion);

        //Si era la primera cancion el cursor arranca en la cabeza
        if (nodoActual == null) {
            nodoActual = lista.getCabeza();
            actual = nodoActual.getDato();
        }
    }

    //Obtener la siguiente cancion
    @Override
    public Cancion siguiente() {
        if (nodoActual == null) {
            return null; //la lista esta vacia, no hay nada que reproducir
        }

        //Como la lista es circular nunca llegamos a null, al final volvemos a la cabeza sola
        nodoActual = nodoActual.getSiguiente();
        actual = nodoActual.getDato();
        return actual;
    }

    //Obtener la anterior cancion
    @Override
    public Cancion anterior() {
        if (nodoActual == null) {
            return null;
        }

        //Mismo caso pero al reves, desde la cabeza saltamos al ultimo nodo
        nodoActual = nodoActual.getAnterior();
        actual = nodoActual.getDato();
        return actual;
    }

    //Eliminar una cancion de la lista
    @Override
    public Cancion eliminarCancion(Cancion c)throws EVacia {

        //Buscamos el nodo que contiene la canción
        Nodo<Cancion> objetivo=lista.buscar(c);

        //La canción no existe
        if(objetivo==null){
            throw new EVacia("La cancion no el nombre de: "+c.getNombre()+" no se encuentra en la lista");
        }

        //comprobamos si la canción que vamos a eliminar
        // es la canción actual
        boolean esActual=actual!=null&&actual.compareTo(c)==0;

        //Guardamos el siguiente nodo antes de eliminar
        Nodo<Cancion> siguiente=objetivo.getSiguiente();

        //Eliminamos el nodo
        lista.eliminar(objetivo);

        //si eliminamos la cancion actual
        if(esActual){
            //si la lista quedo vacia
            if(lista.estaVacia()){
                nodoActual=null;
                actual=null;
            }else{
                //pasamos a la siguiente cancion
                nodoActual=siguiente;
                actual=nodoActual.getDato();
            }
        }
        return c;
    }
}