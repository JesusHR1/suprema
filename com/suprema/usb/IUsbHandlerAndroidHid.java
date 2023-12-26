package com.suprema.usb;

import com.suprema.hid.Hid;

public interface IUsbHandlerAndroidHid extends IUsbHandler {
   int hidCommand(Hid.Pac var1, Hid.Cmd var2, Hid.Sub var3);

   int hidCommand(int var1, int var2, int var3);

   int hidCommand(int var1, int var2, int var3, int var4);

   int hidCommand(Hid.Pac var1, Hid.Cmd var2, Hid.Sub var3, byte[] var4);

   int hidCommand(Hid.Pac var1, Hid.Cmd var2, int var3, int var4, byte[] var5);

   int hidCommand(int var1, int var2, int var3, int var4, byte[] var5);

   int hidCommand(int var1, int var2, int var3, byte[] var4);

   int echo(Hid.Pac var1, Hid.Cmd var2, Hid.Sub var3);

   int echo(int var1, int var2, int var3);

   byte[] getLastEcho();

   byte[] getLastEchoData();

   int hidReceiveSize();

   boolean hidReceive(byte[] var1);

   byte[] hidReceive();

   boolean hidSend(byte[] var1, int var2);

   int setUSBPacketMode(int var1);

   int resetDevice();
}
