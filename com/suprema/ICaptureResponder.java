package com.suprema;

import android.graphics.Bitmap;

public interface ICaptureResponder {
   void onCapture(Object var1, IBioMiniDevice.FingerState var2);

   boolean onCaptureEx(Object var1, Bitmap var2, IBioMiniDevice.TemplateData var3, IBioMiniDevice.FingerState var4);

   void onCaptureError(Object var1, int var2, String var3);
}
