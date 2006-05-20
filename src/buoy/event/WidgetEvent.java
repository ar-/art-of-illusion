package buoy.event;

import buoy.widget.*;

/**
 * This interface defines an event generated by a Widget.
 *
 * @author Peter Eastman
 */

public interface WidgetEvent
{
  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget();
}