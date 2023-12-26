package com.suprema.hid;

import android.hardware.usb.UsbDevice;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;

public interface IBioMiniHid {
   int DEVICE_TYPE_BMSS = 0;
   int DEVICE_TYPE_BMS2S = 1;
   int UFA_TEMPLATE_TYPE_SUPREMA = 2001;
   int UFA_TEMPLATE_TYPE_ISO19794_2 = 2002;
   int UFA_TEMPLATE_TYPE_ANSI378 = 2003;
   int UFA_IMAGE_TYPE_RAW = 0;
   int UFA_IMAGE_TYPE_BMP = 1;
   int UFA_IMAGE_TYPE_ISO = 2;
   int UFA_IMAGE_TYPE_WSQ = 3;
   int UFA_SLEEP_MODE = 1;
   int UFA_WAKEUP_MODE = 2;
   int UFA_FIDX_LEFT_LITTLE = 2048;
   int UFA_FIDX_LEFT_RING = 1024;
   int UFA_FIDX_LEFT_MIDDLE = 512;
   int UFA_FIDX_LEFT_INDEX = 256;
   int UFA_FIDX_LEFT_THUMB = 128;
   int UFA_FIDX_UNDEFINE_0 = 64;
   int UFA_FIDX_UNDEFINE_1 = 32;
   int UFA_FIDX_RIGHT_LITTLE = 1;
   int UFA_FIDX_RIGHT_RING = 2;
   int UFA_FIDX_RIGHT_MIDDLE = 4;
   int UFA_FIDX_RIGHT_INDEX = 8;
   int UFA_FIDX_RIGHT_THUMB = 16;
   int UFA_NORMAL_USER = 0;
   int UFA_POWER_USER = 1;
   int UFA_UN_TEMPLATE_NO_ENC = 0;
   int UFA_UN_TEMPLATE_ENC0 = 1;
   int UFA_UN_TEMPLATE_ENC1 = 2;
   int UFA_UN_TEMPLATE_ENC2 = 3;

   boolean Init(int var1);

   int GetDeviceCount();

   String GetDeviceName();

   String GetDeviceSN();

   String GetDeviceFWVersion();

   String GetDeviceHWVersion();

   String GetDevicePath();

   int GetHidProtocolVersion(byte[] var1);

   int GetImageWidth();

   int GetImageHeight();

   int GetDeviceType();

   boolean SetCommandTimeout(int var1);

   int HidCommand(Hid.Pac var1, Hid.Cmd var2, Hid.Sub var3, byte[] var4);

   int HidCommand(Hid.Pac var1, Hid.Cmd var2, Hid.Sub var3, Hid.Sub var4, byte[] var5);

   int HidCommand(Hid.Pac var1, Hid.Cmd var2, int var3, int var4, byte[] var5);

   byte[] GetHidEcho();

   byte[] GetHidEchoData();

   String GetErrorString(int var1);

   byte[] ReceiveData(Hid.Sub var1, int var2, int var3);

   int SendData(Hid.Sub var1, byte[] var2);

   int Init();

   int Uninit();

   int SetDevice(UsbDevice var1);

   String GetVersionString();

   IBioMiniDevice.ErrorCode _captureSingle(IBioMiniDevice.CaptureOption var1, ICaptureResponder var2);

   IBioMiniDevice.ErrorCode _setParameter(String var1, int var2);

   IBioMiniDevice.ErrorCode _getParameter(String var1, int[] var2);

   IBioMiniDevice.ErrorCode _startCapturing();

   IBioMiniDevice.ErrorCode _autoCapture();

   IBioMiniDevice.ErrorCode _abortCapturing();

   boolean IsCapturing();

   byte[] ReceiveData(Hid.Sub var1);

   IBioMiniDevice.ErrorCode GetTemplateQuality(int[] var1);

   IBioMiniDevice.ErrorCode setPacketMode(IBioMiniDevice.PacketMode var1);

   IBioMiniDevice.ErrorCode SaveBufferInPictureDirectory(byte[] var1, String var2, boolean var3);
}
