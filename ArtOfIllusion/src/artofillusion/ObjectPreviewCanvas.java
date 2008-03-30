/* Copyright (C) 1999-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.view.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** The ObjectPreviewCanvas class displays a single object which the user can move and rotate, but
    not edit. */

public class ObjectPreviewCanvas extends ViewerCanvas
{
  private ObjectInfo objInfo;
  private boolean sizeSet;
  private boolean hasBeenDrawn;

  /** Create an ObjectPreviewCanvas for previewing a particular object. */

  public ObjectPreviewCanvas(ObjectInfo obj)
  {
    this(obj, new RowContainer());
  }

  /** Create an ObjectPreviewCanvas for previewing a particular object.  The controls for setting scale,
      projection, and view direction will be add to the Panel. */

  public ObjectPreviewCanvas(ObjectInfo obj, RowContainer p)
  {
    super(ModellingApp.getPreferences().getUseOpenGL() && isOpenGLAvailable());
    if (obj != null)
    {
      objInfo = obj.duplicate();
      objInfo.getCoords().setOrigin(new Vec3());
      objInfo.getCoords().setOrientation(Vec3.vz(), Vec3.vy());
      objInfo.clearDistortion();
    }
    buildChoices(p);
    setTool(new RotateViewTool(null));
    setMetaTool(new MoveViewTool(null));
    setRenderMode(RENDER_SMOOTH);
    hideBackfaces = false;
    prefSize = new Dimension(200, 200);
  }
  
  /** This should be called whenever the object has changed. */
  
  public void objectChanged()
  {
    getObject().clearCachedMeshes();
  }
  
  /** Get the object being previewed. */

  public ObjectInfo getObject()
  {
    return objInfo;
  }

  /** Set the object being previewed. */

  public void setObject(Object3D obj)
  {
    if (objInfo == null)
      objInfo = new ObjectInfo(obj, new CoordinateSystem(), "");
    else
      objInfo.setObject(obj);
    objInfo.clearCachedMeshes();
  }
  
  /** Estimate the range of depth values that the camera will need to render.  This need not be exact,
      but should err on the side of returning bounds that are slightly too large.
      @return the two element array {minDepth, maxDepth}
   */

  public double[] estimateDepthRange()
  {
    Mat4 toView = theCamera.getWorldToView();
    
    // Find the depth range for the object being edited.
    
    BoundingBox bounds = objInfo.getBounds();
    double dx = bounds.maxx-bounds.minx;
    double dy = bounds.maxy-bounds.miny;
    double dz = bounds.maxz-bounds.minz;
    double size = 0.5*Math.sqrt(dx*dx+dy*dy+dz*dz);
    double depth = toView.times(new Vec3()).z;
    return new double [] {depth-size, depth+size};
  }

  public synchronized void updateImage()
  {
    adjustCamera(isPerspective());
    super.updateImage();
    if (objInfo == null)
      return;

    // Draw the object.

    theCamera.setObjectTransform(objInfo.getCoords().fromLocal());
    renderObject();

    // Finish up.

    drawBorder();
    if (showAxes)
      drawCoordinateAxes();
    if (!hasBeenDrawn)
    {
      // This is a workaround for a bug (presumably in Jogl?) where the first repaint
      // doesn't get displayed.

      hasBeenDrawn = true;
      repaint();
    }
  }

  /** Draw the object. */

  protected void renderObject()
  {
    RenderingMesh mesh;
    
    if (objInfo == null)
      return;
    if (!sizeSet)
    {
      // When the canvas first comes up, calculate an initial scale that allows the entire object to be seen.
      
      Rectangle dim = getBounds();
      Vec3 objSize = objInfo.getObject().getBounds().getSize();
      double scale = 0.8*Math.min(dim.width, dim.height)/Math.max(Math.max(objSize.x, objSize.y), objSize.z);
      setScale(scale);
      theCamera.setScreenParams(0, scale, dim.width, dim.height);
      sizeSet = true;
    }
    if (renderMode == RENDER_WIREFRAME)
      renderWireframe(objInfo.getWireframePreview(), theCamera, lineColor);
    else
    {
      mesh = objInfo.getPreviewMesh();
      if (mesh == null)
        renderWireframe(objInfo.getWireframePreview(), theCamera, lineColor);
      else if (renderMode == RENDER_TRANSPARENT)
        renderMeshTransparent(mesh, new ConstantVertexShader(transparentColor), theCamera, theCamera.getViewToWorld().timesDirection(Vec3.vz()), null);
      else
      {
        Vec3 viewDir = theCamera.getViewToWorld().timesDirection(Vec3.vz());
        VertexShader shader;
        if (renderMode == RENDER_FLAT)
          shader = new FlatVertexShader(mesh, surfaceRGBColor, viewDir);
        else if (renderMode == RENDER_SMOOTH)
          shader = new SmoothVertexShader(mesh, surfaceRGBColor, viewDir);
        else
          shader = new TexturedVertexShader(mesh, objInfo.getObject(), 0.0, viewDir).optimize();
        renderMesh(mesh, shader, theCamera, objInfo.getObject().isClosed(), null);
      }
    }
  }

  /** When the user presses the mouse, forward events to the current tool. */

  protected void mousePressed(WidgetMouseEvent e)
  {
    requestFocus();
    activeTool = currentTool;
    if (metaTool != null && e.isMetaDown())
      activeTool = metaTool;
    activeTool.mousePressed(e, this);
  }

  protected void mouseDragged(WidgetMouseEvent e)
  {
    activeTool.mouseDragged(e, this);
  }

  protected void mouseReleased(WidgetMouseEvent e)
  {
    activeTool.mouseReleased(e, this);
  }
}