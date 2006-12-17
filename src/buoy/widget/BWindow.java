package buoy.widget;

import buoy.event.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import javax.swing.*;

/**
 * A BWindow is a WidgetContainer corresponding to an undecorated window.  It has no title bar or
 * pulldown menus.  It may contain a single Widget (usually a WidgetContainer of some sort) which
 * fills the window.
 * <p>
 * In addition to the event types generated by all Widgets, BWindows generate the following event types:
 * <ul>
 * <li>{@link buoy.event.RepaintEvent RepaintEvent}</li>
 * <li>{@link buoy.event.WindowActivatedEvent WindowActivatedEvent}</li>
 * <li>{@link buoy.event.WindowClosingEvent WindowClosingEvent}</li>
 * <li>{@link buoy.event.WindowDeactivatedEvent WindowDeactivatedEvent}</li>
 * <li>{@link buoy.event.WindowDeiconifiedEvent WindowDeiconifiedEvent}</li>
 * <li>{@link buoy.event.WindowIconifiedEvent WindowIconifiedEvent}</li>
 * <li>{@link buoy.event.WindowResizedEvent WindowResizedEvent}</li>
 * </ul>
 *
 * @author Peter Eastman
 */

public class BWindow extends WindowWidget
{
  /**
   * Create a new BWindow.
   */
  
  public BWindow()
  {
    component = createComponent();
    ((JWindow) component).getContentPane().setLayout(null);
    component.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent ev)
      {
        if (lastSetSize == null || !lastSetSize.equals(component.getSize()))
        {
          lastSetSize = null;
          layoutChildren();
          BWindow.this.dispatchEvent(new WindowResizedEvent(BWindow.this));
        }
        else
          lastSetSize = null;
      }
    });
  }
  
  /**
   * Create the JWindow which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JWindow createComponent()
  {
    return new BWindowComponent();
  }

  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    return (content == null ? 0 : 1);
  }
  
  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public Collection getChildren()
  {
    ArrayList ls = new ArrayList(1);
    if (content != null)
      ls.add(content);
    return ls;
  }
  
  /**
   * Remove a child Widget from this container.
   */
  
  public void remove(Widget widget)
  {
    if (content == widget)
    {
      ((JWindow) component).getContentPane().remove(widget.component);
      removeAsParent(content);
      content = null;
    }
  }
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public void removeAll()
  {
    if (content != null)
      remove(content);
  }

  /**
   * Get the JRootPane for this Widget's component.
   */

  protected JRootPane getRootPane()
  {
    return ((JWindow) getComponent()).getRootPane();
  }

  /**
   * This is the JWindow subclass which is used as the Component for a BWindow.
   */
  
  private class BWindowComponent extends JWindow
  {
    public BWindowComponent()
    {
      super();
    }

    public void paintComponent(Graphics g)
    {
      BWindow.this.dispatchEvent(new RepaintEvent(BWindow.this, (Graphics2D) g));
    }
  }
}