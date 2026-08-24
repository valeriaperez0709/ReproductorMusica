package ui;

import java.awt.Color;
import java.awt.Font;

//Paleta y tipografia central de toda la aplicacion ("Glamour": negro + dorado).
//Centralizar los colores y fuentes aqui evita repetirlos en cada ventana/dialogo
//y permite cambiar el estilo completo de la app editando un solo archivo.
public final class Tema {

    private Tema() {
        // Clase de solo constantes: no se instancia.
    }

    //Fondos
    public static final Color BG_APP     = new Color(12, 11, 10);
    public static final Color BG_SIDEBAR = new Color(7, 7, 6);
    public static final Color BG_CARD    = new Color(21, 19, 17);
    public static final Color BG_FIELD   = new Color(32, 29, 25);
    public static final Color BG_PLAYER  = new Color(16, 15, 13);
    public static final Color BG_HOVER   = new Color(40, 36, 30);

    //Texto
    public static final Color TXT_PRIMARY   = new Color(240, 233, 217); // marfil calido
    public static final Color TXT_SECONDARY = new Color(163, 153, 132); // gris calido
    public static final Color TXT_SOBRE_ORO = new Color(24, 20, 12);    // texto oscuro sobre botones dorados

    //Acento dorado
    public static final Color ORO         = new Color(199, 161, 79);
    public static final Color ORO_CLARO   = new Color(231, 197, 128);
    public static final Color ORO_OSCURO  = new Color(140, 109, 45);

    //Otros
    public static final Color DIVIDER     = new Color(46, 41, 34);
    public static final Color PELIGRO     = new Color(176, 64, 58); // rojo vino, para eliminar
    public static final Color EXITO       = new Color(126, 166, 108);

    //Tipografia
    //Se usan familias logicas ("Serif"/"SansSerif") en vez de fuentes como "Segoe UI":
    //Java las mapea siempre a una fuente disponible en cualquier sistema operativo
    //(Windows, macOS o Linux), asi el proyecto se ve igual sin importar donde se ejecute.
    public static final Font FONT_LOGO    = new Font("Serif", Font.BOLD, 25);
    public static final Font FONT_TITLE   = new Font("Serif", Font.BOLD, 19);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 11);
    public static final Font FONT_BODY    = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_SONG    = new Font("Serif", Font.BOLD, 15);
    public static final Font FONT_ITALIC  = new Font("Serif", Font.ITALIC, 12);
}
