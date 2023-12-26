package com.suprema;

import com.suprema.util.IBridgeCallback;

public interface IBioMiniDevice {
   boolean isCapturing();

   int abortCapturing();

   int startCapturing(IBioMiniDevice.CaptureOption var1, ICaptureResponder var2);

   boolean captureSingle(IBioMiniDevice.CaptureOption var1, ICaptureResponder var2, boolean var3);

   int captureAuto(IBioMiniDevice.CaptureOption var1, ICaptureResponder var2);

   boolean setParameter(IBioMiniDevice.Parameter var1);

   IBioMiniDevice.Parameter getParameter(IBioMiniDevice.ParameterType var1);

   byte[] getCapturedBuffer(IBioMiniDevice.ImageOptions var1);

   boolean clearCaptureImageBuffer();

   int getImageWidth();

   int getImageHeight();

   IBioMiniDevice.TemplateData extractTemplate(IBioMiniDevice.CaptureOption.ExtractOption var1);

   IBioMiniDevice.TemplateData extractTemplate();

   boolean verify(byte[] var1, int var2, byte[] var3, int var4);

   boolean verify(byte[] var1, byte[] var2);

   byte[] getCaptureImageAs19794_4();

   byte[] getCaptureImageAsWsq(int var1, int var2, float var3, int var4);

   byte[] getCaptureImageAsBmp();

   byte[] getCaptureImageAsRAW_8();

   int getFeatureNumber(byte[] var1, int var2);

   int getFPQuality(byte[] var1, int var2, int var3, int var4);

   String getCompanyID();

   IBioMiniDevice.DeviceInfo getDeviceInfo();

   void setBridgeCallback(IBridgeCallback var1);

   void makeBridge(Object var1);

   int[] getCoreCoordinate();

   void setEncryptionKey(byte[] var1);

   byte[] encrypt(byte[] var1);

   byte[] decrypt(byte[] var1);

   IBioMiniDevice.ErrorCode getLastError();

   String popPerformanceLog();

   boolean isEqual(Object var1);

   IBioMiniDevice.ErrorCode setPacketMode(IBioMiniDevice.PacketMode var1);

   boolean canChangePacketMode();

   public static class DeviceInfo {
      public String deviceName;
      public String deviceSN;
      public String versionSDK;
      public IBioMiniDevice.ScannerType scannerType;
   }

   public static class FingerState {
      public boolean isFingerExist;

      public FingerState(boolean is_finger_exist) {
         this.isFingerExist = is_finger_exist;
      }
   }

   public static class TemplateData {
      public byte[] data;
      public IBioMiniDevice.TemplateType type;
      public int quality;

      public TemplateData(byte[] _data, IBioMiniDevice.TemplateType _type, int _quality) {
         this.data = _data;
         this.type = _type;
         this.quality = _quality;
      }
   }

   public static class Parameter {
      public IBioMiniDevice.ParameterType type;
      public long value;

      public Parameter(IBioMiniDevice.ParameterType type, long value) {
         this.type = type;
         this.value = value;
      }

      public Parameter() {
         this.type = IBioMiniDevice.ParameterType.INVALID;
         this.value = 0L;
      }
   }

   public static class ImageOptions {
      public IBioMiniDevice.ImageType imageType;
      public float compressionRatio;
   }

   public static class CaptureOption {
      public int captureTimeout = -1;
      public boolean captureImage = true;
      public IBioMiniDevice.CaptureOption.ExtractOption extractParam = new IBioMiniDevice.CaptureOption.ExtractOption();
      /** @deprecated */
      @Deprecated
      public boolean captureTemplate = true;
      public IBioMiniDevice.FrameRate frameRate;

      public CaptureOption() {
         this.frameRate = IBioMiniDevice.FrameRate.SHIGH;
      }

      public CaptureOption(int timeout, boolean capture_image, boolean capture_template, IBioMiniDevice.FrameRate frame_rate) {
         this.frameRate = IBioMiniDevice.FrameRate.SHIGH;
         this.captureTimeout = timeout;
         this.captureImage = capture_image;
         this.extractParam.captureTemplate = capture_template;
         this.frameRate = frame_rate;
      }

      public CaptureOption(int timeout, boolean capture_image, IBioMiniDevice.CaptureOption.ExtractOption extract_Opts, IBioMiniDevice.FrameRate frame_rate) {
         this.frameRate = IBioMiniDevice.FrameRate.SHIGH;
         this.captureTimeout = timeout;
         this.captureImage = capture_image;
         this.extractParam = extract_Opts;
         this.frameRate = frame_rate;
      }

      public static class ExtractOption {
         public boolean captureTemplate = false;
         public boolean Rotate = false;
         public IBioMiniDevice.MaxTemplateSize maxTemplateSize;

         public ExtractOption() {
            this.maxTemplateSize = IBioMiniDevice.MaxTemplateSize.MAX_TEMPLATE_1024;
         }
      }
   }

   public static enum PacketMode {
      PACKET_64(1),
      PACKET_512(2);

      int _packetMode = 1;

      private PacketMode(int packetMode) {
         this._packetMode = packetMode;
      }

      public int value() {
         return this._packetMode;
      }
   }

   public static enum ScannerType {
      BIOMINI(4, IBioMiniDevice.ScannerClass.UNIVERSIAL_DEVICE),
      BIOMINI_SLIM(5, IBioMiniDevice.ScannerClass.UNIVERSIAL_DEVICE),
      BIOMINI_PLUS2(6, IBioMiniDevice.ScannerClass.UNIVERSIAL_DEVICE),
      BIOMINI_SLIM2(7, IBioMiniDevice.ScannerClass.UNIVERSIAL_DEVICE),
      BIOMINI_SLIMS(10, IBioMiniDevice.ScannerClass.HID_DEVICE),
      BIOMINI_SLIM2S(11, IBioMiniDevice.ScannerClass.HID_DEVICE);

      int _type;
      IBioMiniDevice.ScannerClass _class;

      private ScannerType(int _type, IBioMiniDevice.ScannerClass _class) {
         this._type = _type;
         this._class = _class;
      }

      public IBioMiniDevice.ScannerClass getDeviceClass() {
         return this._class;
      }

      public int getDeviceType() {
         return this._type;
      }
   }

   public static enum ScannerClass {
      UNIVERSIAL_DEVICE,
      HID_DEVICE;
   }

   public static enum TransferMode {
      MODE1,
      MODE2;
   }

   public static enum MaxTemplateSize {
      MAX_TEMPLATE_1024(0),
      MAX_TEMPLATE_384(1);

      private int mValue;

      private MaxTemplateSize(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      public static IBioMiniDevice.MaxTemplateSize fromInt(int value) {
         IBioMiniDevice.MaxTemplateSize[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            IBioMiniDevice.MaxTemplateSize mt = var1[var3];
            if (value == mt.value()) {
               return mt;
            }
         }

         return MAX_TEMPLATE_1024;
      }
   }

   public static enum ScanningMode {
      SCANNING_MODE_FULL(0),
      SCANNING_MODE_CROP(1);

      private int mValue;

      private ScanningMode(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      public static boolean isValid(int value) {
         return value >= 0 && value <= 1;
      }

      public static IBioMiniDevice.ScanningMode fromInt(int value) {
         IBioMiniDevice.ScanningMode[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            IBioMiniDevice.ScanningMode so = var1[var3];
            if (value == so.value()) {
               return so;
            }
         }

         return SCANNING_MODE_CROP;
      }
   }

   public static enum ErrorCode {
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
      ERR_UNKNOWN(-1),
      CTRL_ERR_FAIL(-1),
      CTRL_ERR_UNKNOWN(-2),
      CTRL_ERR_INVALID_PARAM(-3),
      CTRL_ERR_UNSUPPORT_CMD(-4),
      CTRL_ERR_NOT_AUTHORIZED(-5),
      CTRL_ERR_INVALID_MODE(-6),
      CTRL_ERR_MEM_ALLOC(-7),
      CTRL_ERR_INVALID_SETTING(-8),
      CTRL_ERR_MEM_OUT(-9),
      CTRL_ERR_GET_DATA(-10),
      CTRL_ERR_NEED_REBOOT(-11),
      CTRL_ERR_SYSTEM_IS_NOT_LOADED(-12),
      CTRL_ERR_FILE_EMPTY(-13),
      CTRL_ERR_PROCESS_CORRUPT(-20),
      CTRL_ERR_CAPPROC_FAILED(-21),
      CTRL_ERR_CAPPROC_CANNOT_START(-22),
      CTRL_ERR_CAPPROC_CORRUPT(-23),
      CTRL_ERR_INVALID_ID(-30),
      CTRL_ERR_INVALID_AUTH(-31),
      CTRL_ERR_INVALID_PWD(-32),
      CTRL_ERR_NO_LOGGED_USER(-34),
      CTRL_ERR_INVALID_TEMPLATE(-35),
      CTRL_ERR_ALREADY_USER_EXIST(-36),
      CTRL_ERR_NO_ENROLLED_USER(-37),
      CTRL_ERR_CAPTURE_TIMEOUT(-40),
      CTRL_ERR_IS_CAPTURING(-41),
      CTRL_ERR_BUFFER_IS_NOT_READY(-42),
      CTRL_ERR_FAKE_FINGER(-43),
      CTRL_ERR_NO_FINGER_FOUND(-44),
      CTRL_ERR_CAPTURE_IS_NOT_RUNNING(-45),
      CTRL_ERR_LFD_FAILED(-46),
      CTRL_ERR_CAPTURE_ABORTED(-47),
      CTRL_ERR_CHECK_STATUS(-48),
      CTRL_ERR_EXTRACTION_FAILED(-60),
      CTRL_ERR_TEMPLATE_ENCRYPT(-61),
      CTRL_ERR_TEMPLATE_DECRYPT(-62),
      CTRL_ERR_VERIFY_FAILED(-63),
      CTRL_ERR_VERIFY_NOT_MATCHED(-64),
      CTRL_ERR_GET_QUALITY(-65),
      CTRL_ERR_BASE64_ENCODING_FAILED(-66),
      CTRL_ERR_NO_ENROLLED_DATA(-67),
      CTRL_ERR_ENCRYPT_OPTION_IS_NOT_SET(-68),
      CTRL_ERR_TEMPLATE_TYPE_NOT_IDENTICAL(-69),
      CTRL_ERR_IMAGE_ENCODING_FAILED(-70),
      CTRL_ERR_FORMATING_FAILED(-71),
      CTRL_ERR_DB_ACCESS_FAIL(-72),
      CTRL_ERR_IDENTIFY_NOT_MATCHED(-73),
      CTRL_ERR_DB_INTERNAL_ERROR(-74),
      CTRL_ERR_IDENTIFY_IS_NOT_RUNNING(-75),
      CTRL_ERR_WEB_READ_DATA(-85),
      CTRL_ERR_WEB_WRITE_DATA(-86),
      CTRL_ERR_DEVICE_MALFUNCTIONING(-90),
      CTRL_ERR_CANNOT_FIND_PERIPHERAL(-91),
      CTRL_ERR_SYS_CONFIG_CORRUPT(-92),
      CTRL_ERR_PERIPHERAL_CTRL_FAILED(-93),
      CTRL_ERR_SET_CONFIG_VALUE(-101),
      CTRL_ERR_GET_CONFIG_VALUE(-102),
      ECH_ERR_GENERAL(-110),
      ECH_ERR_NACK(-111),
      ECH_ERR_NOT_RESPOND(-112),
      ECH_ERR_TIMEOUT(-113),
      ECH_ERR_NOT_INITIALIZED(-114),
      ECH_ERR_ABNORMAL_STATE(-115),
      ECH_ERR_INVALID_COMMAND(-116),
      ECH_ERR_INVALID_PARAMETER(-117),
      ECH_ERR_USB_IO(-118),
      ECH_ERR_INVALID_PROTOCOL(-119),
      ECH_ERR_PERMISSION_DENIED(-120),
      ECH_ERR_NO_DEVICE_FOUND(-121),
      ECH_WRN_GENERAL(110),
      ECH_WRN_ALREADY_DONE(111);

      private int mValue;

      private ErrorCode(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      public static IBioMiniDevice.ErrorCode fromInt(int value) {
         IBioMiniDevice.ErrorCode[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            IBioMiniDevice.ErrorCode error = var1[var3];
            if (value == error.value()) {
               return error;
            }
         }

         return ERROR;
      }
   }

   public static enum TemplateType {
      SUPREMA(2001),
      ISO19794_2(2002),
      ANSI378(2003);

      private int mValue;

      private TemplateType(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }

      public static IBioMiniDevice.TemplateType fromInt(int value) {
         IBioMiniDevice.TemplateType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            IBioMiniDevice.TemplateType tt = var1[var3];
            if (value == tt.value()) {
               return tt;
            }
         }

         return SUPREMA;
      }
   }

   public static enum ParameterType {
      TIMEOUT(201, IBioMiniDevice.ParameterValueType.INT),
      SENSITIVITY(203, IBioMiniDevice.ParameterValueType.INT),
      SCANNING_MODE(220, IBioMiniDevice.ParameterValueType.INT),
      FAST_MODE(301, IBioMiniDevice.ParameterValueType.INT),
      SECURITY_LEVEL(302, IBioMiniDevice.ParameterValueType.INT),
      DETECT_FAKE(312, IBioMiniDevice.ParameterValueType.INT),
      AUTO_ROTATE(321, IBioMiniDevice.ParameterValueType.INT),
      DETECT_CORE(401, IBioMiniDevice.ParameterValueType.INT),
      TEMPLATE_TYPE(402, IBioMiniDevice.ParameterValueType.INT),
      ENABLE_AUTOSLEEP(501, IBioMiniDevice.ParameterValueType.INT),
      EXT_TRIGGER(601, IBioMiniDevice.ParameterValueType.INT),
      EXTRACT_MODE_BIOSTAR(450, IBioMiniDevice.ParameterValueType.INT),
      IMGAE_FLIP(701, IBioMiniDevice.ParameterValueType.INT),
      INVALID(-1, IBioMiniDevice.ParameterValueType.INT);

      private int mValue;
      private IBioMiniDevice.ParameterValueType mType;

      private ParameterType(int value, IBioMiniDevice.ParameterValueType type) {
         this.mValue = value;
         this.mType = type;
      }

      public int value() {
         return this.mValue;
      }

      public IBioMiniDevice.ParameterValueType type() {
         return this.mType;
      }
   }

   public static enum ParameterValueType {
      INT,
      STRING;
   }

   public static enum FrameRate {
      LOW,
      MID,
      HIGH,
      ELOW,
      SHIGH,
      SLOW,
      DEFAULT;
   }

   public static enum ImageType {
      RAW_8,
      BITMAP,
      WSQ;
   }
}
