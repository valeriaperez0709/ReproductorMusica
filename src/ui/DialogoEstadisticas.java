package ui;

import modelo.Cancion;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//Ventana de estadisticas de reproduccion (funcionalidad de bonificacion).
//Toda la informacion se calcula a partir de la biblioteca real: no hay datos
//de ejemplo ni "hardcodeados".
public class DialogoEstadisticas extends JDialog {

    public DialogoEstadisticas(Frame padre, List<Cancion> canciones) {
        super(padre, "Estadísticas de reproducción", true);
        setUndecorated(true);
        getRootPane().setBorder(javax.swing.BorderFactory.createLineBorder(Tema.ORO_OSCURO, 1));

        int totalReproducciones = canciones.stream().mapToInt(Cancion::getReproducciones).sum();
        long favoritas = canciones.stream().filter(Cancion::isFavorita).count();
        double promedioCalificacion = canciones.isEmpty() ? 0
                : canciones.stream().mapToInt(Cancion::getCalificacion).average().orElse(0);
        Cancion masReproducida = canciones.stream()
                .max(Comparator.comparingInt(Cancion::getReproducciones))
                .filter(c -> c.getReproducciones() > 0)
                .orElse(null);

        Componentes.PanelRedondeado contenido = new Componentes.PanelRedondeado(Tema.BG_CARD, 20);
        contenido.setLayout(new BorderLayout(0, 16));
        contenido.setBorder(new EmptyBorder(26, 28, 22, 28));

        JPanel norte = new JPanel();
        norte.setOpaque(false);
        norte.setLayout(new javax.swing.BoxLayout(norte, javax.swing.BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Estadísticas de reproducción");
        titulo.setFont(Tema.FONT_TITLE);
        titulo.setForeground(Tema.ORO_CLARO);

        JLabel subtitulo = new JLabel(masReproducida == null
                ? "Reproduce algunas canciones completas para empezar a ver datos aquí."
                : "Tu canción más escuchada es “" + masReproducida.getNombre() + "” de " + masReproducida.getArtista());
        subtitulo.setFont(Tema.FONT_SMALL);
        subtitulo.setForeground(Tema.TXT_SECONDARY);
        subtitulo.setBorder(new EmptyBorder(2, 0, 0, 0));

        norte.add(titulo);
        norte.add(subtitulo);

        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 12, 0));
        tarjetas.setOpaque(false);
        tarjetas.setBorder(new EmptyBorder(16, 0, 4, 0));
        tarjetas.add(tarjetaEstadistica(String.valueOf(canciones.size()), "Canciones"));
        tarjetas.add(tarjetaEstadistica(String.valueOf(totalReproducciones), "Reproducciones"));
        tarjetas.add(tarjetaEstadistica(String.valueOf(favoritas), "Favoritas"));
        tarjetas.add(tarjetaEstadistica(String.format("%.0f", promedioCalificacion), "Calif. promedio"));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);

        JLabel labelGenero = new JLabel("Canciones por género");
        labelGenero.setFont(Tema.FONT_SECTION);
        labelGenero.setForeground(Tema.TXT_SECONDARY);
        labelGenero.setBorder(new EmptyBorder(0, 2, 8, 0));

        Map<String, Integer> porGenero = new TreeMap<>();
        for (Cancion c : canciones) {
            porGenero.merge(c.getGenero(), 1, Integer::sum);
        }

        PanelBarras panelBarras = new PanelBarras(porGenero);
        panelBarras.setPreferredSize(new Dimension(420, 180));

        centro.add(labelGenero, BorderLayout.NORTH);
        centro.add(panelBarras, BorderLayout.CENTER);

        JButton cerrar = Componentes.botonPildora("Cerrar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        cerrar.addActionListener(e -> dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.setOpaque(false);
        sur.add(cerrar);

        JPanel encabezadoYTarjetas = new JPanel(new BorderLayout());
        encabezadoYTarjetas.setOpaque(false);
        encabezadoYTarjetas.add(norte, BorderLayout.NORTH);
        encabezadoYTarjetas.add(tarjetas, BorderLayout.SOUTH);

        contenido.add(encabezadoYTarjetas, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        setContentPane(contenido);
        getContentPane().setBackground(Tema.BG_CARD);
        setSize(480, 430);
        setLocationRelativeTo(padre);
    }

    private JPanel tarjetaEstadistica(String numero, String etiqueta) {
        Componentes.PanelRedondeado tarjeta = new Componentes.PanelRedondeado(Tema.BG_FIELD, 14);
        tarjeta.setLayout(new javax.swing.BoxLayout(tarjeta, javax.swing.BoxLayout.Y_AXIS));
        tarjeta.setBorder(new EmptyBorder(14, 8, 12, 8));

        JLabel labelNumero = new JLabel(numero);
        labelNumero.setFont(new Font("Serif", Font.BOLD, 24));
        labelNumero.setForeground(Tema.ORO_CLARO);
        labelNumero.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JLabel labelTexto = new JLabel(etiqueta);
        labelTexto.setFont(Tema.FONT_SMALL);
        labelTexto.setForeground(Tema.TXT_SECONDARY);
        labelTexto.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        tarjeta.add(labelNumero);
        tarjeta.add(labelTexto);
        return tarjeta;
    }

    //Grafico de barras horizontales simple, dibujado a mano (sin librerias de graficos):
    //una barra dorada por genero, ordenadas de mayor a menor, con su valor al final.
    private static class PanelBarras extends JPanel {
        private final LinkedHashMap<String, Integer> datos;

        PanelBarras(Map<String, Integer> origen) {
            setOpaque(false);
            datos = new LinkedHashMap<>();
            origen.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(8)
                    .forEach(e -> datos.put(e.getKey(), e.getValue()));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (datos.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Tema.TXT_SECONDARY);
                g2.setFont(Tema.FONT_BODY);
                g2.drawString("Agrega canciones para ver esta gráfica.", 8, 24);
                g2.dispose();
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = datos.values().stream().max(Integer::compareTo).orElse(1);
            int filaAlto = Math.max(22, getHeight() / Math.max(1, datos.size()));
            int etiquetaAncho = 110;
            int margenDerecho = 40;
            int y = 4;

            g2.setFont(Tema.FONT_SMALL);
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                int anchoDisponible = getWidth() - etiquetaAncho - margenDerecho;
                int anchoBarra = (int) (anchoDisponible * (entry.getValue() / (double) max));

                g2.setColor(Tema.TXT_PRIMARY);
                g2.drawString(recortar(entry.getKey(), 14), 0, y + filaAlto / 2 + 4);

                g2.setColor(Tema.BG_FIELD);
                g2.fillRoundRect(etiquetaAncho, y + 3, anchoDisponible, filaAlto - 10, 8, 8);

                g2.setColor(Tema.ORO);
                g2.fillRoundRect(etiquetaAncho, y + 3, Math.max(6, anchoBarra), filaAlto - 10, 8, 8);

                g2.setColor(Tema.TXT_SECONDARY);
                g2.drawString(String.valueOf(entry.getValue()), etiquetaAncho + anchoDisponible + 8, y + filaAlto / 2 + 4);

                y += filaAlto;
            }
            g2.dispose();
        }

        private String recortar(String texto, int max) {
            return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
        }
    }
}
