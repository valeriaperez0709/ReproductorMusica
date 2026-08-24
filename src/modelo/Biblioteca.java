package modelo;

import Exceptions.ECancion;
import Exceptions.ENumeroNegativo;
import Exceptions.EVacia;

import java.util.ArrayList;
import java.util.ListIterator;

public class Biblioteca {

    //Atributo donde se van a guardar las canciones en un principio
    private ArrayList<Cancion> canciones;

    //Constructor
    public Biblioteca() {
        canciones = new ArrayList<>();
    }

    //metodos crud
    public void agregarCancion(Cancion cancion) {
        canciones.add(cancion);
    }

    public void eliminarCancion(Cancion cancion) {
        canciones.remove(cancion);
    }

    public Cancion buscarCancion(String nombre) throws ECancion{

        ListIterator <Cancion> iterator=canciones.listIterator();
        Cancion x=new Cancion();
        x=iterator.next();
        while(iterator.hasNext()&&!x.getNombre().equalsIgnoreCase(nombre)){
            x=iterator.next();
        }
        if(x!=null){
            return x;
        }else{
            throw new ECancion(nombre);
        }
    }

    //Metodo de actualizar cancion
    public void actualizarCancion(Cancion c, String newNombre,
                                  String newArtista, String newAlbum,
                                  int newDuracionEnSegundos, String newGenero,
                                  int newAnio) throws EVacia, ENumeroNegativo{
        c.setNombre(newNombre);
        c.setArtista(newArtista);
        c.setAlbum(newAlbum);
        c.setDuracionEnSegundos(newDuracionEnSegundos);
        c.setGenero(newGenero);
        c.setAnioLanzamiento(newAnio);
    }


    //Getters and setters
    public ArrayList<Cancion> getCanciones() {
        return canciones;
    }

    public boolean estaVacia() {
        return canciones.isEmpty();
    }

    public int getTamano() {
        return canciones.size();
    }
}
