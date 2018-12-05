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
package com.mobdev.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.mobdev.card.curl.CurlPage;
import com.mobdev.card.curl.CurlView;

public class CardContainerView extends LinearLayout implements CurlView.PageProvider {

    private CurlView cardView;
    private int cardValue = 1;


    private static final int FRONTSIDE = 0;
    private static final int BACKSIDE = 1;

    public CardContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CardContainerView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.card_container, this, true);

        cardView = findViewById(R.id.card);
        cardView.setPageProvider(this);
        cardView.setSizeChangedObserver(new SizeChangedObserver());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight();
        if (height > 0) {
            LinearLayout.LayoutParams cardParams1 = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            cardParams1.width = height * 3 / 5;
            cardParams1.height = height * 93 / 100;
            cardView.setLayoutParams(cardParams1);
        }
    }

    public void reset() {
        cardView.reset();
    }

    public void setVisible(int visible) {
        if (visible == View.INVISIBLE) {
            cardView.onPause();
            cardView.reset();
            cardView.setVisibility(View.GONE);
            setVisibility(View.INVISIBLE);
        } else {
            cardView.onResume();
            cardView.setVisibility(View.VISIBLE);
            setVisibility(View.VISIBLE);
        }
    }

    // for one card mode
    public void setCardValue(int value) {
        cardValue = value;
    }


    private Bitmap loadBitmap(CurlView view, int index, int side) {
        Bitmap bitmap;
        Rect rect;

        Drawable drawable = getResources().getDrawable(index);
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        if (view.isLandscape()) {
            bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
            rect = new Rect((height - width)/2, (width - height)/2, (height + width)/2, (height + width)/2);
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            rect = new Rect(0, 0, width, height);
        }

        Canvas c = new Canvas(bitmap);
        c.save();

        switch (view.orientation) {
            case CurlView.LANDSCAPERIGHT:
                c.rotate(90, height / 2, width / 2);
                break;
            case CurlView.LANDSCAPELEFT:
                c.rotate(270, height / 2, width / 2);
                break;
            case CurlView.PORTRAITUPSIDEDOWN:
                c.rotate(180, width / 2, height / 2);
                break;
        }

        drawable.setBounds(rect);
        drawable.draw(c);

        c.restore();
        //Draw the rotate pinch image at the back of card.
        if (side == FRONTSIDE) {
            drawable = getResources().getDrawable(R.drawable.card_rotate);
            int halfOfWidth = width / 6;
            if (view.isLandscape()) {
                rect = new Rect(height / 2 - halfOfWidth, width / 2 -halfOfWidth, height / 2 + halfOfWidth, width / 2 + halfOfWidth);
            } else {
                rect = new Rect(width / 2 - halfOfWidth, height / 2 - halfOfWidth, width / 2 + halfOfWidth, height / 2 + halfOfWidth);
            }
            drawable.setBounds(rect);
            drawable.draw(c);
        }

        return bitmap;
    }

    @Override
    public void updatePage(CurlView view, CurlPage page, int width, int height, int index) {
        Bitmap front = loadBitmap(view, R.drawable.card_00_blue, FRONTSIDE);
        page.setTexture(front, CurlPage.SIDE_FRONT);
        front.recycle();

        int id = R.drawable.card_00_red + cardValue;
        Bitmap back= loadBitmap(view, id, BACKSIDE);
        page.setTexture(back, CurlPage.SIDE_BACK);
        back.recycle();
    }

    @Override
    public void onCompleteCurl(final CurlView view) {
    }

    @Override
    public void onRotateBegan(CurlView view, CurlPage page) {
    }

    /**
     * CurlView size changed observer.
     */
    private class SizeChangedObserver implements CurlView.SizeChangedObserver {
        @Override
        public void onSizeChanged(CurlView view) {
            view.setMargins();
        }
    }

}
