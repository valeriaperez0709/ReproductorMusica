package Exceptions;

public class EAnioInvalido extends Exception{
    public EAnioInvalido(int n){
        super("El numero del año ingresado no puede ser negativo, el numero ingresado fue: "+n);
    }
}
