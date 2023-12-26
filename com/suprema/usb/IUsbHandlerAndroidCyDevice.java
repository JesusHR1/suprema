package com.suprema.usb;

public interface IUsbHandlerAndroidCyDevice extends IUsbHandler {
   void setBulkTimeout(int var1);

   int getBulkTimeout();

   boolean hasInterruptEndpoint();

   boolean isValid();

   boolean isNative();

   boolean read(byte[] var1, int var2, byte var3, IUsbHandler.IReadProcessor var4);

   boolean read(byte[] var1, int var2, byte var3, IUsbHandler.IReadProcessorAdv var4);

   boolean read(byte[] var1, int var2, byte var3, IUsbHandler.IReadProcessorAdv var4, int var5);

   boolean readSync(byte[] var1, int var2, byte var3, IUsbHandler.IReadProcessorAdv var4);

   boolean readSync(byte[] var1, int var2, byte var3, IUsbHandler.IReadProcessorAdv var4, int var5);

   boolean readSync(byte[] var1, int var2, int var3);

   void resetBulkPipe(boolean var1);

   void setBulkRx(int var1);

   void setBulkTx(int var1);

   boolean isReading();
}
