package com.suprema.devices;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import com.suprema.ABioMiniDevice;
import com.suprema.IBioMiniDevice;
import com.suprema.IBioMiniInterops;
import com.suprema.ICaptureResponder;
import com.suprema.IUsbEventHandler;
import com.suprema.IUsbStatusChangeListener;
import com.suprema.android.BioMiniJni;
import com.suprema.usb.IUsbHandlerAndroidCyDevice;
import com.suprema.usb.UsbHandlerAndroidCyDevice;
import com.suprema.util.IBridgeCallback;
import com.suprema.util.Logger;
import java.util.Arrays;

public abstract class BioMiniBase extends ABioMiniDevice implements IBioMiniCyDevice, IBioMiniInterops {
   String TAG = "BioMiniBase";
   static final int USB_FULL_SPEED = 0;
   static final int USB_HI_SPEED = 1;
   static final int TRUE = 1;
   static final int FALSE = 0;
   final int MAX_TEMPLATE_SIZE = 1024;
   boolean mEnableAutoSleep = false;
   boolean mDetectedFake = false;
   int mDetectedFakeScore = 0;
   long mDetectedFakeTime = 0L;
   boolean isCaptured = false;
   long m_TimeOut;
   long m_oldTimeOut;
   int m_DetectFake;
   boolean m_bImageFlip = false;
   int m_nSensitivity = 7;
   int m_bExtTrigger = 0;
   int m_nExtractMode = 3001;
   IBioMiniDevice.ScanningMode m_nScanningMode;
   private static final boolean mSaveDebugImage = false;
   private static final boolean mPrintDebugMsg = false;
   boolean mIsInUse;
   IUsbHandlerAndroidCyDevice mUsbHandler;
   UsbDevice mCurrentDevice;
   boolean bInitialized;
   public int mProductId;
   boolean mIsTimeoutOccurred;
   protected IUsbStatusChangeListener m_UsbStatusChangeListener;
   private static final boolean dflag_bPrintTimetags = false;
   private static final boolean dflag_UseDebugImage = false;
   private IBridgeCallback mCrossLibraryCheckupCallback;

   public BioMiniBase() {
      this.m_nScanningMode = IBioMiniDevice.ScanningMode.SCANNING_MODE_CROP;
      this.mIsInUse = false;
      this.mUsbHandler = null;
      this.mCurrentDevice = null;
      this.bInitialized = false;
      this.mProductId = 0;
      this.mIsTimeoutOccurred = false;
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

   void ImageLogD(String filename, byte[] image, int width, int height) {
   }

   void LogD(Object msg) {
      Logger.LogD(this.TAG, msg);
   }

   void LogE(Object msg) {
      Logger.LogE(this.TAG, msg);
   }

   void LogV(Object msg) {
      Logger.LogV(this.TAG, msg);
   }

   void LogI(Object msg) {
      Logger.LogI(this.TAG, msg);
   }

   void LogW(Object msg) {
      Logger.LogW(this.TAG, msg);
   }

   void LogProcessStart(String name) {
   }

   void LogProcessEnd(String name) {
   }

   void LogPublicProcessStart(String name) {
      Logger.StartProcessingLog(name);
   }

   void LogPublicProcessEnd(String name) {
      Logger.EndProcessingLog(name);
   }

   public String popPerformanceLog() {
      String re = Logger.GetProcessingLog();
      Logger.ClearProcessingLog();
      return re;
   }

   void printTimeTag(String msg) {
   }

   public String errString(int errCode) {
      return IBioMiniDevice.ErrorCode.fromInt(errCode).toString();
   }

   void drawDebugMap(int bFingerOn, int bcheckCorr, int NHEH, int AEH, byte[] normalImage, byte[] bgImage, byte[] advImage, int nIntWidth, int nIntHeight, byte[] targetImage, int nTargetWidth, int nTargetHeight) {
   }

   void initParameters() {
      BioMiniJni.SetParameter(301, this.m_FastMode);
      BioMiniJni.SetParameter(302, this.m_SecurityLevel);
      BioMiniJni.SetParameter(321, this.m_AutoRotate);
      BioMiniJni.SetParameter(401, this.m_bDetectCore);
   }

   void setTempCaptureOpts() {
      if (this.mCurrentCaptureOption.captureTimeout != -1) {
         this.m_oldTimeOut = this.m_TimeOut;
         if (this.mCurrentCaptureOption.captureTimeout > 0 && this.mCurrentCaptureOption.captureTimeout < 1000) {
            this.m_TimeOut = (long)(this.mCurrentCaptureOption.captureTimeout * 1000);
         } else {
            this.m_TimeOut = (long)this.mCurrentCaptureOption.captureTimeout;
         }
      }

   }

   void resetCaptureOpts() {
      if (this.m_oldTimeOut != -1L) {
         this.m_TimeOut = this.m_oldTimeOut;
         this.m_oldTimeOut = -1L;
      }

   }

   public boolean isInUse() {
      return this.mIsInUse;
   }

   public boolean activate() {
      if (this.mUsbHandler != null) {
         this.mIsInUse = true;
         return true;
      } else {
         Log.e(this.TAG, "activate failed");
         return false;
      }
   }

   public boolean activate(Object appContext, Object deviceContext, Object bUsbLibusb) {
      this.LogD("activate...");
      UsbManager usbManager = null;
      if (appContext instanceof Context) {
         usbManager = (UsbManager)((Context)appContext).getSystemService("usb");
      } else {
         if (!(appContext instanceof UsbManager)) {
            this.LogE("activate failed : invalid context");
            return false;
         }

         usbManager = (UsbManager)appContext;
      }

      UsbDevice usbDevice = (UsbDevice)deviceContext;
      boolean _transfer_mode = false;

      try {
         if (bUsbLibusb instanceof Boolean) {
            Log.d(this.TAG, "Transfer_mode (instanceof true )");
            _transfer_mode = (Boolean)bUsbLibusb;
         }
      } catch (Exception var8) {
         this.LogD("Invalid transfer mode...");
      }

      this.mUsbHandler = new UsbHandlerAndroidCyDevice(this, usbManager, usbDevice, this.getMaxBulkSize(), _transfer_mode);
      if (!this.mUsbHandler.isValid()) {
         this.LogE("mUsbHandler is not valid");
         this.mUsbHandler = null;
         return false;
      } else {
         this.wakeUp();
         if (usbDevice != null) {
            int res = BioMiniJni.init(usbDevice);
            if (res != 0 && !this.mUsbHandler.hasInterruptEndpoint()) {
               return false;
            }
         }

         this.mIsInUse = true;
         this.mCurrentDevice = usbDevice;
         this.mProductId = usbDevice.getProductId();
         this.Setting(this.getProductId());
         this.m_TimeOut = 10000L;
         this.m_oldTimeOut = -1L;
         this.m_nSensitivity = 7;
         this.m_DetectFake = 0;
         this.m_TemplateType = IBioMiniDevice.TemplateType.SUPREMA;
         this.m_bExtTrigger = 0;
         this.initParameters();
         this.bInitialized = true;
         return true;
      }
   }

   public boolean activate(Object appContext, Object deviceContext) {
      Log.i(this.TAG, "Transfer_mode (activate(object, object ) : ");
      return this.activate(appContext, deviceContext, false);
   }

   public int getProductId() {
      return this.mProductId;
   }

   public boolean deactivate(IUsbEventHandler.DisconnectionCause reason) {
      this.mIsInUse = false;
      if (reason == IUsbEventHandler.DisconnectionCause.USB_UNPLUGGED) {
         if (this.mUsbHandler != null) {
            this.mUsbHandler.close();
            this.mUsbHandler = null;
         }

         this.mCurrentDevice = null;
      }

      return false;
   }

   public boolean isEqual(Object dev) {
      UsbDevice _dev = (UsbDevice)dev;
      return _dev != null && this.mCurrentDevice != null ? this.mCurrentDevice.getDeviceName().equals(_dev.getDeviceName()) : false;
   }

   public boolean setParameter(IBioMiniDevice.Parameter parameter) {
      switch(parameter.type.value()) {
      case 201:
         this.m_TimeOut = parameter.value;
         if (this.m_TimeOut < 0L) {
            this.m_TimeOut = 0L;
         }

         if (this.m_TimeOut > 60000L) {
            this.m_TimeOut = 60000L;
         }

         return true;
      case 203:
         this.m_nSensitivity = (int)parameter.value;
         return true;
      case 220:
         this.m_nScanningMode = IBioMiniDevice.ScanningMode.fromInt((int)parameter.value);
         return true;
      case 301:
         this.m_FastMode = (int)parameter.value;
         return BioMiniJni.SetParameter(301, this.m_FastMode) == 0;
      case 302:
         this.m_SecurityLevel = (int)parameter.value;
         return BioMiniJni.SetParameter(302, this.m_SecurityLevel) == 0;
      case 312:
         this.m_DetectFake = (int)parameter.value;
         return true;
      case 321:
         this.m_AutoRotate = (int)parameter.value;
         return BioMiniJni.SetParameter(321, this.m_AutoRotate) == 0;
      case 401:
         this.m_bDetectCore = (int)parameter.value;
         return BioMiniJni.SetParameter(401, this.m_bDetectCore) == 0;
      case 402:
         this.m_TemplateType = IBioMiniDevice.TemplateType.fromInt((int)parameter.value);
         return BioMiniJni.SetTemplateType(this.m_TemplateType.value()) == 0;
      case 450:
         this.m_nExtractMode = parameter.value == 1L ? 3002 : 3001;
         return true;
      case 501:
         this.mEnableAutoSleep = parameter.value == 1L;
         if (this.mEnableAutoSleep) {
            this.hibernate();
         } else {
            this.wakeUp();
         }

         return true;
      case 601:
         this.m_bExtTrigger = (int)parameter.value;
         return true;
      case 701:
         this.m_bImageFlip = (int)parameter.value == 1;
         return true;
      default:
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_INVALID_PARAMETERS;
         return false;
      }
   }

   private IBioMiniDevice.Parameter getParameter(String strName) {
      String strComp = strName.toUpperCase();
      byte var4 = -1;
      switch(strComp.hashCode()) {
      case -1626735195:
         if (strComp.equals("SECURITY_LEVEL")) {
            var4 = 3;
         }
         break;
      case -1577602433:
         if (strComp.equals("SENSITIVITY")) {
            var4 = 1;
         }
         break;
      case -1505889370:
         if (strComp.equals("FAST_MODE")) {
            var4 = 2;
         }
         break;
      case -1354352708:
         if (strComp.equals("EXTRACT_MODE_BIOSTAR")) {
            var4 = 11;
         }
         break;
      case -1329184801:
         if (strComp.equals("TEMPLATE_TYPE")) {
            var4 = 8;
         }
         break;
      case -784136725:
         if (strComp.equals("AUTO_ROTATE")) {
            var4 = 5;
         }
         break;
      case -595928767:
         if (strComp.equals("TIMEOUT")) {
            var4 = 0;
         }
         break;
      case -47716239:
         if (strComp.equals("SCANNING_MODE")) {
            var4 = 6;
         }
         break;
      case 538501804:
         if (strComp.equals("ENABLE_AUTOSLEEP")) {
            var4 = 9;
         }
         break;
      case 965084506:
         if (strComp.equals("EXT_TRIGGER")) {
            var4 = 10;
         }
         break;
      case 2007200699:
         if (strComp.equals("DETECT_CORE")) {
            var4 = 7;
         }
         break;
      case 2007276401:
         if (strComp.equals("DETECT_FAKE")) {
            var4 = 4;
         }
      }

      switch(var4) {
      case 0:
         this.LogD("timeout - : " + this.m_TimeOut);
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.TIMEOUT, this.m_TimeOut);
      case 1:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.SENSITIVITY, (long)this.m_nSensitivity);
      case 2:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.FAST_MODE, (long)this.m_FastMode);
      case 3:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.SECURITY_LEVEL, (long)this.m_SecurityLevel);
      case 4:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.DETECT_FAKE, (long)this.m_DetectFake);
      case 5:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.AUTO_ROTATE, (long)this.m_AutoRotate);
      case 6:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.SCANNING_MODE, (long)this.m_nScanningMode.value());
      case 7:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.DETECT_CORE, (long)this.m_bDetectCore);
      case 8:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.TEMPLATE_TYPE, (long)this.m_TemplateType.value());
      case 9:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.ENABLE_AUTOSLEEP, this.mEnableAutoSleep ? 1L : 0L);
      case 10:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.EXT_TRIGGER, (long)this.m_bExtTrigger);
      case 11:
         return new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.EXTRACT_MODE_BIOSTAR, this.m_nExtractMode == 3002 ? 1L : 0L);
      default:
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_INVALID_PARAMETERS;
         return null;
      }
   }

   public IBioMiniDevice.Parameter getParameter(IBioMiniDevice.ParameterType type) {
      return this.getParameter(type.toString());
   }

   public void setEncryptionKey(byte[] key) {
      this.mEnableEcryption = key != null && BioMiniJni.SetEncryptKey(key) == 0;
   }

   public byte[] decrypt(byte[] data) {
      return BioMiniJni.Decrypt(data);
   }

   public byte[] encrypt(byte[] data) {
      return BioMiniJni.Encrypt(data);
   }

   public IBioMiniDevice.TemplateData extractTemplate(IBioMiniDevice.CaptureOption.ExtractOption extOptions) {
      if (this.mCurrentCaptureOption == null) {
         this.mCurrentCaptureOption = new IBioMiniDevice.CaptureOption();
      }

      this.mCurrentCaptureOption.extractParam = extOptions;
      return this.extractTemplate();
   }

   public IBioMiniDevice.TemplateData extractTemplate() {
      if (!this.isCaptured) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_CAPTURED;
         return null;
      } else {
         int exMaxTemplateSize = 1024;
         if (this.mCurrentCaptureOption.extractParam.maxTemplateSize == IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_384) {
            exMaxTemplateSize = 384;
         }

         byte[] exTemplate = new byte[exMaxTemplateSize];
         int[] exTemplateSize = new int[4];
         int[] exQuality = new int[4];
         if (this.mCurrentCaptureOption.extractParam.Rotate && (this.mProductId == 1031 || this.mProductId == 1032)) {
            this.rotateImage();
         }

         int res = BioMiniJni.Extract(this, exTemplate, exTemplateSize, exMaxTemplateSize, exQuality);
         if (res == 0) {
            this.LogD("Extracting template successful : " + exTemplateSize[0]);
            byte[] reTemplate = new byte[exTemplateSize[0]];
            System.arraycopy(exTemplate, 0, reTemplate, 0, exTemplateSize[0]);
            this.m_LastError = IBioMiniDevice.ErrorCode.OK;
            return this.mEnableEcryption ? new IBioMiniDevice.TemplateData(BioMiniJni.Encrypt(reTemplate), this.m_TemplateType, exQuality[0]) : new IBioMiniDevice.TemplateData(reTemplate, this.m_TemplateType, exQuality[0]);
         } else {
            this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(res);
            return null;
         }
      }
   }

   public byte[] getCaptureImageAsWsq(int width, int height, float fBitRate, int rotate) {
      if (!this.isCaptured) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_CAPTURED;
         return null;
      } else if (this.getImageHeight() > 0 && this.getImageHeight() > 0) {
         byte[] outImage = new byte[this.getImageWidth() * this.getImageHeight() + 1024];
         int[] outImageSize = new int[4];
         int bufferSize = outImage.length;
         int res;
         if (width > 0 && height > 0) {
            if (rotate != 0 && rotate != 180) {
               res = BioMiniJni.SaveCaptureImageBufferToWSQBufferVar(this, this.getImageWidth(), this.getImageHeight(), outImage, outImageSize, bufferSize, fBitRate, width, height);
            } else {
               res = BioMiniJni.SaveCaptureImageBufferToWSQBufferVarEx(this, this.getImageWidth(), this.getImageHeight(), outImage, outImageSize, bufferSize, fBitRate, width, height, rotate);
            }
         } else {
            res = BioMiniJni.SaveCaptureImageBufferToWSQBuffer(this, this.getImageWidth(), this.getImageHeight(), outImage, outImageSize, bufferSize, fBitRate);
         }

         this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(res);
         if (res == IBioMiniDevice.ErrorCode.OK.value()) {
            this.LogD("WSQ encoding successful : " + outImageSize[0]);
            byte[] pWSQImage = new byte[outImageSize[0]];
            System.arraycopy(outImage, 0, pWSQImage, 0, outImageSize[0]);
            return pWSQImage;
         } else {
            Log.e(this.TAG, "WSQ encoding failed");
            return null;
         }
      } else {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
         return null;
      }
   }

   public byte[] getCaptureImageAsBmp() {
      if (!this.isCaptured) {
         return null;
      } else {
         int nBMPImageSize = BioMiniJni.GetBMPFileSize(this.getImageWidth(), this.getImageHeight());
         byte[] pBMPImage = new byte[nBMPImageSize];
         int[] olen = new int[4];
         if (BioMiniJni.SaveCaptureImageBufferToBMPBuffer(this, this.getImageWidth(), this.getImageHeight(), pBMPImage, olen) == 0) {
            return pBMPImage;
         } else {
            Log.e(this.TAG, "BioMiniJni.SaveCaptureImageBufferToBMPBuffer failed");
            return null;
         }
      }
   }

   public boolean verify(byte[] pTemplate1, int nTemplate1Size, byte[] pTemplate2, int nTemplate2Size) {
      int[] verificationResult = new int[4];
      int res = BioMiniJni.Verify(pTemplate1, nTemplate1Size, pTemplate2, nTemplate2Size, verificationResult);
      this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(res);
      if (this.m_TemplateType == IBioMiniDevice.TemplateType.SUPREMA) {
         return verificationResult[0] >= this.MATCHING_THRESHOLD[this.m_SecurityLevel - 1];
      } else {
         return verificationResult[0] >= this.MATCHING_THRESHOLD_SIF[this.m_SecurityLevel - 1];
      }
   }

   public boolean verify(byte[] pTemplate1, byte[] pTemplate2) {
      return this.verify(pTemplate1, pTemplate1.length, pTemplate2, pTemplate2.length);
   }

   public int getFeatureNumber(byte[] template, int template_size) {
      int[] feature_number = new int[4];
      this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(BioMiniJni.GetFeatureNumber(template, template_size, feature_number));
      return feature_number[0];
   }

   public int getFPQuality(byte[] FPImage, int nWidth, int nHeight, int nFPQualityMode) {
      int[] FPQuality = new int[4];
      this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(BioMiniJni.GetFPQuality(FPImage, nWidth, nHeight, FPQuality, nFPQualityMode));
      return FPQuality[0];
   }

   public String getCompanyID() {
      return "";
   }

   public IBioMiniDevice.DeviceInfo getDeviceInfo() {
      return this.mDeviceInfo;
   }

   public int[] getCoreCoordinate() {
      int[] coord = new int[2];
      return BioMiniJni.GetCoreCoordinate(coord) == 0 ? coord : null;
   }

   public IBioMiniDevice.ErrorCode getLastError() {
      IBioMiniDevice.ErrorCode re = this.m_LastError;
      this.m_LastError = IBioMiniDevice.ErrorCode.OK;
      return re;
   }

   public void setBridgeCallback(IBridgeCallback brdgCallback) {
      this.mCrossLibraryCheckupCallback = brdgCallback;
   }

   public void makeBridge(Object obj) {
      try {
         this.mCrossLibraryCheckupCallback.checkCallback(BioMiniJni.SetBridgeNative((byte[])((byte[])obj)));
      } catch (Exception var3) {
         this.LogE(var3.toString());
      }

   }

   abstract void rotateImage();

   public abstract byte[] getCaptureImageAs19794_4();

   public IBioMiniDevice.ErrorCode setPacketMode(IBioMiniDevice.PacketMode packetMode) {
      return IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
   }

   public boolean canChangePacketMode() {
      return false;
   }

   class RawImageItem {
      public String message;
      public byte[] imageData;

      RawImageItem(byte[] image_data, String msg) {
         this.imageData = image_data;
         this.message = msg;
      }
   }

   class MDRCapturedPair {
      public BioMiniBase.MDRImagePair MdrA;
      public BioMiniBase.MDRImagePair MdrN;

      MDRCapturedPair(BioMiniBase.MDRImagePair mdrA, BioMiniBase.MDRImagePair mdrN) {
         this.MdrA = mdrA;
         this.MdrN = mdrN;
      }
   }

   class MDRExposurePair {
      public int ExposureA;
      public int ExposureN;

      MDRExposurePair(int exposureN, int exposureA) {
         this.ExposureA = exposureA;
         this.ExposureN = exposureN;
      }
   }

   class MDRImagePair {
      public int TagIndex;
      public byte[] Image;
      public int Exposure;

      MDRImagePair(int tagIndex, byte[] image, int exposure) {
         this.TagIndex = tagIndex;
         this.Image = image;
         this.Exposure = exposure;
      }
   }
}
