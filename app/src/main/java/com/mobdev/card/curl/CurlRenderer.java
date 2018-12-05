/**
 * Copyright (c) 2018 mobdev313. Allright reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.mobdev.card.curl;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class CurlRenderer implements GLSurfaceView.Renderer {

    // Constant for requesting left page rect.
    public static final int PAGE_TOP = 1;
    // Constant for requesting right page rect.
    public static final int PAGE_BOTTOM = 2;

    // Set to true for checking quickly how perspective projection looks.
    private static final boolean USE_PERSPECTIVE_PROJECTION = true;
    // Background fill color.
    private int mBackgroundColor;
    // Curl meshes used for static and dynamic rendering.
    private Vector<CurlMesh> mCurlMeshes;
    private RectF mMargins = new RectF();
    private CurlRenderer.Observer mObserver;
    // Page rectangles.
    private RectF mPageRectTop;
    private RectF mPageRectBottom;
    // View mode.
//	private int mViewMode = SHOW_ONE_PAGE;
    // Screen size.
    private int mViewportWidth, mViewportHeight;
    // Rect for render area.
    private RectF mViewRect = new RectF();

    // Flag for Rotate View
    private boolean hasCurled = false;
    public float rotation = 0;

    /**
     * Basic constructor.
     */
    public CurlRenderer(CurlRenderer.Observer observer) {
        mObserver = observer;
        mCurlMeshes = new Vector<CurlMesh>();
        mPageRectTop = new RectF();
        mPageRectBottom = new RectF();
        mBackgroundColor = Color.TRANSPARENT;
    }

    /**
     * Adds CurlMesh to this renderer.
     */
    public synchronized void addCurlMesh(CurlMesh mesh) {
        removeCurlMesh(mesh);
        mCurlMeshes.add(mesh);
    }

    /**
     * Returns rect reserved for left or right page. Value page should be
     * PAGE_LEFT or PAGE_RIGHT.
     */
    public synchronized RectF getPageRect(int page) {
        if (page == PAGE_TOP) {
            return mPageRectTop;
        } else if (page == PAGE_BOTTOM) {
            return mPageRectBottom;
        }
        return null;
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glClearColor(Color.red(mBackgroundColor) / 255f,
                Color.green(mBackgroundColor) / 255f,
                Color.blue(mBackgroundColor) / 255f,
                Color.alpha(mBackgroundColor) / 255f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();

        if (USE_PERSPECTIVE_PROJECTION) {
            gl.glTranslatef(0, 0, -6f);
        }

        mObserver.onDrawFrame();

        //rotate the view
        if (rotation != 0) {
            gl.glRotatef(-rotation, 0, 0, 1);
        }

        for (int i = 0; i < mCurlMeshes.size(); ++i) {
            mCurlMeshes.get(i).onDrawFrame(gl);
        }

    }

    public void setCurlState(boolean isCurled) {
        hasCurled = isCurled;
    }

//	public void rotateAnimation(GL10 gl) {
//		gl.glRotatef(mDegrees, 0, 0, 1);
//	}

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        mViewportWidth = width;
        mViewportHeight = height;
        float ratio = (float) width / height;
        mViewRect.top = 1.0f;
        mViewRect.bottom = -1.0f;
        mViewRect.left = -ratio;
        mViewRect.right = ratio;
        updatePageRects();

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        if (USE_PERSPECTIVE_PROJECTION) {
            GLU.gluPerspective(gl, 20f, (float) width / height, .1f, 100f);
        } else {
            GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right,
                    mViewRect.bottom, mViewRect.top);
        }

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f, 0f, 0f, 0f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_CULL_FACE);

        mObserver.onSurfaceCreated();
    }

    /**
     * Removes CurlMesh from this renderer.
     */
    public synchronized void removeCurlMesh(CurlMesh mesh) {
        while (mCurlMeshes.remove(mesh))
            ;
    }

    /**
     * Change background/clear color.
     */
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    /**
     * Set margins or padding. Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    public synchronized void setMargins(float left, float top, float right, float bottom) {
        mMargins.left = left;
        mMargins.top = top;
        mMargins.right = right;
        mMargins.bottom = bottom;

        updatePageRects();
    }

    /**
     * Sets visible page count to one or two. Should be either SHOW_ONE_PAGE or
     * SHOW_TWO_PAGES.
     */
    public synchronized void setViewMode() {
        updatePageRects();
    }

    /**
     * Translates screen coordinates into view coordinates.
     */
    public void translate(PointF pt) {
        pt.x = mViewRect.left + (mViewRect.width() * pt.x / mViewportWidth);
        pt.y = mViewRect.top - (-mViewRect.height() * pt.y / mViewportHeight);
    }

    /**
     * Recalculates page rectangles.
     */
    private synchronized void updatePageRects() {
        if (mViewRect.width() == 0 || mViewRect.height() == 0) {
            return;
        } else {
            mPageRectBottom.set(mViewRect);
            mPageRectBottom.left += mViewRect.width() * mMargins.left;
            mPageRectBottom.right -= mViewRect.width() * mMargins.right;
            mPageRectBottom.top += mViewRect.height() * mMargins.top;
            mPageRectBottom.bottom -= mViewRect.height() * mMargins.bottom;

            mPageRectTop.set(mPageRectBottom);
            if (!hasCurled) {
                mPageRectTop.offset(0, -mPageRectBottom.height() / 2);
            }

            int bitmapW = (int) ((mPageRectBottom.width() * mViewportWidth) / mViewRect
                    .width());
            int bitmapH = (int) ((mPageRectBottom.height() * mViewportHeight) / mViewRect
                    .height());
            mObserver.onPageSizeChanged(bitmapW, bitmapH);
        }
    }

    /**
     * Observer for waiting render engine/state updates.
     */
    public interface Observer {
        /**
         * Called from onDrawFrame called before rendering is started. This is
         * intended to be used for animation purposes.
         */
        public void onDrawFrame();

        /**
         * Called once page size is changed. Width and height tell the page size
         * in pixels making it possible to update textures accordingly.
         */
        public void onPageSizeChanged(int width, int height);

        /**
         * Called from onSurfaceCreated to enable texture re-initialization etc
         * what needs to be done when this happens.
         */
        public void onSurfaceCreated();

    }
}
