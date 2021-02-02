package org.workcraft.gui.tools;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.*;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.controls.FlatHeaderRenderer;
import org.workcraft.gui.controls.SpeedSlider;
import org.workcraft.gui.events.GraphEditorKeyEvent;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.layouts.WrapLayout;
import org.workcraft.plugins.builtin.settings.SimulationDecorationSettings;
import org.workcraft.traces.Solution;
import org.workcraft.traces.Trace;
import org.workcraft.types.Func;
import org.workcraft.utils.GuiUtils;
import org.workcraft.utils.TraceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SimulationTool extends AbstractGraphEditorTool implements ClipboardOwner {

    private static final ImageIcon ICON_PLAY = GuiUtils.createIconFromSVG("images/simulation-play.svg");
    private static final ImageIcon ICON_PAUSE = GuiUtils.createIconFromSVG("images/simulation-pause.svg");
    private static final ImageIcon ICON_BACKWARD = GuiUtils.createIconFromSVG("images/simulation-backward.svg");
    private static final ImageIcon ICON_FORWARD = GuiUtils.createIconFromSVG("images/simulation-forward.svg");
    private static final ImageIcon ICON_RECORD = GuiUtils.createIconFromSVG("images/simulation-record.svg");
    private static final ImageIcon ICON_STOP = GuiUtils.createIconFromSVG("images/simulation-stop.svg");
    private static final ImageIcon ICON_EJECT = GuiUtils.createIconFromSVG("images/simulation-eject.svg");

    private static final String HINT_PLAY = "Play through the trace";
    private static final String HINT_PAUSE = "Pause trace playback";
    private static final String HINT_BACKWARD = "Step backward ([)";
    private static final String HINT_FORWARD = "Step forward (])";
    private static final String HINT_RECORD = "Generate a random trace";
    private static final String HINT_STOP = "Stop trace generation";
    private static final String HINT_EJECT = "Reset the trace";

    private static final ImageIcon ICON_TIMING_DIAGRAM = GuiUtils.createIconFromSVG("images/simulation-trace-graph.svg");
    private static final ImageIcon ICON_COPY_STATE = GuiUtils.createIconFromSVG("images/simulation-trace-copy.svg");
    private static final ImageIcon ICON_PASTE_STATE = GuiUtils.createIconFromSVG("images/simulation-trace-paste.svg");
    private static final ImageIcon ICON_MERGE_TRACE = GuiUtils.createIconFromSVG("images/simulation-trace-merge.svg");
    private static final ImageIcon ICON_SAVE_INITIAL_STATE = GuiUtils.createIconFromSVG("images/simulation-marking-save.svg");

    private static final String HINT_TIMING_DIAGRAM = "Generate trace timing diagram";
    private static final String HINT_COPY_STATE = "Copy trace to clipboard";
    private static final String HINT_PASTE_STATE = "Paste trace from clipboard";
    private static final String HINT_MERGE_TRACE = "Merge branch into trace";
    private static final String HINT_SAVE_INITIAL_STATE = "Save current state as initial";

    private VisualModel underlyingModel;

    protected JPanel controlPanel;
    protected JPanel infoPanel;
    protected JSplitPane splitPane;
    protected JScrollPane tracePane;
    protected JScrollPane statePane;
    protected JTable traceTable;

    private SpeedSlider speedSlider;
    private JButton playButton;
    private JButton backwardButton;
    private JButton forwardButton;
    private JButton recordButton;
    private JButton ejectButton;
    private JPanel panel;

    // cache of "excited" containers (the ones containing the excited simulation elements)
    protected HashMap<Container, Boolean> excitedContainers = new HashMap<>();

    protected Map<? extends Node, Integer> initialState;
    public HashMap<? extends Node, Integer> savedState;
    protected final Trace mainTrace = new Trace();
    protected final Trace branchTrace = new Trace();
    private  int loopPosition = -1;

    private Timer timer = null;
    private boolean random = false;

    private final boolean enableTraceGraph;

    public SimulationTool(boolean enableTraceGraph) {
        super();
        this.enableTraceGraph = enableTraceGraph;
    }

    @Override
    public JPanel getControlsPanel(final GraphEditor editor) {
        if (panel != null) {
            return panel;
        }

        playButton = GuiUtils.createIconButton(ICON_PLAY, HINT_PLAY);
        backwardButton = GuiUtils.createIconButton(ICON_BACKWARD, HINT_BACKWARD);
        forwardButton = GuiUtils.createIconButton(ICON_FORWARD, HINT_FORWARD);
        recordButton = GuiUtils.createIconButton(ICON_RECORD, HINT_RECORD);
        ejectButton = GuiUtils.createIconButton(ICON_EJECT, HINT_EJECT);

        speedSlider = new SpeedSlider();

        JButton generateGraphButton = GuiUtils.createIconButton(ICON_TIMING_DIAGRAM, HINT_TIMING_DIAGRAM);
        JButton copyStateButton = GuiUtils.createIconButton(ICON_COPY_STATE, HINT_COPY_STATE);
        JButton pasteStateButton = GuiUtils.createIconButton(ICON_PASTE_STATE, HINT_PASTE_STATE);
        JButton mergeTraceButton = GuiUtils.createIconButton(ICON_MERGE_TRACE, HINT_MERGE_TRACE);
        JButton saveInitStateButton = GuiUtils.createIconButton(ICON_SAVE_INITIAL_STATE, HINT_SAVE_INITIAL_STATE);

        JPanel simulationControl = new JPanel();
        simulationControl.add(playButton);
        simulationControl.add(backwardButton);
        simulationControl.add(forwardButton);
        simulationControl.add(recordButton);
        simulationControl.add(ejectButton);
        GuiUtils.setButtonPanelLayout(simulationControl, playButton.getPreferredSize());

        JPanel speedControl = new JPanel();
        speedControl.add(speedSlider);
        GuiUtils.setButtonPanelLayout(speedControl, speedSlider.getPreferredSize());

        JPanel traceControl = new JPanel();
        if (enableTraceGraph) {
            traceControl.add(generateGraphButton);
        }
        traceControl.add(copyStateButton);
        traceControl.add(pasteStateButton);
        traceControl.add(mergeTraceButton);
        traceControl.add(saveInitStateButton);
        GuiUtils.setButtonPanelLayout(simulationControl, copyStateButton.getPreferredSize());

        controlPanel = new JPanel();
        controlPanel.setLayout(new WrapLayout());
        controlPanel.add(simulationControl);
        controlPanel.add(speedControl);
        controlPanel.add(traceControl);

        traceTable = new JTable(new TraceTableModel());
        traceTable.getTableHeader().setDefaultRenderer(new FlatHeaderRenderer());
        traceTable.getTableHeader().setReorderingAllowed(false);
        traceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        traceTable.setRowHeight(SizeHelper.getComponentHeightFromFont(traceTable.getFont()));
        traceTable.setDefaultRenderer(Object.class, new TraceTableCellRendererImplementation());

        tracePane = new JScrollPane();
        tracePane.setViewportView(traceTable);
        tracePane.setMinimumSize(new Dimension(1, 50));

        statePane = new JScrollPane();
        statePane.setMinimumSize(new Dimension(1, 50));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tracePane, statePane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.add(splitPane, BorderLayout.CENTER);
        speedSlider.addChangeListener(e -> {
            if (timer != null) {
                timer.stop();
                int delay = speedSlider.getDelay();
                timer.setInitialDelay(delay);
                timer.setDelay(delay);
                timer.start();
            }
            updateState(editor);
            editor.requestFocus();
        });

        recordButton.addActionListener(event -> {
            if (timer == null) {
                timer = new Timer(speedSlider.getDelay(), event1 -> randomStep(editor));
                timer.start();
                random = true;
            } else if (random) {
                timer.stop();
                timer = null;
                random = false;
            } else {
                random = true;
            }
            updateState(editor);
            editor.requestFocus();
        });

        playButton.addActionListener(event -> {
            if (timer == null) {
                timer = new Timer(speedSlider.getDelay(), event1 -> step(editor));
                timer.start();
                random = false;
            } else if (!random) {
                timer.stop();
                timer = null;
                random = false;
            } else {
                random = false;
            }
            updateState(editor);
            editor.requestFocus();
        });

        ejectButton.addActionListener(event -> {
            clearTraces(editor);
            editor.requestFocus();
        });

        backwardButton.addActionListener(event -> {
            stepBack(editor);
            editor.requestFocus();
        });

        forwardButton.addActionListener(event -> {
            step(editor);
            editor.requestFocus();
        });

        generateGraphButton.addActionListener(event -> {
            generateTraceGraph(editor);
            editor.requestFocus();
        });

        copyStateButton.addActionListener(event -> {
            copyState(editor);
            editor.requestFocus();
        });

        pasteStateButton.addActionListener(event -> {
            pasteState(editor);
            editor.requestFocus();
        });

        mergeTraceButton.addActionListener(event -> {
            mergeTrace(editor);
            editor.requestFocus();
        });

        saveInitStateButton.addActionListener(event -> {
            savedState = readModelState();
            editor.requestFocus();
        });

        traceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = traceTable.getSelectedColumn();
                int row = traceTable.getSelectedRow();
                if (col == 0) {
                    if (row < mainTrace.size()) {
                        boolean hasProgress = true;
                        while (hasProgress && (branchTrace.getPosition() > 0)) {
                            hasProgress = quietStepBack();
                        }
                        while (hasProgress && (mainTrace.getPosition() > row)) {
                            hasProgress = quietStepBack();
                        }
                        while (hasProgress && (mainTrace.getPosition() < row)) {
                            hasProgress = quietStep();
                        }
                    }
                } else {
                    if ((row >= mainTrace.getPosition()) && (row < mainTrace.getPosition() + branchTrace.size())) {
                        boolean hasProgress = true;
                        while (hasProgress && (mainTrace.getPosition() + branchTrace.getPosition() > row)) {
                            hasProgress = quietStepBack();
                        }
                        while (hasProgress && (mainTrace.getPosition() + branchTrace.getPosition() < row)) {
                            hasProgress = quietStep();
                        }
                    }
                }
                updateState(editor);
                editor.requestFocus();
            }
        });

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(0, 0));
        return panel;
    }

    public void setStatePaneVisibility(boolean visible) {
        statePane.setVisible(visible);
        splitPane.setDividerSize(visible ? 10 : 0);
    }

    @Override
    public void activated(final GraphEditor editor) {
        super.activated(editor);
        generateUnderlyingModel(editor.getModel());
        editor.getWorkspaceEntry().captureMemento();
        initialState = readModelState();
        setStatePaneVisibility(false);
        resetTraces(editor);
    }

    @Override
    public void deactivated(final GraphEditor editor) {
        super.deactivated(editor);
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        editor.getWorkspaceEntry().cancelMemento();
        applySavedState(editor);
        savedState = null;
        underlyingModel = null;
    }

    @Override
    public void setPermissions(final GraphEditor editor) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        we.setCanModify(false);
        we.setCanSelect(false);
        we.setCanCopy(false);
    }

    public void generateUnderlyingModel(VisualModel model) {
        setUnderlyingModel(model);
    }

    public void setUnderlyingModel(VisualModel model) {
        this.underlyingModel = model;
    }

    public VisualModel getUnderlyingModel() {
        return underlyingModel;
    }

    public boolean isActivated() {
        return underlyingModel != null;
    }

    public void updateState(final GraphEditor editor) {
        if (timer == null) {
            playButton.setIcon(ICON_PLAY);
            playButton.setToolTipText(HINT_PLAY);
            recordButton.setIcon(ICON_RECORD);
            recordButton.setToolTipText(HINT_RECORD);
        } else {
            if (random) {
                playButton.setIcon(ICON_PLAY);
                playButton.setToolTipText(HINT_PLAY);
                recordButton.setIcon(ICON_STOP);
                recordButton.setToolTipText(HINT_STOP);
                timer.setDelay(speedSlider.getDelay());
            } else if (branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress())) {
                playButton.setIcon(ICON_PAUSE);
                playButton.setToolTipText(HINT_PAUSE);
                recordButton.setIcon(ICON_RECORD);
                timer.setDelay(speedSlider.getDelay());
            } else {
                playButton.setIcon(ICON_PLAY);
                playButton.setToolTipText(HINT_PLAY);
                recordButton.setIcon(ICON_RECORD);
                recordButton.setToolTipText(HINT_RECORD);
                timer.stop();
                timer = null;
            }
        }
        playButton.setEnabled(branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress()));
        ejectButton.setEnabled(!mainTrace.isEmpty() || !branchTrace.isEmpty());
        backwardButton.setEnabled((mainTrace.getPosition() > 0) || (branchTrace.getPosition() > 0));
        forwardButton.setEnabled(branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress()));
        traceTable.tableChanged(new TableModelEvent(traceTable.getModel()));
        editor.repaint();
    }

    public void scrollTraceToBottom() {
        JScrollBar verticalScrollBar = tracePane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private boolean quietStepBack() {
        excitedContainers.clear();

        boolean result = false;
        String ref = null;
        boolean decMain = false;
        boolean decBranch = false;
        if (branchTrace.getPosition() > 0) {
            ref = branchTrace.get(branchTrace.getPosition() - 1);
            decBranch = true;
        } else if (mainTrace.getPosition() > 0) {
            ref = mainTrace.get(mainTrace.getPosition() - 1);
            decMain = true;
        }
        if (unfire(ref)) {
            if (decMain) {
                mainTrace.decPosition();
            }
            if (decBranch) {
                branchTrace.decPosition();
            }
            if ((branchTrace.getPosition() == 0) && !mainTrace.isEmpty()) {
                branchTrace.clear();
            }
            result = true;
        }
        return result;
    }

    private boolean stepBack(final GraphEditor editor) {
        boolean ret = quietStepBack();
        updateState(editor);
        return ret;
    }

    private boolean quietStep() {
        excitedContainers.clear();

        boolean result = false;
        String ref = null;
        boolean incMain = false;
        boolean incBranch = false;
        if (branchTrace.canProgress()) {
            ref = branchTrace.getCurrent();
            incBranch = true;
        } else if (mainTrace.canProgress()) {
            ref = mainTrace.getCurrent();
            incMain = true;
        }
        if (fire(ref)) {
            if (incMain) {
                mainTrace.incPosition();
            }
            if (incBranch) {
                branchTrace.incPosition();
            }
            result = true;
            if (!branchTrace.canProgress() && !mainTrace.canProgress() && (loopPosition >= 0)) {
                mainTrace.setPosition(loopPosition);
            }
        }
        return result;
    }

    private boolean step(final GraphEditor editor) {
        boolean ret = quietStep();
        updateState(editor);
        return ret;
    }

    private boolean randomStep(final GraphEditor editor) {
        ArrayList<? extends Node> enabledTransitions = getEnabledNodes();
        if (enabledTransitions.isEmpty()) {
            return false;
        }
        int randomIndex = (int) (Math.random() * enabledTransitions.size());
        Node transition = enabledTransitions.get(randomIndex);
        executeTransition(editor, transition);
        return true;
    }

    private void resetTraces(final GraphEditor editor) {
        writeModelState(initialState);
        mainTrace.setPosition(0);
        branchTrace.clear();
        updateState(editor);
    }

    private void clearTraces(final GraphEditor editor) {
        writeModelState(initialState);
        mainTrace.clear();
        branchTrace.clear();
        loopPosition = -1;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        updateState(editor);
    }

    public void generateTraceGraph(final GraphEditor editor) {
    }

    private void copyState(final GraphEditor editor) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Solution solution = new Solution(mainTrace, branchTrace);
        solution.setLoopPosition(loopPosition);
        StringSelection stringSelection = new StringSelection(TraceUtils.serialiseSolution(solution));
        clip.setContents(stringSelection, this);
        updateState(editor);
    }

    private void pasteState(final GraphEditor editor) {
        String str = getClipboardText();
        writeModelState(initialState);
        Solution solution = TraceUtils.deserialiseSolution(str);
        mainTrace.clear();
        if (solution.getMainTrace() != null) {
            mainTrace.addAll(solution.getMainTrace());
            while (mainTrace.getPosition() < solution.getMainTrace().getPosition()) {
                if (!quietStep()) break;
            }
        }
        branchTrace.clear();
        if (solution.getBranchTrace() != null) {
            branchTrace.addAll(solution.getBranchTrace());
            while (branchTrace.getPosition() < solution.getBranchTrace().getPosition()) {
                if (!quietStep()) break;
            }
        }
        loopPosition = solution.getLoopPosition();
        updateState(editor);
    }

    private String getClipboardText() {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clip.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        String result = "";
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                System.out.println(e);
            }
        }
        return result;
    }

    public Trace getCombinedTrace() {
        Trace result = new Trace();
        if (branchTrace.isEmpty()) {
            result.addAll(mainTrace);
            result.setPosition(mainTrace.getPosition());
        } else {
            List<String> commonTrace = mainTrace.subList(0, mainTrace.getPosition());
            result.addAll(commonTrace);
            result.addAll(branchTrace);
            result.setPosition(mainTrace.getPosition() + branchTrace.getPosition());
        }
        return result;
    }

    private void mergeTrace(final GraphEditor editor) {
        if (!branchTrace.isEmpty()) {
            Trace combinedTrace = getCombinedTrace();
            mainTrace.clear();
            branchTrace.clear();
            loopPosition = -1;
            mainTrace.addAll(combinedTrace);
            mainTrace.setPosition(combinedTrace.getPosition());
        }
        updateState(editor);
    }

    private final class TraceTableCellRendererImplementation implements TableCellRenderer {
        private final JLabel label = new JLabel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paint(g);
            }
        };

        private boolean isActive(int row, int column) {
            if (column == 0) {
                if (!mainTrace.isEmpty() && branchTrace.isEmpty()) {
                    return row == mainTrace.getPosition();
                }
            } else {
                if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition())
                        && (row < mainTrace.getPosition() + branchTrace.size())) {
                    return row == mainTrace.getPosition() + branchTrace.getPosition();
                }
            }
            return false;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel result = null;
            label.setBorder(GuiUtils.getTableCellBorder());
            if (isActivated() && (value instanceof String)) {
                label.setText(value.toString());
                if (isActive(row, column)) {
                    label.setForeground(table.getSelectionForeground());
                    label.setBackground(table.getSelectionBackground());
                } else {
                    label.setForeground(table.getForeground());
                    label.setBackground(table.getBackground());
                }
                result = label;
            }
            return result;
        }
    }

    private class TraceTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return (column == 0) ? "<html><b>Trace</b></html>" : "<html><b>Branch</b></html>";
        }

        @Override
        public int getRowCount() {
            return Math.max(mainTrace.size(), mainTrace.getPosition() + branchTrace.size());
        }

        @Override
        public Object getValueAt(int row, int column) {
            String ref = null;
            if (column == 0) {
                if (!mainTrace.isEmpty() && (row < mainTrace.size())) {
                    ref = mainTrace.get(row);
                }
            } else {
                if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition()) && (row < mainTrace.getPosition() + branchTrace.size())) {
                    ref = branchTrace.get(row - mainTrace.getPosition());
                }
            }

            String result = getTraceLabelByReference(ref);
            if ((result != null) && (loopPosition >= 0) && (column == 0) && (row >= loopPosition)) {
                result = TraceUtils.addLoopDecoration(result, row == loopPosition, row == mainTrace.size() - 1);
            }
            return result;
        }
    }

    @Override
    public boolean keyPressed(GraphEditorKeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_OPEN_BRACKET) {
            stepBack(e.getEditor());
            return true;
        }
        if (e.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET) {
            step(e.getEditor());
            return true;
        }
        return super.keyPressed(e);
    }

    public void executeTransition(final GraphEditor editor, Node candidateNode) {
        if (candidateNode == null) return;

        String ref = null;
        // If clicked on the trace event, do the step forward.
        if (branchTrace.isEmpty() && !mainTrace.isEmpty() && (mainTrace.getPosition() < mainTrace.size())) {
            ref = mainTrace.get(mainTrace.getPosition());
        }
        // Otherwise form/use the branch trace.
        if (!branchTrace.isEmpty() && (branchTrace.getPosition() < branchTrace.size())) {
            ref = branchTrace.get(branchTrace.getPosition());
        }
        MathModel mathModel = getUnderlyingModel().getMathModel();
        if ((mathModel != null) && (ref != null)) {
            Node node = mathModel.getNodeByReference(ref);
            if (node == candidateNode) {
                step(editor);
                return;
            }
        }
        while (branchTrace.getPosition() < branchTrace.size()) {
            branchTrace.removeCurrent();
        }
        if (mathModel != null) {
            String candidateRef = mathModel.getNodeReference(candidateNode);
            if (candidateRef != null) {
                branchTrace.add(candidateRef);
            }
        }
        step(editor);
        scrollTraceToBottom();
    }

    @Override
    public void mousePressed(GraphEditorMouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            VisualModel model = e.getModel();
            Func<Node, Boolean> filter = node -> {
                if (node instanceof VisualComponent) {
                    VisualComponent component = (VisualComponent) node;
                    return isEnabledNode(component.getReferencedComponent());
                }
                return false;
            };
            Node deepestNode = HitMan.hitDeepest(e.getPosition(), model.getRoot(), filter);
            if (deepestNode instanceof VisualComponent) {
                VisualComponent component = (VisualComponent) deepestNode;
                executeTransition(e.getEditor(), component.getReferencedComponent());
            }
        }
    }

    @Override
    public String getHintText(final GraphEditor editor) {
        return "Click on a highlighted node to fire it.";
    }

    @Override
    public String getLabel() {
        return "Simulation";
    }

    @Override
    public int getHotKeyCode() {
        return KeyEvent.VK_M;
    }

    @Override
    public Icon getIcon() {
        return GuiUtils.createIconFromSVG("images/tool-simulation.svg");
    }

    @Override
    public Cursor getCursor(boolean menuKeyDown, boolean shiftKeyDown, boolean altKeyDown) {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    public void setTraces(Trace mainTrace, Trace branchTrace, int loopPosition, GraphEditor editor) {
        this.mainTrace.clear();
        if (mainTrace != null) {
            this.mainTrace.addAll(mainTrace);
        }
        this.branchTrace.clear();
        if (branchTrace != null) {
            this.branchTrace.addAll(branchTrace);
        }
        this.loopPosition = loopPosition;
        updateState(editor);
    }

    public MathNode getTraceCurrentNode() {
        String ref = null;
        if (branchTrace.canProgress()) {
            ref = branchTrace.getCurrent();
        } else if (branchTrace.isEmpty() && mainTrace.canProgress()) {
            ref = mainTrace.getCurrent();
        }
        MathNode result = null;
        MathModel mathModel = getUnderlyingModel().getMathModel();
        if ((mathModel != null) && (ref != null)) {
            result = mathModel.getNodeByReference(ref);
        }
        return result;
    }

    @Override
    public Decorator getDecorator(final GraphEditor editor) {
        return node -> {
            if ((node instanceof VisualPage) || (node instanceof VisualGroup)) {
                return getContainerDecoration((Container) node);
            }
            if (node instanceof VisualConnection) {
                return getConnectionDecoration((VisualConnection) node);
            }
            if (node instanceof VisualComponent) {
                return getComponentDecoration((VisualComponent) node);
            }
            return null;
        };
    }

    public Decoration getContainerDecoration(Container container) {
        final boolean isExcited = isContainerExcited(container);
        return new ContainerDecoration() {
            @Override
            public Color getColorisation() {
                return null;
            }
            @Override
            public Color getBackground() {
                return null;
            }
            @Override
            public boolean isContainerExcited() {
                return isExcited;
            }
        };
    }

    public Decoration getConnectionDecoration(VisualConnection connection) {
        final boolean isExcited = isConnectionExcited(connection);
        return new Decoration() {
            @Override
            public Color getColorisation() {
                return isExcited ? SimulationDecorationSettings.getExcitedComponentColor() : null;
            }
            @Override
            public Color getBackground() {
                return null;
            }
        };
    }

    public Decoration getComponentDecoration(VisualComponent component) {
        Node node = component.getReferencedComponent();
        Node currentTraceNode = getTraceCurrentNode();
        final boolean isExcited = isEnabledNode(node);
        final boolean isSuggested = isExcited && (node == currentTraceNode);
        return new Decoration() {
            @Override
            public Color getColorisation() {
                return isExcited ? SimulationDecorationSettings.getExcitedComponentColor() : null;
            }
            @Override
            public Color getBackground() {
                return isSuggested ? SimulationDecorationSettings.getSuggestedComponentColor() : null;
            }
        };
    }

    public boolean isContainerExcited(Container container) {
        if (excitedContainers.containsKey(container)) return excitedContainers.get(container);
        boolean result = false;
        for (Node node : container.getChildren()) {
            if (node instanceof VisualComponent) {
                VisualComponent component = (VisualComponent) node;
                result = isEnabledNode(component.getReferencedComponent());
            } else if (node instanceof Container) {
                result = isContainerExcited((Container) node);
            }
            if (result) break;
        }
        excitedContainers.put(container, result);
        return result;
    }

    public boolean isConnectionExcited(VisualConnection connection) {
        return false;
    }

    @Override
    public void lostOwnership(Clipboard clip, Transferable arg) {
    }

    public String getTraceLabelByReference(String ref) {
        return ref;
    }

    public abstract HashMap<? extends Node, Integer> readModelState();

    public abstract void writeModelState(Map<? extends Node, Integer> state);

    public abstract void applySavedState(GraphEditor editor);

    public abstract ArrayList<? extends Node> getEnabledNodes();

    public abstract boolean isEnabledNode(Node node);

    public abstract boolean fire(String ref);

    public abstract boolean unfire(String ref);

}
