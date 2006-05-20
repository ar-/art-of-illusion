package buoy.widget;

import buoy.event.*;
import buoy.internal.*;
import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * BTabbedPane is a WidgetContainer which arranges its child Widgets in a row.
 * <p>
 * In addition to the event types generated by all Widgets, BTabbedPanes generate the following event types:
 * <ul>
 * <li>{@link buoy.event.SelectionChangedEvent SelectionChangedEvent}</li>
 * </ul>
 *
 * @author Peter Eastman
 */

public class BTabbedPane extends WidgetContainer
{
  private ArrayList child;
  private int suppressEvents;
  
  public static final TabPosition TOP = new TabPosition(SwingConstants.TOP);
  public static final TabPosition LEFT = new TabPosition(SwingConstants.LEFT);
  public static final TabPosition BOTTOM = new TabPosition(SwingConstants.BOTTOM);
  public static final TabPosition RIGHT = new TabPosition(SwingConstants.RIGHT);

  static
  {
    WidgetEncoder.setPersistenceDelegate(TabPosition.class, new StaticFieldDelegate(BTabbedPane.class));
    WidgetEncoder.setPersistenceDelegate(BTabbedPane.class, new IndexedContainerDelegate(new String [] {"getChild", "getTabName", "getTabImage"}));
  }

  /**
   * Create a new BTabbedPane with the tabs along the top.
   */
  
  public BTabbedPane()
  {
    this(TOP);
  }
  
  /**
   * Create a new TabbedContainer.
   *
   * @param pos      the position for the tabs (TOP, LEFT, BOTTOM, or RIGHT)
   */
  
  public BTabbedPane(TabPosition pos)
  {
    component = createComponent(pos);
    ((JTabbedPane) component).addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev)
      {
        if (suppressEvents == 0)
          dispatchEvent(new SelectionChangedEvent(BTabbedPane.this));
      }
    });
    component.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent ev)
      {
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            layoutChildren();
          }
        });
      }
    });
    child = new ArrayList();
  }
  
  /**
   * Create the JTabbedPane which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   *
   * @param pos      the position for the tabs (TOP, LEFT, BOTTOM, or RIGHT)

   */
  
  protected JTabbedPane createComponent(TabPosition pos)
  {
    return new JTabbedPane(pos.value);
  }

  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    return child.size();
  }
  
  /**
   * Get the i'th child of this container.
   */
  
  public Widget getChild(int i)
  {
    return (Widget) child.get(i);
  }

  /**
   * Get an Iterator listing all child Widgets.
   */
  
  public Iterator getChildren()
  {
    return child.iterator();
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    getComponent().validate();
    for (int i = 0; i < child.size(); i++)
    {
      Widget w = (Widget) child.get(i);
      if (w instanceof WidgetContainer)
        ((WidgetContainer) w).layoutChildren();
    }
  }
  
  /**
   * Add a Widget to this container.
   *
   * @param widget     the Widget to add
   * @param tabName    the name to display on the tab
   */
  
  public void add(Widget widget, String tabName)
  {
    add(widget, tabName, null, child.size());
  }
  
  /**
   * Add a Widget to this container.
   *
   * @param widget     the Widget to add
   * @param tabName    the name to display on the tab
   * @param image      the image to display on the tab
   */
  
  public void add(Widget widget, String tabName, Icon image)
  {
    add(widget, tabName, image, child.size());
  }
  
  /**
   * Add a Widget to this container.
   *
   * @param widget     the Widget to add
   * @param tabName    the name to display on the tab
   * @param image      the image to display on the tab
   * @param index      the position at which to add this tab
   */
  
  public void add(Widget widget, String tabName, Icon image, int index)
  {
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    child.add(index, widget);
    ((JTabbedPane) component).insertTab(tabName, image, new SingleWidgetPanel(widget), null, index);
    setAsParent(widget);
    invalidateSize();
  }
    
  /**
   * Remove a child Widget from this container.
   *
   * @param widget     the Widget to remove
   */
  
  public void remove(Widget widget)
  {
    int index = child.indexOf(widget);
    if (index > -1)
      remove(index);
  }
  
  /**
   * Remove a child Widget from this container.
   *
   * @param index     the index of the Widget to remove
   */
  
  public void remove(int index)
  {
    Widget w = (Widget) child.get(index);
    ((JTabbedPane) component).remove(index);
    child.remove(index);
    removeAsParent(w);
    invalidateSize();
  }
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public void removeAll()
  {
    ((JTabbedPane) component).removeAll();
    for (int i = 0; i < child.size(); i++)
      removeAsParent((Widget) child.get(i));
    child.clear();
    invalidateSize();
  }

  /**
   * Get the index of a particular Widget.
   *
   * @param widget      the Widget to locate
   * @return the position of the Widget within this container
   */
  
  public int getChildIndex(Widget widget)
  {
    return child.indexOf(widget);
  }
  
  /**
   * Get the position of the tabs (TOP, LEFT, BOTTOM, or RIGHT).
   */
  
  public TabPosition getTabPosition()
  {
    switch (((JTabbedPane) component).getTabPlacement())
    {
      case SwingConstants.TOP:
        return TOP;
      case SwingConstants.LEFT:
        return LEFT;
      case SwingConstants.BOTTOM:
        return BOTTOM;
      default:
        return RIGHT;
    }
  }
  
  /**
   * Set the position of the tabs (TOP, LEFT, BOTTOM, or RIGHT).
   */
  
  public void setTabPosition(TabPosition pos)
  {
    ((JTabbedPane) component).setTabPlacement(pos.value);
  }
  
  /**
   * Get the name displayed on a particular tab.
   *
   * @param index     the index of the tab
   */
  
  public String getTabName(int index)
  {
    return ((JTabbedPane) component).getTitleAt(index);
  }
  
  /**
   * Set the name displayed on a particular tab.
   *
   * @param index     the index of the tab
   * @param name      the name to display
   */
  
  public void setTabName(int index, String name)
  {
    ((JTabbedPane) component).setTitleAt(index, name);
  }
  
  /**
   * Get the image displayed on a particular tab.
   *
   * @param index     the index of the tab
   */
  
  public Icon getTabImage(int index)
  {
    return ((JTabbedPane) component).getIconAt(index);
  }
  
  /**
   * Set the image displayed on a particular tab.
   *
   * @param index   the index of the tab
   * @param image   the image to display
   */
  
  public void setTabImage(int index, Icon image)
  {
    ((JTabbedPane) component).setIconAt(index, image);
  }
  
  /**
   * Get the index of the tab which is currently selected.
   */
  
  public int getSelectedTab()
  {
    return ((JTabbedPane) component).getSelectedIndex();
  }
  
  /**
   * Set which tab is selected.
   *
   * @param index    the index of the tab
   */
  
  public void setSelectedTab(int index)
  {
    try
    {
      suppressEvents++;
      ((JTabbedPane) component).setSelectedIndex(index);
    }
    finally
    {
      suppressEvents--;
    }
  }
  
  /**
   * This inner class represents a position for the tabs.
   */
  
  public static class TabPosition
  {
    protected int value;
    
    private TabPosition(int value)
    {
      this.value = value;
    }
  }
}
