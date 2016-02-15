/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.qrcode.encoder;

/**
 * @author z@chary.us (Zach Bloomquist)
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class RGBMatrix {

    private final int[][] pixels; // 2D array of RGB ints (0xFFFFFF)
    private final int width;
    private final int height;

    public RGBMatrix(int width, int height) {
        pixels = new int[height][width];
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int get(int x, int y) {
        return pixels[y][x];
    }

    /**
     * @return an internal representation as ints, in row-major order. array[y][x] represents point (x,y)
     */
    public int[][] getArray() {
        return pixels;
    }

    public void set(int x, int y, int value) {
        pixels[y][x] = value;
    }

    // to provide syntax backwards-compatibility with ByteMatrix
    public void set(int x, int y, byte value) {
        pixels[y][x] = (int) value;
    }

    public void set(int x, int y, boolean value) {
        pixels[y][x] = (value ? 1 : 0);
    }

    /**
     * <p>Sets a square region of the RGB matrix to a value.</p>
     *
     * @param left The horizontal position to begin at (inclusive)
     * @param top The vertical position to begin at (inclusive)
     * @param width The width of the region
     * @param height The height of the region
     */
    public void setRegion(int left, int top, int width, int height, int value) {
        if (top < 0 || left < 0) {
            throw new IllegalArgumentException("Left and top must be nonnegative");
        }
        if (height < 1 || width < 1) {
            throw new IllegalArgumentException("Height and width must be at least 1");
        }
        int right = left + width;
        int bottom = top + height;
        if (bottom > this.height || right > this.width) {
            throw new IllegalArgumentException("The region must fit inside the matrix");
        }
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                set(x,y,value);
            }
        }
    }

    public void clear(int value) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                pixels[y][x] = value;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(2 * width * height + 2);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                switch (pixels[y][x]) {
                    case 0:
                        result.append(" 0");
                        break;
                    case 1:
                        result.append(" 1");
                        break;
                    case 2:
                        result.append(" M");
                        break;
                    default:
                        result.append("  ");
                        break;
                }
            }
            result.append('\n');
        }
        return result.toString();
    }

}
