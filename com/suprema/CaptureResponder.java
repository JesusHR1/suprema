package com.suprema;

import android.graphics.Bitmap;

public abstract class CaptureResponder implements ICaptureResponder {
   public void onCapture(Object context, IBioMiniDevice.FingerState fingerState) {
   }

   public boolean onCaptureEx(Object context, Bitmap capturedImage, IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
      return false;
   }

   public void onCaptureError(Object context, int errorCode, String error) {
   }
}
