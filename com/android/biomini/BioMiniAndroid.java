package com.android.biomini;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;
import com.suprema.BioMiniFactory;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;
import com.suprema.IUsbEventHandler;
import com.suprema.android.BioMiniJni;
import com.suprema.devices.BioMiniBase;
import com.suprema.usb.UsbHandlerAndroidCyDevice;
import com.suprema.util.IBioMiniCallback;
import com.suprema.util.IBioMiniDeviceCallback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BioMiniAndroid {
   BioMiniFactory mFactory;
   BioMiniJni miniJni;
   private static BioMiniAndroid mInstance = null;
   int mCurrentDeviceIndex = 0;
   IBioMiniDevice mCurrentDevice;
   List<IBioMiniDevice> mDeviceList;
   private int mCycleMilliSec = 100;
   private int _timeout = 5000;
   private int _sensitivity = 7;
   private int _security_level = 0;
   private int _auto_rotate = 0;
   private int _detect_core = 0;
   private boolean _enable_autosleep = false;
   private BioMiniAndroid.SCANNER_OPTIONS _scanning_mode;
   private int _fast_mode;
   private int _detect_fake;
   private int _ext_rotate;
   private BioMiniAndroid.TEMPLATE_TYPE mTemplateType;
   public static final int UFA_TEMPLATE_TYPE_SUPREMA = 2001;
   public static final int UFA_TEMPLATE_TYPE_ISO_19794_2 = 2002;
   public static final int UFA_TEMPLATE_TPYE_ANSI378 = 2003;
   private static final int UFA_PARAM_TIMEOUT = 201;
   private static final int UFA_PARAM_SENSITIVITY = 203;
   private static final int UFA_PARAM_SCANNING_MODE = 220;
   private static final int UFA_PARAM_FAST_MODE = 301;
   private static final int UFA_PARAM_SECURITY_LEVEL = 302;
   private static final int UFA_PARAM_DETECT_FAKE = 312;
   private static final int UFA_PARAM_AUTO_ROTATE = 321;
   private static final int UFA_PARAM_DETECT_CORE = 401;
   private static final int UFA_PARAM_ENABLE_AUTOSLEEP = 501;
   private static final int CMD_SET_CIS_TIME = 192;
   private static final int CMD_CIS_START_POS = 193;
   private static final int CMD_LED_CTRL = 194;
   private static final int CMD_SET_REG = 195;
   private static final int CMD_READ_FRAME = 198;
   private static final int CMD_GET_CID = 199;
   private static final int CMD_GET_CID_SLIM = 217;
   private static final int CMD_GET_SN = 201;
   private static final int CMD_CONNECT_INFO = 209;
   private static final int CMD_SET_OV_EEPROM = 216;
   private static final int CMD_GET_OV_EEPROM = 217;
   private static final int CMD_READ_FRAME_N = 225;
   private static final int CMD_READ_FRAME_A = 226;
   private static final int CMD_READ_FRAME_DONE = 239;
   private static final int CMD_SET_EEPROM = 214;
   private static final int CMD_GET_EEPROM = 215;
   private static final int CMD_SENSOR_EEPROM_GET_ADDR = 222;
   private static final int CMD_SENSOR_EEPROM_GET_DATA = 223;
   private static final int CMD_EEPROM_WP_ENABLE = 201;
   private static final int CMD_GET_SYSTEM_STATUS = 197;
   private static final int CMD_SET_SLEEPMODE = 204;
   private static final int CMD_MULTI_EXP_ENABLE = 195;
   private static final int CMD_READ_FRAME_LFD_U = 227;
   private static final int CMD_READ_FRAME_LFD_L = 228;
   private static final int CMD_READ_LFD_2ND = 232;
   private static final int CMD_READ_LFD_2ND_DONE = 233;
   private static final int CMD_READ_LFD_3RD = 232;
   private static final int CMD_READ_LFD_3RD_DONE = 233;
   private static final int CMD_ANDROID_256K_MODE = 234;
   private static final int CMD_ANDROID_256K_SXSY = 235;
   private static final int CMD_READ_LFD_4TH = 236;
   private static final int CMD_READ_LFD_ALL_DONE = 238;
   private static final int CMD_SET_LED = 194;
   private static final int OV_IIC_EEPROM_ADDR = 174;
   private static final int UFA_OK = 0;
   private static final int UFA_ERROR = -1;
   private static final int UFA_ERR_LICENSE_NOT_MATCH = -102;
   private static final int UFA_ERR_NOT_SUPPORTED = -111;
   private static final int UFA_ERR_INVALID_PARAMETERS = -112;
   private static final int UFA_ERR_ALREADY_INITIALIZED = -201;
   private static final int UFA_ERR_NOT_INITIALIZED = -202;
   private static final int UFA_ERR_NO_DEVICE = -205;
   private static final int UFA_ERR_PERMISSION_DENIED = -206;
   private static final int UFA_ERR_CAPTURE_RUNNING = -211;
   private static final int UFA_ERR_CAPTURE_FAILED = -212;
   private static final int UFA_ERR_NOT_CAPTURED = -213;
   private static final int UFA_ERR_CAPTURE_TIMEOUT = -11;
   private static final int UFA_ERR_FAKE_FINGER = -214;
   private static final int UFA_ERR_EXTRACTION_FAILED = -302;
   private static final int UFA_ERR_TEMPLATE_TYPE = -411;
   private static final int UFA_ERR_FILE_EXIST_ALREADY = 4001;
   private static final int UFA_ERR_CORE_NOT_DETECTED = -351;
   private static final int UFA_ERR_CORE_TO_LEFT = -352;
   private static final int UFA_ERR_CORE_TO_LEFT_TOP = -353;
   private static final int UFA_ERR_CORE_TO_TOP = -354;
   private static final int UFA_ERR_CORE_TO_RIGHT_TOP = -355;
   private static final int UFA_ERR_CORE_TO_RIGHT = -356;
   private static final int UFA_ERR_CORE_TO_RIGHT_BOTTOM = -357;
   private static final int UFA_ERR_CORE_TO_BOTTOM = -358;
   private static final int UFA_ERR_CORE_TO_LEFT_BOTTOM = -359;
   private static final int IMG_XMAX_SLIM = 896;
   private static final int IMG_YMAX_SLIM = 432;
   private static final int IMG_YMAX_SLIM_256 = 336;
   private static final int IMG_XMAX_SLIM_256 = 768;
   private static final int SLIM_IMAGE_WIDTH_D = 352;
   private static final int SLIM_IMAGE_HEIGHT_D = 496;
   private static final int SLIM_IMAGE_WIDTH = 320;
   private static final int SLIM_IMAGE_HEIGHT = 480;
   private static final int IMG_XMAX_PLUS2 = 640;
   private static final int IMG_YMAX_PLUS2 = 480;
   private static final int IMG_XMAX_PLUS2_256 = 576;
   private static final int IMG_YMAX_PLUS2_256 = 448;
   private static final int PLUS2_256K_ACTIVE_AREA_X = 560;
   private static final int PLUS2_256K_ACTIVE_AREA_Y = 440;
   private static final int PLUS2_START_X_MAX_OFFSET = 64;
   private static final int PLUS2_START_Y_MAX_OFFSET = 40;
   private static final int PLUS2_IMAGE_WIDTH_D = 315;
   private static final int PLUS2_IMAGE_HEIGHT_D = 354;
   private static final int PLUS2_IMAGE_WIDTH = 315;
   private static final int PLUS2_IMAGE_HEIGHT = 354;
   private static final int PLUS2_CROP_IMAGE_WIDTH = 288;
   private static final int PLUS2_CROP_IMAGE_HEIGHT = 340;
   private static final int IMG_XMAX_SLIM2 = 1240;
   private static final int IMG_YMAX_SLIM2 = 422;
   private static final int IMG_XMAX_SLIM2_256 = 640;
   private static final int IMG_YMAX_SLIM2_256 = 408;
   private static final int SLIM2_IMAGE_WIDTH_D = 300;
   private static final int SLIM2_IMAGE_HEIGHT_D = 433;
   private static final int SLIM2_IMAGE_WIDTH = 300;
   private static final int SLIM2_IMAGE_HEIGHT = 433;
   private static final int SLIM2_CROP_WIDTH = 300;
   private static final int SLIM2_CROP_HEIGHT = 400;
   private static final int IMG_XMAX_OC4 = 640;
   private static final int IMG_XMAX_OC4_EX = 656;
   private static final int IMG_YMAX_OC4 = 480;
   private static final int OC4_IMAGE_WIDTH = 288;
   private static final int OC4_IMAGE_HEIGHT = 320;
   private static final int OC4_EXP_PRE = 80;
   private static final int OC4_GAIN_PRE = 60;
   private static final int CAPTURE_BUFFER_SIZE = 12;
   private static final int DELAY_FOR_SUCCESSFUL_CAPTURE = 130;
   private long mLastNotification;
   private long mLastWait;
   private int mPacketsToSkipOC4;
   private int mPacketsToSkipSlim;
   private int mPacketsToSkipPLUS2;
   private byte mPaddingValue;
   private boolean mEnableAutoSleep;
   private boolean m256K_Mode;
   private int mPLUS2_StartPosX;
   private int mPLUS2_StartPosY;
   private IBioMiniCallback mCallbackHandler;
   private IBioMiniDeviceCallback mCallbackDeviceHandler;
   private UsbDevice mDevice;
   private UsbHandlerAndroidCyDevice mUsbHandler;

   public static void ConditionD(String msg, boolean bFlag) {
      if (bFlag) {
         D(msg);
      } else if (!bFlag) {
         E(msg);
      }

   }

   public static void D(String msg) {
      Log.d(">>" + BioMiniAndroid.class.toString() + "<<", msg);
   }

   public static void E(String msg) {
      Log.e(">>" + BioMiniAndroid.class.toString() + "<<", msg);
   }

   public static void I(String msg) {
      Log.i(">>" + BioMiniAndroid.class.toString() + "<<", msg);
   }

   public static BioMiniAndroid getInstance(Context context) {
      if (mInstance == null) {
         mInstance = new BioMiniAndroid(context);
      }

      return mInstance;
   }

   public static BioMiniAndroid getInstance(Context context, UsbManager usbManager) {
      if (mInstance != null) {
         mInstance = new BioMiniAndroid(context, usbManager);
      }

      return mInstance;
   }

   private BioMiniAndroid(Context context, UsbManager _usbManager) {
      this._scanning_mode = BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_CROP;
      this._fast_mode = 0;
      this._detect_fake = 0;
      this._ext_rotate = 0;
      this.mLastNotification = 0L;
      this.mLastWait = 0L;
      this.mPacketsToSkipOC4 = 2;
      this.mPacketsToSkipSlim = 1;
      this.mPacketsToSkipPLUS2 = 2;
      this.mPaddingValue = -1;
      this.mEnableAutoSleep = false;
      this.m256K_Mode = false;
      this.mPLUS2_StartPosX = 0;
      this.mPLUS2_StartPosY = 0;
      this.mCallbackDeviceHandler = null;
      this.mFactory = new BioMiniFactory(context, _usbManager) {
         public void onDeviceChange(IUsbEventHandler.DeviceChangeEvent event, Object dev) {
            BioMiniAndroid.I("DeviceStatus changed :" + event.toString() + "(dev infos:" + dev.toString());
         }
      };
      this.initialize();
      this.mFactory.setTransferMode(IBioMiniDevice.TransferMode.MODE1);
   }

   private BioMiniAndroid(Context context) {
      this._scanning_mode = BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_CROP;
      this._fast_mode = 0;
      this._detect_fake = 0;
      this._ext_rotate = 0;
      this.mLastNotification = 0L;
      this.mLastWait = 0L;
      this.mPacketsToSkipOC4 = 2;
      this.mPacketsToSkipSlim = 1;
      this.mPacketsToSkipPLUS2 = 2;
      this.mPaddingValue = -1;
      this.mEnableAutoSleep = false;
      this.m256K_Mode = false;
      this.mPLUS2_StartPosX = 0;
      this.mPLUS2_StartPosY = 0;
      this.mCallbackDeviceHandler = null;
      this.mFactory = new BioMiniFactory(context) {
         public void onDeviceChange(IUsbEventHandler.DeviceChangeEvent event, Object dev) {
            BioMiniAndroid.I("DeviceStatus changed :" + event.toString() + "(dev infos:" + dev.toString());
         }
      };
      this.initialize();
      this.mFactory.setTransferMode(IBioMiniDevice.TransferMode.MODE1);
   }

   private void initialize() {
      this.mDeviceList = new ArrayList();
   }

   public int getImageWidth() {
      return this.getTargetWidth();
   }

   public int getImageHeight() {
      return this.getTargetHeight();
   }

   private int getTargetWidth() {
      switch(this.getProductId()) {
      case 1030:
         return 288;
      case 1031:
         return 320;
      case 1033:
         if (this._scanning_mode == BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_FULL) {
            return 315;
         } else if (this._scanning_mode == BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_CROP) {
            return 288;
         }
      case 1032:
         return 300;
      case 1056:
      case 1057:
         return 320;
      default:
         return -1;
      }
   }

   private int getTargetHeight() {
      switch(this.getProductId()) {
      case 1030:
         return 320;
      case 1031:
         return 480;
      case 1033:
         if (this._scanning_mode == BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_FULL) {
            return 354;
         } else if (this._scanning_mode == BioMiniAndroid.SCANNER_OPTIONS.PLUS2_SCANNING_MODE_CROP) {
            return 340;
         }
      case 1032:
         return 400;
      case 1056:
      case 1057:
         return 480;
      default:
         return -1;
      }
   }

   public BioMiniAndroid.ECODE UFA_FindDevice() {
      if (this.mFactory == null) {
         return BioMiniAndroid.ECODE.ERROR;
      } else {
         ConditionD("attached Device count :" + this.mFactory.getDeviceCount(), this.mFactory.getDeviceCount() > 0);
         this.mDeviceList.clear();

         for(int i = 0; i < this.mFactory.getDeviceCount(); ++i) {
            this.mDeviceList.add(this.mFactory.getDevice(0));
         }

         D("BiominiDevice List : " + this.mDeviceList.size());
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_SetDevice(UsbDevice _device) {
      return this.mCurrentDevice == null ? BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED : BioMiniAndroid.ECODE.OK;
   }

   private void init() {
      this.mTemplateType = BioMiniAndroid.TEMPLATE_TYPE.SUPREMA;
   }

   public BioMiniAndroid.ECODE UFA_Init() {
      this.mCurrentDevice = this.mFactory.getDevice(this.mCurrentDeviceIndex);
      this.init();
      return this.mCurrentDevice == null ? BioMiniAndroid.ECODE.ERROR : BioMiniAndroid.ECODE.OK;
   }

   public String UFA_GetSerialNumber() {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED.toString();
      } else {
         return this.mCurrentDevice == null ? BioMiniAndroid.ECODE.ERROR.toString() : this.mCurrentDevice.getDeviceInfo().deviceSN;
      }
   }

   public BioMiniAndroid.ECODE UFA_SetParameter(BioMiniAndroid.PARAM param, int value) {
      D("Param Type : " + param.toString() + " / valud : " + value);
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         switch(param.value()) {
         case 201:
            int timeout = value;
            if (value < 0) {
               timeout = 0;
            }

            this._timeout = timeout;
            return BioMiniAndroid.ECODE.OK;
         case 203:
            if (value < 0) {
               value = 0;
            }

            if (value > 7) {
               value = 7;
            }

            this._sensitivity = value;
            return BioMiniAndroid.ECODE.OK;
         case 220:
            if (BioMiniAndroid.SCANNER_OPTIONS.isValid(value)) {
               this._scanning_mode = BioMiniAndroid.SCANNER_OPTIONS.fromInt(value);
               this.SwitchScanningMode(value);
               return BioMiniAndroid.ECODE.OK;
            }

            return BioMiniAndroid.ECODE.ERR_INVALID_PARAMETERS;
         case 301:
            if (value < 1) {
               value = 0;
            } else {
               value = 1;
            }

            this._fast_mode = value;
            if (this.mCurrentDevice.setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.FAST_MODE, (long)value))) {
               return BioMiniAndroid.ECODE.OK;
            }
         case 302:
            if (value < 1) {
               value = 1;
            }

            if (value > 7) {
               value = 7;
            }

            this._security_level = value;
            if (this.mCurrentDevice.setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.SECURITY_LEVEL, (long)value))) {
               return BioMiniAndroid.ECODE.OK;
            }
         case 312:
            if (this.getProductId() != 1032 && this.getProductId() != 1033) {
               return BioMiniAndroid.ECODE.ERR_NOT_SUPPORTED;
            }

            if (value < 0) {
               value = 0;
            }

            if (value > 5) {
               value = 5;
            }

            this._detect_fake = value;
            return BioMiniAndroid.ECODE.OK;
         case 321:
            if (value != 0 && value != 1) {
               return BioMiniAndroid.ECODE.ERR_INVALID_PARAMETERS;
            } else {
               this._auto_rotate = value;
               boolean res = this.mCurrentDevice.setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.AUTO_ROTATE, (long)this._auto_rotate));
               if (!res) {
                  return BioMiniAndroid.ECODE.ERROR;
               }

               return BioMiniAndroid.ECODE.OK;
            }
         case 401:
            this._detect_core = value;
            if (this.mCurrentDevice.setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.AUTO_ROTATE, (long)this._detect_core))) {
               return BioMiniAndroid.ECODE.OK;
            }
         case 501:
            this._enable_autosleep = value == 1;
            return BioMiniAndroid.ECODE.OK;
         default:
            return BioMiniAndroid.ECODE.ERROR;
         }
      }
   }

   public BioMiniAndroid.ECODE UFA_GetParameter(BioMiniAndroid.PARAM param, int[] value) {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         switch(param.value()) {
         case 201:
            value[0] = this._timeout;
            return BioMiniAndroid.ECODE.OK;
         case 203:
            value[0] = this._sensitivity;
            return BioMiniAndroid.ECODE.OK;
         case 220:
            value[0] = this._scanning_mode.value();
            return BioMiniAndroid.ECODE.OK;
         case 301:
            value[0] = this._fast_mode;
            return BioMiniAndroid.ECODE.OK;
         case 302:
            value[0] = this._security_level;
            return BioMiniAndroid.ECODE.OK;
         case 312:
            if (this.getProductId() != 1032 && this.getProductId() != 1033) {
               return BioMiniAndroid.ECODE.ERR_NOT_SUPPORTED;
            }

            value[0] = this._detect_fake;
            return BioMiniAndroid.ECODE.OK;
         case 321:
            value[0] = this._auto_rotate;
            return BioMiniAndroid.ECODE.OK;
         case 401:
            value[0] = this._detect_core;
            return BioMiniAndroid.ECODE.OK;
         case 501:
            value[0] = this._enable_autosleep ? 1 : 0;
            return BioMiniAndroid.ECODE.OK;
         default:
            return BioMiniAndroid.ECODE.ERROR;
         }
      }
   }

   public BioMiniAndroid.ECODE UFA_Verify(byte[] pTemplate1, int nTemplate1Size, byte[] pTemplate2, int nTemplate2Size, int[] nResult) {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         boolean res = this.mCurrentDevice.verify(pTemplate1, nTemplate1Size, pTemplate2, nTemplate2Size);
         if (res) {
            nResult[0] = 1;
            return BioMiniAndroid.ECODE.OK;
         } else {
            nResult[0] = 0;
            return BioMiniAndroid.ECODE.ERROR;
         }
      }
   }

   public boolean UFA_IsCapturing() {
      return this.mCurrentDevice == null ? false : this.mCurrentDevice.isCapturing();
   }

   public BioMiniAndroid.ECODE UFA_StartCapturing() {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else if (this.mCurrentDevice.isCapturing()) {
         D("startCapturing : Cannot start capturing (another capturing processing is on going...)");
         return BioMiniAndroid.ECODE.ERR_CAPTURE_RUNNING;
      } else {
         IBioMiniDevice.ErrorCode errcode = IBioMiniDevice.ErrorCode.OK;
         IBioMiniDevice.CaptureOption opts = new IBioMiniDevice.CaptureOption();
         opts.captureTimeout = this._timeout;
         opts.captureImage = true;
         final boolean[] IsDoneCapture = new boolean[]{false};
         final boolean[] IsErrorOccured = new boolean[]{false};
         final int[] errorcode = new int[]{0};
         this.mCurrentDevice.startCapturing(opts, new ICaptureResponder() {
            public void onCapture(Object context, IBioMiniDevice.FingerState fingerState) {
            }

            public boolean onCaptureEx(Object context, Bitmap capturedImage, IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
               byte[] pImage_rawx = null;
               byte[] pImage_raw = BioMiniAndroid.this.mCurrentDevice.getCaptureImageAsRAW_8();
               if (pImage_raw != null) {
                  BioMiniAndroid.D("Captured Image buffer. size(" + pImage_raw.length + ")");
                  BioMiniAndroid.D("Captured Image buffer. [" + Arrays.toString(pImage_raw));
                  BioMiniAndroid.D(" Width : " + BioMiniAndroid.this.getImageWidth() + " / Height  :" + BioMiniAndroid.this.getImageHeight() + " / FingerState : " + fingerState.isFingerExist);
                  BioMiniAndroid.this.mCallbackHandler.onCaptureCallback(pImage_raw, BioMiniAndroid.this.getImageWidth(), BioMiniAndroid.this.getTargetHeight(), 500, fingerState.isFingerExist);
               } else {
                  BioMiniAndroid.D(" null buffer.");
               }

               return true;
            }

            public void onCaptureError(Object context, int errorCode, String error) {
               BioMiniAndroid.E("ErrCode : " + errorCode + " msg : " + error);
               IsDoneCapture[0] = true;
               IsErrorOccured[0] = true;
               errorcode[0] = errorCode;
            }
         });
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_CaptureSingle(byte[] pImage) {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         IBioMiniDevice.ErrorCode errcode = IBioMiniDevice.ErrorCode.OK;
         IBioMiniDevice.CaptureOption opts = new IBioMiniDevice.CaptureOption();
         opts.captureTimeout = this._timeout;
         opts.captureImage = true;
         final boolean[] IsDoneCapture = new boolean[]{false};
         final boolean[] IsErrorOccured = new boolean[]{false};
         final int[] errorcode = new int[]{0};
         this.mCurrentDevice.captureSingle(opts, new ICaptureResponder() {
            public void onCapture(Object context, IBioMiniDevice.FingerState fingerState) {
               BioMiniAndroid.D("CaptureSIngle fingerstate : " + fingerState.isFingerExist);
               IsDoneCapture[0] = true;
            }

            public boolean onCaptureEx(Object context, Bitmap capturedImage, IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
               return false;
            }

            public void onCaptureError(Object context, int errorCode, String error) {
               BioMiniAndroid.E("ErrCode : " + errorCode + " msg : " + error);
               IsDoneCapture[0] = true;
               IsErrorOccured[0] = true;
               errorcode[0] = errorCode;
            }
         }, false);
         int nLimit = this._timeout / this.mCycleMilliSec;
         int nCnt = false;
         D("cycleMilliSec : " + this.mCycleMilliSec + " / _limit : " + nLimit);

         while(this.mCurrentDevice.isCapturing()) {
            SystemClock.sleep((long)this.mCycleMilliSec);
            D("Capture is running... wait:" + this.mCycleMilliSec);
         }

         D(" IsDoneCapture  : " + IsDoneCapture[0] + " / IsErrorUccured :" + IsErrorOccured[0]);
         IBioMiniDevice.ErrorCode ret = this.mCurrentDevice.getLastError();
         if (ret != IBioMiniDevice.ErrorCode.OK) {
            return this.ErrorCodeToECODE(ret);
         } else {
            D("Capture Success..");
            byte[] _pImage = this.mCurrentDevice.getCaptureImageAsRAW_8();
            D("captured data length : " + _pImage.length);
            System.arraycopy(_pImage, 0, pImage, 0, this.getImageHeight() * this.getImageWidth());
            return BioMiniAndroid.ECODE.OK;
         }
      }
   }

   public BioMiniAndroid.ECODE UFA_SetTemplateType(BioMiniAndroid.TEMPLATE_TYPE _templateType) {
      D("input TemplateType : " + _templateType + " (2001:suprema / 2002 : ISO / 2003 ANSI)");
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         this.mTemplateType = _templateType;
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_GetTemplateType(BioMiniAndroid.TEMPLATE_TYPE[] _templateType) {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         _templateType[0] = this.mTemplateType;
         D("GetTemplateType : " + _templateType[0] + "    // " + this.mTemplateType);
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_ExtractTemplate(byte[] poutTemplate, int[] pnTemplateSize, int[] pnQuality, int nBufferSize) {
      D("input MaxTemplate size: " + nBufferSize);
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         IBioMiniDevice.CaptureOption.ExtractOption extOpts = new IBioMiniDevice.CaptureOption.ExtractOption();
         extOpts.Rotate = this._ext_rotate == 1;
         extOpts.captureTemplate = true;
         if (nBufferSize == IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_1024.value()) {
            extOpts.maxTemplateSize = IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_1024;
         } else if (nBufferSize == IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_384.value()) {
            extOpts.maxTemplateSize = IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_384;
         }

         IBioMiniDevice.Parameter param = new IBioMiniDevice.Parameter();
         param.type = IBioMiniDevice.ParameterType.TEMPLATE_TYPE;
         param.value = (long)this.mTemplateType.value();
         this.mCurrentDevice.setParameter(param);
         IBioMiniDevice.TemplateData _templateData = this.mCurrentDevice.extractTemplate();
         if (_templateData.data.length == 0) {
            return BioMiniAndroid.ECODE.ERR_EXTRACTION_FAILED;
         } else {
            System.arraycopy(_templateData.data, 0, poutTemplate, 0, _templateData.data.length);
            pnTemplateSize[0] = _templateData.data.length;
            pnQuality[0] = _templateData.quality;
            D("============================================================================================================");
            D("[TEMPLATEDATA] : " + Arrays.toString(poutTemplate) + "\n");
            D("[Datalength ] : " + pnTemplateSize[0] + "\n");
            D("[Template Quality] : " + pnQuality[0] + "\n");
            D("============================================================================================================");
            return BioMiniAndroid.ECODE.OK;
         }
      }
   }

   public BioMiniAndroid.ECODE UFA_GetFeatureNumber(byte[] pTemplate, int nTemplate_size, int[] pnOutFeature_number) {
      pnOutFeature_number[0] = this.mCurrentDevice.getFeatureNumber(pTemplate, nTemplate_size);
      D("inTemplate : \n" + Arrays.toString(pTemplate));
      D("nInTemplateSize : " + nTemplate_size);
      D("outFeatureNumber :" + pnOutFeature_number[0]);
      return BioMiniAndroid.ECODE.OK;
   }

   public BioMiniAndroid.ECODE UFA_GetFPQuality(byte[] FPImage, int nWidth, int nHeight, int[] FPQuality, int nFPQualityMode) {
      if (FPImage == null) {
         return BioMiniAndroid.ECODE.ERR_INVALID_PARAMETERS;
      } else {
         FPQuality[0] = this.mCurrentDevice.getFPQuality(FPImage, nWidth, nHeight, nFPQualityMode);
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_GetCoreCoordinate(int[] coord) {
      if (coord.length < 2) {
         return BioMiniAndroid.ECODE.ERROR;
      } else {
         return this.GetCoreCoordinate(coord) == 0 ? BioMiniAndroid.ECODE.OK : BioMiniAndroid.ECODE.ERROR;
      }
   }

   public BioMiniAndroid.ECODE UFA_AbortCapturing() {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         D("AbortCapture()");
         this.mCurrentDevice.abortCapturing();
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_Uninit() {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         D("Uninit()");
         this.mCurrentDevice = null;
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public String UFA_GetVersionString() {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED.toString();
      } else {
         return this.mCurrentDevice == null ? BioMiniAndroid.ECODE.ERROR.toString() : String.valueOf(this.mFactory.getSDKInfo());
      }
   }

   public String UFA_GetErrorString(BioMiniAndroid.ECODE res) {
      switch(res.value()) {
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
         return "Fake finger detected";
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
      case -1:
         return "ERROR";
      case 0:
         return "OK";
      default:
         return "Code Error";
      }
   }

   public BioMiniAndroid.ECODE UFA_SetCallback(IBioMiniCallback callbackHandler) {
      if (this.mCurrentDevice == null) {
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      } else {
         this.mCallbackHandler = callbackHandler;
         return BioMiniAndroid.ECODE.OK;
      }
   }

   public BioMiniAndroid.ECODE UFA_SetDeviceCallback(IBioMiniDeviceCallback callbackHandler) {
      this.mCallbackDeviceHandler = callbackHandler;
      return BioMiniAndroid.ECODE.OK;
   }

   public int UFA_GetCompanyID(byte[] CID) {
      int ufa_res = BioMiniAndroid.ECODE.ERROR.value();
      CID = this.mCurrentDevice.getCompanyID().getBytes();
      return ufa_res;
   }

   private byte[] OC4_GetCompanyID() {
      byte[] buffer = new byte[64];
      return this.mUsbHandler.controlRx(199, buffer, 2) ? buffer : null;
   }

   public static boolean isSupported(int vid, int pid) {
      if (vid == 5841) {
         if (pid == 1031 || pid == 1030 || pid == 1033 || pid == 1032) {
            return true;
         }

         if (pid == 1056 || pid == 1057) {
            return false;
         }
      }

      return false;
   }

   public BioMiniAndroid.ECODE ErrorCodeToECODE(IBioMiniDevice.ErrorCode err) {
      switch(err.value()) {
      case -411:
         return BioMiniAndroid.ECODE.ERR_TEMPLATE_TYPE;
      case -359:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_LEFT_BOTTOM;
      case -358:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_BOTTOM;
      case -357:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_RIGHT_BOTTOM;
      case -356:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_RIGHT;
      case -355:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_RIGHT_TOP;
      case -354:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_TOP;
      case -353:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_LEFT_TOP;
      case -352:
         return BioMiniAndroid.ECODE.ERR_CORE_TO_LEFT;
      case -351:
         return BioMiniAndroid.ECODE.ERR_CORE_NOT_DETECTED;
      case -302:
         return BioMiniAndroid.ECODE.ERR_EXTRACTION_FAILED;
      case -214:
         return BioMiniAndroid.ECODE.ERR_FAKE_FINGER;
      case -213:
         return BioMiniAndroid.ECODE.ERR_NOT_CAPTURED;
      case -212:
         return BioMiniAndroid.ECODE.ERR_CAPTURE_FAILED;
      case -211:
         return BioMiniAndroid.ECODE.ERR_CAPTURE_RUNNING;
      case -206:
         return BioMiniAndroid.ECODE.ERR_PERMISSION_DENIED;
      case -205:
         return BioMiniAndroid.ECODE.ERR_NO_DEVICE;
      case -202:
         return BioMiniAndroid.ECODE.ERR_NOT_INITIALIZED;
      case -201:
         return BioMiniAndroid.ECODE.ERR_ALREADY_INITIALIZED;
      case -112:
         return BioMiniAndroid.ECODE.ERR_INVALID_PARAMETERS;
      case -111:
         return BioMiniAndroid.ECODE.ERR_NOT_SUPPORTED;
      case -102:
         return BioMiniAndroid.ECODE.ERR_LICENSE_NOT_MATCH;
      case -11:
         return BioMiniAndroid.ECODE.ERR_CAPTURE_TIMEOUT;
      case -1:
         return BioMiniAndroid.ECODE.ERROR;
      case 0:
         return BioMiniAndroid.ECODE.OK;
      case 4001:
         return BioMiniAndroid.ECODE.ERR_FILE_EXIST_ALREADY;
      default:
         return BioMiniAndroid.ECODE.ERR_UNKNOWN;
      }
   }

   public int getProductId() {
      return ((BioMiniBase)this.mCurrentDevice).mProductId;
   }

   public int GetFPQuality(byte[] FPImage, int nWidth, int nHeight, int[] FPQuality, int nFPQualityMode) {
      return -111;
   }

   private native int SetTemplateType(int var1);

   private native int GetCoreCoordinate(int[] var1);

   private native int SwitchScanningMode(int var1);

   private native int GetFeatureNumber(byte[] var1, int var2, int[] var3);

   public static enum TEMPLATE_TYPE {
      SUPREMA(2001),
      ISO19794_2(2002),
      ANSI378(2003);

      private int mValue;

      private TEMPLATE_TYPE(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }
   }

   public static enum ECODE {
      OK(0),
      ERROR(-1),
      ERR_LICENSE_NOT_MATCH(-102),
      ERR_NOT_SUPPORTED(-111),
      ERR_INVALID_PARAMETERS(-112),
      ERR_ALREADY_INITIALIZED(-201),
      ERR_NOT_INITIALIZED(-202),
      ERR_NO_DEVICE(-205),
      ERR_PERMISSION_DENIED(-206),
      ERR_CAPTURE_RUNNING(-211),
      ERR_CAPTURE_FAILED(-212),
      ERR_NOT_CAPTURED(-213),
      ERR_EXTRACTION_FAILED(-302),
      ERR_TEMPLATE_TYPE(-411),
      ERR_FILE_EXIST_ALREADY(4001),
      ERR_CORE_NOT_DETECTED(-351),
      ERR_CORE_TO_LEFT(-352),
      ERR_CORE_TO_LEFT_TOP(-353),
      ERR_CORE_TO_TOP(-354),
      ERR_CORE_TO_RIGHT_TOP(-355),
      ERR_CORE_TO_RIGHT(-356),
      ERR_CORE_TO_RIGHT_BOTTOM(-357),
      ERR_CORE_TO_BOTTOM(-358),
      ERR_CORE_TO_LEFT_BOTTOM(-359),
      ERR_FAKE_FINGER(-214),
      ERR_CAPTURE_TIMEOUT(-11),
      ERR_UNKNOWN(-1);

      private int mValue;

      private ECODE(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      public static BioMiniAndroid.ECODE fromInt(int value) {
         switch(value) {
         case -411:
            return ERR_TEMPLATE_TYPE;
         case -359:
            return ERR_CORE_TO_LEFT_BOTTOM;
         case -358:
            return ERR_CORE_TO_BOTTOM;
         case -357:
            return ERR_CORE_TO_RIGHT_BOTTOM;
         case -356:
            return ERR_CORE_TO_RIGHT;
         case -355:
            return ERR_CORE_TO_RIGHT_TOP;
         case -354:
            return ERR_CORE_TO_TOP;
         case -353:
            return ERR_CORE_TO_LEFT_TOP;
         case -352:
            return ERR_CORE_TO_LEFT;
         case -351:
            return ERR_CORE_NOT_DETECTED;
         case -302:
            return ERR_EXTRACTION_FAILED;
         case -214:
            return ERR_FAKE_FINGER;
         case -213:
            return ERR_NOT_CAPTURED;
         case -212:
            return ERR_CAPTURE_FAILED;
         case -211:
            return ERR_CAPTURE_RUNNING;
         case -206:
            return ERR_PERMISSION_DENIED;
         case -205:
            return ERR_NO_DEVICE;
         case -202:
            return ERR_NOT_INITIALIZED;
         case -201:
            return ERR_ALREADY_INITIALIZED;
         case -112:
            return ERR_INVALID_PARAMETERS;
         case -111:
            return ERR_NOT_SUPPORTED;
         case -102:
            return ERR_LICENSE_NOT_MATCH;
         case -11:
            return ERR_CAPTURE_TIMEOUT;
         case -1:
            return ERROR;
         case 0:
            return OK;
         case 4001:
            return ERR_FILE_EXIST_ALREADY;
         default:
            return ERR_UNKNOWN;
         }
      }
   }

   public static enum SCANNER_OPTIONS {
      PLUS2_SCANNING_MODE_FULL(0),
      PLUS2_SCANNING_MODE_CROP(1);

      private int mValue;

      private SCANNER_OPTIONS(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      static boolean isValid(int value) {
         return value >= 0 && value <= 1;
      }

      public static BioMiniAndroid.SCANNER_OPTIONS fromInt(int value) {
         switch(value) {
         case 0:
            return PLUS2_SCANNING_MODE_FULL;
         case 1:
            return PLUS2_SCANNING_MODE_CROP;
         default:
            return PLUS2_SCANNING_MODE_CROP;
         }
      }
   }

   public static enum PARAM {
      TIMEOUT(201),
      SENSITIVITY(203),
      SCANNING_MODE(220),
      FAST_MODE(301),
      SECURITY_LEVEL(302),
      DETECT_FAKE(312),
      AUTO_ROTATE(321),
      DETECT_CORE(401),
      ENABLE_AUTOSLEEP(501);

      private int mValue;

      private PARAM(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }
   }
}
