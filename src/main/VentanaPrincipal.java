package main;

import Estructuras.Pila;
import Exceptions.ECalificacion;
import Exceptions.ENumeroNegativo;
import Exceptions.EVacia;
import audio.ReproductorAudio;
import modelo.Biblioteca;
import modelo.Cancion;
import persistencia.GestorPersistencia;
import reproduccion.ModoAlfabetico;
import reproduccion.ModoAleatorio;
import reproduccion.ModoLlegada;
import reproduccion.ModoReproduccion;
import ui.Componentes;
import ui.DialogoAdivina;
import ui.DialogoEstadisticas;
import ui.DialogoHistorial;
import ui.Tema;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

//Ventana principal del reproductor "AUREA".
//Se encarga UNICAMENTE de la presentacion (interfaz grafica); la logica de cada
//estructura de datos vive en Estructuras/ y reproduccion/, la del audio real en
//audio/, y la de guardar/cargar en persistencia/. Los estilos compartidos
//(colores, fuentes, botones, tarjetas) viven en ui/, para que esta clase y los
//dialogos (Historial, Estadisticas, el juego) se vean exactamente igual.
//
//Cada cancion que se agrega vive simultaneamente en 4 lugares: la Biblioteca
//(lista general) y en las 3 estructuras de cada modo (ListaCircularDoble, Cola,
//ArbolBinarioBusqueda), todas apuntando al MISMO objeto Cancion.
public class VentanaPrincipal extends JFrame {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    //Datos y logica
    private final Biblioteca biblioteca = new Biblioteca();
    private final ModoAleatorio modoAleatorio = new ModoAleatorio();
    private final ModoLlegada modoLlegada = new ModoLlegada();
    private final ModoAlfabetico modoAlfabetico = new ModoAlfabetico();
    private ModoReproduccion modoActivo = modoLlegada;

    private final ReproductorAudio reproductorAudio = new ReproductorAudio();
    private final GestorPersistencia persistencia = new GestorPersistencia();
    //Historial de reproduccion respaldado por una pila (LIFO): lo ultimo que sono
    //es siempre lo primero que se muestra. Limitado a 300 entradas por sesion.
    private final Pila<String> historial = new Pila<>(300);

    //Canciones actualmente mostradas en la tabla (respeta busqueda + filtros activos)
    private ArrayList<Cancion> filasVisibles = new ArrayList<>();

    //Estado de reproduccion
    private static final int INTERVALO_TIMER_MS = 400;

    private boolean reproduciendo = false;
    private int segundoActual = 0;
    //Acumulador de segundos "simulados" (cuando la cancion no tiene audio real).
    //Se necesita como double porque el timer marca cada 400ms: si sumaramos 1
    //segundo entero por cada tick, la cancion terminaria 2.5 veces mas rapido
    //de lo que indica su duracion configurada.
    private double segundosSimuladosAcumulados = 0;
    private Timer timerProgreso;
    //Cancion cuyo audio/duracion esta cargado en este momento (para no recargar
    //el archivo .wav en cada refresco de pantalla, solo cuando realmente cambia)
    private Cancion cancionCargadaActualmente = null;

    //Componentes de interfaz
    private DefaultTableModel tableModel;
    private JTable tabla;
    private Componentes.CampoRedondeado campoBusqueda;
    private JComboBox<String> comboGenero;
    private JComboBox<String> comboArtista;
    private JCheckBox checkFavoritas;
    private JList<String> listaModos;
    private JButton btnAnterior;
    private JButton btnPlayPause;
    private JLabel labelTituloActual;
    private JLabel labelArtistaActual;
    private JLabel labelEstadoAudio;
    private JLabel labelTiempoActual;
    private JLabel labelTiempoTotal;
    private JProgressBar barraProgreso;
    private Componentes.PanelPortada portada;
    private JLabel labelEstructuraActiva;

    private final String[] NOMBRES_MODO = {"Por orden de llegada", "Aleatorio", "Alfabético"};

    public VentanaPrincipal() {
        configurarVentana();
        cargarBibliotecaGuardada();

        reproductorAudio.setAlTerminar(() ->
                SwingUtilities.invokeLater(() -> {
                    Cancion queTermino = modoActivo.getActual();
                    if (queTermino != null) onCancionTerminada(queTermino);
                }));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.BG_APP);
        raiz.add(construirSidebar(), BorderLayout.WEST);
        raiz.add(construirPanelCentral(), BorderLayout.CENTER);
        raiz.add(construirPlayerBar(), BorderLayout.SOUTH);
        setContentPane(raiz);

        actualizarFiltros();
        filtrarBiblioteca();
        actualizarInfoActual();
        //Dispara el ListSelectionListener (ya conectado) para sincronizar la
        //descripcion de estructura y el boton "Anterior" con el modo por defecto.
        listaModos.setSelectedIndex(0);

        configurarAtajosDeTeclado();
        configurarGuardadoAlCerrar();
    }

    private void configurarVentana() {
        setTitle("AUREA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 740);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        UIManager.put("OptionPane.background", Tema.BG_CARD);
        UIManager.put("Panel.background", Tema.BG_CARD);
    }

    private void cargarBibliotecaGuardada() {
        for (Cancion c : persistencia.cargar()) {
            agregarATodasLasEstructuras(c);
        }
    }

    private void configurarGuardadoAlCerrar() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                persistencia.guardar(biblioteca.getCanciones());
                reproductorAudio.liberar();
            }
        });
    }

    //Atajos de teclado
    private void configurarAtajosDeTeclado() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "atajoPlayPause");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "atajoSiguiente");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "atajoAnterior");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "atajoBuscar");

        root.getActionMap().put("atajoPlayPause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!escribiendoEnCampoDeTexto()) togglePlayPause();
            }
        });
        root.getActionMap().put("atajoSiguiente", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!escribiendoEnCampoDeTexto()) irSiguiente();
            }
        });
        root.getActionMap().put("atajoAnterior", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!escribiendoEnCampoDeTexto() && btnAnterior.isEnabled()) irAnterior();
            }
        });
        root.getActionMap().put("atajoBuscar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                campoBusqueda.requestFocusInWindow();
            }
        });
    }

    private boolean escribiendoEnCampoDeTexto() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent;
    }

    //Sidebar (selector de modo + extras)
    private JPanel construirSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Tema.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(232, 0));
        sidebar.setBorder(new EmptyBorder(22, 18, 20, 18));

        JLabel logo = new JLabel("AUREA");
        logo.setFont(Tema.FONT_LOGO);
        logo.setForeground(Tema.ORO_CLARO);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Lenguajes y Compiladores");
        subtitulo.setFont(Tema.FONT_ITALIC);
        subtitulo.setForeground(Tema.TXT_SECONDARY);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setBorder(new EmptyBorder(2, 0, 26, 0));

        JLabel seccionModos = etiquetaSeccion("MODO DE REPRODUCCIÓN");

        listaModos = new JList<>(NOMBRES_MODO);
        listaModos.setBackground(Tema.BG_SIDEBAR);
        listaModos.setForeground(Tema.TXT_PRIMARY);
        listaModos.setFont(Tema.FONT_BODY);
        listaModos.setFixedCellHeight(38);
        listaModos.setAlignmentX(Component.LEFT_ALIGNMENT);
        listaModos.setSelectionBackground(Tema.ORO);
        listaModos.setSelectionForeground(Tema.TXT_SOBRE_ORO);
        listaModos.setCellRenderer(new ModoCellRenderer());
        listaModos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cambiarModoActivo(listaModos.getSelectedIndex());
            }
        });

        JLabel seccionInfo = etiquetaSeccion("ESTRUCTURA ACTIVA");
        seccionInfo.setBorder(new EmptyBorder(22, 4, 6, 0));

        labelEstructuraActiva = new JLabel(descripcionEstructura(0));
        labelEstructuraActiva.setFont(Tema.FONT_SMALL);
        labelEstructuraActiva.setForeground(Tema.TXT_SECONDARY);
        labelEstructuraActiva.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelEstructuraActiva.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel seccionExtras = etiquetaSeccion("EXTRAS");
        seccionExtras.setBorder(new EmptyBorder(26, 4, 8, 0));

        JButton btnHistorial = botonSidebar("🕘  Historial");
        btnHistorial.addActionListener(e -> new DialogoHistorial(this, historial.recorrer()).setVisible(true));

        JButton btnEstadisticas = botonSidebar("📊  Estadísticas");
        btnEstadisticas.addActionListener(e -> new DialogoEstadisticas(this, biblioteca.getCanciones()).setVisible(true));

        JButton btnJuego = botonSidebar("🎴  Adivina la canción");
        btnJuego.addActionListener(e -> new DialogoAdivina(this, biblioteca.getCanciones()).setVisible(true));

        sidebar.add(logo);
        sidebar.add(subtitulo);
        sidebar.add(seccionModos);
        sidebar.add(listaModos);
        sidebar.add(seccionInfo);
        sidebar.add(labelEstructuraActiva);
        sidebar.add(seccionExtras);
        sidebar.add(btnHistorial);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnEstadisticas);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnJuego);
        sidebar.add(Box.createVerticalGlue());

        JLabel pie = new JLabel("Universidad EIA");
        pie.setFont(Tema.FONT_SMALL);
        pie.setForeground(Tema.TXT_SECONDARY);
        pie.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(pie);

        return sidebar;
    }

    private JLabel etiquetaSeccion(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(Tema.FONT_SECTION);
        label.setForeground(Tema.ORO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 4, 8, 0));
        return label;
    }

    private JButton botonSidebar(String texto) {
        JButton boton = Componentes.botonPildora(texto, Tema.BG_SIDEBAR, Tema.TXT_PRIMARY);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        boton.setBorder(new EmptyBorder(7, 8, 7, 8));
        return boton;
    }

    private String descripcionEstructura(int indiceModo) {
        switch (indiceModo) {
            case 0: return "Cola simple (FIFO). Cada canción sale de la cola una vez reproducida.";
            case 1: return "Lista circular doble. Navegación infinita en ambas direcciones.";
            case 2: return "Árbol binario de búsqueda. Recorrido inorden alfabético.";
            default: return "";
        }
    }

    private void cambiarModoActivo(int indice) {
        switch (indice) {
            case 0 -> modoActivo = modoLlegada;
            case 1 -> modoActivo = modoAleatorio;
            case 2 -> modoActivo = modoAlfabetico;
        }
        labelEstructuraActiva.setText("<html><body style='width:160px'>" + descripcionEstructura(indice) + "</body></html>");
        pausarSiEstaReproduciendo();
        //El modo "Por orden de llegada" no permite regresar (lo exige la especificacion)
        btnAnterior.setEnabled(indice != 0);
        actualizarInfoActual();
    }

    private class ModoCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(new EmptyBorder(0, 10, 0, 0));
            label.setOpaque(true);
            label.setFont(Tema.FONT_BODY);
            if (isSelected) {
                label.setBackground(Tema.ORO);
                label.setForeground(Tema.TXT_SOBRE_ORO);
            } else {
                label.setBackground(Tema.BG_SIDEBAR);
                label.setForeground(Tema.TXT_PRIMARY);
            }
            return label;
        }
    }

    //Panel Central (busqueda + filtros + tabla de biblioteca)
    private JPanel construirPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.BG_APP);
        panel.setBorder(new EmptyBorder(20, 20, 10, 20));

        JPanel norte = new JPanel();
        norte.setOpaque(false);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(construirBarraSuperior());
        norte.add(construirBarraFiltros());

        panel.add(norte, BorderLayout.NORTH);
        panel.add(construirTabla(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(0, 0, 12, 0));

        campoBusqueda = new Componentes.CampoRedondeado("Buscar por nombre o artista...  (Ctrl+F)");
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrarBiblioteca(); }
            public void removeUpdate(DocumentEvent e) { filtrarBiblioteca(); }
            public void changedUpdate(DocumentEvent e) { filtrarBiblioteca(); }
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        JButton btnEditar = Componentes.botonPildora("Editar", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        btnEditar.addActionListener(e -> editarSeleccionada());

        JButton btnCalificar = Componentes.botonPildora("★ Calificar", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        btnCalificar.addActionListener(e -> calificarSeleccionada());

        JButton btnFavorita = Componentes.botonPildora("♡ Favorita", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        btnFavorita.addActionListener(e -> alternarFavoritaSeleccionada());

        JButton btnEliminar = Componentes.botonPildora("Eliminar", Tema.PELIGRO, Color.WHITE);
        btnEliminar.addActionListener(e -> eliminarSeleccionada());

        JButton btnAgregar = Componentes.botonPildora("+ Agregar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        btnAgregar.addActionListener(e -> abrirDialogoCancion(null));

        botones.add(btnEditar);
        botones.add(btnCalificar);
        botones.add(btnFavorita);
        botones.add(btnEliminar);
        botones.add(btnAgregar);

        barra.add(campoBusqueda, BorderLayout.CENTER);
        barra.add(botones, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirBarraFiltros() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.setOpaque(false);

        JLabel etiqueta = new JLabel("Filtrar por:");
        etiqueta.setFont(Tema.FONT_SMALL);
        etiqueta.setForeground(Tema.TXT_SECONDARY);

        comboGenero = new JComboBox<>();
        estilizarCombo(comboGenero);
        comboGenero.addActionListener(e -> filtrarBiblioteca());

        comboArtista = new JComboBox<>();
        estilizarCombo(comboArtista);
        comboArtista.addActionListener(e -> filtrarBiblioteca());

        checkFavoritas = new JCheckBox("Solo favoritas");
        checkFavoritas.setOpaque(false);
        checkFavoritas.setForeground(Tema.TXT_PRIMARY);
        checkFavoritas.setFont(Tema.FONT_SMALL);
        checkFavoritas.setFocusPainted(false);
        checkFavoritas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkFavoritas.addActionListener(e -> filtrarBiblioteca());

        barra.add(etiqueta);
        barra.add(comboGenero);
        barra.add(comboArtista);
        barra.add(checkFavoritas);
        return barra;
    }

    private void estilizarCombo(JComboBox<String> combo) {
        combo.setBackground(Tema.BG_FIELD);
        combo.setForeground(Tema.TXT_PRIMARY);
        combo.setFont(Tema.FONT_SMALL);
        combo.setFocusable(false);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                label.setBackground(isSelected ? Tema.ORO_OSCURO : Tema.BG_FIELD);
                label.setForeground(Tema.TXT_PRIMARY);
                label.setBorder(new EmptyBorder(4, 10, 4, 10));
                return label;
            }
        });
    }

    //Recalcula las opciones de los combos de Genero/Artista a partir de la biblioteca actual,
    //conservando la seleccion previa si sigue siendo valida.
    private void actualizarFiltros() {
        String generoPrevio = (String) comboGenero.getSelectedItem();
        String artistaPrevio = (String) comboArtista.getSelectedItem();

        TreeSet<String> generos = new TreeSet<>();
        TreeSet<String> artistas = new TreeSet<>();
        for (Cancion c : biblioteca.getCanciones()) {
            generos.add(c.getGenero());
            artistas.add(c.getArtista());
        }

        comboGenero.removeAllItems();
        comboGenero.addItem("Todos los géneros");
        generos.forEach(comboGenero::addItem);
        if (generoPrevio != null) comboGenero.setSelectedItem(generoPrevio);

        comboArtista.removeAllItems();
        comboArtista.addItem("Todos los artistas");
        artistas.forEach(comboArtista::addItem);
        if (artistaPrevio != null) comboArtista.setSelectedItem(artistaPrevio);
    }

    private JScrollPane construirTabla() {
        String[] columnas = {"★", "Nombre", "Artista", "Álbum", "Duración", "Género", "Año", "Calificación"};
        tableModel = new DefaultTableModel(columnas, 0) {
            //La calificacion y los favoritos no se editan en la celda: se gestionan con
            //los botones dedicados de la barra superior.
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(tableModel);
        tabla.setAutoCreateRowSorter(true);
        tabla.setBackground(Tema.BG_CARD);
        tabla.setForeground(Tema.TXT_PRIMARY);
        tabla.setSelectionBackground(Componentes.aclarar(Tema.BG_FIELD, 14));
        tabla.setSelectionForeground(Tema.TXT_PRIMARY);
        tabla.setFont(Tema.FONT_BODY);
        tabla.setRowHeight(32);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setBackground(Tema.BG_CARD);
        tabla.getTableHeader().setForeground(Tema.TXT_SECONDARY);
        tabla.getTableHeader().setFont(Tema.FONT_SECTION);
        tabla.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.DIVIDER));
        tabla.setDefaultRenderer(Object.class, new FilaRenderer());

        tabla.getColumnModel().getColumn(0).setMaxWidth(34);
        tabla.getColumnModel().getColumn(0).setMinWidth(34);

        //Doble clic sobre una fila reproduce esa cancion directamente (como en Apple Music)
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = filaSeleccionadaEnModelo();
                    if (fila >= 0 && fila < filasVisibles.size()) {
                        reproducirDesdeTabla(filasVisibles.get(fila));
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Tema.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.DIVIDER));
        return scroll;
    }

    //La tabla permite ordenar por columnas (RowSorter), asi que la fila "vista" que
    //el usuario selecciono no necesariamente coincide con su indice en el modelo:
    //siempre hay que convertirla antes de indexar filasVisibles.
    private int filaSeleccionadaEnModelo() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) return -1;
        return tabla.convertRowIndexToModel(vista);
    }

    private class FilaRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int filaModelo = table.convertRowIndexToModel(row);
            boolean esActual = filaModelo < filasVisibles.size()
                    && modoActivo.getActual() != null
                    && filasVisibles.get(filaModelo).equals(modoActivo.getActual());

            if (isSelected) {
                c.setBackground(Componentes.aclarar(Tema.BG_FIELD, 14));
            } else if (esActual) {
                c.setBackground(new Color(46, 36, 14)); // tinte dorado sutil
            } else {
                c.setBackground(Tema.BG_CARD);
            }
            c.setForeground(esActual ? Tema.ORO_CLARO : Tema.TXT_PRIMARY);
            setHorizontalAlignment(column == 0 ? CENTER : LEFT);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return c;
        }
    }

    //Barra de reproduccion (inferior)
    private JPanel construirPlayerBar() {
        JPanel barra = new JPanel(new BorderLayout(16, 0));
        barra.setBackground(Tema.BG_PLAYER);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.DIVIDER),
                new EmptyBorder(12, 20, 12, 20)));
        barra.setPreferredSize(new Dimension(0, 92));

        //Info de la cancion actual (izquierda)
        portada = new Componentes.PanelPortada();
        portada.setPreferredSize(new Dimension(58, 58));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 12, 0, 0));

        labelTituloActual = new JLabel("Sin canciones");
        labelTituloActual.setFont(Tema.FONT_SONG);
        labelTituloActual.setForeground(Tema.TXT_PRIMARY);

        labelArtistaActual = new JLabel("Agrega una canción para comenzar");
        labelArtistaActual.setFont(Tema.FONT_SMALL);
        labelArtistaActual.setForeground(Tema.TXT_SECONDARY);

        labelEstadoAudio = new JLabel(" ");
        labelEstadoAudio.setFont(Tema.FONT_SMALL);
        labelEstadoAudio.setForeground(Tema.ORO);

        info.add(labelTituloActual);
        info.add(labelArtistaActual);
        info.add(labelEstadoAudio);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setOpaque(false);
        izquierda.add(portada, BorderLayout.WEST);
        izquierda.add(info, BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(300, 0));

        //Controles + progreso (centro)
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        controles.setOpaque(false);

        btnAnterior = crearBotonControl(IconoControl.ANTERIOR);
        btnAnterior.addActionListener(e -> irAnterior());

        btnPlayPause = crearBotonCircular(IconoControl.PLAY);
        btnPlayPause.addActionListener(e -> togglePlayPause());

        JButton btnSiguiente = crearBotonControl(IconoControl.SIGUIENTE);
        btnSiguiente.addActionListener(e -> irSiguiente());

        controles.add(btnAnterior);
        controles.add(btnPlayPause);
        controles.add(btnSiguiente);

        JPanel progresoPanel = new JPanel(new BorderLayout(8, 0));
        progresoPanel.setOpaque(false);
        progresoPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        labelTiempoActual = new JLabel("0:00");
        labelTiempoActual.setFont(Tema.FONT_SMALL);
        labelTiempoActual.setForeground(Tema.TXT_SECONDARY);

        labelTiempoTotal = new JLabel("0:00");
        labelTiempoTotal.setFont(Tema.FONT_SMALL);
        labelTiempoTotal.setForeground(Tema.TXT_SECONDARY);

        barraProgreso = new JProgressBar(0, 100);
        barraProgreso.setValue(0);
        barraProgreso.setForeground(Tema.ORO);
        barraProgreso.setBackground(Tema.BG_FIELD);
        barraProgreso.setBorderPainted(false);
        barraProgreso.setPreferredSize(new Dimension(0, 5));

        progresoPanel.add(labelTiempoActual, BorderLayout.WEST);
        progresoPanel.add(barraProgreso, BorderLayout.CENTER);
        progresoPanel.add(labelTiempoTotal, BorderLayout.EAST);

        centro.add(controles);
        centro.add(Box.createVerticalStrut(6));
        centro.add(progresoPanel);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(centro, BorderLayout.CENTER);
        return barra;
    }

    //Logica de reproduccion
    private void reproducirDesdeTabla(Cancion c) {
        //Avanzamos el modo activo hasta llegar a la cancion elegida (o simplemente
        //la confirmamos si ya es la actual). Mantiene la logica dentro de cada modo.
        int intentos = biblioteca.getTamano() + 1;
        while (intentos-- > 0 && modoActivo.getActual() != null && !modoActivo.getActual().equals(c)) {
            if (modoActivo.siguiente() == null) break;
        }
        actualizarInfoActual();
        if (!reproduciendo) togglePlayPause();
    }

    private void irSiguiente() {
        modoActivo.siguiente();
        actualizarInfoActual();
    }

    private void irAnterior() {
        try {
            modoActivo.anterior();
        } catch (UnsupportedOperationException ex) {
            // El modo "Por orden de llegada" no permite retroceder; el boton ya deberia
            // estar deshabilitado en ese caso, esto es solo una proteccion extra.
        }
        actualizarInfoActual();
    }

    private void togglePlayPause() {
        Cancion actual = modoActivo.getActual();
        if (actual == null) return;

        reproduciendo = !reproduciendo;
        btnPlayPause.putClientProperty("icono", reproduciendo ? IconoControl.PAUSA : IconoControl.PLAY);
        btnPlayPause.repaint();

        boolean audioReal = actual.tieneAudioReal() && reproductorAudio.estaCargado();

        if (reproduciendo) {
            if (audioReal) reproductorAudio.reproducir();
            iniciarTimerProgreso();
        } else {
            if (audioReal) reproductorAudio.pausar();
            if (timerProgreso != null) timerProgreso.stop();
        }
    }

    private void pausarSiEstaReproduciendo() {
        if (reproduciendo) {
            reproduciendo = false;
            btnPlayPause.putClientProperty("icono", IconoControl.PLAY);
            btnPlayPause.repaint();
            reproductorAudio.pausar();
            if (timerProgreso != null) timerProgreso.stop();
        }
    }

    private void detenerReproduccion() {
        reproduciendo = false;
        btnPlayPause.putClientProperty("icono", IconoControl.PLAY);
        btnPlayPause.repaint();
        reproductorAudio.pausar();
        if (timerProgreso != null) timerProgreso.stop();
    }

    //Un unico timer de progreso, que se adapta segun si la cancion actual tiene
    //audio real cargado (mide el tiempo real del Clip) o no (avanza un contador simulado).
    private void iniciarTimerProgreso() {
        if (timerProgreso != null) timerProgreso.stop();
        timerProgreso = new Timer(INTERVALO_TIMER_MS, e -> tickProgreso());
        timerProgreso.start();
    }

    private void tickProgreso() {
        Cancion actual = modoActivo.getActual();
        if (actual == null) {
            detenerReproduccion();
            return;
        }

        if (actual.tieneAudioReal() && reproductorAudio.estaCargado()) {
            segundoActual = (int) Math.round(reproductorAudio.getPosicionSegundos());
            int duracion = Math.max(1, (int) Math.round(reproductorAudio.getDuracionSegundos()));
            barraProgreso.setValue((int) ((segundoActual / (double) duracion) * 100));
            labelTiempoActual.setText(formatoTiempo(segundoActual));
            //El final de la reproduccion real lo notifica el callback "alTerminar"
            //de ReproductorAudio (ver el constructor), no hace falta compararlo aqui.
        } else {
            //Sumamos fraccion de segundo real transcurrido (no 1 segundo entero por
            //tick), para que la duracion configurada se respete sin importar cada
            //cuantos milisegundos dispare el Timer.
            segundosSimuladosAcumulados += INTERVALO_TIMER_MS / 1000.0;
            segundoActual = (int) segundosSimuladosAcumulados;
            int duracion = Math.max(actual.getDuracionEnSegundos(), 1);
            barraProgreso.setValue((int) ((segundoActual / (double) duracion) * 100));
            labelTiempoActual.setText(formatoTiempo(segundoActual));

            if (segundosSimuladosAcumulados >= duracion) {
                onCancionTerminada(actual);
            }
        }
    }

    //Se llama cuando una cancion termina de sonar completa (real o simulada):
    //registra la reproduccion para las estadisticas, la apila en el historial
    //y avanza automaticamente a la siguiente, tal como haria un reproductor real.
    private void onCancionTerminada(Cancion queTermino) {
        queTermino.registrarReproduccion();
        historial.apilar(formatoEntradaHistorial(queTermino));

        Cancion siguienteCancion = modoActivo.siguiente();

        //El modo "Por orden de llegada" tiene un contrato de dos fases en su cola FIFO
        //(ver ModoLlegada.siguiente()): la PRIMERA llamada solo confirma que la cancion
        //al frente empezo a sonar, sin sacarla todavia; recien la SEGUNDA la saca de la
        //cola. Si el usuario le dio Play directamente (sin pasar antes por "Siguiente"),
        //esa confirmacion nunca ocurrio, y al terminar la cancion seguiriamos viendo la
        //misma que acaba de sonar. La detectamos y llamamos siguiente() una vez mas,
        //para cumplir la regla del enunciado: "una vez reproducida, sale de la cola".
        if (modoActivo == modoLlegada && siguienteCancion != null && siguienteCancion.equals(queTermino)) {
            modoActivo.siguiente();
        }

        segundoActual = 0;
        segundosSimuladosAcumulados = 0;
        actualizarInfoActual();

        if (modoActivo.getActual() == null) {
            detenerReproduccion();
        }
    }

    private String formatoEntradaHistorial(Cancion c) {
        return LocalTime.now().format(FORMATO_HORA) + " — " + c.getNombre() + " — " + c.getArtista();
    }

    //Refresca toda la informacion visible de la cancion actual. Solo recarga el
    //motor de audio (o reinicia el contador simulado) cuando la cancion realmente
    //cambio, para no interrumpir la reproduccion al simplemente refrescar la pantalla
    //(por ejemplo, al calificar o editar otra cancion mientras esta suena).
    private void actualizarInfoActual() {
        actualizarInfoActual(false);
    }

    private void actualizarInfoActual(boolean forzarRecarga) {
        Cancion actual = modoActivo.getActual();
        boolean cambioDeCancion = forzarRecarga || actual != cancionCargadaActualmente;

        if (cambioDeCancion) {
            segundoActual = 0;
            segundosSimuladosAcumulados = 0;
            barraProgreso.setValue(0);
            labelTiempoActual.setText("0:00");
            reproductorAudio.liberar();
            labelEstadoAudio.setText(" ");

            if (actual != null && actual.tieneAudioReal()) {
                try {
                    reproductorAudio.cargar(actual.getRutaAudio());
                    labelEstadoAudio.setText("🔊 Reproduciendo audio real (.wav)");
                    if (reproduciendo) reproductorAudio.reproducir();
                } catch (Exception ex) {
                    labelEstadoAudio.setText("⚠ No se pudo abrir el archivo de audio, se usa el modo simulado");
                }
            } else if (actual != null) {
                labelEstadoAudio.setText("🎚 Barra de progreso simulada");
            }
            cancionCargadaActualmente = actual;
        }

        if (actual == null) {
            labelTituloActual.setText("Sin canciones");
            labelArtistaActual.setText("Agrega una canción para comenzar");
            labelTiempoTotal.setText("0:00");
            labelEstadoAudio.setText(" ");
            portada.setRutaImagen(null);
        } else {
            labelTituloActual.setText(actual.getNombre());
            labelArtistaActual.setText(actual.getArtista() + " — " + actual.getAlbum());
            int duracion = (actual.tieneAudioReal() && reproductorAudio.estaCargado())
                    ? (int) Math.round(reproductorAudio.getDuracionSegundos())
                    : actual.getDuracionEnSegundos();
            labelTiempoTotal.setText(formatoTiempo(duracion));
            portada.setRutaImagen(actual.getRutaPortada());
        }
        tabla.repaint();
    }

    private String formatoTiempo(int segundos) {
        return String.format("%d:%02d", segundos / 60, segundos % 60);
    }

    //Crud de canciones
    private void filtrarBiblioteca() {
        String texto = campoBusqueda.getText().trim().toLowerCase();
        String genero = comboGenero == null ? null : (String) comboGenero.getSelectedItem();
        String artista = comboArtista == null ? null : (String) comboArtista.getSelectedItem();
        boolean soloFavoritas = checkFavoritas != null && checkFavoritas.isSelected();

        ArrayList<Cancion> resultado = new ArrayList<>();
        for (Cancion c : biblioteca.getCanciones()) {
            boolean coincideTexto = texto.isEmpty()
                    || c.getNombre().toLowerCase().contains(texto)
                    || c.getArtista().toLowerCase().contains(texto);
            boolean coincideGenero = genero == null || genero.equals("Todos los géneros") || genero.equals(c.getGenero());
            boolean coincideArtista = artista == null || artista.equals("Todos los artistas") || artista.equals(c.getArtista());
            boolean coincideFavorita = !soloFavoritas || c.isFavorita();

            if (coincideTexto && coincideGenero && coincideArtista && coincideFavorita) {
                resultado.add(c);
            }
        }
        refrescarTabla(resultado);
    }

    private void refrescarTabla(ArrayList<Cancion> canciones) {
        filasVisibles = canciones;
        tableModel.setRowCount(0);
        for (Cancion c : canciones) {
            tableModel.addRow(new Object[]{
                    c.isFavorita() ? "★" : "",
                    c.getNombre(), c.getArtista(), c.getAlbum(),
                    formatoTiempo(c.getDuracionEnSegundos()), c.getGenero(),
                    String.valueOf(c.getAnioLanzamiento()), String.valueOf(c.getCalificacion())
            });
        }
    }

    private void agregarATodasLasEstructuras(Cancion c) {
        biblioteca.agregarCancion(c);
        modoAleatorio.agregarCancion(c);
        modoLlegada.agregarCancion(c);
        modoAlfabetico.agregarCancion(c);
    }

    private void eliminarDeTodasLasEstructuras(Cancion c) {
        biblioteca.eliminarCancion(c);
        try { modoAleatorio.eliminarCancion(c); } catch (EVacia ignored) { }
        try { modoLlegada.eliminarCancion(c); } catch (EVacia ignored) { }
        modoAlfabetico.eliminarCancion(c); // no lanza excepcion si no la encuentra
    }

    private void eliminarSeleccionada() {
        int fila = filaSeleccionadaEnModelo();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una canción de la tabla primero.");
            return;
        }
        Cancion c = filasVisibles.get(fila);
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + c.getNombre() + "\" de la biblioteca?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        eliminarDeTodasLasEstructuras(c);
        actualizarFiltros();
        filtrarBiblioteca();
        actualizarInfoActual(true);
    }

    private void editarSeleccionada() {
        int fila = filaSeleccionadaEnModelo();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una canción de la tabla primero.");
            return;
        }
        abrirDialogoCancion(filasVisibles.get(fila));
    }

    private void alternarFavoritaSeleccionada() {
        int fila = filaSeleccionadaEnModelo();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una canción de la tabla primero.");
            return;
        }
        Cancion c = filasVisibles.get(fila);
        c.setFavorita(!c.isFavorita());
        filtrarBiblioteca();
    }

    //Dialogo unico para Agregar (cancionExistente == null) y Editar (cancionExistente != null),
    //con tarjeta redondeada propia para mantener el estilo "glamour" en toda la app.
    private void abrirDialogoCancion(Cancion cancionExistente) {
        boolean esEdicion = cancionExistente != null;

        Componentes.CampoRedondeado campoNombre = new Componentes.CampoRedondeado("Ej: Symphony No. 5");
        Componentes.CampoRedondeado campoArtista = new Componentes.CampoRedondeado("Ej: Ludwig van Beethoven");
        Componentes.CampoRedondeado campoAlbum = new Componentes.CampoRedondeado("Ej: Sinfonías completas");
        Componentes.CampoRedondeado campoDuracion = new Componentes.CampoRedondeado("Segundos, ej: 210");
        Componentes.CampoRedondeado campoGenero = new Componentes.CampoRedondeado("Ej: Clásica");
        Componentes.CampoRedondeado campoAnio = new Componentes.CampoRedondeado("Ej: 1808");

        if (esEdicion) {
            campoNombre.setText(cancionExistente.getNombre());
            campoArtista.setText(cancionExistente.getArtista());
            campoAlbum.setText(cancionExistente.getAlbum());
            campoDuracion.setText(String.valueOf(cancionExistente.getDuracionEnSegundos()));
            campoGenero.setText(cancionExistente.getGenero());
            campoAnio.setText(String.valueOf(cancionExistente.getAnioLanzamiento()));
        }

        String[] rutaAudioSel = { esEdicion ? cancionExistente.getRutaAudio() : "" };
        String[] rutaPortadaSel = { esEdicion ? cancionExistente.getRutaPortada() : "" };

        JLabel labelAudioElegido = new JLabel(descripcionArchivo(rutaAudioSel[0], "Sin archivo (se usará barra simulada)"));
        estiloEtiquetaArchivo(labelAudioElegido);
        JButton botonAudio = Componentes.botonPildora("🎵  Elegir audio .wav", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        botonAudio.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            selector.setFileFilter(new FileNameExtensionFilter("Archivos de audio WAV", "wav"));
            if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                rutaAudioSel[0] = selector.getSelectedFile().getAbsolutePath();
                labelAudioElegido.setText(descripcionArchivo(rutaAudioSel[0], "Sin archivo (se usará barra simulada)"));
            }
        });

        JLabel labelPortadaElegida = new JLabel(descripcionArchivo(rutaPortadaSel[0], "Sin imagen (se usará portada genérica)"));
        estiloEtiquetaArchivo(labelPortadaElegida);
        JButton botonPortada = Componentes.botonPildora("🖼  Elegir portada", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        botonPortada.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            selector.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
            if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                rutaPortadaSel[0] = selector.getSelectedFile().getAbsolutePath();
                labelPortadaElegida.setText(descripcionArchivo(rutaPortadaSel[0], "Sin imagen (se usará portada genérica)"));
            }
        });

        JDialog dialogo = new JDialog(this, esEdicion ? "Editar canción" : "Agregar canción", true);
        dialogo.setUndecorated(true);
        dialogo.getRootPane().setBorder(BorderFactory.createLineBorder(Tema.ORO_OSCURO, 1));

        Componentes.PanelRedondeado contenido = new Componentes.PanelRedondeado(Tema.BG_CARD, 22);
        contenido.setLayout(new BorderLayout(0, 14));
        contenido.setBorder(new EmptyBorder(26, 30, 22, 30));

        JLabel titulo = new JLabel(esEdicion ? "Editar canción" : "Agregar canción");
        titulo.setFont(Tema.FONT_TITLE);
        titulo.setForeground(Tema.ORO_CLARO);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 16, 10));
        formulario.setOpaque(false);
        formulario.add(campoConEtiqueta("Nombre", campoNombre));
        formulario.add(campoConEtiqueta("Artista", campoArtista));
        formulario.add(campoConEtiqueta("Álbum", campoAlbum));
        formulario.add(campoConEtiqueta("Duración (segundos)", campoDuracion));
        formulario.add(campoConEtiqueta("Género", campoGenero));
        formulario.add(campoConEtiqueta("Año de lanzamiento", campoAnio));

        JPanel filaAudio = new JPanel(new BorderLayout(10, 0));
        filaAudio.setOpaque(false);
        filaAudio.setBorder(new EmptyBorder(4, 0, 0, 0));
        filaAudio.add(botonAudio, BorderLayout.WEST);
        filaAudio.add(labelAudioElegido, BorderLayout.CENTER);

        JPanel filaPortada = new JPanel(new BorderLayout(10, 0));
        filaPortada.setOpaque(false);
        filaPortada.setBorder(new EmptyBorder(8, 0, 0, 0));
        filaPortada.add(botonPortada, BorderLayout.WEST);
        filaPortada.add(labelPortadaElegida, BorderLayout.CENTER);

        JPanel archivosOpcionales = new JPanel();
        archivosOpcionales.setOpaque(false);
        archivosOpcionales.setLayout(new BoxLayout(archivosOpcionales, BoxLayout.Y_AXIS));
        archivosOpcionales.setBorder(new EmptyBorder(14, 0, 0, 0));
        archivosOpcionales.add(filaAudio);
        archivosOpcionales.add(filaPortada);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(archivosOpcionales, BorderLayout.SOUTH);

        JButton btnCancelar = Componentes.botonPildora("Cancelar", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnGuardar = Componentes.botonPildora("Guardar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        btnGuardar.addActionListener(e -> {
            try {
                int duracion = Integer.parseInt(campoDuracion.getText().trim());
                int anio = Integer.parseInt(campoAnio.getText().trim());
                String nuevoNombre = campoNombre.getText().trim();
                String nuevoArtista = campoArtista.getText().trim();

                if (esEdicion) {
                    //El Arbol Binario de Busqueda ordena por nombre+artista (ver Cancion.compareTo).
                    //Si esos campos cambian, el nodo debe reubicarse: lo sacamos ANTES de mutar
                    //los datos (para que la busqueda use el valor viejo) y lo reinsertamos despues.
                    boolean cambiaOrdenAlfabetico =
                            !nuevoNombre.equalsIgnoreCase(cancionExistente.getNombre())
                                    || !nuevoArtista.equalsIgnoreCase(cancionExistente.getArtista());

                    if (cambiaOrdenAlfabetico) {
                        modoAlfabetico.eliminarCancion(cancionExistente);
                    }

                    biblioteca.actualizarCancion(cancionExistente, nuevoNombre, nuevoArtista,
                            campoAlbum.getText().trim(), duracion, campoGenero.getText().trim(), anio);
                    cancionExistente.setRutaAudio(rutaAudioSel[0]);
                    cancionExistente.setRutaPortada(rutaPortadaSel[0]);

                    if (cambiaOrdenAlfabetico) {
                        modoAlfabetico.agregarCancion(cancionExistente);
                    }
                } else {
                    Cancion nueva = new Cancion(nuevoNombre, nuevoArtista, campoAlbum.getText().trim(),
                            duracion, campoGenero.getText().trim(), anio);
                    nueva.setRutaAudio(rutaAudioSel[0]);
                    nueva.setRutaPortada(rutaPortadaSel[0]);
                    agregarATodasLasEstructuras(nueva);
                }

                actualizarFiltros();
                filtrarBiblioteca();
                actualizarInfoActual(true);
                dialogo.dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogo,
                        "Duración y año deben ser números válidos.",
                        "Datos inválidos", JOptionPane.ERROR_MESSAGE);
            } catch (EVacia | ENumeroNegativo ex) {
                JOptionPane.showMessageDialog(dialogo,
                        "Revisa los datos ingresados: ningún campo puede estar vacío ni ser negativo.",
                        "Datos inválidos", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.setBorder(new EmptyBorder(16, 0, 0, 0));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        dialogo.setContentPane(contenido);
        dialogo.getContentPane().setBackground(Tema.BG_CARD);
        dialogo.setSize(600, 560);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    private JPanel campoConEtiqueta(String etiqueta, JComponent campo) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(etiqueta);
        label.setFont(Tema.FONT_SECTION);
        label.setForeground(Tema.TXT_SECONDARY);
        label.setBorder(new EmptyBorder(0, 4, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setPreferredSize(new Dimension(220, 38));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        panel.add(label);
        panel.add(campo);
        return panel;
    }

    private void estiloEtiquetaArchivo(JLabel label) {
        label.setFont(Tema.FONT_SMALL);
        label.setForeground(Tema.TXT_SECONDARY);
    }

    private String descripcionArchivo(String ruta, String siVacio) {
        if (ruta == null || ruta.isBlank()) return siVacio;
        String nombre = new java.io.File(ruta).getName();
        return "Seleccionado: " + nombre;
    }

    //Calificar canciones
    private void calificarSeleccionada() {
        int fila = filaSeleccionadaEnModelo();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una canción de la tabla primero.");
            return;
        }
        Cancion c = filasVisibles.get(fila);
        abrirDialogoCalificacion(c);
    }

    //Dialogo con un slider libre (0-100) para calificar la cancion seleccionada.
    private void abrirDialogoCalificacion(Cancion c) {
        JDialog dialogo = new JDialog(this, "Calificar canción", true);
        dialogo.setUndecorated(true);
        dialogo.getRootPane().setBorder(BorderFactory.createLineBorder(Tema.ORO_OSCURO, 1));

        Componentes.PanelRedondeado contenido = new Componentes.PanelRedondeado(Tema.BG_CARD, 20);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel titulo = new JLabel(c.getNombre());
        titulo.setFont(Tema.FONT_SONG);
        titulo.setForeground(Tema.TXT_PRIMARY);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel(c.getArtista());
        subtitulo.setFont(Tema.FONT_SMALL);
        subtitulo.setForeground(Tema.TXT_SECONDARY);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitulo.setBorder(new EmptyBorder(2, 0, 22, 0));

        JLabel labelValor = new JLabel(c.getCalificacion() + " / 100");
        labelValor.setFont(Tema.FONT_TITLE);
        labelValor.setForeground(Tema.ORO);
        labelValor.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelValor.setBorder(new EmptyBorder(0, 0, 10, 0));

        JSlider slider = new JSlider(0, 100, c.getCalificacion());
        slider.setOpaque(false);
        slider.setForeground(Tema.TXT_SECONDARY);
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setAlignmentX(Component.CENTER_ALIGNMENT);
        slider.setPreferredSize(new Dimension(280, 45));
        slider.addChangeListener(e -> labelValor.setText(slider.getValue() + " / 100"));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botones.setOpaque(false);
        botones.setBorder(new EmptyBorder(18, 0, 0, 0));

        JButton btnCancelar = Componentes.botonPildora("Cancelar", Tema.BG_FIELD, Tema.TXT_PRIMARY);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnGuardar = Componentes.botonPildora("Guardar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        btnGuardar.addActionListener(e -> {
            try {
                c.setCalificacion(slider.getValue());
            } catch (ECalificacion ex) {
                JOptionPane.showMessageDialog(dialogo,
                        "La calificación debe estar entre 0 y 100.",
                        "Calificación inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            filtrarBiblioteca();
            dialogo.dispose();
        });

        botones.add(btnCancelar);
        botones.add(btnGuardar);

        contenido.add(titulo);
        contenido.add(subtitulo);
        contenido.add(labelValor);
        contenido.add(slider);
        contenido.add(botones);

        dialogo.setContentPane(contenido);
        dialogo.getContentPane().setBackground(Tema.BG_CARD);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    //Componentes visuales del reproductor
    //Iconos que se dibujan a mano (evita depender de que la fuente del sistema
    //tenga los glifos Unicode de los simbolos de reproduccion, que no siempre estan disponibles)
    private enum IconoControl { PLAY, PAUSA, ANTERIOR, SIGUIENTE }

    private void dibujarIcono(Graphics2D g2, IconoControl icono, int w, int h, Color color) {
        g2.setColor(color);
        int cx = w / 2, cy = h / 2;
        switch (icono) {
            case PLAY -> {
                int lado = Math.min(w, h) / 3;
                int[] xs = {cx - lado / 2, cx - lado / 2, cx + lado};
                int[] ys = {cy - lado, cy + lado, cy};
                g2.fillPolygon(xs, ys, 3);
            }
            case PAUSA -> {
                int barW = Math.max(w / 8, 3), barH = h / 3, espacio = barW + 2;
                g2.fillRect(cx - espacio, cy - barH / 2, barW, barH);
                g2.fillRect(cx + espacio - barW, cy - barH / 2, barW, barH);
            }
            case SIGUIENTE -> {
                int lado = h / 3;
                int[] xs = {cx - lado, cx - lado, cx};
                int[] ys = {cy - lado, cy + lado, cy};
                g2.fillPolygon(xs, ys, 3);
                g2.fillRect(cx, cy - lado, Math.max(w / 12, 2), lado * 2);
            }
            case ANTERIOR -> {
                int lado = h / 3;
                int[] xs = {cx + lado, cx + lado, cx};
                int[] ys = {cy - lado, cy + lado, cy};
                g2.fillPolygon(xs, ys, 3);
                g2.fillRect(cx - Math.max(w / 12, 2), cy - lado, Math.max(w / 12, 2), lado * 2);
            }
        }
    }

    //Boton circular grande (Play/Pausa), con acento dorado e icono dibujado a mano
    private JButton crearBotonCircular(IconoControl iconoInicial) {
        JButton boton = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? Tema.ORO_CLARO : Tema.ORO);
                g2.fillOval(0, 0, getWidth(), getHeight());
                IconoControl actual = (IconoControl) getClientProperty("icono");
                if (actual == null) actual = iconoInicial;
                dibujarIcono(g2, actual, getWidth(), getHeight(), Tema.TXT_SOBRE_ORO);
                g2.dispose();
            }
        };
        boton.putClientProperty("icono", iconoInicial);
        boton.setPreferredSize(new Dimension(44, 44));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    //Boton de control simple (Anterior/Siguiente), sin fondo, solo icono dibujado a mano
    private JButton crearBotonControl(IconoControl icono) {
        JButton boton = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                dibujarIcono(g2, icono, getWidth(), getHeight(), isEnabled() ? Tema.TXT_PRIMARY : Tema.TXT_SECONDARY);
                g2.dispose();
            }
        };
        boton.setPreferredSize(new Dimension(28, 28));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
