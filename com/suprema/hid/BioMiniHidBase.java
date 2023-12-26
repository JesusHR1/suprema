package com.suprema.hid;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import com.suprema.ABioMiniDevice;
import com.suprema.IBioMiniDevice;
import com.suprema.IBioMiniInterops;
import com.suprema.ICaptureResponder;
import com.suprema.IUsbEventHandler;
import com.suprema.devices.util;
import com.suprema.usb.UsbHandlerAndroidHid;
import com.suprema.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public abstract class BioMiniHidBase extends ABioMiniDevice implements IBioMiniHid, IBioMiniInterops {
   private static final String TAG = "BioMini SDK";
   private boolean mIsInUse = false;
   private static final boolean g_bPrintLog = false;
   private static final boolean g_bPrintTimetags = false;
   private static final int SLIMS_IMAGE_WIDTH = 320;
   private static final int SLIMS_IMAGE_HEIGHT = 480;
   private static final int SLIM2S_IMAGE_WIDTH = 300;
   private static final int SLIM2S_IMAGE_HEIGHT = 400;
   private static final int TRUE = 1;
   private static final int FALSE = 0;
   private static final String ACTION_USB_PERMISSION = "com.supremahq.samples.USB_PERMISSION";
   private final Context mApplicationContext;
   private UsbManager mUsbManager = null;
   private UsbDevice mDevice = null;
   private UsbHandlerAndroidHid mUsbHandler = null;
   private static final int IMG_BUF_MAX = 153600;
   private static final int IMG_INT_BUF_MAX = 153600;
   private final byte[] m_ImageLast = new byte[153600];
   private boolean bIsAfterAbortCpaturing = true;
   private boolean bInitialized = false;
   private boolean bUninitSet = false;
   private boolean mDeviceFound = false;
   private boolean bUSBisdominated = false;
   private int mCurrentPID = 0;
   private Runnable mSLoop;
   private Runnable mAutoLoop;
   private Thread mUsbThread;
   private boolean bThreadFlag;
   private boolean bAbortCapturing = false;
   private boolean isCaptured = false;
   private boolean bHidProtocolv10 = false;
   private static final boolean mSaveDebugImage = false;
   private static final boolean mPrintDebugMsg = false;
   boolean m_bImageFlip = false;
   private BioMiniHidBase.PermissionReceiver mPermissionReceiver = new BioMiniHidBase.PermissionReceiver(new BioMiniHidBase.IPermissionListener() {
      public void onPermissionDenied(UsbDevice d) {
         BioMiniHidBase.this.LogD("Permission denied on " + d.getDeviceId());
      }
   });
   private ICaptureResponder mCapturerResponder;

   public BioMiniHidBase(Activity parentActivity) {
      this.mApplicationContext = parentActivity.getApplicationContext();
      this.mUsbManager = (UsbManager)this.mApplicationContext.getSystemService("usb");
   }

   public BioMiniHidBase(UsbManager usbManager) {
      this.mApplicationContext = null;
      this.mUsbManager = usbManager;
   }

   private void prepareDevice() {
      this.mDeviceInfo.deviceName = this.GetDeviceName();
      this.mDeviceInfo.deviceSN = this.GetDeviceSN();
      this.mDeviceInfo.versionSDK = this.BASE_VERSION;
   }

   private void printTimeTag(String msg) {
   }

   private void init() {
      this.enumerate(new BioMiniHidBase.IPermissionListener() {
         public void onPermissionDenied(UsbDevice d) {
            if (BioMiniHidBase.this.mApplicationContext != null && BioMiniHidBase.this.mUsbManager != null) {
               PendingIntent pi = PendingIntent.getBroadcast(BioMiniHidBase.this.mApplicationContext, 0, new Intent("com.supremahq.samples.USB_PERMISSION"), 0);
               BioMiniHidBase.this.mApplicationContext.registerReceiver(BioMiniHidBase.this.mPermissionReceiver, new IntentFilter("com.supremahq.samples.USB_PERMISSION"));
               BioMiniHidBase.this.mUsbManager.requestPermission(d, pi);
            }

         }
      });
   }

   private void enumerate(BioMiniHidBase.IPermissionListener listener) {
      this.LogD("enumerating");
      if (this.mUsbManager != null) {
         label38: {
            HashMap<String, UsbDevice> devlist = this.mUsbManager.getDeviceList();
            Iterator var3 = devlist.values().iterator();

            UsbDevice d;
            do {
               do {
                  if (!var3.hasNext()) {
                     break label38;
                  }

                  d = (UsbDevice)var3.next();
                  this.LogD("Found device: " + String.format("0x%04X:0x%04X", d.getVendorId(), d.getProductId()));
               } while(d.getVendorId() != 5841);
            } while(d.getProductId() != 1056 && d.getProductId() != 1057);

            this.mCurrentPID = d.getProductId();
            this.mDeviceFound = true;
            this.LogD("Device under: " + d.getDeviceName());
            if (this.mUsbManager.hasPermission(d)) {
               this.LogD("startHandler_enumerate");
               this.mDevice = d;
               return;
            }

            this.LogD("onPermissionDenied called");
            listener.onPermissionDenied(d);
         }
      }

      if (!this.mDeviceFound) {
         this.LogD("no device found");
      }

   }

   private int getTargetWidth() {
      switch(this.GetProductId()) {
      case 1056:
         return 320;
      case 1057:
         return 300;
      default:
         return -1;
      }
   }

   private int getTargetHeight() {
      switch(this.GetProductId()) {
      case 1056:
         return 480;
      case 1057:
         return 400;
      default:
         return -1;
      }
   }

   public int GetProductId() {
      return this.mCurrentPID;
   }

   public static boolean IsSupported(int vid, int pid) {
      return vid == 5841 && (pid == 1056 || pid == 1057);
   }

   public int SetDevice(UsbDevice device) {
      if (this.mDevice == device) {
         return IBioMiniDevice.ErrorCode.OK.value();
      } else {
         this.mDevice = device;
         this.Uninit();
         if (device == null) {
            this.mDeviceFound = false;
            this.mCurrentPID = 0;
            return IBioMiniDevice.ErrorCode.OK.value();
         } else if (device.getVendorId() != 5841 || device.getProductId() != 1056 && device.getProductId() != 1057) {
            return IBioMiniDevice.ErrorCode.ECH_ERR_NO_DEVICE_FOUND.value();
         } else {
            this.LogD("Device under: " + device.getDeviceName());
            if (!this.mUsbManager.hasPermission(device)) {
               this.LogD("Permission denied");
               this.mDeviceFound = false;
               return IBioMiniDevice.ErrorCode.ECH_ERR_PERMISSION_DENIED.value();
            } else {
               this.LogD("Device assigned " + device);
               this.mCurrentPID = device.getProductId();
               this.mDevice = device;
               this.mDeviceFound = true;
               int ret = this.Setting(device.getProductId());
               return ret != IBioMiniDevice.ErrorCode.OK.value() ? IBioMiniDevice.ErrorCode.ECH_ERR_NO_DEVICE_FOUND.value() : IBioMiniDevice.ErrorCode.OK.value();
            }
         }
      }
   }

   public int Init() {
      if (this.bInitialized) {
         this.LogD("Init >> Already initialized");
         return IBioMiniDevice.ErrorCode.ECH_WRN_ALREADY_DONE.value();
      } else if (this.mDevice == null) {
         this.LogD("Init >> No device found");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NO_DEVICE_FOUND.value();
      } else {
         UsbDeviceConnection conn = this.mUsbManager.openDevice(this.mDevice);
         UsbEndpoint epIN = null;
         UsbEndpoint epOUT = null;
         if (conn == null) {
            this.Uninit();
            return IBioMiniDevice.ErrorCode.ECH_ERR_PERMISSION_DENIED.value();
         } else if (!conn.claimInterface(this.mDevice.getInterface(0), true)) {
            this.LogD("Can not connect device");
            return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
         } else if (this.mDevice.getInterfaceCount() <= 0) {
            this.LogD("Device has no interface");
            return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
         } else {
            UsbInterface usbIf = this.mDevice.getInterface(0);
            this.LogD("USB interface count: " + usbIf.getEndpointCount());

            for(int i = 0; i < usbIf.getEndpointCount(); ++i) {
               if (usbIf.getEndpoint(i).getType() == 3) {
                  this.LogD("EndPoint No (interrupt) : " + usbIf.getEndpoint(i).getEndpointNumber() + ", " + usbIf.getEndpoint(i));
                  if (usbIf.getEndpoint(i).getDirection() == 128) {
                     epIN = usbIf.getEndpoint(i);
                  } else {
                     epOUT = usbIf.getEndpoint(i);
                  }
               }
            }

            this.mUsbHandler = new UsbHandlerAndroidHid(this, conn, epIN, epOUT, 153600);
            this.bInitialized = true;
            this.Setting(this.mDevice.getProductId());
            byte[] nHidProtocol = new byte[2];
            int Ret = this.GetHidProtocolVersion(nHidProtocol);
            if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
               if (nHidProtocol[1] == 1 && nHidProtocol[0] == 0) {
                  this.bHidProtocolv10 = true;
               }

               this.LogD(String.format(Locale.US, "HidProtocol Version (%d.%d)", nHidProtocol[1], nHidProtocol[0]));
            }

            this.LogD(this.GetDeviceName() + " : " + this.GetDeviceSN());
            return IBioMiniDevice.ErrorCode.OK.value();
         }
      }
   }

   public int Uninit() {
      this.LogD("Uninitializing...");
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else if (this.bUninitSet) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
      } else {
         this.bUninitSet = true;
         if (!this.bAbortCapturing) {
            this._abortCapturing();
            this.LogD("Abort Capturing");
         } else {
            this.LogD("Capture is already aborted");
         }

         while(this.bUSBisdominated) {
         }

         this.bInitialized = false;
         this.mDevice = null;
         this.mCurrentPID = 0;
         this.mUsbHandler.close();
         this.mUsbHandler = null;
         this.bUninitSet = false;
         return IBioMiniDevice.ErrorCode.OK.value();
      }
   }

   public boolean Init(int index) {
      return false;
   }

   public int GetDeviceCount() {
      return 0;
   }

   public boolean isEqual(Object dev) {
      if (this.mDevice == null) {
         return false;
      } else {
         UsbDevice _dev = (UsbDevice)dev;
         return this.mDevice.getDeviceName().equals(_dev.getDeviceName());
      }
   }

   public boolean activate(Object appContext, Object deviceContext, Object transfer_mode) {
      return this.activate(appContext, deviceContext);
   }

   public boolean activate(Object appContext, Object device) {
      if (!this.mIsInUse && this.SetDevice((UsbDevice)device) == 0) {
         this.mIsInUse = this.Init() == 0;
         return this.mIsInUse;
      } else if (this.mDevice == null) {
         return false;
      } else {
         UsbDevice _dev = (UsbDevice)device;
         return false;
      }
   }

   public boolean activate() {
      if (!this.mIsInUse && this.mDevice != null && this.mUsbHandler != null) {
         this.mIsInUse = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean deactivate(IUsbEventHandler.DisconnectionCause reason) {
      if (this.mIsInUse) {
         this.mIsInUse = false;
         this.mDevice = null;
         return true;
      } else {
         return false;
      }
   }

   public boolean isInUse() {
      return false;
   }

   public String GetDeviceName() {
      String dev_info = null;
      this.LogD("GetDeviceName Request");
      if (this.mUsbHandler != null) {
         dev_info = "N/A";
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_DEV_INFO, Hid.Sub.CCT_NA, (byte[])null);
         if (Ret == 0) {
            dev_info = (new String(this.mUsbHandler.getLastEchoData(), 0, 16)).trim();
         } else {
            this.LogD("GetDeviceName error : " + Hid.errString(Ret));
         }
      }

      return dev_info;
   }

   public String GetDeviceSN() {
      String dev_info = null;
      this.LogD("GetDeviceSN Request");
      if (this.mUsbHandler != null) {
         dev_info = "N/A";
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_DEV_INFO, Hid.Sub.CCT_NA, (byte[])null);
         if (Ret == 0) {
            dev_info = (new String(this.mUsbHandler.getLastEchoData(), 16, 16)).trim();
         } else {
            this.LogD("GetDeviceSN error : " + Hid.errString(Ret));
         }
      }

      return dev_info;
   }

   public String GetDeviceFWVersion() {
      String dev_info = null;
      this.LogD("GetDeviceFWVersion Request");
      if (this.mUsbHandler != null) {
         dev_info = "N/A";
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_DEV_INFO, Hid.Sub.CCT_NA, (byte[])null);
         if (Ret == 0) {
            dev_info = (new String(this.mUsbHandler.getLastEchoData(), 32, 8)).trim();
         } else {
            this.LogD("GetDeviceFWVersion error : " + Hid.errString(Ret));
         }
      }

      return dev_info;
   }

   public String GetDeviceHWVersion() {
      String dev_info = null;
      this.LogD("GetDeviceHWVersion Request");
      if (this.mUsbHandler != null) {
         dev_info = "N/A";
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_DEV_INFO, Hid.Sub.CCT_NA, (byte[])null);
         if (Ret == 0) {
            dev_info = (new String(this.mUsbHandler.getLastEchoData(), 40, 8)).trim();
         } else {
            this.LogD("GetDeviceHWVersion error : " + Hid.errString(Ret));
         }
      }

      return dev_info;
   }

   public int GetHidProtocolVersion(byte[] nVersion) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_HID_PROTOCOL_VER, Hid.Sub.SUB_NA);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, nVersion, 0, 2);
         }

         return Ret;
      }
   }

   public int GetUsbPacketMode(int[] mode) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_USB_TYPE, Hid.Sub.SUB_NA);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            mode[0] = temp[0];
         }

         return Ret;
      }
   }

   public String GetDevicePath() {
      return this.mDevice != null ? this.mDevice.getDeviceName() : null;
   }

   public boolean SetCommandTimeout(int timeout) {
      return false;
   }

   public int HidCommand(Hid.Pac pac, Hid.Cmd cmd, Hid.Sub sub, byte[] data) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         return this.mUsbHandler != null ? this.mUsbHandler.hidCommand(pac, cmd, sub, data) : IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
      }
   }

   public int HidCommand(Hid.Pac pac, Hid.Cmd cmd, Hid.Sub sub_f1, Hid.Sub sub_f2, byte[] data) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         return this.mUsbHandler != null ? this.mUsbHandler.hidCommand(pac, cmd, sub_f1.value(), sub_f2.value(), data) : IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
      }
   }

   public int HidCommand(Hid.Pac pac, Hid.Cmd cmd, int sub_f1, int sub_f2, byte[] data) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         return this.mUsbHandler != null ? this.mUsbHandler.hidCommand(pac, cmd, sub_f1, sub_f2, data) : IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
      }
   }

   public byte[] GetHidEcho() {
      return this.mUsbHandler.getLastEcho();
   }

   public byte[] GetHidEchoData() {
      return this.mUsbHandler.getLastEchoData();
   }

   public byte[] ReceiveData(Hid.Sub type, int interval, int delay) {
      if (!this.bInitialized) {
         return null;
      } else {
         if (this.mUsbHandler != null) {
            byte[] extra = new byte[8];
            Hid.putInt(extra, 0, interval);
            Hid.putInt(extra, 4, delay);
            int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_RECEIVE_DATA, type, extra);
            if (Ret == 0) {
               byte[] buf = new byte[this.mUsbHandler.hidReceiveSize()];
               if (this.mUsbHandler.hidReceive(buf)) {
                  return buf;
               }

               Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_RECEIVE_DATA, type, extra);
               if (Ret == 0) {
                  this.mUsbHandler.hidReceive(buf);
                  return buf;
               }
            }
         }

         return null;
      }
   }

   public int SendData(Hid.Sub type, byte[] data) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else if (this.mUsbHandler != null) {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         Hid.putInt(info, 0, data.length);
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_SEND_DATA, type, info);
         if (Ret == 0 && !this.mUsbHandler.hidSend(data, data.length)) {
            Ret = IBioMiniDevice.ErrorCode.ECH_ERR_USB_IO.value();
         }

         return Ret;
      } else {
         return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
      }
   }

   public boolean isOnDestroying() {
      return this.bUninitSet;
   }

   public int GetImageWidth() {
      return this.getTargetWidth();
   }

   public int GetDeviceType() {
      switch(this.GetProductId()) {
      case 1056:
         return 0;
      case 1057:
         return 1;
      default:
         return -1;
      }
   }

   public int GetImageHeight() {
      return this.getTargetHeight();
   }

   public String GetErrorString(int res) {
      return Hid.errString(res);
   }

   public IBioMiniDevice.ErrorCode _captureSingle(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      this.LogD("Capturing single image...");
      byte[] pImage = new byte[153600];
      Arrays.fill(pImage, (byte)-1);
      int nTargetWidth = this.getTargetWidth();
      int nTargetHeight = this.getTargetHeight();
      if (this.bInitialized && this.mUsbHandler != null) {
         if (this.bUSBisdominated) {
            this.LogD("handle busy");
            return IBioMiniDevice.ErrorCode.CTRL_ERR_IS_CAPTURING;
         } else {
            this.bAbortCapturing = false;
            this.bUSBisdominated = true;
            this.mUsbHandler.flagToFix = false;
            int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_CAPTURE, Hid.Sub.CCT_SINGLE);
            if (Ret == 0) {
               Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_RECEIVE_DATA, Hid.Sub.DBI_CAPTURED_IMAGE);
               if (Ret == 0 && !this.mUsbHandler.hidReceive(pImage)) {
                  Ret = IBioMiniDevice.ErrorCode.ECH_ERR_USB_IO.value();
               }
            } else {
               this.LogD("CaptureSingle error : " + Hid.errString(Ret));
            }

            IBioMiniDevice.ErrorCode _ecode = IBioMiniDevice.ErrorCode.fromInt(Ret);
            if (_ecode == IBioMiniDevice.ErrorCode.OK) {
               if (responder != null) {
                  this.onCapture(responder, pImage, this.getTargetWidth(), this.getTargetHeight(), true);
               }
            } else if (responder != null) {
               this.onCaptureError(responder, _ecode.value(), _ecode.toString());
            }

            this.bUSBisdominated = false;
            return _ecode;
         }
      } else {
         this.LogD("no init");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public byte[] ReceiveData(Hid.Sub type) {
      return this.ReceiveData(type, 0, 0);
   }

   public IBioMiniDevice.Parameter getParameter(IBioMiniDevice.ParameterType type) {
      int[] nValue = new int[1];
      this._getParameter(type.name(), nValue);
      this.LogD("getParameter: " + type.name() + " / " + nValue[0]);
      return new IBioMiniDevice.Parameter(type, (long)nValue[0]);
   }

   public boolean setParameter(IBioMiniDevice.Parameter parameter) {
      IBioMiniDevice.ErrorCode e = this._setParameter(parameter.type.name(), (int)parameter.value);
      this.LogD("setParameter : " + parameter.type.name() + "  / " + (int)parameter.value);
      return e == IBioMiniDevice.ErrorCode.OK;
   }

   public IBioMiniDevice.ErrorCode _setParameter(String sName, int nValue) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      } else if (this.bUSBisdominated) {
         return IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_IS_NOT_RUNNING;
      } else {
         String var3 = sName.toLowerCase();
         byte var4 = -1;
         switch(var3.hashCode()) {
         case -1820683195:
            if (var3.equals("wsq_comp_ratio")) {
               var4 = 3;
            }
            break;
         case -1442758754:
            if (var3.equals("image_type")) {
               var4 = 8;
            }
            break;
         case -1313911455:
            if (var3.equals("timeout")) {
               var4 = 0;
            }
            break;
         case -1123232358:
            if (var3.equals("ext_trigger")) {
               var4 = 7;
            }
            break;
         case -384470517:
            if (var3.equals("sleep_mode")) {
               var4 = 9;
            }
            break;
         case -362150459:
            if (var3.equals("security_level")) {
               var4 = 2;
            }
            break;
         case -180013089:
            if (var3.equals("template_type")) {
               var4 = 6;
            }
            break;
         case -175237263:
            if (var3.equals("detect_fake")) {
               var4 = 5;
            }
            break;
         case 564403871:
            if (var3.equals("sensitivity")) {
               var4 = 1;
            }
            break;
         case 1120131375:
            if (var3.equals("lfd_level")) {
               var4 = 4;
            }
         }

         switch(var4) {
         case 0:
            if (nValue < 0) {
               nValue = 0;
            }

            if (nValue > 60000) {
               nValue = 60000;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetTimeout(nValue));
         case 1:
            if (nValue < 0) {
               nValue = 0;
            }

            if (nValue > 7) {
               nValue = 7;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetSensitivity(nValue));
         case 2:
            if (nValue < 0) {
               nValue = 0;
            }

            if (nValue > 7) {
               nValue = 7;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetSecurityLevel(nValue));
         case 3:
            if ((double)nValue < 1.0D) {
               nValue = 1;
            }

            if ((double)nValue > 75.0D) {
               nValue = 75;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetWSQCompRatio(nValue));
         case 4:
         case 5:
            if (nValue < 0) {
               nValue = 0;
            }

            if (nValue > 7) {
               nValue = 7;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetLfdLevel(nValue));
         case 6:
            if (nValue < 2001) {
               nValue = 2001;
            }

            if (nValue > 2003) {
               nValue = 2003;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetTemplateType(nValue));
         case 7:
            boolean bExtOn;
            if (nValue == 0) {
               bExtOn = false;
            } else {
               bExtOn = true;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetExtTrigger(bExtOn));
         case 8:
            if (nValue < 0) {
               nValue = 0;
            }

            if (nValue > 3) {
               nValue = 3;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetImageType(nValue));
         case 9:
            if (nValue != 1 && nValue != 2) {
               return IBioMiniDevice.ErrorCode.ECH_ERR_INVALID_PARAMETER;
            }

            return IBioMiniDevice.ErrorCode.fromInt(this.SetSleepMode(nValue));
         default:
            return IBioMiniDevice.ErrorCode.ECH_ERR_INVALID_PARAMETER;
         }
      }
   }

   public IBioMiniDevice.ErrorCode _getParameter(String sName, int[] nValue) {
      if (!this.bInitialized) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      } else {
         String var3 = sName.toLowerCase();
         byte var4 = -1;
         switch(var3.hashCode()) {
         case -1820683195:
            if (var3.equals("wsq_comp_ratio")) {
               var4 = 3;
            }
            break;
         case -1442758754:
            if (var3.equals("image_type")) {
               var4 = 8;
            }
            break;
         case -1313911455:
            if (var3.equals("timeout")) {
               var4 = 0;
            }
            break;
         case -1123232358:
            if (var3.equals("ext_trigger")) {
               var4 = 7;
            }
            break;
         case -384470517:
            if (var3.equals("sleep_mode")) {
               var4 = 9;
            }
            break;
         case -362150459:
            if (var3.equals("security_level")) {
               var4 = 2;
            }
            break;
         case -180013089:
            if (var3.equals("template_type")) {
               var4 = 6;
            }
            break;
         case -175237263:
            if (var3.equals("detect_fake")) {
               var4 = 5;
            }
            break;
         case 564403871:
            if (var3.equals("sensitivity")) {
               var4 = 1;
            }
            break;
         case 1120131375:
            if (var3.equals("lfd_level")) {
               var4 = 4;
            }
         }

         switch(var4) {
         case 0:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetTimeout(nValue));
         case 1:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetSensitivity(nValue));
         case 2:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetSecurityLevel(nValue));
         case 3:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetWSQCompRatio(nValue));
         case 4:
         case 5:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetLfdLevel(nValue));
         case 6:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetTemplateType(nValue));
         case 7:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetExtTrigger(nValue));
         case 8:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetImageType(nValue));
         case 9:
            return IBioMiniDevice.ErrorCode.fromInt(this.GetSleepMode(nValue));
         default:
            return IBioMiniDevice.ErrorCode.ECH_ERR_INVALID_PARAMETER;
         }
      }
   }

   public boolean verify(byte[] pTemplate1, int nTemplate1Size, byte[] pTemplate2, int nTemplate2Size) {
      int[] nTempalteType = new int[1];
      this.GetTemplateType(nTempalteType);
      Hid.Sub subtype = Hid.Sub.CST_SUPREMA_TEMPLATE;
      if (nTempalteType[0] == 2002) {
         subtype = Hid.Sub.CST_ISO_TEMPLATE;
      } else if (nTempalteType[0] == 2003) {
         subtype = Hid.Sub.CST_ANSI_TEMPLATE;
      }

      int ufa_res = this.SendData(subtype, pTemplate1);
      if (ufa_res != IBioMiniDevice.ErrorCode.OK.value()) {
         this.LogE("data transfer error. - verify." + this.GetErrorString(ufa_res));
         this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(ufa_res);
         return false;
      } else {
         int ret = this.HidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_VERIFY, Hid.Sub.CVM_TRANSFERED_TEMPLATE, (byte[])null);
         if (ret == IBioMiniDevice.ErrorCode.OK.value()) {
            this.LogD("Verify successed");
            return true;
         } else {
            this.LogE("Verify error." + this.GetErrorString(ret));
            this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(ret);
            return false;
         }
      }
   }

   public boolean verify(byte[] pTemplate1, byte[] pTemplate2) {
      return this.verify(pTemplate1, pTemplate1.length, new byte[0], -1);
   }

   public IBioMiniDevice.ErrorCode _startCapturing() {
      this.LogD("Start capturing...");
      if (this.GetProductId() == 1057) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
         return IBioMiniDevice.ErrorCode.ECH_ERR_INVALID_COMMAND;
      } else if (this.IsCapturing()) {
         this.LogD("Cannot start capturing, another capturing is going...");
         return IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_IS_NOT_RUNNING;
      } else if (this.bInitialized && this.mUsbHandler != null) {
         if (this.bUSBisdominated) {
            this.LogD("Handle is busy");
            return IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_IS_NOT_RUNNING;
         } else if (this.bAbortCapturing) {
            this.LogD("Abort Capturing");
            return IBioMiniDevice.ErrorCode.CTRL_ERR_FAIL;
         } else {
            int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_CAPTURE, Hid.Sub.CCT_PREVIEW);
            if (Ret == 0) {
               SystemClock.sleep(100L);
               this.bThreadFlag = true;
               this.bUSBisdominated = true;
               this.bAbortCapturing = false;
               this.mSLoop = new BioMiniHidBase.CapturingHidLoop();
               this.mUsbThread = new Thread(this.mSLoop);
               this.mUsbThread.start();
            } else {
               this.LogD("StartCapturing error : " + Hid.errString(Ret));
            }

            return IBioMiniDevice.ErrorCode.fromInt(Ret);
         }
      } else {
         this.LogD("Not initialized");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public IBioMiniDevice.ErrorCode _autoCapture() {
      this.LogD("Auto Capture start...");
      if (this.IsCapturing()) {
         this.LogD("Cannot auto capture, another capturing is going...");
         return IBioMiniDevice.ErrorCode.ECH_ERR_ABNORMAL_STATE;
      } else if (this.bInitialized && this.mUsbHandler != null) {
         if (this.bUSBisdominated) {
            this.LogD("Handle is busy");
            return IBioMiniDevice.ErrorCode.ECH_ERR_ABNORMAL_STATE;
         } else if (this.bAbortCapturing) {
            this.LogD("Abort Capturing");
            return IBioMiniDevice.ErrorCode.CTRL_ERR_FAIL;
         } else {
            int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_CAPTURE, Hid.Sub.CCT_LOOP);
            if (Ret == 0) {
               SystemClock.sleep(100L);
               this.bThreadFlag = true;
               this.bUSBisdominated = true;
               this.bAbortCapturing = false;
               this.mAutoLoop = new BioMiniHidBase.AutoCaptureHidLoop();
               this.mUsbThread = new Thread(this.mAutoLoop);
               this.mUsbThread.start();
            } else {
               this.LogD("AutoCapture error : " + Hid.errString(Ret));
            }

            return IBioMiniDevice.ErrorCode.fromInt(Ret);
         }
      } else {
         this.LogD("Not initialized");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public boolean IsCapturing() {
      return this.bUSBisdominated;
   }

   private void CaptureFrameStop() {
      this.LogD("Stops capturing...");
      if (this.mUsbThread != null) {
         this.mUsbThread.interrupt();
         this.mUsbThread = null;
      }

      this.mSLoop = null;
      this.mUsbThread = null;
      this.bUSBisdominated = false;
      this.LogD("Capture stopped");
   }

   public IBioMiniDevice.ErrorCode _abortCapturing() {
      int nResult = IBioMiniDevice.ErrorCode.OK.value();
      if (!this.bIsAfterAbortCpaturing) {
         return IBioMiniDevice.ErrorCode.OK;
      } else if (this.bInitialized && this.mUsbHandler != null) {
         if (!this.IsCapturing()) {
            return IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_IS_NOT_RUNNING;
         } else if (this.bAbortCapturing) {
            return IBioMiniDevice.ErrorCode.CTRL_ERR_FAIL;
         } else {
            this.bAbortCapturing = true;
            this.bIsAfterAbortCpaturing = true;
            this.mUsbHandler.flagToFix = true;
            SystemClock.sleep(300L);
            nResult = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_CAPTURE, Hid.Sub.CCT_STOP);
            if (nResult != IBioMiniDevice.ErrorCode.OK.value()) {
               this.LogD("AbortCaptuging error : " + Hid.errString(nResult));
            }

            if (this.mUsbThread != null) {
               this.LogD("interrupt");
               this.mUsbThread.interrupt();
               this.mUsbThread = null;
            }

            return IBioMiniDevice.ErrorCode.fromInt(nResult);
         }
      } else {
         this.LogD("no init");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public int ClearCaptureImageBuffer() {
      if (!this.bInitialized) {
         this.LogD("no init");
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         Arrays.fill(this.m_ImageLast, 0, this.getTargetWidth() * this.getTargetHeight(), (byte)-1);
         this.isCaptured = false;
         this.LogD("set isCaptured false(ClearCaptureImageBuffer)");
         return IBioMiniDevice.ErrorCode.OK.value();
      }
   }

   public String GetVersionString() {
      return "BioMini HID SDK for Android v1.0.009";
   }

   public IBioMiniDevice.ErrorCode SaveBufferInPictureDirectory(byte[] outputBuf, String fileName, boolean bOverWrite) {
      int ufa_res = IBioMiniDevice.ErrorCode.OK.value();
      if (outputBuf == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_INVALID_PARAMETER;
      } else {
         File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
         if (!f.exists() && !f.mkdir()) {
            return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL;
         } else {
            String fileNameNew = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + fileName;
            f = new File(fileNameNew);
            if (!f.exists()) {
               try {
                  if (!f.createNewFile()) {
                     return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL;
                  }
               } catch (IOException var11) {
                  var11.printStackTrace();
                  return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL;
               }
            } else {
               if (!bOverWrite) {
                  return IBioMiniDevice.ErrorCode.ECH_ERR_PERMISSION_DENIED;
               }

               try {
                  if (!f.delete() || !f.createNewFile()) {
                     return IBioMiniDevice.ErrorCode.ECH_ERR_PERMISSION_DENIED;
                  }
               } catch (IOException var12) {
                  var12.printStackTrace();
               }
            }

            FileOutputStream os = null;

            try {
               os = new FileOutputStream(f);
               os.write(outputBuf);
               os.flush();
               os.close();
            } catch (FileNotFoundException var9) {
               var9.printStackTrace();
               return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL;
            } catch (Exception var10) {
               return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL;
            }

            return IBioMiniDevice.ErrorCode.fromInt(ufa_res);
         }
      }
   }

   private int SetTimeout(int nTimeout) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         Hid.putShort(info, 0, nTimeout);
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_TIMEOUT, info);
      }
   }

   private int SetLfdLevel(int nLevel) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         info[0] = (byte)nLevel;
         int resuslt = this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_LFD_LEVEL, info);
         return resuslt;
      }
   }

   private int SetSecurityLevel(int nLevel) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         info[0] = (byte)nLevel;
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_VERIFIY_OPT, Hid.Sub.VVT_SECURITY_LEVEL, info);
      }
   }

   private int SetSensitivity(int nSensitivity) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         info[0] = (byte)nSensitivity;
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_SENSITIVITY, info);
      }
   }

   private int SetWSQCompRatio(int nCompRatio) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = Arrays.copyOf(Float.toString((float)nCompRatio / 10.0F).getBytes(Charset.forName("UTF-8")), 8);
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_IMAGE_OPT, Hid.Sub.IVT_COMPRESS_RATIO, info);
      }
   }

   private int SetTemplateType(int nTemplateType) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         Hid.putShort(info, 0, nTemplateType);
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_TEMPLATE_OPT, Hid.Sub.TVT_TEMPLATE_FORMAT, info);
      }
   }

   private int SetImageType(int nImageType) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         info[0] = (byte)nImageType;
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_IMAGE_OPT, Hid.Sub.IVT_IMAGE_FORMAT, info);
      }
   }

   private int SetExtTrigger(boolean bOn) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         if (bOn) {
            info[0] = 1;
         } else {
            info[0] = 0;
         }

         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_SET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_EX_TRIGGER, info);
      }
   }

   private int SetSleepMode(int nMode) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         byte[] info = new byte[64];
         Arrays.fill(info, 0, 64, (byte)0);
         info[0] = (byte)nMode;
         return this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_DEVICE_CTRL, Hid.Sub.CDT_SET_SLEEP_MODE, info);
      }
   }

   private int GetTimeout(int[] nTimeout) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_TIMEOUT);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 2);
            nTimeout[0] = Hid.retrieveShort(temp, 0);
         }

         return Ret;
      }
   }

   private int GetLfdLevel(int[] nLevel) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_LFD_LEVEL);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            nLevel[0] = temp[0];
         }

         return Ret;
      }
   }

   private int GetSecurityLevel(int[] nLevel) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_VERIFIY_OPT, Hid.Sub.VVT_SECURITY_LEVEL);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            nLevel[0] = temp[0];
         }

         return Ret;
      }
   }

   private int GetSensitivity(int[] nSensitivity) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_SENSITIVITY);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            nSensitivity[0] = temp[0];
         }

         return Ret;
      }
   }

   private int GetWSQCompRatio(int[] nCompRatio) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_IMAGE_OPT, Hid.Sub.IVT_COMPRESS_RATIO);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 8);

            try {
               String out_str = (new String(temp, "US-ASCII")).replaceAll("[^0-9.]", "");
               nCompRatio[0] = (int)(Float.parseFloat(out_str) * 10.0F);
               this.LogD("nCompRatio[0] : " + nCompRatio[0]);
            } catch (Exception var5) {
               return IBioMiniDevice.ErrorCode.ECH_ERR_GENERAL.value();
            }
         }

         return Ret;
      }
   }

   private int GetTemplateType(int[] nTemplateType) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_TEMPLATE_OPT, Hid.Sub.TVT_TEMPLATE_FORMAT);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 2);
            nTemplateType[0] = Hid.retrieveShort(temp, 0);
         }

         return Ret;
      }
   }

   private int GetImageType(int[] nImageType) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_IMAGE_OPT, Hid.Sub.IVT_IMAGE_FORMAT);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            nImageType[0] = temp[0];
         }

         return Ret;
      }
   }

   private int GetExtTrigger(int[] bOn) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_CAPTURE_OPT, Hid.Sub.CVT_EX_TRIGGER);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            bOn[0] = temp[0];
         }

         return Ret;
      }
   }

   private int GetSleepMode(int[] nMode) {
      if (this.mUsbHandler == null) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED.value();
      } else {
         int Ret = this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_DEVICE_CTRL, Hid.Sub.CDT_GET_SLEEP_MODE);
         if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
            byte[] temp = new byte[8];
            System.arraycopy(this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
            nMode[0] = temp[0];
         }

         return Ret;
      }
   }

   public IBioMiniDevice.ErrorCode GetTemplateQuality(int[] nQuality) {
      if (this.bInitialized && this.mUsbHandler != null) {
         byte[] temp = new byte[8];
         System.arraycopy(this.mUsbHandler.getLastEcho(), 4, temp, 0, 1);
         nQuality[0] = temp[0];
         return IBioMiniDevice.ErrorCode.OK;
      } else {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public int Setting(int pid) {
      this.LogD("Setting... PID(" + String.format(Locale.ENGLISH, "0x%02x", pid) + " )");
      if (pid != 1056 && pid != 1057) {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NO_DEVICE_FOUND.value();
      } else {
         this.mDeviceInfo.versionSDK = this.BASE_VERSION;
         this.mDeviceInfo.deviceSN = this.GetDeviceSN();
         this.mDeviceInfo.deviceName = this.GetDeviceName();
         if (pid == 1056) {
            this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI_SLIMS;
         } else if (pid == 1057) {
            this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI_SLIM2S;
         }

         return IBioMiniDevice.ErrorCode.OK.value();
      }
   }

   public int getMaxBulkSize() {
      return 0;
   }

   void ImageLogD(String filename, byte[] image, int width, int height) {
   }

   void LogD(Object msg) {
      Logger.LogD("BioMini SDK", msg);
   }

   void LogE(Object msg) {
      Logger.LogE("BioMini SDK", msg);
   }

   void LogV(Object msg) {
      Logger.LogV("BioMini SDK", msg);
   }

   void LogI(Object msg) {
      Logger.LogI("BioMini SDK", msg);
   }

   void LogW(Object msg) {
      Logger.LogW("BioMini SDK", msg);
   }

   boolean onCapture(ICaptureResponder responder, byte[] imageData, int width, int height, boolean isFingerOn) {
      if (responder == null) {
         return false;
      } else {
         if (this.m_bImageFlip) {
            byte[] _tmp = Arrays.copyOf(imageData, imageData.length);

            for(int i = 0; i < height; ++i) {
               System.arraycopy(_tmp, i * width, imageData, (height - 1 - i) * width, width);
            }
         }

         IBioMiniDevice.FingerState fingerState = new IBioMiniDevice.FingerState(isFingerOn);
         Bitmap capturedImage = null;
         IBioMiniDevice.TemplateData capturedTemplate = null;
         this.LogV("" + this.mCurrentCaptureOption);
         this.LogV("captureImage = " + this.mCurrentCaptureOption.captureImage);
         this.LogV("captureTemplate = " + this.mCurrentCaptureOption.extractParam.captureTemplate);
         if (this.mCurrentCaptureOption != null && this.mCurrentCaptureOption.captureImage) {
            capturedImage = util.toBitmap(imageData, width, height);
         }

         if (this.mCurrentCaptureOption != null && (this.mCurrentCaptureOption.extractParam.captureTemplate || this.mCurrentCaptureOption.captureTemplate)) {
            capturedTemplate = this.extractTemplate();
         }

         if (!responder.onCaptureEx(this, capturedImage, capturedTemplate, fingerState)) {
            responder.onCapture(this, fingerState);
         }

         return true;
      }
   }

   boolean onCaptureError(ICaptureResponder responder, int error, String errorString) {
      if (responder != null) {
         responder.onCaptureError(this, error, errorString);
         return true;
      } else {
         return false;
      }
   }

   public IBioMiniDevice.ErrorCode getLastError() {
      return IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
   }

   public boolean isCapturing() {
      return this.IsCapturing();
   }

   public int abortCapturing() {
      return this._abortCapturing().value();
   }

   public int startCapturing(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      this.mCurrentCaptureOption = opt;
      this.mCapturerResponder = responder;
      this._startCapturing();
      return 0;
   }

   public boolean captureSingle(final IBioMiniDevice.CaptureOption opt, final ICaptureResponder responder, boolean bAsync) {
      this.mCurrentCaptureOption = opt;
      this.mCapturerResponder = responder;
      if (bAsync) {
         Runnable captureObj = new Runnable() {
            public void run() {
               BioMiniHidBase.this._captureSingle(opt, responder);
            }
         };
         (new Thread(captureObj)).start();
         return true;
      } else {
         IBioMiniDevice.ErrorCode eCode = this._captureSingle(opt, responder);
         return eCode == IBioMiniDevice.ErrorCode.OK;
      }
   }

   public IBioMiniDevice.TemplateData extractTemplate() {
      int ufa_res = this.HidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_EXTRACT, Hid.Sub.DBI_CAPTURED_TEMPLATE, (byte[])null);
      if (ufa_res != IBioMiniDevice.ErrorCode.OK.value()) {
         this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(ufa_res);
         return null;
      } else {
         byte[] _template = this.ReceiveData(Hid.Sub.DBI_CAPTURED_TEMPLATE);
         if (_template != null) {
            byte[] _echo = this.GetHidEchoData();
            if (_echo[0] == IBioMiniDevice.ErrorCode.OK.value()) {
               int[] pnQ = new int[1];
               int[] pnTemplateType = new int[1];
               this.GetTemplateQuality(pnQ);
               this._getParameter("template_type", pnTemplateType);
               IBioMiniDevice.TemplateData _data = new IBioMiniDevice.TemplateData(_template, IBioMiniDevice.TemplateType.fromInt(pnTemplateType[0]), pnQ[0]);
               return _data;
            }
         }

         this.m_LastError = IBioMiniDevice.ErrorCode.CTRL_ERR_EXTRACTION_FAILED;
         return null;
      }
   }

   public byte[] getCaptureImageAs19794_4() {
      this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
      return new byte[0];
   }

   public byte[] getCaptureImageAsWsq(int width, int height, float fBitRate, int rotate) {
      this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
      return new byte[0];
   }

   public byte[] getCaptureImageAsBmp() {
      this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
      return new byte[0];
   }

   public byte[] getCaptureImageAsRAW_8() {
      this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
      return new byte[0];
   }

   public IBioMiniDevice.ErrorCode setPacketMode(IBioMiniDevice.PacketMode packetMode) {
      if (this.mUsbHandler != null) {
         int res = this.mUsbHandler.setUSBPacketMode(packetMode.value());
         if (res == 0) {
            return IBioMiniDevice.ErrorCode.OK;
         } else if (res == IBioMiniDevice.ErrorCode.CTRL_ERR_NEED_REBOOT.value()) {
            Log.d("BioMini SDK", "===== Device is goint to Reboot.");
            res = this.mUsbHandler.resetDevice();
            return res == 0 ? IBioMiniDevice.ErrorCode.OK : IBioMiniDevice.ErrorCode.CTRL_ERR_FAIL;
         } else {
            return IBioMiniDevice.ErrorCode.CTRL_ERR_FAIL;
         }
      } else {
         return IBioMiniDevice.ErrorCode.ECH_ERR_NOT_INITIALIZED;
      }
   }

   public boolean canChangePacketMode() {
      return true;
   }

   private class AutoCaptureHidLoop implements Runnable {
      private AutoCaptureHidLoop() {
      }

      public void run() {
         BioMiniHidBase.this.LogD("AutoCaptureHidLoop Init ");
         int nTargetWidth = BioMiniHidBase.this.getTargetWidth();
         int nTargetHeight = BioMiniHidBase.this.getTargetHeight();

         while(BioMiniHidBase.this.bThreadFlag && !BioMiniHidBase.this.bAbortCapturing) {
            int Ret = BioMiniHidBase.this.mUsbHandler.hidCommand(Hid.Pac.PAC_GET, Hid.Cmd.VAT_IS_STREAM_UPDATE, Hid.Sub.SUB_NA);

            try {
               Thread.sleep(50L);
            } catch (InterruptedException var5) {
               var5.printStackTrace();
            }

            if (Ret == IBioMiniDevice.ErrorCode.OK.value()) {
               byte[] temp = new byte[8];
               System.arraycopy(BioMiniHidBase.this.mUsbHandler.getLastEchoData(), 0, temp, 0, 1);
               if (temp[0] == 1) {
                  Ret = BioMiniHidBase.this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_RECEIVE_DATA, Hid.Sub.DBI_CAPTURED_IMAGE);
                  if (Ret == 0) {
                     if (BioMiniHidBase.this.mUsbHandler.hidReceive(BioMiniHidBase.this.m_ImageLast)) {
                        if (BioMiniHidBase.this.mCapturerResponder != null) {
                           BioMiniHidBase.this.onCapture(BioMiniHidBase.this.mCapturerResponder, BioMiniHidBase.this.m_ImageLast, nTargetWidth, nTargetHeight, true);
                        }
                     } else {
                        boolean var6 = true;
                     }
                  } else {
                     BioMiniHidBase.this.LogD("Receiving last captured data error : " + Hid.errString(Ret));
                  }
               }
            }
         }

         BioMiniHidBase.this.bThreadFlag = false;
         BioMiniHidBase.this.bAbortCapturing = false;
         BioMiniHidBase.this.LogD("CapturingHidLoop end ");
         BioMiniHidBase.this.CaptureFrameStop();
      }

      // $FF: synthetic method
      AutoCaptureHidLoop(Object x1) {
         this();
      }
   }

   private class CapturingHidLoop implements Runnable {
      private CapturingHidLoop() {
      }

      public void run() {
         BioMiniHidBase.this.LogD("CapturingHidLoop Init ");
         int nTargetWidth = BioMiniHidBase.this.getTargetWidth();
         int nTargetHeight = BioMiniHidBase.this.getTargetHeight();

         while(BioMiniHidBase.this.bThreadFlag && !BioMiniHidBase.this.bAbortCapturing) {
            BioMiniHidBase.this.isCaptured = false;
            int Ret = BioMiniHidBase.this.mUsbHandler.hidCommand(Hid.Pac.PAC_CMD, Hid.Cmd.CMT_RECEIVE_DATA, Hid.Sub.DBI_PREVIEW_IMAGE);
            if (Ret == 0) {
               if (!BioMiniHidBase.this.mUsbHandler.hidReceive(BioMiniHidBase.this.m_ImageLast)) {
                  Ret = -1;
               }
            } else {
               BioMiniHidBase.this.LogD("Receiving preview data error : " + Hid.errString(Ret));
            }

            if (Ret == IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_IS_NOT_RUNNING.value()) {
               BioMiniHidBase.this.LogD("Receiving captured image data error : " + Hid.errString(Ret));
               if (BioMiniHidBase.this.mCapturerResponder != null) {
                  BioMiniHidBase.this.mCapturerResponder.onCaptureError(BioMiniHidBase.this.mApplicationContext, Ret, "StartCapturing failed : Capture is not running");
               }
               break;
            }

            if (Ret == IBioMiniDevice.ErrorCode.CTRL_ERR_CAPTURE_TIMEOUT.value()) {
               BioMiniHidBase.this.LogD("Receiving captured image data error : " + Hid.errString(Ret));
               if (BioMiniHidBase.this.mCapturerResponder != null) {
                  BioMiniHidBase.this.mCapturerResponder.onCaptureError(BioMiniHidBase.this.mApplicationContext, Ret, "StartCapturing failed : Capture timeout");
               }
               break;
            }

            if (Ret == IBioMiniDevice.ErrorCode.CTRL_ERR_FAKE_FINGER.value()) {
               BioMiniHidBase.this.LogD("Receiving captured image data error : " + Hid.errString(Ret));
               if (BioMiniHidBase.this.mCapturerResponder != null) {
                  BioMiniHidBase.this.mCapturerResponder.onCaptureError(BioMiniHidBase.this.mApplicationContext, Ret, "StartCapturing failed : Fake finger");
               }
               break;
            }

            if (BioMiniHidBase.this.mCapturerResponder != null) {
               BioMiniHidBase.this.onCapture(BioMiniHidBase.this.mCapturerResponder, BioMiniHidBase.this.m_ImageLast, nTargetWidth, nTargetHeight, true);
            }
         }

         BioMiniHidBase.this.bThreadFlag = false;
         BioMiniHidBase.this.bAbortCapturing = false;
         BioMiniHidBase.this.LogD("CapturingHidLoop end ");
         BioMiniHidBase.this.CaptureFrameStop();
      }

      // $FF: synthetic method
      CapturingHidLoop(Object x1) {
         this();
      }
   }

   private class PermissionReceiver extends BroadcastReceiver {
      private final BioMiniHidBase.IPermissionListener mPermissionListener;

      public PermissionReceiver(BioMiniHidBase.IPermissionListener permissionListener) {
         this.mPermissionListener = permissionListener;
      }

      public void onReceive(Context context, Intent intent) {
         BioMiniHidBase.this.LogD("onReceive");
         if (BioMiniHidBase.this.mApplicationContext != null) {
            BioMiniHidBase.this.mApplicationContext.unregisterReceiver(this);
            if (intent.getAction().equals("com.supremahq.samples.USB_PERMISSION")) {
               if (!intent.getBooleanExtra("permission", false)) {
                  this.mPermissionListener.onPermissionDenied((UsbDevice)intent.getParcelableExtra("device"));
               } else {
                  BioMiniHidBase.this.LogD("Permission granted");
                  UsbDevice dev = (UsbDevice)intent.getParcelableExtra("device");
                  BioMiniHidBase.this.LogD("device not present!");
               }
            }
         }

      }
   }

   private interface IPermissionListener {
      void onPermissionDenied(UsbDevice var1);
   }
}
