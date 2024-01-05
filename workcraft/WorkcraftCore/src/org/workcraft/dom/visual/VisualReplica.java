package org.workcraft.dom.visual;

import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.gui.properties.PropertyDeclaration;
import org.workcraft.gui.tools.Decoration;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.builtin.settings.VisualCommonSettings;
import org.workcraft.utils.ColorUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class VisualReplica extends VisualTransformableNode implements Replica, Drawable {
    public static final String PROPERTY_NAME_POSITIONING = "Name positioning";
    public static final String PROPERTY_NAME_COLOR = "Name color";
    public static final String PROPERTY_COLOR = "Color";
    public static final String PROPERTY_FILL_COLOR = "Fill color";

    public static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.ITALIC, 1);

    protected double size = VisualCommonSettings.getNodeSize();
    protected double strokeWidth = VisualCommonSettings.getStrokeWidth();
    private Color foregroundColor = VisualCommonSettings.getBorderColor();
    private Color fillColor = VisualCommonSettings.getFillColor();

    private Positioning namePositioning = VisualCommonSettings.getNamePositioning();
    private RenderedText nameRenderedText = new RenderedText("", getNameFont(), getNamePositioning(), getNameOffset());
    private Color nameColor = VisualCommonSettings.getNameColor();

    private VisualComponent master = null;

    public VisualReplica() {
        this(true, true);
    }

    public VisualReplica(boolean hasColorProperties, boolean hasNameProperties) {
        super();
        if (hasColorProperties) {
            addColorPropertyDeclarations();
        }
        if (hasNameProperties) {
            addNamePropertyDeclarations();
        }
    }

    private void addColorPropertyDeclarations() {
        addPropertyDeclaration(new PropertyDeclaration<>(Color.class, PROPERTY_COLOR,
                this::setForegroundColor, this::getForegroundColor).setCombinable().setTemplatable());

        addPropertyDeclaration(new PropertyDeclaration<>(Color.class, PROPERTY_FILL_COLOR,
                this::setFillColor, this::getFillColor).setCombinable().setTemplatable());
    }

    private void addNamePropertyDeclarations() {
        addPropertyDeclaration(new PropertyDeclaration<>(Positioning.class, PROPERTY_NAME_POSITIONING,
                this::setNamePositioning, this::getNamePositioning).setCombinable().setTemplatable());

        addPropertyDeclaration(new PropertyDeclaration<>(Color.class, PROPERTY_NAME_COLOR,
                this::setNameColor, this::getNameColor).setCombinable().setTemplatable());
    }

    public Color getNameColor() {
        return nameColor;
    }

    public void setNameColor(Color value) {
        if (!nameColor.equals(value)) {
            nameColor = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_NAME_COLOR));
        }
    }

    public Positioning getNamePositioning() {
        return namePositioning;
    }

    public void setNamePositioning(Positioning value) {
        if (namePositioning != value) {
            namePositioning = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_NAME_POSITIONING));
        }
    }

    public Font getNameFont() {
        return NAME_FONT.deriveFont((float) VisualCommonSettings.getNameFontSize());
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color value) {
        if (!foregroundColor.equals(value)) {
            foregroundColor = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_COLOR));
        }
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color value) {
        if (!fillColor.equals(value)) {
            fillColor = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_FILL_COLOR));
        }
    }

    public Point2D getOffset(Positioning positioning) {
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        double xOffset = (positioning.xSign < 0) ? bb.getMinX() : (positioning.xSign > 0) ? bb.getMaxX() : bb.getCenterX();
        double yOffset = (positioning.ySign < 0) ? bb.getMinY() : (positioning.ySign > 0) ? bb.getMaxY() : bb.getCenterY();
        return new Point2D.Double(xOffset, yOffset);
    }

    public boolean getNameVisibility() {
        return VisualCommonSettings.getNameVisibility();
    }

    public Point2D getNameOffset() {
        return getOffset(getNamePositioning());
    }

    private void cacheNameRenderedText(DrawRequest r) {
        String name = null;
        MathModel mathModel = r.getModel().getMathModel();
        if (master != null) {
            MathNode mathNode = master.getReferencedComponent();
            name = mathModel.getNodeReference(mathNode);
        }

        if (name == null) {
            name = "";
        }
        cacheNameRenderedText(name, getNameFont(), getNamePositioning(), getNameOffset());
    }

    protected void cacheNameRenderedText(String text, Font font, Positioning positioning, Point2D offset) {
        if (nameRenderedText.isDifferent(text, font, positioning, offset)) {
            nameRenderedText = new RenderedText(text, font, positioning, offset);
        }
    }

    protected void drawNameInLocalSpace(DrawRequest r) {
        if (getNameVisibility() && (nameRenderedText != null) && !nameRenderedText.isEmpty()) {
            cacheNameRenderedText(r);
            Graphics2D g = r.getGraphics();
            Decoration d = r.getDecoration();
            g.setColor(ColorUtils.colorise(getNameColor(), d.getColorisation()));
            nameRenderedText.draw(g);
        }
    }

    // This method is needed for VisualGroup to update the rendered text of its children
    // before they were drawn, which is necessary for computing their bounding boxes
    public void cacheRenderedText(DrawRequest r) {
        cacheNameRenderedText(r);
    }

    /*
     * The internal bounding box does not include the related label and name of the node
     */
    public Rectangle2D getInternalBoundingBoxInLocalSpace() {
        return new Rectangle2D.Double(-size / 2, -size / 2, size, size);
    }

    @Override
    public Rectangle2D getBoundingBoxInLocalSpace() {
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        if (getNameVisibility()) {
            bb = BoundingBoxHelper.union(bb, getNameBoundingBox());
        }
        return bb;
    }

    public Rectangle2D getNameBoundingBox() {
        if ((nameRenderedText != null) && !nameRenderedText.isEmpty()) {
            return nameRenderedText.getBoundingBox();
        } else {
            return new Rectangle2D.Double();
        }
    }

    @Override
    public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
        return getInternalBoundingBoxInLocalSpace().contains(pointInLocalSpace);
    }

    @Override
    public void copyStyle(Stylable src) {
        super.copyStyle(src);
        if (src instanceof VisualReplica) {
            VisualReplica srcReplica = (VisualReplica) src;
            setForegroundColor(srcReplica.getForegroundColor());
            setFillColor(srcReplica.getFillColor());
            setNameColor(srcReplica.getNameColor());
            setNamePositioning(srcReplica.getNamePositioning());
        }
    }

    @Override
    public void rotateClockwise() {
        setNamePositioning(getNamePositioning().rotateClockwise());
        super.rotateClockwise();
    }

    @Override
    public void rotateCounterclockwise() {
        setNamePositioning(getNamePositioning().rotateCounterclockwise());
        super.rotateCounterclockwise();
    }

    @Override
    public void flipHorizontal() {
        setNamePositioning(getNamePositioning().flipHorizontal());
        super.flipHorizontal();
    }

    @Override
    public void flipVertical() {
        setNamePositioning(getNamePositioning().flipVertical());
        super.flipVertical();
    }

    @Override
    public void setMaster(VisualComponent value) {
        if (master != value) {
            if (master != null) {
                master.removeReplica(this);
            }
            master = value;
            if (master != null) {
                master.addReplica(this);
            }
        }
    }

    @Override
    public VisualComponent getMaster() {
        return master;
    }

    @Override
    public void draw(DrawRequest r) {
        drawNameInLocalSpace(r);
    }

}
