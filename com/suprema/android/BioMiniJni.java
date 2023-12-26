package com.suprema.android;

import android.hardware.usb.UsbDevice;
import com.suprema.devices.IBioMiniCyDevice;

public class BioMiniJni {
   public static final int UFA_OK = 0;
   public static final int UFA_ERROR = -1;
   public static final int UFA_ERR_LICENSE_NOT_MATCH = -102;
   public static final int UFA_ERR_NOT_SUPPORTED = -111;
   public static final int UFA_ERR_INVALID_PARAMETERS = -112;
   public static final int UFA_ERR_ALREADY_INITIALIZED = -201;
   public static final int UFA_ERR_NOT_INITIALIZED = -202;
   public static final int UFA_ERR_NO_DEVICE = -205;
   public static final int UFA_ERR_PERMISSION_DENIED = -206;
   public static final int UFA_ERR_CAPTURE_RUNNING = -211;
   public static final int UFA_ERR_CAPTURE_FAILED = -212;
   public static final int UFA_ERR_NOT_CAPTURED = -213;
   public static final int UFA_ERR_CAPTURE_TIMEOUT = -11;
   public static final int UFA_ERR_FAKE_FINGER = -214;
   public static final int UFA_ERR_EXTRACTION_FAILED = -302;
   public static final int UFA_ERR_TEMPLATE_TYPE = -411;
   public static final int UFA_ERR_FILE_EXIST_ALREADY = 4001;
   public static final int UFA_ERR_CORE_NOT_DETECTED = -351;
   public static final int UFA_ERR_CORE_TO_LEFT = -352;
   public static final int UFA_ERR_CORE_TO_LEFT_TOP = -353;
   public static final int UFA_ERR_CORE_TO_TOP = -354;
   public static final int UFA_ERR_CORE_TO_RIGHT_TOP = -355;
   public static final int UFA_ERR_CORE_TO_RIGHT = -356;
   public static final int UFA_ERR_CORE_TO_RIGHT_BOTTOM = -357;
   public static final int UFA_ERR_CORE_TO_BOTTOM = -358;
   public static final int UFA_ERR_CORE_TO_LEFT_BOTTOM = -359;
   public static final int UFA_TEMPLATE_TYPE_SUPREMA = 2001;
   public static final int UFA_TEMPLATE_TYPE_ISO19794_2 = 2002;
   public static final int UFA_TEMPLATE_TYPE_ANSI378 = 2003;
   public static final int UFA_EXTRACT_MODE_NORMAL = 3001;
   public static final int UFA_EXTRACT_MODE_BIOSTAR = 3002;
   public static final int UFA_PARAM_TIMEOUT = 201;
   public static final int UFA_PARAM_SENSITIVITY = 203;
   public static final int UFA_PARAM_SCANNING_MODE = 220;
   public static final int UFA_PARAM_FAST_MODE = 301;
   public static final int UFA_PARAM_SECURITY_LEVEL = 302;
   public static final int UFA_PARAM_DETECT_FAKE = 312;
   public static final int UFA_PARAM_AUTO_ROTATE = 321;
   public static final int UFA_PARAM_DETECT_CORE = 401;
   public static final int UFA_PARAM_TEMPLATE_TYPE = 402;
   public static final int UFA_PARAM_ENABLE_AUTOSLEEP = 501;
   public static final int UFA_PARAM_EXTRACT_MODE = 450;
   public static final int UFA_PARAM_EXT_TRIGGER = 601;
   public static final int UFA_PARAM_FLIP_IMAGE = 701;

   public static String GetErrorString(int res) {
      switch(res) {
      case -411:
         return "Template type error";
      case -359:
         return "Core should be located more to the bottom left";
      case -358:
         return "Core should be located lower";
      case -357:
         return "Core should be located more to the bottom right";
      case -356:
         return "Core should be located more to the right";
      case -355:
         return "Core should be located more to the upper right";
      case -354:
         return "Core should be located upper";
      case -353:
         return "Core should be located more to the upper lfet";
      case -352:
         return "Core should be located more to the left";
      case -351:
         return "Core is not detected";
      case -302:
         return "Extraction failed";
      case -214:
         return "Fake finger is detected";
      case -213:
         return "There is no properly captured image";
      case -212:
         return "Capture failed";
      case -211:
         return "Capture is running";
      case -206:
         return "The USB permission is canceled. After re-plug the device and try Uninit > Find device > Init";
      case -205:
         return "No device. connecting a device, try Find Device first";
      case -202:
         return "SDK is not initialized";
      case -201:
         return "SDK is already initialized";
      case -112:
         return "Invalid Parameter";
      case -111:
         return "Not supported operation";
      case -102:
         return "License Error";
      case -11:
         return "Capture timeout";
      case -1:
         return "ERROR";
      case 0:
         return "OK";
      default:
         return "Code Error";
      }
   }

   public static native byte[] SetBridgeNative(byte[] var0);

   public static native int init(UsbDevice var0);

   public static native int uninit();

   public static native int SetParameter(int var0, int var1);

   public static native int SetTemplateType(int var0);

   public static native int Extract(Object var0, byte[] var1, int[] var2, int var3, int[] var4);

   public static native int Verify(byte[] var0, int var1, byte[] var2, int var3, int[] var4);

   public static native int SaveCaptureImageBufferToWSQBuffer(Object var0, int var1, int var2, byte[] var3, int[] var4, int var5, float var6);

   public static native int SaveCaptureImageBufferToWSQBufferVar(Object var0, int var1, int var2, byte[] var3, int[] var4, int var5, float var6, int var7, int var8);

   public static native int SaveCaptureImageBufferToWSQBufferVarEx(Object var0, int var1, int var2, byte[] var3, int[] var4, int var5, float var6, int var7, int var8, int var9);

   public static native int SaveCaptureImageBufferToBMPBuffer(Object var0, int var1, int var2, byte[] var3, int[] var4);

   public static native int GetBMPFileSize(int var0, int var1);

   public static native int setESA(int var0, int var1, int var2, int var3, float var4, float var5);

   public static native int GetOptimalExposureValue(byte[] var0, byte[] var1, byte[] var2, int var3, int var4, int var5, double var6, int var8, int var9, int var10, int var11, int var12);

   public static native int DetectFingerprintArea(byte[] var0, byte[] var1, int var2, int var3, int var4, int var5, int var6);

   public static native int DetectFingerprintArea_For_Background(byte[] var0, byte[] var1, int var2, int var3, int var4, int var5, int var6);

   public static native int GetAvg(int[] var0);

   public static native int AdjustRaw(byte[] var0);

   public static native int Comp(byte[] var0, byte[] var1, int var2);

   public static native int CheckCorrelation(byte[] var0, byte[] var1, int var2);

   public static native int CheckCorrelationRAW(byte[] var0, byte[] var1, int var2);

   public static native int GetPreprocessedImage(byte[] var0, byte[] var1, byte[] var2, byte[] var3, float var4, float var5, float var6, int var7, int var8, int var9, int var10);

   public static native int GetPreprocessedImageEx(byte[] var0, byte[] var1, byte[] var2, byte[] var3, float var4, float var5, float var6, int var7, int var8, int var9, int var10, boolean var11);

   public static native int BKCompensateSelf2D(byte[] var0, byte[] var1, byte[] var2, int var3, int var4);

   public static native int SwitchScanningMode(int var0);

   public static native int GetLFDResult(byte[] var0, int var1, int var2, int var3, int[] var4);

   public static native int CheckCorrelationOP(int[] var0, byte[] var1, byte[] var2);

   public static native int CompOC4(byte[] var0, byte[] var1, int var2, int var3);

   public static native int ConvertImage2(byte[] var0, byte[] var1, int var2, int var3);

   public static native boolean IsOC4FingerTouch(byte[] var0, int var1, int var2, int var3, int var4);

   public static native int IsFingerOn(byte[] var0, int[] var1, double[] var2);

   public static native int HQSHPFiltering(byte[] var0, byte[] var1, int var2, int var3);

   public static native int OC4BrightnessCompensation(byte[] var0, int var1);

   public static native int SetOC4IsFingerParams(boolean var0);

   public static native boolean IsFingerOnOC4(IBioMiniCyDevice var0, byte[] var1, int var2, int var3, boolean var4, int[] var5, int var6);

   public static native int OC4PostProcessing(byte[] var0, byte[] var1, byte[] var2);

   public static native int GetFeatureNumber(byte[] var0, int var1, int[] var2);

   public static native int GetFPQuality(byte[] var0, int var1, int var2, int[] var3, int var4);

   public static native int GetCoreCoordinate(int[] var0);

   public static native int SetEncryptKey(byte[] var0);

   public static native byte[] Encrypt(byte[] var0);

   public static native byte[] Decrypt(byte[] var0);

   static {
      System.loadLibrary("BioMiniJni");
   }
}
