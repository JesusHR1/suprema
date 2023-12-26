package com.suprema.hid;

import android.hardware.usb.UsbManager;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;
import com.suprema.util.IBridgeCallback;

public class BioMiniHid extends BioMiniHidBase {
   public BioMiniHid(UsbManager usbManager) {
      super(usbManager);
   }

   public String errString(int errCode) {
      return null;
   }

   public void captured() {
   }

   public boolean isAwake() {
      return false;
   }

   public boolean wakeUp() {
      return false;
   }

   public boolean hibernate() {
      return false;
   }

   public String popPerformanceLog() {
      return "";
   }

   public int captureAuto(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      return 0;
   }

   public byte[] getCapturedBuffer(IBioMiniDevice.ImageOptions options) {
      return new byte[0];
   }

   public boolean clearCaptureImageBuffer() {
      return false;
   }

   public int getImageWidth() {
      return this.GetImageWidth();
   }

   public int getImageHeight() {
      return this.GetImageHeight();
   }

   public IBioMiniDevice.TemplateData extractTemplate(IBioMiniDevice.CaptureOption.ExtractOption extOptions) {
      return null;
   }

   public int getFeatureNumber(byte[] template, int template_size) {
      return 0;
   }

   public int getFPQuality(byte[] FPImage, int nWidth, int nHeight, int nFPQualityMode) {
      return 0;
   }

   public String getCompanyID() {
      return null;
   }

   public void setBridgeCallback(IBridgeCallback brdgCallback) {
   }

   public void makeBridge(Object obj) {
   }

   public int[] getCoreCoordinate() {
      return new int[0];
   }

   public void setEncryptionKey(byte[] key) {
   }

   public byte[] encrypt(byte[] data) {
      return new byte[0];
   }

   public byte[] decrypt(byte[] data) {
      return new byte[0];
   }
}
