package Estructuras;

import java.util.ArrayList;

//Implementacion de una pila simple (LIFO), usada para el historial de reproduccion.
//La ultima cancion que sono es siempre la primera que se muestra al abrir el historial.
public class Pila<P> {
    private Nodo<P> tope;
    private int tamano;
    //Limite opcional para no crecer indefinidamente en una sesion muy larga (0 = sin limite)
    private final int capacidadMaxima;

    public Pila() {
        this(0);
    }

    public Pila(int capacidadMaxima) {
        this.tope = null;
        this.tamano = 0;
        this.capacidadMaxima = capacidadMaxima;
    }

    public boolean estaVacia() {
        return tope == null;
    }

    public int getTamano() {
        return tamano;
    }

    //Agrega un elemento en el tope de la pila
    public void apilar(P dato) {
        Nodo<P> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(tope);
        tope = nuevo;
        tamano++;

        //Si hay un limite de capacidad, descartamos silenciosamente el mas antiguo
        //(no tiene sentido guardar un historial infinito en memoria)
        if (capacidadMaxima > 0 && tamano > capacidadMaxima) {
            recortarAlFinal();
        }
    }

    //Quita y devuelve el elemento del tope; null si esta vacia
    public P desapilar() {
        if (estaVacia()) return null;
        P dato = tope.getDato();
        tope = tope.getSiguiente();
        tamano--;
        return dato;
    }

    //Mira el tope sin sacarlo
    public P verTope() {
        return estaVacia() ? null : tope.getDato();
    }

    //Devuelve los elementos del mas reciente al mas antiguo (para mostrarlos en la interfaz)
    public ArrayList<P> recorrer() {
        ArrayList<P> elementos = new ArrayList<>();
        Nodo<P> actual = tope;
        while (actual != null) {
            elementos.add(actual.getDato());
            actual = actual.getSiguiente();
        }
        return elementos;
    }

    //Elimina el nodo mas antiguo (el ultimo de la cadena) cuando se supera la capacidad maxima
    private void recortarAlFinal() {
        if (tope == null || tope.getSiguiente() == null) return;
        Nodo<P> actual = tope;
        while (actual.getSiguiente().getSiguiente() != null) {
            actual = actual.getSiguiente();
        }
        actual.setSiguiente(null);
        tamano--;
    }

    public void vaciar() {
        tope = null;
        tamano = 0;
    }
}
