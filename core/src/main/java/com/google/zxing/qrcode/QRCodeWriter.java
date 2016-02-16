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

package com.google.zxing.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.google.zxing.qrcode.encoder.RGBMatrix;

import java.util.Map;

/**
 * This object renders a QR Code as a BitMatrix 2D array of greyscale values or an RGBMatrix of int RGB values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author z@chary.us
 */
public final class QRCodeWriter implements Writer {

  private static final int QUIET_ZONE_SIZE = 4;
  public static final int SHAPE_CIRCLE = 1;
  public static final int SHAPE_SQUARE = 2;
  public static final int SHAPE_SQUARE_OUTLINED = 4;

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height)
      throws WriterException {

    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {

    if (contents.isEmpty()) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (format != BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
          height);
    }

    ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
    int quietZone = QUIET_ZONE_SIZE;
    if (hints != null) {
      if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
        errorCorrectionLevel = ErrorCorrectionLevel.valueOf(hints.get(EncodeHintType.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(EncodeHintType.MARGIN)) {
        quietZone = Integer.parseInt(hints.get(EncodeHintType.MARGIN).toString());
      }
    }

    QRCode code = Encoder.encode(contents, errorCorrectionLevel, hints);
    return renderResult(code, width, height, quietZone);
  }

  public RGBMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          RGBMatrix[] masks,
                          int[] shapes,
                          Map<EncodeHintType,?> hints) throws WriterException {

    if (contents.isEmpty()) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (format != BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
              height);
    }

    ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
    int quietZone = QUIET_ZONE_SIZE;
    if (hints != null) {
      if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
        errorCorrectionLevel = ErrorCorrectionLevel.valueOf(hints.get(EncodeHintType.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(EncodeHintType.MARGIN)) {
        quietZone = Integer.parseInt(hints.get(EncodeHintType.MARGIN).toString());
      }
    }

    QRCode code = Encoder.encode(contents, errorCorrectionLevel, hints);
    return renderResult(code, width, height, quietZone, masks, shapes);
  }

  // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
  // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
  private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
    ByteMatrix input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + (quietZone * 2);
    int qrHeight = inputHeight + (quietZone * 2);
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
    // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
    // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
    // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
    // handle all the padding from 100x100 (the actual QR) up to 200x160.
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY) > 0) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }
    return output;
  }

  // This method is the same as the above but it returns an RGBMatrix according to the supplied mask. Byte
  // values of 2 are masked and byte values of 1 are blacked and values of 0 are whited.
  private static RGBMatrix renderResult(QRCode code, int width, int height, int quietZone, RGBMatrix[] masks, int[] shapes) {
    ByteMatrix input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + (quietZone * 2);
    int qrHeight = inputHeight + (quietZone * 2);
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    RGBMatrix output = new RGBMatrix(outputWidth, outputHeight);
    output.clear(0xFFFFFF);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY) == 1) {
          output.setRegion(outputX, outputY, multiple, multiple, 0x000000);
        } else if(input.get(inputX, inputY) > 1) {
          // loop through all columns and rows contained within current region and apply respective area of mask
          if(shapes[input.get(inputX, inputY)]==SHAPE_SQUARE_OUTLINED){
            for (int cx = outputX + 1; cx < outputX + multiple; cx++) {
              for (int cy = outputY + 1; cy < outputY + multiple; cy++) {
                output.set(cx, cy, masks[input.get(inputX, inputY)].get(cx, cy));
              }
            }
          } else if(shapes[input.get(inputX, inputY)]==SHAPE_CIRCLE){
            for (int cx = outputX-4; cx < outputX + multiple + 4; cx++) {
              for (int cy = outputY-4; cy < outputY + multiple + 4; cy++) {
                // (x - (center X))^2 + (y - (center Y))^2 < radius^2
                float radius = multiple/2, centerX = outputX + multiple/2, centerY = outputY + multiple/2;
                if(Math.pow(cx-centerX,2) + Math.pow(cy-centerY,2) < Math.pow(radius,2)) {
                  output.set(cx, cy, masks[input.get(inputX, inputY)].get(cx, cy));
                }
              }
            }
          } else {
            for (int cx = outputX; cx < outputX + multiple; cx++) {
              for (int cy = outputY; cy < outputY + multiple; cy++) {
                output.set(cx, cy, masks[input.get(inputX, inputY)].get(cx, cy));
              }
            }
          }
        }
      }
    }
    return output;
  }

}
