package buoyx.docking;

import buoy.widget.*;
import buoy.event.*;

import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * This is a Widget that can be added to a {@link DockingContainer}, and rearranged by the user.  It is a
 * container which holds a single content Widget and draws a border around it.  By clicking on the
 * border, the user can drag the DockableWidget to any other DockingContainer in the same window,
 * group the Widgets in a DockingContainer into tabs, and reorder the Widgets within a tab.
 * <p>
 * The border drawn by this class consists of a fairly simple title bar.  You can change the appearance
 * of the border by creating a subclass and overriding {@link #getBorderInsets()},
 * {@link #paintBorder(java.awt.Graphics2D)}, and {@link #isInDragRegion(java.awt.Point)}.
 *
 * @author Peter Eastman
 */

public class DockableWidget extends WidgetContainer
{
  private Widget content;
  private String label;

  /**
   * Create a DockableWidget with no content Widget or label.
   */

  public DockableWidget()
  {
    component = new DockableWidgetPanel();
    DragManager manager = DragManager.getDragManager();
    addEventLink(MousePressedEvent.class, manager, "beginDraggingWidget");
    addEventLink(MouseDraggedEvent.class, manager, "mouseDragged");
    addEventLink(MouseReleasedEvent.class, manager, "mouseReleased");
  }

  /**
   * Create a DockableWidget.
   *
   * @param content     the content Widget
   * @param label       the label to appear in the title bar
   */

  public DockableWidget(Widget content, String label)
  {
    this();
    setContent(content);
    setLabel(label);
  }

  /**
   * Get the content Widget.
   */

  public Widget getContent()
  {
    return content;
  }

  /**
   * Set the content Widget.
   */

  public void setContent(Widget widget)
  {
    if (content != null)
      remove(content);
    content = widget;
    if (content != null)
    {
      if (content.getParent() != null)
        content.getParent().remove(content);
      ((Container) getComponent()).add(content.getComponent());
      setAsParent(content);
    }
    invalidateSize();
  }

  /**
   * Get the label which appears in the title bar.
   */

  public String getLabel()
  {
    return label;
  }

  /**
   * Set the label which appears in the title bar.
   */

  public void setLabel(String label)
  {
    this.label = label;
  }

  /**
   * This method is called to determine the thickness of the border on each side.  If you
   * customize the appearance of the border, override this method to return the correct insets.
   */

  protected Insets getBorderInsets()
  {
    FontMetrics fm = getComponent().getFontMetrics(getComponent().getFont());
    return new Insets(fm.getMaxAscent()+fm.getMaxDescent()+4, 0, 0, 0);
  }

  /**
   * This method is called to paint the border.  To customize the appearance of the border,
   * override this method.
   */

  protected void paintBorder(Graphics2D g)
  {
    Rectangle bounds = getBounds();
    Insets insets = getBorderInsets();
    g.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, insets.top, Color.LIGHT_GRAY));
    g.fillRect(0, 0, bounds.width, insets.top);
    g.setColor(Color.DARK_GRAY);
    g.drawLine(0, insets.top-1, bounds.width, insets.top-1);
    g.setColor(Color.BLACK);
    if (label != null)
    {
      FontMetrics fm = getComponent().getFontMetrics(getComponent().getFont());
      g.drawString(label, 2, fm.getMaxAscent()+2);
    }
  }

  /**
   * When the user clicks inside the DockableWidget, this method is called to determine whether
   * the click was in the "drag region".  If it returns true, a drag operation will be initiated
   * to move the DockableWidget to a new location.
   */

  protected boolean isInDragRegion(Point pos)
  {
    return (pos.y < getBorderInsets().top);
  }

  public Dimension getPreferredSize()
  {
    Dimension size = (content == null ? new Dimension(0, 0) : new Dimension(content.getPreferredSize()));
    Insets insets = getBorderInsets();
    size.width += insets.left+insets.right;
    size.height += insets.top+insets.bottom;
    return size;
  }

  public Dimension getMinimumSize()
  {
    Dimension size = (content == null ? new Dimension(0, 0) : new Dimension(content.getMinimumSize()));
    Insets insets = getBorderInsets();
    if (size.width > 0)
      size.width += insets.left+insets.right;
    if (size.height > 0)
      size.height += insets.top+insets.bottom;
    return size;
  }

  public int getChildCount()
  {
    return (content == null ? 0 : 1);
  }

  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> children = new ArrayList<Widget>();
    if (content != null)
      children.add(content);
    return children;
  }

  public void remove(Widget widget)
  {
    if (content == widget)
    {
      ((JPanel) getComponent()).remove(widget.getComponent());
      removeAsParent(widget);
      invalidateSize();
    }
  }

  public void removeAll()
  {
    if (content != null)
      remove(content);
  }

  public void layoutChildren()
  {
    if (content == null)
      return;
    Insets insets = getBorderInsets();
    Rectangle bounds = getBounds();
    content.getComponent().setBounds(insets.left, insets.top, Math.max(0, bounds.width-insets.left-insets.right), Math.max(0, bounds.height-insets.top-insets.bottom));
    if (content instanceof WidgetContainer)
      ((WidgetContainer) content).layoutChildren();
  }

  public Rectangle getBounds()
  {
    Rectangle bounds = super.getBounds();
    Widget parentWidget = getParent();
    Container parent = getComponent().getParent();
    while (parent != parentWidget.getComponent())
    {
      bounds.x += parent.getX();
      bounds.y += parent.getY();
      parent = parent.getParent();
    }
    return bounds;
  }

  private class DockableWidgetPanel extends JPanel
  {
    public DockableWidgetPanel()
    {
      setLayout(null);
    }

    /**
     * Optionally fill the component with its background color, then paint the border.
     */

    public void paintComponent(Graphics g)
    {
      if (isOpaque())
      {
        Dimension size = getSize();
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(getForeground());
      }
      DockableWidget.this.paintBorder((Graphics2D) g);
    }

    /**
     * This component is opaque if its WidgetContainer is set to be opaque.
     */

    public boolean isOpaque()
    {
      return DockableWidget.this.isOpaque();
    }
  }
}
