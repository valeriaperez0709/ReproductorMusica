package ui;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

//Componentes visuales reutilizables (botones, campos y tarjetas con esquinas
//redondeadas). Vivir en su propio paquete "ui", separado de "main", permite que
//tanto la ventana principal como los dialogos (historial, estadisticas, el juego)
//compartan exactamente el mismo estilo sin duplicar codigo.
public final class Componentes {

    private Componentes() {
        // Clase de utilidades: no se instancia.
    }

    //Boton tipo "pildora" (rectangulo con esquinas muy redondeadas).
    public static JButton botonPildora(String texto, Color fondo, Color colorTexto) {
        JButton boton = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover() ? aclarar(fondo, 22) : fondo;
                if (getModel().isPressed()) base = oscurecer(fondo, 25);
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        boton.setForeground(colorTexto);
        boton.setBackground(fondo);
        boton.setFont(Tema.FONT_SECTION);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setBorder(new EmptyBorder(9, 18, 9, 18));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    public static Color aclarar(Color c, int cantidad) {
        return new Color(
                Math.min(255, c.getRed() + cantidad),
                Math.min(255, c.getGreen() + cantidad),
                Math.min(255, c.getBlue() + cantidad));
    }

    public static Color oscurecer(Color c, int cantidad) {
        return new Color(
                Math.max(0, c.getRed() - cantidad),
                Math.max(0, c.getGreen() - cantidad),
                Math.max(0, c.getBlue() - cantidad));
    }

    //Panel liso con esquinas redondeadas, usado como "tarjeta" de fondo.
    public static class PanelRedondeado extends JPanel {
        private final Color colorFondo;
        private final int radio;

        public PanelRedondeado(Color colorFondo, int radio) {
            this.colorFondo = colorFondo;
            this.radio = radio;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colorFondo);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radio, radio));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    //Campo de texto con esquinas redondeadas, texto de ayuda (placeholder) y un
    //borde dorado sutil cuando tiene el foco.
    public static class CampoRedondeado extends JTextField {
        private final String placeholder;

        public CampoRedondeado(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setForeground(Tema.TXT_PRIMARY);
            setCaretColor(Tema.ORO);
            setFont(Tema.FONT_BODY);
            setBorder(new EmptyBorder(9, 16, 9, 16));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.BG_FIELD);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
            if (isFocusOwner()) {
                g2.setColor(Tema.ORO_OSCURO);
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(new RoundRectangle2D.Float(0.7f, 0.7f, getWidth() - 1.4f, getHeight() - 1.4f, 16, 16));
            }
            g2.dispose();

            //Cuando el campo esta vacio y sin foco, mostramos el texto de ayuda
            //EN LUGAR DE llamar a super.paintComponent(g): si lo llamamos igual
            //(aunque no haya texto real que pintar), el delegado interno de
            //JTextField deja el Graphics en un estado que corrompe visualmente
            //el primer caracter que dibujamos justo despues (se ve como ":" en
            //vez de, por ejemplo, "E"). Evitar la llamada en este caso evita el
            //problema de raiz en vez de parchar el sintoma.
            boolean mostrarPlaceholder = getText().isEmpty() && !isFocusOwner();
            if (!mostrarPlaceholder) {
                super.paintComponent(g);
            } else {
                Graphics2D gp = (Graphics2D) g.create();
                gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gp.setColor(Tema.TXT_SECONDARY);
                gp.setFont(Tema.FONT_BODY);
                gp.drawString(placeholder, 16, getHeight() / 2 + 5);
                gp.dispose();
            }
        }
    }

    //Portada del album: si recibe una ruta de imagen valida la dibuja recortada en
    //un rectangulo redondeado; si no hay imagen (o no se pudo leer), dibuja un
    //placeholder degradado dorado con una nota musical, en vez de dejar un espacio vacio.
    public static class PanelPortada extends JPanel {
        private Image imagen;

        public PanelPortada() {
            setOpaque(false);
        }

        public void setRutaImagen(String ruta) {
            imagen = null;
            if (ruta != null && !ruta.isBlank()) {
                try {
                    BufferedImage cargada = ImageIO.read(new File(ruta));
                    if (cargada != null) imagen = cargada;
                } catch (Exception ignorada) {
                    // Ruta invalida o formato no soportado: nos quedamos con el placeholder.
                }
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14);

            if (imagen != null) {
                g2.setClip(forma);
                g2.drawImage(imagen, 0, 0, getWidth(), getHeight(), this);
                g2.setClip(null);
            } else {
                GradientPaint degradado = new GradientPaint(
                        0, 0, Tema.ORO_OSCURO, getWidth(), getHeight(), new Color(38, 31, 14));
                g2.setPaint(degradado);
                g2.fill(forma);
                g2.setColor(new Color(255, 255, 255, 210));
                g2.setFont(new Font("Serif", Font.PLAIN, Math.max(14, getHeight() / 3)));
                FontMetrics fm = g2.getFontMetrics();
                String nota = "♪";
                int x = (getWidth() - fm.stringWidth(nota)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 4;
                g2.drawString(nota, x, y);
            }

            g2.setColor(new Color(255, 255, 255, 35));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(forma);
            g2.dispose();
        }
    }
}
