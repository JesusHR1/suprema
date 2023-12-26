package com.suprema;

public abstract class ABioMiniDevice implements IBioMiniDevice {
   protected final int[] MATCHING_THRESHOLD = new int[]{65, 115, 200, 325, 465, 630, 820};
   protected final int[] MATCHING_THRESHOLD_SIF = new int[]{65, 115, 195, 300, 425, 570, 715};
   protected int m_SecurityLevel = 4;
   protected int m_AutoRotate;
   protected int m_FastMode = 1;
   protected int m_bDetectCore = 0;
   protected boolean mEnableEcryption = false;
   protected IBioMiniDevice.TemplateType m_TemplateType;
   public IBioMiniDevice.DeviceInfo mDeviceInfo = new IBioMiniDevice.DeviceInfo();
   public String BASE_VERSION = "v2.0";
   public IBioMiniDevice.ErrorCode m_LastError;
   public IBioMiniDevice.CaptureOption mCurrentCaptureOption;

   public ABioMiniDevice() {
      this.m_LastError = IBioMiniDevice.ErrorCode.OK;
   }

   public abstract boolean activate(Object var1, Object var2, Object var3);

   public abstract boolean activate(Object var1, Object var2);

   public abstract boolean activate();

   public abstract boolean deactivate(IUsbEventHandler.DisconnectionCause var1);

   public abstract boolean isInUse();

   public abstract String errString(int var1);

   public abstract void captured();

   public abstract boolean isAwake();

   public abstract boolean wakeUp();

   public abstract boolean hibernate();

   public IBioMiniDevice.DeviceInfo getDeviceInfo() {
      return this.mDeviceInfo;
   }
}
