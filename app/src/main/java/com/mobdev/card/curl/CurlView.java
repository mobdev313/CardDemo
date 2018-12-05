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

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mobdev.card.curl.RotationGestureDetector.OnRotationListener;


public class CurlView extends GLSurfaceView implements View.OnTouchListener, CurlRenderer.Observer, OnRotationListener {

    public static final int PORTRAIT = 0;
    public static final int LANDSCAPERIGHT = 1;
    public static final int PORTRAITUPSIDEDOWN = 2;
    public static final int LANDSCAPELEFT = 3;

    // Constants for mAnimationTargetEvent.
    private static final int SET_CURL_TO_TOP = 1;
    private static final int SET_CURL_TO_BOTTOM = 2;

    private static final float MAX_DEGREES = 90.f;
    private static final float STEP_DEGREES = 3.f;

    // remember some things for rotating
    private boolean isRotating = false;		// flag to show rotating be doing.

    private long mAnimationDurationTime = 500;
    private PointF mAnimationSource = new PointF();
    private long mAnimationStartTime;
    private PointF mAnimationTarget = new PointF();
    private int mAnimationTargetEvent;

    // Settings for rotate config

    private boolean isCurled = false;
    private boolean mAnimate = false;
    private boolean curling = false;
    //	private boolean isCurling = false;
    public int orientation = 0;

    private PointF mCurlDir = new PointF();
    private PointF mCurlPos = new PointF();

    // Start position for dragging.
    private PointF mDragStartPos = new PointF();

    // Bitmap size. These are updated from renderer once it's initialized.
    private int mPageBitmapHeight = -1;
    private int mPageBitmapWidth = -1;

    // Page meshes. Left and right meshes are 'static' while curl is used to
    // show page flipping.
    private CurlMesh mPageCurl;
    private CurlMesh mPageTop;
    private CurlMesh mPageBottom;

    private PointF mPointerPos = new PointF();

    private CurlRenderer mRenderer;
    private PageProvider mPageProvider;
    private SizeChangedObserver mSizeChangedObserver;

    private RotationGestureDetector gestureDetector;

    /**
     * Default constructor.
     */
    public CurlView(Context ctx) {
        super(ctx);
        initialize();
    }

    /**
     * Default constructor.
     */
    public CurlView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        initialize();
    }

    /**
     * Default constructor.
     */
    public CurlView(Context ctx, AttributeSet attrs, int defStyle) {
        this(ctx, attrs);
    }


    /**
     *Get flag that current page had curled.
     */
    public boolean isCurled() {
        return isCurled;
    }

    /**
     * Initialize method.
     */
    public void initialize() {
        isCurled = false;
        orientation = PORTRAIT;
        isRotating = false;
        curling = false;

        mAnimate = false;
        mPageBitmapHeight = -1;
        mPageBitmapWidth = -1;
        mPointerPos.x = 0;
        mPointerPos.y = 0;

        gestureDetector = new RotationGestureDetector();
        gestureDetector.setOnRotationListener(this);

        mRenderer = new CurlRenderer(this);

        this.setZOrderOnTop(true);
        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setOnTouchListener(this);
        // Even though left and right pages are static we have to allocate room
        // for curl on them too as we are switching meshes. Another way would be
        // to swap texture ids only.
        mPageTop = new CurlMesh(10);
        mPageBottom = new CurlMesh(10);
        mPageCurl = new CurlMesh(10);
        mPageTop.setFlipTexture(true);
        mPageBottom.setFlipTexture(false);
        requestRender();
    }

    public void reset() {
        isCurled = false;
        orientation = PORTRAIT;
        isRotating = false;
        curling = false;

        mAnimate = false;
        mPageBitmapHeight = -1;
        mPageBitmapWidth = -1;
        mPointerPos.x = 0;
        mPointerPos.y = 0;

        mPageTop.reset();
        mPageBottom.reset();
        mPageCurl.reset();
        mPageTop.setFlipTexture(true);
        mPageBottom.setFlipTexture(false);
        setMargins();
        updatePages();
        requestRender();
    }


    @Override
    public void onDrawFrame() {
        if (mAnimate) {
            long currentTime = System.currentTimeMillis();
            // If animation is done.
            if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
                if (mAnimationTargetEvent == SET_CURL_TO_BOTTOM) {
                    // Switch curled page to right.
                    CurlMesh bottomside = mPageCurl;
                    CurlMesh curl = mPageBottom;
                    bottomside.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM));
                    bottomside.setFlipTexture(false);
                    bottomside.reset();
                    mRenderer.removeCurlMesh(curl);
                    mPageCurl = curl;
                    mPageBottom = bottomside;
                    curling = false;
                    mAnimate = false;
                    requestRender();
                } else if (mAnimationTargetEvent == SET_CURL_TO_TOP) {
                    finishCurling();
                }
            } else {
                mPointerPos.set(mAnimationSource);
                float t = 1f - ((float) (currentTime - mAnimationStartTime) / mAnimationDurationTime);
                t = 1f - (t * t * t * (3 - 2 * t));
                mPointerPos.x += (mAnimationTarget.x - mAnimationSource.x) * t;
                mPointerPos.y += (mAnimationTarget.y - mAnimationSource.y) * t;
                updateCurlPos(mPointerPos);
            }
        }

        if (isRotating) {
            float rotation = mRenderer.rotation;
            float degree = Math.abs(rotation);
            if (degree == 0 || degree >= MAX_DEGREES) {
                isRotating = false;
                mRenderer.rotation = 0;
                mSizeChangedObserver.onSizeChanged(this); //rotate completed
            } else {
                if (degree < 45) {
                    if (rotation < 0) {
                        rotation = Math.min(rotation + STEP_DEGREES, 0);
                    } else {
                        rotation = Math.max(rotation - STEP_DEGREES, 0);
                    }
                } else {
                    if (rotation < 0) {
                        rotation = Math.max(rotation - STEP_DEGREES, -MAX_DEGREES);
                    } else {
                        rotation = Math.min(rotation + STEP_DEGREES, MAX_DEGREES);
                    }
                }

                mRenderer.rotation = rotation;
                requestRender();
            }
        }
    }

    @Override
    public void onPageSizeChanged(int width, int height) {
        mPageBitmapWidth = width;
        mPageBitmapHeight = height;
        updatePages();
        requestRender();
    }

    @Override
    public void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        requestRender();
        if (mSizeChangedObserver != null) {
            mSizeChangedObserver.onSizeChanged(this);
        }
    }

    @Override
    public void onSurfaceCreated() {
        // In case surface is recreated, let page meshes drop allocated texture
        // ids and ask for new ones. There's no need to set textures here as
        // onPageSizeChanged should be called later on.
        mPageTop.resetTexture();
        mPageBottom.resetTexture();
        mPageCurl.resetTexture();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (isRotating || mAnimate || mPageProvider == null) {
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchCurlEvent(event);
                return true;
            case MotionEvent.ACTION_UP:
                touchCurlEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d("pid number", String.valueOf(event.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT));
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (event.getPointerId(i) == 0) touchCurlEvent(event);
                }
                break;
        }

        if (!curling) {
            gestureDetector.touchEvent(event);
        }

        return true;
    }

    private void touchCurlEvent(MotionEvent event) {
        // No dragging during animation at the moment.
        // Stop animation on touch event and return to drag mode.
        if (isCurled) return;

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        // We need page rects quite extensively so get them for later use.
        RectF bottomRect = mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM);
        RectF topRect = mRenderer.getPageRect(CurlRenderer.PAGE_TOP);

        // Store pointer position.
        mPointerPos.set(x, y);
        mRenderer.translate(mPointerPos);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {

                // Once we receive pointer down event its position is mapped to
                // right or left edge of page and that'll be the position from where
                // user is holding the paper to make curl happen.
                mDragStartPos.set(mPointerPos);

                // First we make sure it's not over or below page. Pages are
                // supposed to be same height so it really doesn't matter do we use
                // left or right one.
                if (mDragStartPos.y > bottomRect.top) {
                    mDragStartPos.y = bottomRect.top;
                } else if (mDragStartPos.y < bottomRect.bottom) {
                    mDragStartPos.y = bottomRect.bottom;
                }

                // Then we have to make decisions for the user whether curl is going
                // to happen from left or right, and on which page.
                float startRangeY = bottomRect.bottom + (bottomRect.top - bottomRect.bottom) / 4;
                if (mDragStartPos.y <= startRangeY) {
                    mDragStartPos.y = bottomRect.bottom;
                    startCurl();
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                updateCurlPos(mPointerPos);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (curling) {
                    // Animation source is the point from where animation starts.
                    // Also it's handled in a way we actually simulate touch events
                    // meaning the output is exactly the same as if user drags the
                    // page to other side. While not producing the best looking
                    // result (which is easier done by altering curl position and/or
                    // direction directly), this is done in a hope it made code a
                    // bit more readable and easier to maintain.
                    mAnimationSource.set(mPointerPos);
                    mAnimationStartTime = System.currentTimeMillis();

                    // Given the explanation, here we decide whether to simulate
                    // drag to left or right end.
                    if ( mPointerPos.y < bottomRect.top) {
                        // On right side target is always right page's right border.
                        mAnimationTarget.set(mDragStartPos);
                        mAnimationTarget.y = mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM).bottom;
                        mAnimationTargetEvent = SET_CURL_TO_BOTTOM;
                    } else {
                        // On left side target depends on visible pages.
                        mAnimationTarget.x = mDragStartPos.x;
                        mAnimationTarget.y = topRect.top;
                        mAnimationTargetEvent = SET_CURL_TO_TOP;
                    }
                    mAnimate = true;
                    requestRender();
                }
                break;
            }
        }
    }

    private void finishCurling(){
        long currentTime = System.currentTimeMillis();
        if (currentTime >= mAnimationStartTime + 2 * mAnimationDurationTime) {
            RectF rectf = mRenderer.getPageRect(CurlRenderer.PAGE_TOP);
            float boundY =  mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM).top;
            if (rectf.top > boundY) {
                CurlMesh topside = mPageCurl;
                rectf.offset(0f, boundY - rectf.top);
                topside.setRect(rectf);
                topside.setFlipTexture(true);
                topside.reset();
                requestRender();
            }

            mPageCurl.reset();
//			mPageCurl = mPageTop;
            mPageTop.reset();
            mPageTop.resetTexture();
//			mPageTop = mPageBottom; 
//			mPageTop.reset();
            curling = false;
            mAnimate = false;
            requestRender();
            isCurled = true;
            requestRender();
            mRenderer.setCurlState(isCurled);
            if (mPageProvider != null) {
                mPageProvider.onCompleteCurl(this);
                updatePages();
//				mPageProvider.updatePage(view, page, width, height, index, rotatedFlag)
            }
        } else {
            // Switch curled page to left.
            CurlMesh topside = mPageCurl;
//			CurlMesh curl = mPageTop;
            RectF rectf = mRenderer.getPageRect(CurlRenderer.PAGE_TOP);
            float offsetY = (float) (currentTime - mAnimationStartTime) / (2 * mAnimationDurationTime);
            offsetY = (1f - (float)Math.pow(offsetY, 4)) / 16;
            float limitY =  mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM).top;
            if ((rectf.top - offsetY) < limitY){
                rectf.offset(0f, limitY - rectf.top);
            } else {
                rectf.offset(0f, -offsetY);
            }
//			rectf.offset(0f, -0.2f);
            topside.setRect(rectf);
            topside.setFlipTexture(true);
            topside.reset();
            requestRender();
        }
    }

    /**
     * Switches meshes and loads new bitmaps if available. Updated to support 2
     * pages in landscape
     */
    private void startCurl() {
        // Remove meshes from renderer.
        mRenderer.removeCurlMesh(mPageTop);
        mRenderer.removeCurlMesh(mPageBottom);
        mRenderer.removeCurlMesh(mPageCurl);

        // We are curling right page.
        CurlMesh curl = mPageBottom;
        mPageBottom = mPageCurl;
        mPageCurl = curl;

        // Add curled page to renderer.
        mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM));
        mPageCurl.setFlipTexture(false);
        mPageCurl.reset();
        mRenderer.addCurlMesh(mPageCurl);

        curling = true;
    }

    /**
     * Updates curl position.
     */
    private void updateCurlPos(PointF pointerPos) {
        RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM);
        PointF tempRect = new PointF(mDragStartPos.x, mDragStartPos.y);
        tempRect.offset(pointerPos.x, pointerPos.y);
        float pageHeight = Math.abs(mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM).height());
//		if (tempRect.length() > pageHeight / 4) isCurling = true;
        // Default curl radius.
        double radius = 0.08f; //pageRect.width() / 4;
        mCurlPos.set(pointerPos);

        // If curl happens on right page, or on left page on two page mode,
        // we'll calculate curl position from pointerPos.
        if (curling) {

            mCurlDir.x = mCurlPos.x - mDragStartPos.x;
            mCurlDir.y = mCurlPos.y - mDragStartPos.y;
            float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
                    * mCurlDir.y);

            // Adjust curl radius so that if page is dragged far enough on
            // opposite side, radius gets closer to zero.
            double curlLen = radius * Math.PI;
            if (dist > (pageHeight * 2) - curlLen) {
                curlLen = Math.max((pageHeight * 2) - dist, 0f);
                curlLen += radius * Math.PI + 0.01f;
            }

            // Actual curl position calculation.
            if (dist >= curlLen) {
                double translate = (dist - curlLen) / 2;
                mCurlPos.x -= mCurlDir.x * translate / dist;
                mCurlPos.y -= mCurlDir.y * translate / dist;
            } else {
                double angle = Math.PI * Math.sqrt(dist / curlLen);
                double translate = radius * Math.sin(angle);
                mCurlPos.x += mCurlDir.x * translate / dist;
                mCurlPos.y += mCurlDir.y * translate / dist;
            }

            if (mCurlPos.y <= pageRect.bottom) {
                mPageCurl.reset();
                requestRender();
                return;
            }

            if (mCurlPos.y > pageRect.top) {
                mCurlPos.y = pageRect.top;
            }
            if (mCurlDir.x != 0) {
                float diffY = pageRect.top - mCurlPos.y;
                float rightX = mCurlPos.x - (diffY * mCurlDir.y / mCurlDir.x);
                if (mCurlDir.x < 0 && rightX < pageRect.right) {
                    mCurlDir.x = mCurlPos.y - pageRect.top;
                    mCurlDir.y = pageRect.right - mCurlPos.x;
                } else if (mCurlDir.x > 0 && rightX > pageRect.left) {
                    mCurlDir.x = pageRect.top - mCurlPos.y;
                    mCurlDir.y = mCurlPos.x - pageRect.left;
                }
            }
        }

        // Finally normalize direction vector and do rendering.
        double dist = Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y * mCurlDir.y);
        if (dist != 0) {
            mCurlDir.x /= dist;
            mCurlDir.y /= dist;
            mPageCurl.curl(mCurlPos, mCurlDir, radius);
        } else {
            mPageCurl.reset();
        }

        requestRender();
    }

    /**
     * Updates given CurlPage via PageProvider for page located at index.
     */
    private void updatePage(CurlPage page, int index) {
        // First reset page to initial state.
        page.reset();
        // Ask page provider to fill it up with bitmaps and colors.
        mPageProvider.updatePage(this, page, mPageBitmapWidth, mPageBitmapHeight, index);
    }

    /**
     * Updates bitmaps for page meshes.
     */
    private void updatePages() {
        if (mPageProvider == null || mPageBitmapWidth <= 0 || mPageBitmapHeight <= 0) {
            return;
        }

        // Remove meshes from renderer.
        mRenderer.removeCurlMesh(mPageTop);
        mRenderer.removeCurlMesh(mPageBottom);
        mRenderer.removeCurlMesh(mPageCurl);

        if (isCurled) {
            updatePage(mPageTop.getTexturePage(), 0);
            mPageTop.setFlipTexture(true);
            mPageTop.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_TOP));
            mPageTop.reset();
            mRenderer.addCurlMesh(mPageTop);
        } else {
            updatePage(mPageBottom.getTexturePage(), 0);
            mPageBottom.setFlipTexture(false);
            mPageBottom.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM));
            mPageBottom.reset();
            mRenderer.addCurlMesh(mPageBottom);
        }

        if (curling) {
            updatePage(mPageCurl.getTexturePage(), 0);
            mPageCurl.setFlipTexture(true);
            mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_BOTTOM));
            mPageCurl.reset();
            mRenderer.addCurlMesh(mPageCurl);
        }
    }

    /**
     * Set margins (or padding). Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    public void setMargins() {
        float width = getWidth();
        float height = getHeight();

        if (width > 0 && height > 0) {
            float dpi = this.getResources().getDisplayMetrics().density;
            float contentWidth = 254.f * dpi;
            float contentHeight = 354.f * dpi;
            float maxHeight = height * 0.5f;//0.67

            if (contentHeight > maxHeight) {
                contentHeight = maxHeight;
                contentWidth = contentHeight * 254.f / 354.f;
            }

            float left, top, right, bottom;
            if (orientation == LANDSCAPELEFT || orientation == LANDSCAPERIGHT) {
                left = (width - contentHeight) / width / 2;
                right = left;
                top = (height - contentWidth) / height / 2;
                bottom = top;
            } else {
                left = (width - contentWidth) / width / 2;
                right = left;
                top = (height - contentHeight) / height / 2;
                bottom = top;
            }

            mRenderer.setMargins(left, top, right, bottom);
        }
    }

    /**
     * Update/set page provider.
     */
    public void setPageProvider(PageProvider pageProvider) {
        mPageProvider = pageProvider;
        updatePages();
        requestRender();
    }


    /**
     * Sets SizeChangedObserver for this View. Call back method is called from
     * this View's onSizeChanged method.
     */
    public void setSizeChangedObserver(SizeChangedObserver observer) {
        mSizeChangedObserver = observer;
    }

    public void rotateView() {
        performRotate(true);
    }

    public boolean isLandscape() {
        return (orientation == LANDSCAPERIGHT || orientation == LANDSCAPELEFT);
    }

    private void performRotate(boolean force) {
        isRotating = true;
        if (force) {
            mRenderer.rotation = 45;
        }

        if (isEnableAutoRotate()) {
            switch (orientation) {
                case PORTRAIT:
                    orientation = (mRenderer.rotation < 0) ? LANDSCAPELEFT : LANDSCAPERIGHT;
                    break;
                case PORTRAITUPSIDEDOWN:
                    orientation = (mRenderer.rotation < 0) ? LANDSCAPERIGHT : LANDSCAPELEFT;
                    break;
                case LANDSCAPELEFT:
                    orientation = (mRenderer.rotation < 0) ? PORTRAITUPSIDEDOWN : PORTRAIT;
                    break;
                case LANDSCAPERIGHT:
                    orientation = (mRenderer.rotation < 0) ? PORTRAIT : PORTRAITUPSIDEDOWN;
                    break;
                default:
                    break;
            }
        }

        requestRender();
    }

    @Override
    public void onRotateBegan() {
        mCurlPos.x = mCurlPos.y = 0;
        mCurlDir.x = mCurlDir.y = 0;
        curling = false;
        mAnimationSource.x = 0;
        mAnimationSource.y = 0;
        mAnimationTarget.x = 0;
        mAnimationTarget.y = 0;
        mAnimationStartTime = 0;
        mAnimationTargetEvent = 0;
        mAnimate = false;
        if (mPageCurl != null) {
            mPageCurl.reset();
        }

        requestRender();
    }

    @Override
    public void onRotation(float degrees) {
        mRenderer.rotation = (degrees < 0) ? Math.max(degrees, -MAX_DEGREES) : Math.min(degrees, MAX_DEGREES);
        requestRender();
    }

    @Override
    public void onRotateFinished() {
        boolean autoRotate = isEnableAutoRotate();
        performRotate(false);
        if (mPageProvider != null && autoRotate) {
            mPageProvider.onRotateBegan(this, null);
        }
    }

    private boolean isEnableAutoRotate() {
        return (Math.abs(mRenderer.rotation) >= 45);
    }

    /**
     * Provider for feeding 'book' with bitmaps which are used for rendering
     * pages.
     */
    public interface PageProvider {

        public void updatePage(CurlView view, CurlPage page, int width, int height, int index);
        public void onCompleteCurl(CurlView view);
        public void onRotateBegan(CurlView view, CurlPage page);

    }

    public interface SizeChangedObserver {
        public void onSizeChanged(CurlView view);
    }

}
