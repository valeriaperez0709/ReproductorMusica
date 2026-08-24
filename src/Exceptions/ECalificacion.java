package Exceptions;

public class ECalificacion extends RuntimeException {
    public ECalificacion(int n) {
        super("La calificacion no puede ser negativa y debe estar en un rango de 0:100 "+"/n"+
                "el numero ingresado fue: "+n);
    }
}
