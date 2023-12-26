package com.suprema;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;
import com.suprema.android.BioMiniJni;
import com.suprema.devices.BioMiniOC4;
import com.suprema.devices.BioMiniPlus2;
import com.suprema.devices.BioMiniSlim;
import com.suprema.devices.BioMiniSlim2;
import com.suprema.hid.BioMiniHid;
import com.suprema.util.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public abstract class BioMiniFactory implements IUsbEventHandler {
   private final String TAG = "BioMiniSDK";
   private int mDeviceLast = -1;
   private boolean mUseInternalPermissionControl = true;
   private UsbManager mUsbManager;
   private final Context mApplicationContext;
   private static final String ACTION_USB_PERMISSION = "com.suprema.USB_PERMISSION";
   private boolean mOldUsbIsdominated = false;
   private BioMiniFactory.SDKInfo mSDKInfo = new BioMiniFactory.SDKInfo();
   private IBioMiniDevice.TransferMode m_transfer_mode;
   private List<BioMiniFactory.BioMiniDeviceEnum> mActiveList;
   private List<BioMiniFactory.BioMiniDeviceEnum> mAvailableList;
   private BioMiniFactory.PermissionReceiver mPermissionReceiver;
   private BroadcastReceiver mUsbAttachReceiver;
   private BroadcastReceiver mUsbDetachReceiver;

   void LogD(Object msg) {
      Logger.LogD("BioMiniSDK", msg);
   }

   void LogE(Object msg) {
      Logger.LogE("BioMiniSDK", msg);
   }

   public BioMiniFactory(Context appContext) {
      this.m_transfer_mode = IBioMiniDevice.TransferMode.MODE1;
      this.mActiveList = new ArrayList();
      this.mAvailableList = new ArrayList();
      this.mPermissionReceiver = new BioMiniFactory.PermissionReceiver(new BioMiniFactory.IPermissionListener() {
         public void onPermissionDenied(UsbDevice d) {
            Log.e("BioMiniSDK", "Permission denied on " + d.getDeviceId());
         }
      });
      this.mUsbAttachReceiver = new BroadcastReceiver() {
         public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action) && BioMiniFactory.this.mUseInternalPermissionControl) {
               BioMiniFactory.this.initPermissionListener();
            }

         }
      };
      this.mUsbDetachReceiver = new BroadcastReceiver() {
         public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
               UsbDevice device = (UsbDevice)intent.getParcelableExtra("device");
               BioMiniFactory.this.removeDevice(device);
            }

         }
      };
      this.LogD("BioMiniFactory created: " + appContext);
      this.mApplicationContext = appContext;
      this.mUsbManager = (UsbManager)this.mApplicationContext.getSystemService("usb");
      this.mUseInternalPermissionControl = true;
      IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
      this.mApplicationContext.registerReceiver(this.mUsbAttachReceiver, filter);
      filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED");
      this.mApplicationContext.registerReceiver(this.mUsbDetachReceiver, filter);

      try {
         this.initPermissionListener();
      } catch (Exception var4) {
         Log.e("BioMiniSDK", "ERROR: BioMiniFactory init caught an exception : " + var4);
      }

      Log.i("BioMiniSDK", "Version : " + this.mSDKInfo.toString());
   }

   public BioMiniFactory(Context appContext, UsbManager usbManager) {
      this.m_transfer_mode = IBioMiniDevice.TransferMode.MODE1;
      this.mActiveList = new ArrayList();
      this.mAvailableList = new ArrayList();
      this.mPermissionReceiver = new BioMiniFactory.PermissionReceiver(new BioMiniFactory.IPermissionListener() {
         public void onPermissionDenied(UsbDevice d) {
            Log.e("BioMiniSDK", "Permission denied on " + d.getDeviceId());
         }
      });
      this.mUsbAttachReceiver = new BroadcastReceiver() {
         public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action) && BioMiniFactory.this.mUseInternalPermissionControl) {
               BioMiniFactory.this.initPermissionListener();
            }

         }
      };
      this.mUsbDetachReceiver = new BroadcastReceiver() {
         public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
               UsbDevice device = (UsbDevice)intent.getParcelableExtra("device");
               BioMiniFactory.this.removeDevice(device);
            }

         }
      };
      this.LogD("BioMiniFactory created: " + appContext + ", " + usbManager);
      this.mApplicationContext = appContext;
      this.mUsbManager = usbManager;
      this.mUseInternalPermissionControl = false;
      IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
      this.mApplicationContext.registerReceiver(this.mUsbAttachReceiver, filter);
      filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED");
      this.mApplicationContext.registerReceiver(this.mUsbDetachReceiver, filter);
   }

   public void setTransferMode(IBioMiniDevice.TransferMode _transfer_mode) {
      Log.i("BioMiniSDK", "Transfer_mode(TransferMode) : " + _transfer_mode.toString());
      this.m_transfer_mode = _transfer_mode;
   }

   public void addDevice(UsbDevice device) {
      Iterator var2 = this.mAvailableList.iterator();

      BioMiniFactory.BioMiniDeviceEnum d;
      label26:
      do {
         while(var2.hasNext()) {
            d = (BioMiniFactory.BioMiniDeviceEnum)var2.next();
            if (d != null && d.usbDevice() != null && d.getDevice() != null) {
               continue label26;
            }

            if (d != null) {
               this.mAvailableList.remove(d);
            }
         }

         this.mAvailableList.add(0, new BioMiniFactory.BioMiniDeviceEnum(device));
         this.onDeviceChange(IUsbEventHandler.DeviceChangeEvent.DEVICE_ATTACHED, device);
         this.LogD("Device added : " + device);
         return;
      } while(!d.usbDevice().getDeviceName().equals(device.getDeviceName()));

      this.LogD("Already have a duplicate");
   }

   private void removeDevice(UsbDevice device) {
      if (device != null) {
         Iterator var2 = this.mAvailableList.iterator();

         while(true) {
            while(var2.hasNext()) {
               BioMiniFactory.BioMiniDeviceEnum d = (BioMiniFactory.BioMiniDeviceEnum)var2.next();
               if (d != null && d.usbDevice() != null && d.getDevice() != null) {
                  if (d.usbDevice().getDeviceName().equals(device.getDeviceName())) {
                     this.mAvailableList.remove(d);
                     this.LogD("" + this.mAvailableList);
                     this.onDeviceChange(IUsbEventHandler.DeviceChangeEvent.DEVICE_DETACHED, d.usbDevice());
                     d.getDevice().deactivate(IUsbEventHandler.DisconnectionCause.USB_UNPLUGGED);
                  }
               } else if (d != null) {
                  this.mAvailableList.remove(d);
               }
            }

            return;
         }
      }
   }

   public void close() {
      if (this.mApplicationContext != null) {
         this.mApplicationContext.unregisterReceiver(this.mUsbAttachReceiver);
         this.mApplicationContext.unregisterReceiver(this.mUsbDetachReceiver);
      }

      BioMiniJni.uninit();
   }

   private boolean isSupportedDevice(UsbDevice dev) {
      int vid = dev.getVendorId();
      int pid = dev.getProductId();
      return vid == 5841 && (pid == 1031 || pid == 1030 || pid == 1033 || pid == 1032 || pid == 1056 || pid == 1057);
   }

   private void enumerate(BioMiniFactory.IPermissionListener listener) {
      if (this.mUsbManager != null) {
         HashMap<String, UsbDevice> devlist = this.mUsbManager.getDeviceList();
         Iterator var3 = devlist.values().iterator();

         while(var3.hasNext()) {
            UsbDevice d = (UsbDevice)var3.next();
            if (this.isSupportedDevice(d)) {
               if (!this.mUsbManager.hasPermission(d)) {
                  Log.d("BioMiniSDK", "No permission permitted. Trying to get permission...");
                  listener.onPermissionDenied(d);
                  return;
               }

               this.addDevice(d);
               return;
            }
         }
      }

   }

   private synchronized boolean initPermissionListener() {
      this.enumerate(new BioMiniFactory.IPermissionListener() {
         public void onPermissionDenied(UsbDevice d) {
            if (BioMiniFactory.this.mApplicationContext != null && BioMiniFactory.this.mUsbManager != null) {
               PendingIntent pi = PendingIntent.getBroadcast(BioMiniFactory.this.mApplicationContext, 0, new Intent("com.suprema.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
               BioMiniFactory.this.mApplicationContext.registerReceiver(BioMiniFactory.this.mPermissionReceiver, new IntentFilter("com.suprema.USB_PERMISSION"));
               BioMiniFactory.this.mUsbManager.requestPermission(d, pi);
               Log.d("=== BioMini Factory ===", "requestPermission");
            }

         }
      });
      return true;
   }

   public int getDeviceCount() {
      return this.mAvailableList.size();
   }

   public IBioMiniDevice getDevice(int i) {
      try {
         if (this.mAvailableList.size() >= i) {
            BioMiniFactory.BioMiniDeviceEnum dev = (BioMiniFactory.BioMiniDeviceEnum)this.mAvailableList.get(i);
            IBioMiniDevice deviceHandle = null;
            if (dev != null) {
               deviceHandle = dev.getDevice();
               if (deviceHandle != null) {
                  return deviceHandle;
               }
            }

            Log.e("BioMiniSDK", "CRITICAL ERROR : device is not available");
            return null;
         }
      } catch (Exception var4) {
         Log.e("BioMiniSDK", "CRITICAL ERROR while BioMiniDeviceEnum.getDevice...");
         Log.e("BioMiniSDK", "" + var4);
      }

      return null;
   }

   public BioMiniFactory.SDKInfo getSDKInfo() {
      return this.mSDKInfo;
   }

   private class PermissionReceiver extends BroadcastReceiver {
      private final BioMiniFactory.IPermissionListener mPermissionListener;

      public PermissionReceiver(BioMiniFactory.IPermissionListener permissionListener) {
         this.mPermissionListener = permissionListener;
      }

      public void onReceive(Context context, Intent intent) {
         if (BioMiniFactory.this.mApplicationContext != null) {
            BioMiniFactory.this.mApplicationContext.unregisterReceiver(this);
            String action = intent.getAction();
            byte var5 = -1;
            switch(action.hashCode()) {
            case -676995248:
               if (action.equals("com.suprema.USB_PERMISSION")) {
                  var5 = 0;
               }
            default:
               switch(var5) {
               case 0:
                  UsbDevice dev;
                  if (!intent.getBooleanExtra("permission", false)) {
                     dev = (UsbDevice)intent.getParcelableExtra("device");
                     this.mPermissionListener.onPermissionDenied(dev);
                     BioMiniFactory.this.onDeviceChange(IUsbEventHandler.DeviceChangeEvent.DEVICE_PERMISSION_DENIED, dev);
                  } else {
                     BioMiniFactory.this.LogD("Permission granted");
                     dev = (UsbDevice)intent.getParcelableExtra("device");
                     if (dev != null) {
                        BioMiniFactory.this.addDevice(dev);
                     } else {
                        Log.e("BioMiniSDK", "BioMini device not present!");
                     }
                  }
               }
            }
         }

      }
   }

   private interface IPermissionListener {
      void onPermissionDenied(UsbDevice var1);
   }

   private class BioMiniDeviceEnum {
      private int mDeviceID;
      private UsbDevice mUsbDevice = null;
      ABioMiniDevice mBioMiniDevice = null;

      BioMiniDeviceEnum(UsbDevice dev) {
         this.mUsbDevice = dev;
         this.mDeviceID = ++BioMiniFactory.this.mDeviceLast;
      }

      UsbDevice usbDevice() {
         return this.mUsbDevice;
      }

      public int deviceID() {
         return this.mDeviceID;
      }

      ABioMiniDevice createDevice() {
         if (this.mBioMiniDevice == null && this.mUsbDevice != null) {
            int nRetrycount = 0;
            switch(this.mUsbDevice.getProductId()) {
            case 1030:
               BioMiniFactory.this.LogD("createDevice 0x0406");
               this.mBioMiniDevice = new BioMiniOC4();
               break;
            case 1031:
               BioMiniFactory.this.LogD("mOldUsbIsdominated = " + BioMiniFactory.this.mOldUsbIsdominated + "nRetrycount = " + nRetrycount);

               while(BioMiniFactory.this.mOldUsbIsdominated && nRetrycount < 100) {
                  Log.d("BioMiniSDK", "waiting for dominate usb handler(0x0407) nRetrycount " + nRetrycount);
                  SystemClock.sleep(50L);
                  ++nRetrycount;
               }

               BioMiniFactory.this.LogD("createDevice 0x0407");
               this.mBioMiniDevice = new BioMiniSlim(new IUsbStatusChangeListener() {
                  public void onStatusChangeListener(boolean bUSBisdominated) {
                     BioMiniFactory.this.mOldUsbIsdominated = bUSBisdominated;
                     BioMiniFactory.this.LogD("mOldUsbIsdominated = bUSBisdominated (Activate) =" + BioMiniFactory.this.mOldUsbIsdominated);
                  }
               });
               break;
            case 1032:
               BioMiniFactory.this.LogD("createDevice 0x0408");
               this.mBioMiniDevice = new BioMiniSlim2();
               break;
            case 1033:
               BioMiniFactory.this.LogD("createDevice 0x0409");
               this.mBioMiniDevice = new BioMiniPlus2();
               break;
            case 1056:
            case 1057:
               BioMiniFactory.this.LogD("createDevice PID=" + String.format(Locale.ENGLISH, "0x%04x", this.mUsbDevice.getProductId()));
               this.mBioMiniDevice = new BioMiniHid(BioMiniFactory.this.mUsbManager);
               break;
            default:
               BioMiniFactory.this.LogD("createDevice failed PID=" + this.mUsbDevice.getProductId());
               return null;
            }

            if (!this.mBioMiniDevice.activate(BioMiniFactory.this.mUsbManager, this.mUsbDevice, BioMiniFactory.this.m_transfer_mode == IBioMiniDevice.TransferMode.MODE2)) {
               this.mBioMiniDevice = null;
               BioMiniFactory.this.LogE("createDevice failed while device activation");
               return null;
            } else {
               BioMiniFactory.this.LogD("createDevice " + this.mBioMiniDevice);
               return this.mBioMiniDevice;
            }
         } else if (!this.mBioMiniDevice.isInUse()) {
            BioMiniFactory.this.LogD("createDevice reusing old instance");
            this.mBioMiniDevice.activate();
            return this.mBioMiniDevice;
         } else {
            BioMiniFactory.this.LogE("mBioMiniDevice = " + this.mBioMiniDevice + ", mUsbDevice = " + this.mUsbDevice);
            return null;
         }
      }

      ABioMiniDevice getDevice() {
         if (this.mBioMiniDevice == null) {
            return this.createDevice();
         } else {
            BioMiniFactory.this.LogD("BioMiniDeviceEnum.getDevice " + this.mBioMiniDevice);
            return this.mBioMiniDevice;
         }
      }
   }

   class SDKInfo {
      public int Major = 2;
      public int Mid = 0;
      public int Minor = 1;
      public int Rev = 2424;

      public String toString() {
         return String.format(Locale.ENGLISH, "BioMini SDK for Android " + this.Major + "." + this.Mid + "." + this.Minor + ".r%04d%s", this.Rev, "");
      }
   }
}
