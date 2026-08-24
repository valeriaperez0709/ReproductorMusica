package ui;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.List;

//Ventana que muestra el historial de reproduccion: que sonó y en qué orden,
//del más reciente al más antiguo (por eso se alimenta desde una Pila en
//VentanaPrincipal: la ultima cancion en sonar es la primera en aparecer aqui).
public class DialogoHistorial extends JDialog {

    public DialogoHistorial(Frame padre, List<String> entradas) {
        super(padre, "Historial de reproducción", true);
        setUndecorated(true);
        getRootPane().setBorder(javax.swing.BorderFactory.createLineBorder(Tema.ORO_OSCURO, 1));

        Componentes.PanelRedondeado contenido = new Componentes.PanelRedondeado(Tema.BG_CARD, 20);
        contenido.setLayout(new BorderLayout(0, 14));
        contenido.setBorder(new EmptyBorder(24, 26, 22, 26));

        JLabel titulo = new JLabel("Historial de reproducción");
        titulo.setFont(Tema.FONT_TITLE);
        titulo.setForeground(Tema.ORO_CLARO);

        JLabel subtitulo = new JLabel(entradas.isEmpty()
                ? "Todavía no has escuchado ninguna canción completa."
                : "Últimas " + entradas.size() + " canciones escuchadas (la más reciente primero):");
        subtitulo.setFont(Tema.FONT_SMALL);
        subtitulo.setForeground(Tema.TXT_SECONDARY);
        subtitulo.setBorder(new EmptyBorder(2, 0, 10, 0));

        javax.swing.JPanel norte = new javax.swing.JPanel();
        norte.setOpaque(false);
        norte.setLayout(new javax.swing.BoxLayout(norte, javax.swing.BoxLayout.Y_AXIS));
        norte.add(titulo);
        norte.add(subtitulo);

        DefaultListModel<String> modelo = new DefaultListModel<>();
        entradas.forEach(modelo::addElement);

        JList<String> lista = new JList<>(modelo);
        lista.setBackground(Tema.BG_FIELD);
        lista.setForeground(Tema.TXT_PRIMARY);
        lista.setFont(Tema.FONT_BODY);
        lista.setFixedCellHeight(30);
        lista.setSelectionBackground(Tema.ORO_OSCURO);
        lista.setSelectionForeground(Tema.TXT_PRIMARY);
        lista.setCellRenderer(numerador());

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setPreferredSize(new Dimension(400, 300));
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Tema.DIVIDER));
        scroll.getViewport().setBackground(Tema.BG_FIELD);

        JButton cerrar = Componentes.botonPildora("Cerrar", Tema.ORO, Tema.TXT_SOBRE_ORO);
        cerrar.addActionListener(e -> dispose());
        javax.swing.JPanel sur = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        sur.setOpaque(false);
        sur.setBorder(new EmptyBorder(10, 0, 0, 0));
        sur.add(cerrar);

        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        setContentPane(contenido);
        getContentPane().setBackground(Tema.BG_CARD);
        pack();
        setLocationRelativeTo(padre);
    }

    private ListCellRenderer<String> numerador() {
        return (list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel((index + 1) + ".  " + value);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            label.setFont(Tema.FONT_BODY);
            label.setBackground(isSelected ? Tema.ORO_OSCURO : Tema.BG_FIELD);
            label.setForeground(Tema.TXT_PRIMARY);
            return label;
        };
    }
}
