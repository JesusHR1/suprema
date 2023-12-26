package com.suprema.hid;

public interface ICaptureCallback {
   void onCapture(byte[] var1, int var2, int var3, int var4, boolean var5);

   void onError(String var1);
}
