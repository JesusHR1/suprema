package com.suprema.usb;

public interface IUsbHandler {
   boolean enumerate();

   int getDeviceCount();

   void setCommandTimeout(int var1);

   boolean open(int var1);

   void close();

   void resize(int var1);

   String getPath();

   boolean initRead(int var1, int var2, boolean var3);

   boolean read(byte[] var1, int var2, int var3, byte var4);

   boolean isEqual(Object var1);

   boolean write(byte[] var1, int var2);

   boolean writeHid(byte[] var1, byte var2, int var3);

   boolean readSmall(byte[] var1, int var2);

   boolean readEEPROM(int var1, int var2, byte[] var3);

   boolean readSensorEEPROM(int var1, int var2, byte[] var3);

   boolean controlRx(int var1, byte[] var2, int var3);

   boolean controlTx(int var1, byte[] var2, int var3);

   public interface IReadProcessorAdv extends IUsbHandler.IReadProcessor {
      void firstBulkReceived(long var1);
   }

   public interface IReadProcessor {
      boolean beforeRead();

      boolean afterRead();
   }
}
