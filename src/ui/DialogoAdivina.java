package ui;

import modelo.Cancion;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Mini-juego "Adivina la Cancion": funcionalidad de creatividad.
//Usa las canciones que la propia usuaria agrego a su biblioteca como preguntas,
//revelando pistas poco a poco (genero y anio, luego artista, luego album) y
//descontando puntos por cada pista extra que se pida.
public class DialogoAdivina extends JDialog {

    private final List<Cancion> biblioteca;
    private final Random random = new Random();

    private Cancion objetivo;
    private int pistasReveladas; // 0 = solo genero/anio, 1 = +artista, 2 = +album, 3 = +numero de letras
    private int puntajeSesion = 0;
    private int rondas = 0;
    private int aciertos = 0;

    private JLabel labelPistas;
    private JLabel labelResultado;
    private JLabel labelPuntaje;
    private ui.Componentes.CampoRedondeado campoRespuesta;
    private JButton botonPista;
    private JButton botonAdivinar;
    private JButton botonRendirse;
    private DiscoGiratorio disco;

    public DialogoAdivina(Frame padre, List<Cancion> biblioteca) {
        super(padre, "Adivina la Canción", true);
        this.biblioteca = biblioteca;

        if (biblioteca == null || biblioteca.isEmpty()) {
            JOptionPane.showMessageDialog(padre,
                    "Agrega al menos una canción a tu biblioteca antes de jugar.",
                    "Todavía no hay canciones", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }

        construirInterfaz();
        nuevaRonda();
    }

    private void construirInterfaz() {
        setUndecorated(true);
        getRootPane().setBorder(javax.swing.BorderFactory.createLineBorder(Tema.ORO_OSCURO, 1));

        Componentes.PanelRedondeado contenido = new Componentes.PanelRedondeado(Tema.BG_CARD, 22);
        contenido.setLayout(new BorderLayout(0, 14));
        contenido.setBorder(new EmptyBorder(28, 34, 26, 34));

        //Encabezado
        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new javax.swing.BoxLayout(encabezado, javax.swing.BoxLayout.Y_AXIS));

        disco = new DiscoGiratorio();
        disco.setPreferredSize(new Dimension(64, 64));
        disco.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titulo = new JLabel("Adivina la Canción");
        titulo.setFont(Tema.FONT_TITLE);
        titulo.setForeground(Tema.ORO_CLARO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("¿Qué tanto conoces tu propia biblioteca?");
        subtitulo.setFont(Tema.FONT_ITALIC);
        subtitulo.setForeground(Tema.TXT_SECONDARY);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitulo.setBorder(new EmptyBorder(2, 0, 10, 0));

        encabezado.add(disco);
        encabezado.add(javax.swing.Box.createVerticalStrut(8));
        encabezado.add(titulo);
        encabezado.add(subtitulo);

        //Pistas
        labelPistas = new JLabel();
        labelPistas.setFont(Tema.FONT_BODY);
        labelPistas.setForeground(Tema.TXT_PRIMARY);
        labelPistas.setHorizontalAlignment(JLabel.CENTER);
        labelPistas.setBorder(new EmptyBorder(6, 6, 10, 6));

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new javax.swing.BoxLayout(centro, javax.swing.BoxLayout.Y_AXIS));
        centro.add(labelPistas);

        campoRespuesta = new Componentes.CampoRedondeado("Escribe el nombre de la canción...");
        campoRespuesta.setPreferredSize(new Dimension(320, 40));
        campoRespuesta.addActionListener(e -> adivinar());

        //Se envuelve en un panel con FlowLayout (en vez de agregarlo directo al
        //BoxLayout de "centro") porque BoxLayout, en el eje que NO controla, no
        //siempre respeta el ancho preferido/maximo de un JTextField: al calcular
        //las posiciones alineadas de todos sus hijos a la vez, puede terminar
        //asignandole bastante menos ancho del pedido. FlowLayout si respeta el
        //tamaño preferido del componente que contiene.
        JPanel envoltorioCampo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envoltorioCampo.setOpaque(false);
        envoltorioCampo.setAlignmentX(Component.CENTER_ALIGNMENT);
        envoltorioCampo.add(campoRespuesta);

        labelResultado = new JLabel(" ");
        labelResultado.setFont(Tema.FONT_SECTION);
        labelResultado.setHorizontalAlignment(JLabel.CENTER);
        labelResultado.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelResultado.setBorder(new EmptyBorder(10, 0, 0, 0));

        centro.add(envoltorioCampo);
        centro.add(labelResultado);

        //Botones
        // Se reparten en DOS filas fijas (en vez de dejar que un solo FlowLayout las
        // envuelva automaticamente): con 4 botones, el ancho del dialogo no alcanza
        // para una sola fila y el salto automatico dejaba la segunda fila cortada
        // fuera del dialogo. Separarlas explicitamente evita ese problema de raiz.
        JPanel filaPrincipal = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        filaPrincipal.setOpaque(false);

        botonPista = Componentes.botonPildora("Pista (-20 pts)", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        botonPista.addActionListener(e -> revelarPista());

        botonAdivinar = Componentes.botonPildora("Adivinar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        botonAdivinar.addActionListener(e -> adivinar());

        botonRendirse = Componentes.botonPildora("Rendirme", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        botonRendirse.addActionListener(e -> rendirse());

        filaPrincipal.add(botonPista);
        filaPrincipal.add(botonAdivinar);
        filaPrincipal.add(botonRendirse);

        JPanel filaSecundaria = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        filaSecundaria.setOpaque(false);
        filaSecundaria.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton botonCerrar = Componentes.botonPildora("Cerrar", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        botonCerrar.addActionListener(e -> dispose());
        filaSecundaria.add(botonCerrar);

        // ---- Puntaje ----
        labelPuntaje = new JLabel();
        labelPuntaje.setFont(Tema.FONT_SMALL);
        labelPuntaje.setForeground(Tema.TXT_SECONDARY);
        labelPuntaje.setHorizontalAlignment(JLabel.CENTER);
        labelPuntaje.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel sur = new JPanel();
        sur.setOpaque(false);
        sur.setLayout(new javax.swing.BoxLayout(sur, javax.swing.BoxLayout.Y_AXIS));
        sur.add(filaPrincipal);
        sur.add(filaSecundaria);
        sur.add(labelPuntaje);

        contenido.add(encabezado, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        setContentPane(contenido);
        getContentPane().setBackground(Tema.BG_CARD);
        setSize(460, 480);
        setLocationRelativeTo(getParent());
    }

    private void nuevaRonda() {
        objetivo = biblioteca.get(random.nextInt(biblioteca.size()));
        pistasReveladas = 0;
        campoRespuesta.setText("");
        labelResultado.setText(" ");
        botonPista.setEnabled(true);
        botonAdivinar.setEnabled(true);
        actualizarPistas();
        actualizarPuntaje();
        campoRespuesta.requestFocusInWindow();
    }

    private void actualizarPistas() {
        StringBuilder sb = new StringBuilder("<html><div style='text-align:center;width:300px'>");
        sb.append("<b>Género:</b> ").append(objetivo.getGenero())
                .append(" &nbsp;•&nbsp; <b>Año:</b> ").append(objetivo.getAnioLanzamiento());

        if (pistasReveladas >= 1) {
            sb.append("<br><b>Artista:</b> ").append(objetivo.getArtista());
        }
        if (pistasReveladas >= 2) {
            sb.append("<br><b>Álbum:</b> ").append(objetivo.getAlbum());
        }
        if (pistasReveladas >= 3) {
            sb.append("<br><b>El título tiene ").append(objetivo.getNombre().trim().length())
                    .append(" letras y empieza por \"").append(objetivo.getNombre().trim().charAt(0)).append("\"</b>");
        }
        sb.append("</div></html>");
        labelPistas.setText(sb.toString());
        botonPista.setEnabled(pistasReveladas < 3);
    }

    private void revelarPista() {
        if (pistasReveladas < 3) {
            pistasReveladas++;
            actualizarPistas();
        }
    }

    private void adivinar() {
        String respuesta = normalizar(campoRespuesta.getText());
        String correcta = normalizar(objetivo.getNombre());

        if (respuesta.isEmpty()) return;

        rondas++;
        if (respuesta.equals(correcta)) {
            int puntos = Math.max(20, 100 - pistasReveladas * 20);
            puntajeSesion += puntos;
            aciertos++;
            labelResultado.setForeground(Tema.EXITO);
            labelResultado.setText("¡Correcto! +" + puntos + " puntos 🎉");
            disco.setGirando(true);
            terminarRonda();
        } else {
            puntajeSesion = Math.max(0, puntajeSesion - 10);
            labelResultado.setForeground(Tema.PELIGRO);
            labelResultado.setText("No es esa... ¡sigue intentando! (-10 pts)");
            actualizarPuntaje();
        }
    }

    private void rendirse() {
        rondas++;
        labelResultado.setForeground(Tema.TXT_SECONDARY);
        labelResultado.setText("Era: “" + objetivo.getNombre() + "” de " + objetivo.getArtista());
        terminarRonda();
    }

    private void terminarRonda() {
        botonPista.setEnabled(false);
        botonAdivinar.setEnabled(false);
        actualizarPuntaje();

        javax.swing.Timer espera = new javax.swing.Timer(1600, e -> {
            disco.setGirando(false);
            nuevaRonda();
        });
        espera.setRepeats(false);
        espera.start();
    }

    private void actualizarPuntaje() {
        labelPuntaje.setText("Puntaje: " + puntajeSesion + "   •   Aciertos: " + aciertos + "/" + rondas);
    }

    //Quita tildes/mayusculas y espacios extra para que "cancion", "Canción " y "CANCION"
    //se consideren la misma respuesta.
    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ");
    }

    //Pequeño disco de vinilo dibujado a mano que gira al acertar (puro Graphics2D,
    //sin imagenes externas ni dependencias).
    private static class DiscoGiratorio extends JPanel {
        private double angulo = 0;
        private javax.swing.Timer animador;

        DiscoGiratorio() {
            setOpaque(false);
        }

        void setGirando(boolean girando) {
            if (girando) {
                if (animador == null) {
                    animador = new javax.swing.Timer(30, e -> {
                        angulo += 0.35;
                        repaint();
                    });
                }
                animador.start();
            } else if (animador != null) {
                animador.stop();
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.rotate(angulo, w / 2.0, h / 2.0);
            g2.setColor(new java.awt.Color(24, 22, 20));
            g2.fill(new Ellipse2D.Float(2, 2, w - 4, h - 4));
            g2.setColor(Tema.ORO);
            g2.fill(new Ellipse2D.Float(w / 2f - 6, h / 2f - 6, 12, 12));
            g2.setColor(new java.awt.Color(255, 255, 255, 30));
            for (int r = 6; r < w / 2; r += 6) {
                g2.draw(new Ellipse2D.Float(w / 2f - r, h / 2f - r, r * 2, r * 2));
            }
            g2.dispose();
        }
    }
}
