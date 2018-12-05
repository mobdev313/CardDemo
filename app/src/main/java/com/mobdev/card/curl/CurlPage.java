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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

public class CurlPage {

    public static final int SIDE_BACK = 2;
    public static final int SIDE_FRONT = 1;

    private Bitmap mTextureBack;
    private Bitmap mTextureFront;
    private RectF mRectBack;
    private RectF mRectFront;
    private boolean mTexturesChanged;

    /**
     * Default constructor.
     */
    public CurlPage() {
        reset();
        mRectBack = new RectF();
        mRectFront = new RectF();
    }

    /**
     * Calculates the next highest power of two for a given integer.
     */
    private int getNextHighestPO2(int n) {
        n -= 1;
        n = n | (n >> 1);
        n = n | (n >> 2);
        n = n | (n >> 4);
        n = n | (n >> 8);
        n = n | (n >> 16);
        n = n | (n >> 32);
        return n + 1;
    }

    /**
     * Generates nearest power of two sized Bitmap for give Bitmap. Returns this
     * new Bitmap using default return statement + original texture coordinates
     * are stored into RectF.
     */
    private Bitmap getTexture(Bitmap bitmap, RectF textureRect) {
        // Bitmap original size.
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        // Bitmap size expanded to next power of two. This is done due to
        // the requirement on many devices, texture width and height should
        // be power of two.
        int newW = getNextHighestPO2(w);
        int newH = getNextHighestPO2(h);

        // Is there another way to create a bigger Bitmap and copy
        // original Bitmap to it more efficiently? Immutable bitmap anyone?
        Bitmap bitmapTex = Bitmap.createBitmap(newW, newH, bitmap.getConfig());
        Canvas c = new Canvas(bitmapTex);
        c.drawBitmap(bitmap, 0, 0, null);

        // Calculate final texture coordinates.
        float texX = (float) w / newW;
        float texY = (float) h / newH;
        textureRect.set(0f, 0f, texX, texY);

        return bitmapTex;
    }

    /**
     * Getter for textures. Creates Bitmap sized to nearest power of two, copies
     * original Bitmap into it and returns it. RectF given as parameter is
     * filled with actual texture coordinates in this new upscaled texture
     * Bitmap.
     */
    public Bitmap getTexture(RectF textureRect, int side) {
        switch (side) {
            case SIDE_FRONT:
                textureRect.set(mRectFront);
                return mTextureFront;
            //return getTexture(mTextureFront, textureRect);
            default:
                //return getTexture(mTextureBack, textureRect);
                textureRect.set(mRectBack);
                return mTextureBack;
        }
    }

    /**
     * Returns true if textures have changed.
     */
    public boolean getTexturesChanged() {
        return mTexturesChanged;
    }

    /**
     * Resets this CurlPage into its initial state.
     */
    public void reset() {
        if (mTextureFront != null) {
            mTextureFront.recycle();
            mTextureFront = null;
        }
        if (mTextureBack != null) {
            mTextureBack.recycle();
            mTextureBack = null;
        }
        mTexturesChanged = false;
    }

    /**
     * Setter for textures.
     */
    public void setTexture(Bitmap texture, int side) {
        switch (side) {
            case SIDE_FRONT:
                if (mTextureFront != null)
                    mTextureFront.recycle();
                mTextureFront = getTexture(texture, mRectFront);
                break;
            case SIDE_BACK:
                if (mTextureBack != null)
                    mTextureBack.recycle();
                mTextureBack = getTexture(texture, mRectBack);
                break;
        }
        mTexturesChanged = true;
    }

}
