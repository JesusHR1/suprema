package com.suprema;

public interface IUsbEventHandler {
   void onDeviceChange(IUsbEventHandler.DeviceChangeEvent var1, Object var2);

   public static enum DisconnectionCause {
      USB_UNPLUGGED,
      SLEEP_MODE,
      DEACTIVATED;
   }

   public static enum DeviceChangeEvent {
      DEVICE_ATTACHED,
      DEVICE_DETACHED,
      DEVICE_PERMISSION_DENIED;
   }
}
