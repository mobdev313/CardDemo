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

import android.view.MotionEvent;

public class RotationGestureDetector {

    private float startDegrees = 0f;		// When touch down, remember first angle.
    private float rotatedDegrees = 0f;	// Angle value to be rotated.
    private boolean isRotate = false;

    private OnRotationListener listener;

    public RotationGestureDetector() {
        startDegrees = 0f;
    }

    public void setOnRotationListener(OnRotationListener listener) {
        this.listener = listener;
    }

    public boolean touchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                startDegrees = rotation(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                rotatedDegrees += rotation(event) - startDegrees;
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 2) {
                    isRotate = true;
                    float newRot = rotation(event) - startDegrees + rotatedDegrees;
                    listener.onRotation(mod(newRot, 360));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                rotatedDegrees = 0;
                startDegrees = 0;
                if (isRotate) {
                    isRotate = false;
                    listener.onRotateFinished();
                }
        }
        return true;
    }

    /**
     * Calculate the degree to be rotated by.
     * @param event
     * @return Degrees
     */
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private static float mod(float src, float divider) {
        return src - divider * ((int)src / (int)divider);
    }

    public interface OnRotationListener {
        void onRotateBegan();
        void onRotation(float degrees);
        void onRotateFinished();
    }
}
