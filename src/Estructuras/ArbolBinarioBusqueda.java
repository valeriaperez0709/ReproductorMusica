package Estructuras;

import modelo.Cancion;

import java.util.ArrayList;

//Implementa el arbol binario de busqueda
//Su responsabilidad es mantener los elementos ordenados según su criterio de comparación.
public class ArbolBinarioBusqueda<T extends Comparable<T>> {

    private NodoArbol<T> raiz;
    private int tamano;

    //Constructor
    public ArbolBinarioBusqueda() {
        raiz = null;
        tamano = 0;
    }

    //metodo para saber si el arbol esta vacio
    public boolean estaVacio() {
        return raiz == null;
    }

    //saber el tamaño del arbol (padre+hijos)
    public int getTamano() {
        return tamano;
    }

    //Insertar dde manera recursiva
    public void insertar(T valor) {
        if (raiz == null) {
            raiz = new NodoArbol<>(valor);
        } else {
            insertarRecursivo(raiz, valor);
        }
        tamano++;
    }

    private NodoArbol<T> insertarRecursivo(NodoArbol<T> actual, T valor) {
        if (actual == null) {
            return new NodoArbol<>(valor);
        }

        // Usamos compareTo() en lugar de < o >
        // compareTo() devuelve < 0 si 'valor' es menor que 'actual.valor'
        if (valor.compareTo(actual.getDato()) < 0) {
            actual.setIzquierda(insertarRecursivo(actual.getIzquierda(), valor));
        }
        // compareTo() devuelve > 0 si 'valor' es mayor que 'actual.valor'
        else if (valor.compareTo(actual.getDato()) > 0) {
            actual.setDerecha(insertarRecursivo(actual.getDerecha(), valor));
        }

        return actual;
    }

    //Buscar un valor dentro del arbol
    public boolean buscar(T valor) {
        return buscarRecursivo(raiz, valor); //llamada recursiva
    }

    /**
     * Lógica recursiva de búsqueda:
     * - Si el nodo actual es null, el valor NO está en el árbol.
     * - Si el valor es igual al nodo actual, lo encontramos.
     * - Si el valor es menor, buscamos en el subárbol izquierdo.
     * - Si el valor es mayor, buscamos en el subárbol derecho.
     */

    private boolean buscarRecursivo(NodoArbol<T> actual, T valor) {
        // Caso base: llegamos a un nodo nulo → no existe
        if (actual == null) {
            return false;
        }

        int comparacion = valor.compareTo(actual.getDato());

        if (comparacion == 0) {
            // ¡Encontrado!
            return true;
        } else if (comparacion < 0) {
            // El valor buscado es menor → ir a la izquierda
            return buscarRecursivo(actual.getIzquierda(), valor);
        } else {
            // El valor buscado es mayor → ir a la derecha
            return buscarRecursivo(actual.getDerecha(), valor);
        }
    }

    //recorrer el arbol en Inorden
    public ArrayList<T> recorridoInorden() {
        ArrayList<T> elementos=new ArrayList<>();
        inOrden(raiz,elementos);
        return elementos;
    }

    private void inOrden(NodoArbol<T> nodo,ArrayList<T> elementos) {
        if (nodo != null) {
            inOrden(nodo.getIzquierda(),elementos);
            elementos.add(nodo.getDato());
            inOrden(nodo.getDerecha(),elementos);
        }
    }

    //Metodo de eliminar pasandole un valor (metodo recursivo)
    public NodoArbol<T> eliminar(T valor) {
        if (buscar(valor)) {
            raiz = eliminarRecursivo(raiz, valor);
            tamano--;
        }
        return raiz;
    }
    private NodoArbol<T> eliminarRecursivo(NodoArbol<T> actual, T valor) {

        if (actual == null) {
            return null;
        }

        int comparacion = valor.compareTo(actual.getDato());

        if (comparacion < 0) {

            actual.setIzquierda(
                    eliminarRecursivo(actual.getIzquierda(), valor)
            );

        } else if (comparacion > 0) {

            actual.setDerecha(
                    eliminarRecursivo(actual.getDerecha(), valor)
            );

        } else {

            // Caso 1: no tiene hijos
            if (actual.getIzquierda() == null &&
                    actual.getDerecha() == null) {

                return null;
            }

            // Caso 2: solamente tiene hijo derecho
            if (actual.getIzquierda() == null) {
                return actual.getDerecha();
            }

            // Caso 2: solamente tiene hijo izquierdo
            if (actual.getDerecha() == null) {
                return actual.getIzquierda();
            }

            // Caso 3: tiene dos hijos
            T sucesor = encontrarMinimo(actual.getDerecha());

            actual.setDato(sucesor);

            actual.setDerecha(
                    eliminarRecursivo(actual.getDerecha(), sucesor)
            );
        }

        return actual;
    }
    //Metodo necesario para encontrar el menor de los mayores(necesario en el metodo de eliminar)
    public T obtenerMinimo(){
        if(raiz==null){
            return null;
        }
        return encontrarMinimo(raiz); //llamada reccursiva
    }

    private T encontrarMinimo(NodoArbol<T> nodo) {

        while (nodo.getIzquierda() != null) {
            nodo = nodo.getIzquierda();
        }

        return nodo.getDato();
    }

    //Para obtener el siguiente valor al que me voy a mover(en este caso canciones)
    public T obtenerSiguiente(T valor) {
        //Iniciamos desde la raiz
        //no tenemos un candidato (el siguiente) todavía
        return obtenerSiguienteRecursivo(raiz, valor, null); //llamada recursiva
    }

    private T obtenerSiguienteRecursivo(
            NodoArbol<T> actual,
            T valor,
            T sucesor) {

        //si llegamos a null, no podemos seguir buscando
        //devolvemos el ultimo sucesor (hijo) que encontramos
        if (actual == null) {
            return sucesor;
        }

        //comparamos el valor que buscamos con el nodo actual
        int comparacion = valor.compareTo(actual.getDato());

        if (comparacion < 0) {
            //El nodo actual es mayor que el valor
            //actualizamos al sucesor
            sucesor = actual.getDato();

            //buscamos algo lo más cercaco posible a valor,
            //continuamos buscando por la izquierda
            return obtenerSiguienteRecursivo(
                    actual.getIzquierda(),
                    valor,
                    sucesor
            );

        //caso contrario el valor es mayor que el nodo actual
        } else if (comparacion > 0) {

            // El nodo actual es menor que el valor,
            // por lo que no puede ser su sucesor.
            // Buscamos hacia la derecha
            return obtenerSiguienteRecursivo(
                    actual.getDerecha(),
                    valor,
                    sucesor
            );

            //si comparacion ==0
            // encontramos exactamente el valor buscado
        } else {

            //si el nodo tiene un subarbol derecho,
            // el siguiente elemento sera el menor elemento de ese subarbol
            if (actual.getDerecha() != null) {
                return encontrarMinimo(actual.getDerecha());
            }

            //si no tiene hijo derecho, utilizamos el sucesor (lo encontramos mientreas haciamos el recorrido)
            return sucesor;
        }
    }
    //Saber cual es el anterior elemento al actual (en este caso canciones)

    //obtiene el elemento que aparece inmediatamente antes
    // de valor segun el orden establecido por compareTo()
    public T obtenerAnterior(T valor) {
        //Comenzamos la busqueda desde la raiz del arbol
        return obtenerAnteriorRecursivo(raiz, valor, null);
    }

    //Metodo recursivo encargado de encontrar el elemento anterior
    private T obtenerAnteriorRecursivo(
            NodoArbol<T> actual,
            T valor,
            T anterior) {

        //Caso base:
        // si llegamos a null, significa que ya no podemos
        // continuar recorriendo el arbol.
        // Devolvemos el ultimo candidato encontrado
        if (actual == null) {
            return anterior;
        }

        // Comparamos el valor que estamos buscando
        // con el dato almacenado en el nodo actual
        int comparacion = valor.compareTo(actual.getDato());

        //si el valor buscado es mayor que el nodo actual
        if (comparacion > 0) {
            // el nodo actual es menor que el valor buscando,
            // por lo tanto, puedde ser un candidato a anterior
            anterior = actual.getDato();

            // Intentamos encontrar un valor todavía mayor,
            // pero que siga siendo menor que el valor buscado.
            // por eso continuamos hacia la derecha
            return obtenerAnteriorRecursivo(
                    actual.getDerecha(),
                    valor,
                    anterior
            );

        // si el valor es menor que el nodo actual
        } else if (comparacion < 0) {

            // El nodo actual es mayor al que estoy buscando,
            // no puede ser su anterior.
            // Debemos buscar hacia la izquierda
            return obtenerAnteriorRecursivo(
                    actual.getIzquierda(),
                    valor,
                    anterior
            );

        // si comparacion ==0:
        // encontramos el valor que estabamos buscando
        } else {

            //si existe un subarbol izquierdo, el anterior
            // es el elemento mayor
            // dentro de ese subarbol
            if (actual.getIzquierda() != null) {

                //Comenzamos desde el hijo izquierdo
                NodoArbol<T> nodo = actual.getIzquierda();

                //Avanzamos hacia la derecha todo lo posible
                // el ultimo nodo sera el mayor del subarbol izquierdo
                while (nodo.getDerecha() != null) {
                    nodo = nodo.getDerecha();
                }

                return nodo.getDato();
            }
            // si no existe subarbol izquiedo, utilizamos el candidato
            // que encontramos (durante el recorrido).
            return anterior;
        }
    }
}