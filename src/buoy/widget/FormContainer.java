package buoy.widget;

import buoy.event.*;
import buoy.internal.*;
import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;
import java.util.*;
import javax.swing.JPanel;

/**
 * FormContainer is a WidgetContainer which arranges its children in a grid.  The width of each row and
 * the height of each column may be different, and a single Widget may occupy a rectangular block of cells.
 * <p>
 * The column widths and row heights are chosen based on the minimum and preferred sizes of the widgets
 * contained in them.  If a FormContainer is made larger than the preferred size required for its children,
 * the extra space is divided between rows and columns based on their weights.  That is, a column with
 * weight 2.0 will have twice as much extra width added to it as a column with weight 1.0.  A row or column
 * with weight 0.0 will never be made larger than is required to hold its children.
 * <p>
 * In addition to the event types generated by all Widgets, FormContainers generate the following
 * event types:
 * <ul>
 * <li>{@link buoy.event.RepaintEvent RepaintEvent}</li>
 * </ul>
 *
 * @author Peter Eastman
 */

public class FormContainer extends WidgetContainer
{
  private double rowWeight[], colWeight[];
  private int minRowSize[], prefRowSize[], minColSize[], prefColSize[];
  private ArrayList child;
  private LayoutInfo defaultLayout;
  
  static
  {
    WidgetEncoder.setPersistenceDelegate(FormContainer.class, new FormContainerDelegate());
  }

  /**
   * Create a new FormContainer.  The number of columns is equal to colWeight.length, and the number of
   * rows is equal to rowWeight.length.
   *
   * @param colWeight     the weights of the columns
   * @param rowWeight     the weights of the rows
   */
  
  public FormContainer(double colWeight[], double rowWeight[])
  {
    this.colWeight = colWeight;
    this.rowWeight = rowWeight;
    component = new WidgetContainerPanel(this);
    child = new ArrayList();
    defaultLayout = new LayoutInfo();
  }
  
  /**
   * Create a new FormContainer.  All of the row and column weights default to 1.0.
   *
   * @param numCols     the number of columns
   * @param numRows     the number of rows
   */
  
  public FormContainer(int numCols, int numRows)
  {
    this(new double [numCols], new double [numRows]);
    for (int i = 0; i < rowWeight.length; i++)
      rowWeight[i] = 1.0;
    for (int i = 0; i < colWeight.length; i++)
      colWeight[i] = 1.0;
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
    return ((ChildInfo) child.get(i)).widget;
  }

  /**
   * Get an Iterator listing all child Widgets.
   */
  
  public Iterator getChildren()
  {
    ArrayList list = new ArrayList();
    for (int i = 0; i < child.size(); i++)
      list.add(((ChildInfo) child.get(i)).widget);
    return list.iterator();
  }
  
  /**
   * Get the number of rows in this FormContainer.
   */
  
  public int getRowCount()
  {
    return rowWeight.length;
  }
  
  /**
   * Get the number of columns in this FormContainer.
   */
  
  public int getColumnCount()
  {
    return colWeight.length;
  }
  
  /**
   * Set the number of rows in this FormContainer.  If this increases the number of rows, the new rows
   * will have weights of 1.0 by default.  If this decreases the number of rows, any child Widgets that
   * extend beyond the last row will be removed.
   */
   
  public void setRowCount(int rows)
  {
    double newWeight[] = new double [rows];
    if (rows > rowWeight.length)
    {
      for (int i = 0; i < rowWeight.length; i++)
        newWeight[i] = rowWeight[i];
      for (int i = rowWeight.length; i < newWeight.length; i++)
        newWeight[i] = 1.0;
    }
    else
    {
      for (int i = child.size()-1; i >= 0; i--)
      {
        ChildInfo info = (ChildInfo) child.get(i);
        if (info.y+info.height > rows)
          remove(i);
      }
      for (int i = 0; i < newWeight.length; i++)
        newWeight[i] = rowWeight[i];
    }
    rowWeight = newWeight;
  }
  
  /**
   * Set the number of columns in this FormContainer.  If this increases the number of columns, the new columns
   * will have weights of 1.0 by default.  If this decreases the number of columns, any child Widgets that
   * extend beyond the last column will be removed.
   */
   
  public void setColumnCount(int columns)
  {
    double newWeight[] = new double [columns];
    if (columns > colWeight.length)
    {
      for (int i = 0; i < colWeight.length; i++)
        newWeight[i] = colWeight[i];
      for (int i = colWeight.length; i < newWeight.length; i++)
        newWeight[i] = 1.0;
    }
    else
    {
      for (int i = child.size()-1; i >= 0; i--)
      {
        ChildInfo info = (ChildInfo) child.get(i);
        if (info.x+info.width > columns)
          remove(i);
      }
      for (int i = 0; i < newWeight.length; i++)
        newWeight[i] = colWeight[i];
    }
    colWeight = newWeight;
  }
  
  /**
   * Get the weight of a particular row.
   *
   * @param row     the index of the row
   */
  
  public double getRowWeight(int row)
  {
    return rowWeight[row];
  }
  
  /**
   * Get the weight of a particular column.
   *
   * @param col     the index of the column
   */
  
  public double getColumnWeight(int col)
  {
    return colWeight[col];
  }
  
  /**
   * Set the weight of a particular row.
   *
   * @param row     the index of the row
   * @param weight  the new weight
   */
  
  public void setRowWeight(int row, double weight)
  {
    rowWeight[row] = weight;
  }
  
  /**
   * Set the weight of a particular column.
   *
   * @param col     the index of the column
   * @param weight  the new weight
   */
  
  public void setColumnWeight(int col, double weight)
  {
    colWeight[col] = weight;
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    if (minColSize == null)
      calculateSizes();
    Dimension size = component.getSize();
    int rowPos[] = calculatePositions(minRowSize, prefRowSize, rowWeight, size.height);
    int colPos[] = calculatePositions(minColSize, prefColSize, colWeight, size.width);
    Rectangle cell = new Rectangle();
    for (int i = 0; i < child.size(); i++)
    {
      ChildInfo info = (ChildInfo) child.get(i);
      LayoutInfo layout = (info.layout == null ? defaultLayout : info.layout);
      cell.x = (info.x == 0 ? 0 : colPos[info.x-1]);
      cell.y = (info.y == 0 ? 0 : rowPos[info.y-1]);
      cell.width = colPos[info.x+info.width-1]-cell.x;
      cell.height = rowPos[info.y+info.height-1]-cell.y;
      info.widget.getComponent().setBounds(layout.getWidgetLayout(info.widget, cell));
      if (info.widget instanceof WidgetContainer)
        ((WidgetContainer) info.widget).layoutChildren();
    }
  }
  
  /**
   * Calculate the actual size of each row or column based on the minimum and preferred sizes, the size
   * of this container, and the row or column weights.
   *
   * @param minSize    the minimum size of each row/column
   * @param prefSize   the preferred size of each row/column
   * @param weight     the weight of each row/column
   * @param totalSize  the total size of the container in the appropriate dimension
   * @return the position of the right edge of each column, or the bottom edge of each row
   */
  
  private int [] calculatePositions(int minSize[], int prefSize[], double weight[], int totalSize)
  {
    int pos[] = new int [minSize.length];
    int totalMin = 0, totalPref = 0;
    for (int i = 0; i < minSize.length; i++)
    {
      totalMin += minSize[i];
      totalPref += prefSize[i];
    }
    if (totalMin > totalSize)
    {
      // Just give every row/column its minimum size (which means that some may extend off the edge of
      // the container).
      
      int current = 0;
      for (int i = 0; i < pos.length; i++)
      {
        current += minSize[i];
        pos[i] = current;
      }
    }
    else if (totalPref > totalSize)
    {
      // Give each row/column its minimum size, plus a fixed fraction of the difference between its
      // minimum and preferred sizes.
      
      double fract = (totalSize-totalMin)/(double) (totalPref-totalMin);
      double current = 0.0f;
      for (int i = 0; i < pos.length; i++)
      {
        current += minSize[i] + fract*(prefSize[i]-minSize[i]);
        pos[i] = ((int) Math.round(current));
      }
    }
    else
    {
      // Give each row/column its preferred size, plus a fraction of the extra size based on its weight.
      
      double realWeight[] = new double [weight.length];
      double totalWeight = 0.0;
      for (int i = 0; i < weight.length; i++)
        totalWeight += weight[i];
      if (totalWeight > 0.0)
        for (int i = 0; i < realWeight.length; i++)
          realWeight[i] = weight[i]/totalWeight;
      double current = 0.0f;
      for (int i = 0; i < pos.length; i++)
      {
        current += prefSize[i] + realWeight[i]*(totalSize-totalPref);
        pos[i] = ((int) Math.round(current));
      }
    }
    return pos;
  }
  
  /**
   * Add a Widget to this container.  The Widget will occupy a single cell, and be positioned using the
   * default LayoutInfo.
   *
   * @param widget    the Widget to add
   * @param col       the column in which to place the Widget
   * @param row       the row in which to place the Widget
   */
  
  public void add(Widget widget, int col, int row)
  {
    add(widget, col, row, null);
  }

  /**
   * Add a Widget to this container.  The Widget will occupy a single cell.
   *
   * @param widget    the Widget to add
   * @param col       the column in which to place the Widget
   * @param row       the row in which to place the Widget
   * @param layout    the LayoutInfo to use for this Widget.  If null, the default LayoutInfo will be used.
   */
  
  public void add(Widget widget, int col, int row, LayoutInfo layout)
  {
    add(widget, col, row, 1, 1, layout);
  }
  
  /**
   * Add a Widget to this container.  The Widget will occupy a rectangular block of cells, and be positioned
   * using the default LayoutInfo.
   *
   * @param widget    the Widget to add
   * @param col       the first column the Widget will occupy
   * @param row       the first row the Widget will occupy
   * @param width     the number of columns the Widget will occupy
   * @param height    the number of rows the Widget will occupy
   */
  
  public void add(Widget widget, int col, int row, int width, int height)
  {
    add(widget, col, row, width, height, null);
  }
  
  /**
   * Add a Widget to this container.  The Widget will occupy a rectangular block of cells.
   *
   * @param widget    the Widget to add
   * @param col       the first column the Widget will occupy
   * @param row       the first row the Widget will occupy
   * @param width     the number of columns the Widget will occupy
   * @param height    the number of rows the Widget will occupy
   * @param layout    the LayoutInfo to use for this Widget.  If null, the default LayoutInfo will be used.
   */
  
  public void add(Widget widget, int col, int row, int width, int height, LayoutInfo layout)
  {
    if (col < 0 || col+width > colWeight.length || row < 0 || row+height > rowWeight.length || width < 1 || height < 1)
      throw new IllegalArgumentException();
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    child.add(new ChildInfo(widget, layout, col, row, width, height));
    ((JPanel) component).add(widget.component);
    setAsParent(widget);
    invalidateSize();
  }

  /**
   * Get the LayoutInfo for a particular Widget.
   *
   * @param index     the index of the Widget for which to get the LayoutInfo
   * @return the LayoutInfo being used for that Widget.  This may return null, which indicates that the
   *         default LayoutInfo is being used.
   */
  
  public LayoutInfo getChildLayout(int index)
  {
    return ((ChildInfo) child.get(index)).layout;
  }

  /**
   * Set the LayoutInfo for a particular Widget.
   *
   * @param index      the index of the Widget for which to set the LayoutInfo
   * @param layout     the new LayoutInfo.  If null, the default LayoutInfo will be used
   */
  
  public void setChildLayout(int index, LayoutInfo layout)
  {
    ((ChildInfo) child.get(index)).layout = layout;
    invalidateSize();
  }
  
  /**
   * Get the LayoutInfo for a particular Widget.
   *
   * @param widget     the Widget for which to get the LayoutInfo
   * @return the LayoutInfo being used for that Widget.  This may return null, which indicates that the
   *         default LayoutInfo is being used.  It will also return null if the specified Widget is not
   *         a child of this container.
   */
  
  public LayoutInfo getChildLayout(Widget widget)
  {
    int index = getWidgetIndex(widget);
    if (index == -1)
      return null;
    return ((ChildInfo) child.get(index)).layout;
  }
  
  /**
   * Set the LayoutInfo for a particular Widget.
   *
   * @param widget     the Widget for which to set the LayoutInfo
   * @param layout     the new LayoutInfo.  If null, the default LayoutInfo will be used
   */
  
  public void setChildLayout(Widget widget, LayoutInfo layout)
  {
    int index = getWidgetIndex(widget);
    if (index == -1)
      return;
    ((ChildInfo) child.get(index)).layout = layout;
    invalidateSize();
  }

  /**
   * Get the default LayoutInfo.
   */
  
  public LayoutInfo getDefaultLayout()
  {
    return defaultLayout;
  }
  
  /**
   * Set the default LayoutInfo.
   */
  
  public void setDefaultLayout(LayoutInfo layout)
  {
    defaultLayout = layout;
    invalidateSize();
  }
  
  /**
   * Get the range of cells occupied by a Widget.
   *
   * @param index      the index of the Widget for which to get the cells
   * @return a Rectangle specifying the range of rows and columns occupied by the Widget
   */
  
  public Rectangle getChildCells(int index)
  {
    ChildInfo info = (ChildInfo) child.get(index);
    return new Rectangle(info.x, info.y, info.width, info.height);
  }
  
  /**
   * Set the range of cells occupied by a Widget.
   *
   * @param index    the index of the Widget for which to set the cells
   * @param cells    a Rectangle specifying the range of rows and columns to be occupied by the Widget
   */
  
  public void setChildCells(int index, Rectangle cells)
  {
    ChildInfo info = (ChildInfo) child.get(index);
    info.x = cells.x;
    info.y = cells.y;
    info.width = cells.width;
    info.height = cells.height;
    invalidateSize();
  }
  
  /**
   * Get the range of cells occupied by a Widget.
   *
   * @param widget      the Widget for which to get the cells
   * @return a Rectangle specifying the range of rows and columns occupied by the Widget, or null if
   * the Widget is not a child of this container
   */
  
  public Rectangle getChildCells(Widget widget)
  {
    int index = getWidgetIndex(widget);
    if (index == -1)
      return null;
    return getChildCells(index);
  }
  
  /**
   * Set the range of cells occupied by a Widget.
   *
   * @param widget   the Widget for which to get the cells
   * @param cells    a Rectangle specifying the range of rows and columns to be occupied by the Widget
   */
  
  public void setChildCells(Widget widget, Rectangle cells)
  {
    int index = getWidgetIndex(widget);
    if (index == -1)
      return;
    setChildCells(index, cells);
  }
    
  /**
   * Remove a child Widget from this container.
   *
   * @param widget     the Widget to remove
   */
  
  public void remove(Widget widget)
  {
    int index = getWidgetIndex(widget);
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
    Widget w = ((ChildInfo) child.get(index)).widget;
    ((JPanel) component).remove(w.component);
    child.remove(index);
    removeAsParent(w);
    invalidateSize();
  }
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public void removeAll()
  {
    ((JPanel) component).removeAll();
    for (int i = 0; i < child.size(); i++)
      removeAsParent(((ChildInfo) child.get(i)).widget);
    child.clear();
    invalidateSize();
  }

  /**
   * Get the index of a particular Widget.
   *
   * @param widget      the Widget to locate
   * @return the position of the Widget within this container
   */
  
  public int getWidgetIndex(Widget widget)
  {
    for (int i = 0; i < child.size(); i++)
      if (((ChildInfo) child.get(i)).widget == widget)
        return i;
    return -1;
  }
  
  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    if (minColSize == null)
      calculateSizes();
    Dimension minSize = new Dimension(0, 0);
    for (int i = 0; i < minColSize.length; i++)
      minSize.width += minColSize[i];
    for (int i = 0; i < minRowSize.length; i++)
      minSize.height += minRowSize[i];
    return minSize;
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    if (prefColSize == null)
      calculateSizes();
    Dimension prefSize = new Dimension(0, 0);
    for (int i = 0; i < prefColSize.length; i++)
      prefSize.width += prefColSize[i];
    for (int i = 0; i < prefRowSize.length; i++)
      prefSize.height += prefRowSize[i];
    return prefSize;
  }
  
  /**
   * Discard the cached row and column sizes when any child's size changes.
   */
  
  protected void invalidateSize()
  {
    minRowSize = minColSize = prefRowSize = prefColSize = null;
    super.invalidateSize();
  }
  
  /**
   * Calculate the minimum and preferred size for every row and column.
   */
  
  private void calculateSizes()
  {
    Dimension dim[] = new Dimension [child.size()];
    for (int i = 0; i < dim.length; i++)
      dim[i] = ((ChildInfo) child.get(i)).widget.getMinimumSize();
    minRowSize = calculateRequiredSizes(dim, true);
    minColSize = calculateRequiredSizes(dim, false);
    for (int i = 0; i < dim.length; i++)
    {
      ChildInfo info = (ChildInfo) child.get(i);
      LayoutInfo layout = (info.layout == null ? defaultLayout : info.layout);
      dim[i] = layout.getPreferredSize(info.widget);
    }
    prefRowSize = calculateRequiredSizes(dim, true);
    prefColSize = calculateRequiredSizes(dim, false);
  }
  
  /**
   * Calculate the minimum or preferred size of every row or column.
   *
   * @param dim    the minimum or preferred size of every child
   * @param row    true if row sizes should be calculated, false if column sizes should be calculated
   * @return the size required for every row or column
   */
  
  private int [] calculateRequiredSizes(Dimension dim[], boolean row)
  {
    // Build a linked list of size requirements of every child.
    
    LinkedList requiredList = new LinkedList();
    for (int i = 0; i < dim.length; i++)
    {
      ChildInfo info = (ChildInfo) child.get(i);
      if (row)
        requiredList.addLast(new int [] {info.y, info.height, dim[i].height});
      else
        requiredList.addLast(new int [] {info.x, info.width, dim[i].width});
    }
    
    // Find the required size for each row or column.
    
    int width[] = new int [row ? rowWeight.length : colWeight.length];
    double weight[] = (row ? rowWeight : colWeight);
    for (int currentWidth = 1; requiredList.size() > 0; currentWidth++)
    {
      // Apply constraints for all children which occupy currentWidth rows or columns.
      
      Iterator iter = requiredList.iterator();
      while (iter.hasNext())
      {
        int req[] = (int []) iter.next();
        if (req[1] != currentWidth)
          continue;
        iter.remove();
        if (currentWidth == 1)
        {
          width[req[0]] = Math.max(width[req[0]], req[2]);
          continue;
        }
        
        // Find how much space is currently available.
        
        int total = 0;
        for (int i = 0; i < currentWidth; i++)
          total += width[req[0]+i];
        if (total >= req[2])
          continue; // It is already wide enough.
        
        // Allocate additional space to the rows or columns, based on their weights.
        
        double totalWeight = 0.0;
        for (int i = 0; i < currentWidth; i++)
          totalWeight += weight[req[0]+i];
        int extra[] = new int [currentWidth];
        int totalExtra = 0;
        for (int i = 0; i < currentWidth-1; i++)
        {
          double w = (totalWeight > 0.0 ? weight[req[0]+i]/totalWeight : 1.0/currentWidth);
          extra[i] += w*(req[2]-total);
          totalExtra += extra[i];
        }
        extra[extra.length-1] = req[2]-total-totalExtra;
        for (int i = 0; i < currentWidth; i++)
          width[req[0]+i] += extra[i];
      }
    }
    return width;
  }

  
  /**
   * This inner class is used to store information about a particular child.
   */
  
  private class ChildInfo
  {
    public Widget widget;
    public LayoutInfo layout;
    public int x, y, width, height;
    
    public ChildInfo(Widget widget, LayoutInfo layout, int x, int y, int width, int height)
    {
      this.widget = widget;
      this.layout = layout;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
  }
}