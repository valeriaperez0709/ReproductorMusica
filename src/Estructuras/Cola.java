package Estructuras;

import java.util.ArrayList;

// Implementación de la cola simple, su responsabilidad es mantener el comportamiento (FIFO)
public class Cola<Q> {
        private Nodo<Q> cabeza; // inicio de la cola(frente)
        private Nodo<Q> fin; //variable auxiliar para saber donde se debe insertar el proximo elemento, evitamos recorrer toda la lista cuando queramos agregar
        private int tamano;

        //Constructor
        public Cola() {
                this.cabeza = null;
                this.fin = null;
                this.tamano = 0;
        }

        //Verificar si esta vacio
        public boolean estaVacia() {
                return cabeza == null;
        }

        // obtener el tamaño de la cola
        public int getTamano() {
                return this.tamano;
        }

        //agregamos un elemento a la cola
        public void encolar(Q dato) {
                Nodo<Q> nuevo = new Nodo<>(dato);

                if (estaVacia()) {
                        cabeza = nuevo;
                        fin = nuevo;
                } else {
                        fin.setSiguiente(nuevo);
                        fin = nuevo;
                }
                tamano++;
        }

        // sacamos un elemento de la cola
        public Q desencolar(){

                if(estaVacia()){
                        return null;
                }

                Q dato= cabeza.getDato();

                cabeza=cabeza.getSiguiente();

                tamano--;

                if(tamano==0){
                        fin=null;
                }
                return dato;
        }

        // obtener el primer elemento de la cola
        public Q cabeza(){ //cabeza o frente (es lo mismo)
                if(estaVacia()){
                        return null;
                }
                return cabeza.getDato();
        }

        //Guardar la cola para mostrarla en la interfaz
        public ArrayList<Q> recorrer() {
                ArrayList<Q> elementos = new ArrayList<>();

                Nodo<Q> actual = cabeza;

                while (actual != null) {
                        elementos.add(actual.getDato());
                        actual = actual.getSiguiente();
                }

                return elementos;
        }




}
