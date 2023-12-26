package com.suprema.devices;

public class BioMiniJniCommon {
   static native int GetCaptureImageBufferTo197944ImageBuffer(byte[] var0, int var1, int var2, byte[] var3, int[] var4);

   static {
      System.loadLibrary("biominicommon");
   }
}
