package buoyx.docking;

import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.dnd.*;

/**
 * This is a non-public class which is used internally by the docking framework.  It coordinates the
 * dragging of DockableWidgets from one container to another.
 *
 * @author Peter Eastman
 */

class DragManager
{
  private boolean inDrag;
  private DockableWidget draggedWidget;
  private DockingContainer originalContainer;
  private int originalTab, originalIndex;
  private Rectangle outline;
  private DragTarget dragTarget;
  private DragMarker dragMarker;

  private static DragManager manager;
  private static TexturePaint ditheredPaint;

  static
  {
    // Created a dithered TexturePaint that will be used to hilighting potential drop targets.

    BufferedImage dithered = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(2, 2);
    dithered.setRGB(0, 0, 0xFFFFFFFF);
    dithered.setRGB(1, 1, 0xFFFFFFFF);
    dithered.setRGB(0, 1, 0xFF000000);
    dithered.setRGB(1, 0, 0xFF000000);
    ditheredPaint = new TexturePaint(dithered, new Rectangle(2, 2));
  }

  private DragManager()
  {
  }

  static DragManager getDragManager()
  {
    if (manager == null)
      manager = new DragManager();
    return manager;
  }

  void beginDraggingWidget(MousePressedEvent ev)
  {
    draggedWidget = (DockableWidget) ev.getWidget();
    if (ev.getButton() == MouseEvent.BUTTON1 && draggedWidget.isInDragRegion(ev.getPoint()))
    {
      inDrag = true;
      originalContainer = (DockingContainer) draggedWidget.getParent();
      originalTab = originalContainer.getChildTabIndex(draggedWidget);
      originalIndex = originalContainer.getChildIndexInTab(draggedWidget);
      outline = draggedWidget.getBounds();
      Point pos = ev.getPoint();
      outline.x = -pos.x;
      outline.y = -pos.y;
    }
  }

  void beginDraggingTab(DockingContainer container, int tab, Rectangle outline)
  {
    inDrag = true;
    originalContainer = container;
    originalTab = tab;
    originalIndex = -1;
    this.outline = outline;
  }

  void mouseDragged(MouseDraggedEvent ev)
  {
    if (!inDrag)
      return;
    if (dragMarker == null)
    {
      // Create a component to act as the marker.

      dragMarker = new DragMarker();
      dragMarker.setVisible(true);
    }
    dragTarget = findDragTarget(ev);

    // Figure out the location for the marker.

    Rectangle targetBounds;
    Component targetComponent;
    if (dragTarget != null)
    {
      DockingContainer dock = dragTarget.container;
      if (dragTarget.index > -1 && dragTarget.tab == dock.getSelectedTab())
        targetBounds = findInsertionAreaBounds(dragTarget.container, dragTarget.tab, dragTarget.index);
      else
        targetBounds = dock.getTabBounds(dragTarget.tab);
      dragMarker.setHilighted(true);
      targetComponent = dock.getComponent();
    }
    else
    {
      // Just draw an outline of the widget or tab being dragged.

      Point pos = ev.getPoint();
      targetBounds = new Rectangle(outline.x+pos.x, outline.y+pos.y, outline.width, outline.height);
      dragMarker.setHilighted(false);
      targetComponent = ev.getComponent().getParent();
    }

    // Transform the rectangle to screen coordinates.

    Point targetOrigin = new Point(targetBounds.x, targetBounds.y);
    SwingUtilities.convertPointToScreen(targetOrigin, targetComponent);
    dragMarker.setBounds(targetOrigin.x, targetOrigin.y, targetBounds.width, targetBounds.height);

    // Also set the cursor.

    Component window = SwingUtilities.getWindowAncestor(ev.getWidget().getComponent());
    if (window != null)
      window.setCursor(dragTarget == null ? DragSource.DefaultMoveNoDrop : DragSource.DefaultMoveDrop);
  }

  void mouseReleased(WidgetMouseEvent ev)
  {
    if (!inDrag)
      return;
    if (dragMarker != null)
      dragMarker.dispose();
    dragMarker = null;
    DockingEvent event = null;
    if (dragTarget != null)
    {
      // Move this Widget to its new location.

      DockingContainer dock = dragTarget.container;
      int tabToDisplay = dock.getSelectedTab();
      if (dragTarget.index == -1)
        dragTarget.index = (dragTarget.tab == dock.getTabCount() ? 0 : dock.getTabChildCount(dragTarget.tab));
      if (originalIndex == -1)
      {
        // A whole tab was being dragged.  First find all the widgets in that tab.

        DockableWidget widget[] = new DockableWidget [originalContainer.getTabChildCount(originalTab)];
        for (int i = 0; i < widget.length; i++)
          widget[i] = originalContainer.getChild(originalTab, i);

        // Remove them from the container.

        for (int i = 0; i < widget.length; i++)
          originalContainer.remove(widget[i]);

        // Now add them at the appropriate location.

        if (originalContainer == dock && originalTab < dragTarget.tab)
          dragTarget.tab--;
        for (int i = 0; i < widget.length; i++)
          dock.addDockableWidget(widget[i], dragTarget.tab, dragTarget.index+i);
        event = new DockingEvent(originalContainer, dock, widget);
      }
      else
      {
        // A single widget was being dragged.

        if (originalContainer == dock)
        {
          // It's being moved within the same DockingContainer it's already in.

          if (originalTab == dragTarget.tab && originalIndex < dragTarget.index)
            dragTarget.index--;
          else if (originalTab < dragTarget.tab && dock.getTabChildCount(originalTab) == 1)
            dragTarget.tab--;
        }
        originalContainer.remove(draggedWidget);
        dock.addDockableWidget(draggedWidget, dragTarget.tab, dragTarget.index);
        event = new DockingEvent(originalContainer, dock, new DockableWidget[] {draggedWidget});
      }
      if (tabToDisplay > -1 && tabToDisplay < dock.getTabCount())
        dock.setSelectedTab(tabToDisplay);
    }

    // Reset the cursor.

    Component window = SwingUtilities.getWindowAncestor(ev.getWidget().getComponent());
    if (window != null)
      window.setCursor(null);
    dragTarget = null;
    inDrag = false;

    // Dispatch an event to report the move.

    if (event != null)
    {
      event.getSourceContainer().dispatchEvent(event);
      if (event.getTargetContainer() != event.getSourceContainer())
        event.getTargetContainer().dispatchEvent(event);
    }
  }

  private DragTarget findDragTarget(WidgetMouseEvent ev)
  {
    // First, find the parent window, and convert the point to the window's coordinate system.

    WidgetContainer parent = ev.getWidget().getParent();
    while (!(parent instanceof WindowWidget))
      parent = parent.getParent();
    Point pos = ev.getPoint();
    tranformPointToWindow(ev.getWidget(), pos);

    // Now see what it is over.

    DockingContainer dock = null;
    DragTarget target = null;
    WidgetContainer container = parent;
    boolean found;
    do
    {
      found = false;
      for (Widget w : container.getChildren())
      {
        if (!w.getComponent().isShowing())
          continue;
        Rectangle bounds = w.getComponent().getBounds();
        tranformRectangleToWindow(w, bounds);
        if (bounds.contains(pos))
        {
          if (w instanceof DockingContainer)
          {
            dock = (DockingContainer) w;

            // See if it's over the docking region.

            int tab = dock.getSelectedTab();
            if (tab > -1)
              for (int i = 0; i <= dock.getTabChildCount(tab); i++)
              {
                Rectangle insertionBounds = findInsertionAreaBounds(dock, tab, i);
                tranformRectangleToWindow(dock, insertionBounds);
                if (insertionBounds.contains(pos))
                  target = new DragTarget(dock, tab, i);
              }

            // See if it's over a tab.

            for (int i = 0; i <= dock.getTabCount(); i++)
            {
              Rectangle tabBounds = dock.getTabBounds(i);
              if (tabBounds == null)
                continue;
              tranformRectangleToWindow(dock, tabBounds);
              if (tabBounds.contains(pos))
                target = new DragTarget(dock, i, -1);
            }
          }
          if (w instanceof WidgetContainer)
          {
            container = (WidgetContainer) w;
            found = true;
          }
          break;
        }
      }
    } while (found && target == null);
    if (target != null)
    {
      if (originalContainer == dock)
      {
        // It's being dragged to a new location within the same DockingContainer it's already
        // in.  Make sure it isn't the same location it's already in.

        if (originalTab == target.tab)
        {
          // It's being dragged within the same tab.

          if (originalIndex == -1 || originalIndex == target.index || originalIndex == target.index-1 || target.index == -1)
            return null; // This wouldn't move it.
        }
      }
    }
    return target;
  }

  private static Rectangle findInsertionAreaBounds(DockingContainer dock, int tab, int index)
  {
    Rectangle bounds = dock.getChild(tab, index == 0 ? 0 : index-1).getBounds();
    boolean vertical = (dock.getTabPosition() == BTabbedPane.LEFT || dock.getTabPosition() == BTabbedPane.RIGHT);
    if (index > 0)
    {
      if (vertical)
        bounds.y += bounds.height/2;
      else
        bounds.x += bounds.width/2;
    }
    if (vertical)
      bounds.height /= 2;
    else
      bounds.width /= 2;
    if (index > 0 && index < dock.getTabChildCount(tab))
    {
      Rectangle nextBounds = dock.getChild(tab, index).getBounds();
      if (vertical)
        bounds.height = nextBounds.y+nextBounds.height/2-bounds.y;
      else
        bounds.width = nextBounds.x+nextBounds.width/2-bounds.x;
    }
    return bounds;
  }

  private static void tranformPointToWindow(Widget widget, Point point)
  {
    Container parent = widget.getComponent().getParent();
    while (!(parent instanceof Window))
    {
      point.x += parent.getX();
      point.y += parent.getY();
      parent = parent.getParent();
    }
  }

  private static void tranformRectangleToWindow(Widget widget, Rectangle rect)
  {
    Container parent = widget.getComponent().getParent();
    while (!(parent instanceof Window))
    {
      rect.x += parent.getX();
      rect.y += parent.getY();
      parent = parent.getParent();
    }
  }

  private static class DragTarget
  {
    public DockingContainer container;
    public int tab;
    public int index;

    public DragTarget(DockingContainer container, int tab, int index)
    {
      this.container = container;
      this.tab = tab;
      this.index = index;
    }
  }

  private static class DragMarker extends JWindow
  {
    private boolean hilight;

    public DragMarker()
    {
      setBackground(new Color(0, 0, 0, 0));
      add(new JPanel() {
        public void paintComponent(Graphics g)
        {
          Dimension size = getSize();
          Graphics2D g2 = (Graphics2D) g;
          if (hilight)
          {
            g2.setPaint(ditheredPaint);
            g2.setStroke(new BasicStroke(2));
          }
          else
          {
            g2.setColor(Color.black);
            g2.setStroke(new BasicStroke(1));
          }
          g2.drawRect(0, 0, size.width-1, size.height-1);
        }
      }, BorderLayout.CENTER);
    }

    public void setHilighted(boolean hilight)
    {
      this.hilight = hilight;
    }
  }
}
