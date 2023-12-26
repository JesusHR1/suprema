package com.suprema.util;

public interface IBioMiniCallback {
   void onCaptureCallback(byte[] var1, int var2, int var3, int var4, boolean var5);

   void onErrorOccurred(String var1);
}
