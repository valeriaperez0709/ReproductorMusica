package Exceptions;

public class ECancion extends Exception{

    public ECancion (String c){
        super("No se encontro canción con el nombre de: "+c);
    }
}
