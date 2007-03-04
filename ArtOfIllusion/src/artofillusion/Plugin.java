/** The Plugin interface provides a very general means for adding features to    Art of Illusion.  *//* Copyright (C) 2001 by Peter Eastman   This program is free software; you can redistribute it and/or modify it under the   terms of the GNU General Public License as published by the Free Software   Foundation; either version 2 of the License, or (at your option) any later version.   This program is distributed in the hope that it will be useful, but WITHOUT ANY    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A    PARTICULAR PURPOSE.  See the GNU General Public License for more details. */package artofillusion;public interface Plugin{  /* The following constants represent messages that can be passed to processMessage(). */    /** This message is sent when the program first starts up.  It is sent after all     initialization has happened, but before the first window has been displayed.     It has no arguments. */    public static final int APPLICATION_STARTING = 0;    /** This message is sent just before the program shuts down.  This gives plugins a     chance to do any necessary cleanup.  It has no arguments. */    public static final int APPLICATION_STOPPING = 1;    /** This message is sent when a new scene editing window is created.  It is sent after     the new window has been fully initialized, and just before it is displayed.  The     LayoutWindow is passed as an argument */    public static final int SCENE_WINDOW_CREATED = 2;    /** This message is sent just after a scene editing window is closed.  The     LayoutWindow is passed as an argument. */    public static final int SCENE_WINDOW_CLOSING = 3;  /** This message is sent when a scene is saved to disk.  The arguments are the File     that has just been created, and the LayoutWindow for the scene that was saved. */    public static final int SCENE_SAVED = 4;  /** This is the main method for the plugin.  message indicates what event     is taking place, and is represented by one of the constants defined above.  args     is a list of arguments, whose meaning depends on the message.          This interface is designed to allow new messages to be created in the future.     Therefore, processMessage() should ignore any messages it is not specifically     intended to deal with. */    public void processMessage(int message, Object args[]);}