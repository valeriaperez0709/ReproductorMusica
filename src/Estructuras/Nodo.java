package Estructuras;

//Representa un elemento dentro de una estructura lineal
//El trabajo de esta clase es guardar datos y mantener referencias.
public class Nodo <E>{

    //Atributos de la clase Nodo
    private E dato;
    private Nodo<E>siguiente;
    private Nodo<E>anterior;

    //Constructor
    public Nodo(E dato){
        this.dato=dato;
        this.siguiente=null;
        this.anterior=null;
    }

    //Getters and Setters
    public E getDato() {
        return dato;
    }

    public void setDato(E dato) {
        this.dato = dato;
    }

    public Nodo<E> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<E> siguiente) {
        this.siguiente = siguiente;
    }

    public Nodo<E> getAnterior() {
        return anterior;
    }

    public void setAnterior(Nodo<E> anterior) {
        this.anterior = anterior;
    }
}
