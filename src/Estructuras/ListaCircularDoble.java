package Estructuras;
import Exceptions.EPosicion;

import java.util.ArrayList;
//La responsabilidad de esta clase es administrar una colección de elementos conectados circularmente en ambas direcciones
public class ListaCircularDoble<T> {
    private Nodo<T> cabeza;
    private int tamano;

    //Constructor
    public ListaCircularDoble() {
        this.cabeza = null;
        tamano=0;
    }

    //Saber si la lista esta vacia
    public boolean estaVacia(){
        return cabeza==null; //compara y devuelve el valor de TRUE si la cabeza es nula
    }

    //Obtener el tamaño de la lista
    public int getTamano(){
        return this.tamano;
    }

    //Devuelve el nodo cabeza para poder recorrer la lista desde afuera
    //Sino ModoAleatorio no tiene por donde empezar a navegar :v
    public Nodo<T> getCabeza(){
        return this.cabeza;
    }

    //para insertar al inicio de la lista
    public void insertarInicio(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);

        if (cabeza == null) {
            // Lista vacía: esto significa que el nodo apunta a si mismo en ambas direcciones
            cabeza = nuevo;
            cabeza.setSiguiente(cabeza);
            cabeza.setAnterior(cabeza);
        } else {
            Nodo<T> cola = cabeza.getAnterior(); // el nodo anterior a la cabeza

            // Conectamos el nuevo nodo entre la cola y la cabeza antigua
            nuevo.setSiguiente(cabeza);      // nuevo -> cabeza_antigua
            nuevo.setAnterior(cola);       // cola <- nuevo

            cabeza.setAnterior(nuevo);     // nuevo <- cabeza
            cola.setSiguiente(nuevo);    // nuevo -> cabeza_antigua

            // Actualizamos la cabeza
            cabeza=nuevo;
        }
        tamano++;
    }

    //Para insertar al final de la lista
    public void insertarFinal(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);

        if (cabeza == null) {
            cabeza = nuevo;
            cabeza.setSiguiente(cabeza);
            cabeza.setAnterior(cabeza);
        } else {
            Nodo<T> cola = cabeza.getAnterior(); // Accesible en O(1)

            // Conectamos el nuevo nodo entre la cola y la cabeza
            cola.setSiguiente(nuevo);      // cola -> nuevo
            nuevo.setAnterior(cola);       // cola <- nuevo
            nuevo.setSiguiente(cabeza);    // nuevo -> cabeza
            cabeza.setAnterior(nuevo);     // nuevo <- cabeza
            // La cabeza NO cambia (a diferencia de insertarInicio)
        }
        tamano++;
    }

    //Insertar en una posicion intermedia o donde yo decida
    public void insertarEnPosicion(int posicion, T valor)throws EPosicion {
        if (posicion < 0 || posicion > tamano) {
            throw new EPosicion("Posición " + posicion + " inválida. "
                    + "Rango válido: [0, " + tamano + "]");
        }

        if (posicion == 0) {
            insertarInicio(valor);
            return;
        }

        if (posicion == tamano) {
            insertarFinal(valor);
            return;
        }

        // Recorremos hasta el nodo ANTERIOR a la posición deseada
        Nodo<T> nuevo = new Nodo<>(valor);
        Nodo<T> anterior = cabeza;

        for (int i = 0; i < posicion - 1; i++) {
            anterior = anterior.getSiguiente();
        }

        // Guardamos el nodo que está después del anterior
        Nodo<T> sigDeAnt = anterior.getSiguiente();

        // Enlazamos (4 punteros)
        anterior.setSiguiente(nuevo);     // anterior -> nuevo
        nuevo.setAnterior(anterior);      // anterior <- nuevo
        nuevo.setSiguiente(sigDeAnt);     // nuevo -> sigDeAnt
        sigDeAnt.setAnterior(nuevo);      // nuevo <- sigDeAnt
        tamano++;
    }

    //Busca un elemento dentro de la lista y devuelve el nodo que lo contiene
    //Si no lo encuentra devuelve null
    public Nodo<T> buscar(T valor) {
        if (estaVacia()) {
            return null;
        }

        Nodo<T> actual = cabeza;

        do {
            //equals compara el contenido, no la direccion de memoria
            if (actual.getDato().equals(valor)) {
                return actual;
            }
            actual = actual.getSiguiente();
        } while (actual != cabeza); //paramos cuando damos la vuelta completa

        return null; //recorrimos toda la lista y no aparecio
    }
    //Elimina el nodo que contiene el valor recibido
    //Devuelve true si lo encontro y lo elimino, false si no estaba
    public boolean eliminar(T valor) {
        Nodo<T> objetivo = buscar(valor); //reutilizamos la busqueda de arriba

        if (objetivo == null) {
            return false; //no esta en la lista, no hay nada que borrar
        }

        // Caso 1: es el unico nodo que queda, la lista queda vacia
        if (tamano == 1) {
            cabeza = null;
            tamano--;
            return true;
        }

        Nodo<T> anterior = objetivo.getAnterior();
        Nodo<T> siguiente = objetivo.getSiguiente();

        // Caso 2: saltamos el nodo objetivo conectando a sus dos vecinos entre si
        anterior.setSiguiente(siguiente);   // anterior -> siguiente
        siguiente.setAnterior(anterior);    // anterior <- siguiente

        // Caso 3: si el que borramos era la cabeza, la cabeza pasa a ser el siguiente
        if (objetivo == cabeza) {
            cabeza = siguiente;
        }

        // Soltamos las referencias del nodo eliminado para que el recolector de basura lo limpie
        objetivo.setSiguiente(null);
        objetivo.setAnterior(null);

        tamano--;
        return true;
    }
    //Devuelve todos los elementos en orden empezando por la cabeza
    //Devolvemos la lista en vez de imprimirla para que la estructura no dependa de la consola
    public ArrayList<T> recorrerAdelante() {
        ArrayList<T> elementos = new ArrayList<>();

        if (estaVacia()) {
            return elementos; //devolvemos una lista sin elementos, nunca null
        }

        Nodo<T> actual = cabeza;

        do {
            elementos.add(actual.getDato());
            actual = actual.getSiguiente();
        } while (actual != cabeza);

        return elementos;
    }

    //Lo mismo pero arrancando desde el ultimo nodo y devolviendose
    public ArrayList<T> recorrerAtras() {
        ArrayList<T> elementos = new ArrayList<>();

        if (estaVacia()) {
            return elementos;
        }

        Nodo<T> cola = cabeza.getAnterior(); //lo guardamos una vez en vez de recalcularlo en cada vuelta
        Nodo<T> actual = cola;

        do {
            elementos.add(actual.getDato());
            actual = actual.getAnterior();
        } while (actual != cola);

        return elementos;
    }


    public void eliminar(Nodo<T> target) {
        //si el objetivo a eliminar no existe
        if (target == null || cabeza == null) return;

        //Caso 1: Es el único nodo de la lista
        if(target.getSiguiente()==target &&target.getAnterior()==target){
            cabeza=null;
        }else {
            //Desenlazar el nodo reconfigurando los vecinos
            Nodo<T> anterior=target.getAnterior();
            Nodo<T> siguiente=target.getSiguiente();

            anterior.setSiguiente(siguiente);
            siguiente.setAnterior(anterior);

            /*
            *   //Es lo mismo que hacer esto:
                target->prev->next = nodo->next;
                target->next->prev = nodo->prev;

            */

            // Caso 2: si el nodo a eliminar es la cabeza
            if(target== cabeza){
                cabeza=target.getSiguiente();
            }
        }
        tamano--;


    }

}
