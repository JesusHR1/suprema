package com.suprema.hid;

import com.suprema.IBioMiniDevice;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public final class Hid {
   public static byte[] byteToBytes(int short1) {
      byte[] re = new byte[]{(byte)(short1 & 255)};
      return re;
   }

   public static byte[] shortToBytes(int short1) {
      byte[] re = new byte[]{(byte)(short1 >> 8 & 255), (byte)(short1 & 255)};
      return re;
   }

   public static byte[] intToBytes(int int1) {
      byte[] re = new byte[]{(byte)(int1 >> 24 & 255), (byte)(int1 >> 16 & 255), (byte)(int1 >> 8 & 255), (byte)(int1 & 255)};
      return re;
   }

   public static byte[] stringToBytes(String str) {
      byte[] re = new byte[8];
      re = str.getBytes(Charset.forName("UTF-8"));
      return re;
   }

   public static void putByte(byte[] dest, int off, int byte1, int byte2) {
      dest[off] = (byte)byte1;
      dest[off + 1] = (byte)byte2;
   }

   public static void putShort(byte[] dest, int off, int short1) {
      dest[off] = (byte)(short1 >> 8 & 255);
      dest[off + 1] = (byte)(short1 & 255);
   }

   public static void putInt(byte[] dest, int off, int int1) {
      dest[off] = (byte)(int1 >> 24 & 255);
      dest[off + 1] = (byte)(int1 >> 16 & 255);
      dest[off + 2] = (byte)(int1 >> 8 & 255);
      dest[off + 3] = (byte)(int1 & 255);
   }

   public static void putReverseInt(byte[] dest, int off, int int1) {
      dest[off + 3] = (byte)(int1 >> 24 & 255);
      dest[off + 2] = (byte)(int1 >> 16 & 255);
      dest[off + 1] = (byte)(int1 >> 8 & 255);
      dest[off] = (byte)(int1 & 255);
   }

   public static int retrieveInt(byte[] src, int off) {
      int s1 = src[off] & 255;
      s1 <<= 24;
      int s2 = src[off + 1] & 255;
      s2 <<= 16;
      int s3 = src[off + 2] & 255;
      s3 <<= 8;
      int s4 = src[off + 3] & 255;
      return s1 | s2 | s3 | s4;
   }

   public static int retrieveShort(byte[] src, int off) {
      int s1 = src[off] & 255;
      s1 <<= 8;
      int s2 = src[off + 1] & 255;
      return s1 | s2;
   }

   public static int retrieveByte(byte[] src, int off) {
      int s1 = src[off] & 255;
      return s1;
   }

   public static String retrieveString(byte[] src, int off, int len) {
      byte[] ss = new byte[len];
      System.arraycopy(src, off, ss, 0, len);

      String str;
      try {
         str = (new String(ss, "UTF-8")).trim();
      } catch (UnsupportedEncodingException var6) {
         str = "";
      }

      return str;
   }

   public static String errString(Error err) {
      return err.toString();
   }

   public static String errString(int err) {
      IBioMiniDevice.ErrorCode[] var1 = IBioMiniDevice.ErrorCode.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         IBioMiniDevice.ErrorCode error = var1[var3];
         if (err == error.value()) {
            return error.toString();
         }
      }

      return "UnKnown";
   }

   public static enum Extra {
      USB_PACKET_64(64),
      USB_PACKET_512(512);

      private final int mValue;

      private Extra(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }
   }

   public static enum Sub {
      SUB_NA(0),
      CCT_NA(0),
      CCT_SINGLE(1),
      CCT_LOOP(2),
      CCT_PREVIEW(3),
      CCT_STOP(16),
      CSI_RAW_IMAGE(2),
      CST_SUPREMA_TEMPLATE(1),
      CST_ENC_SUPREMA_TEMPLATE(4097),
      CST_ISO_TEMPLATE(257),
      CST_ENC_ISO_TEMPLATE(4353),
      CST_ANSI_TEMPLATE(513),
      CST_ENC_ANSI_TEMPLATE(4609),
      CDT_NA(0),
      CDT_RESET_DEVICE(2),
      CDT_SET_SLEEP_MODE(12),
      CDT_GET_SLEEP_MODE(13),
      CDT_START_FW_SEND(21),
      CDT_FINISH_FW_SEND(22),
      CDT_START_DB_SEND(28),
      CDT_FINISH_DB_SEND(29),
      CTI_DB_IMAGE(3),
      CTI_FW_IMAGE(4),
      DBI_CAPTURED_TEMPLATE(1),
      DBI_TRANSFERED_TEMPLATE(2),
      DBI_CAPTURED_IMAGE(17),
      DBI_TRANSFERED_IMAGE(18),
      DBI_PREVIEW_IMAGE(19),
      DBI_CAPTURED_TEMPLATE_ENC(4097),
      DBI_TRANSFERED_TEMPLATE_ENC(4098),
      UDI_USER_LIST(33),
      UDI_TEMPLATE(34),
      DII_DB_IMANGE(49),
      CVT_TIMEOUT(1),
      CVT_SENSITIVITY(2),
      CVT_LFD_LEVEL(3),
      CVT_EX_TRIGGER(4),
      TVT_TEMPLATE_FORMAT(1),
      TVT_TEMPLATE_SIZE(2),
      VVT_SECURITY_LEVEL(1),
      IVT_AUTO_ROTATE(2),
      IVT_FAST_MODE(3),
      IVT_TIMEOUT(4),
      IVT_IMAGE_FORMAT(1),
      IVT_COMPRESS_RATIO(2),
      LVT_LFD_RESULT(2),
      UVT_USB_FULLSPEED(1),
      UVT_USB_HIGHSPEED(2),
      UVT_USB_FIDO(3),
      DVT_DB_IMPORT_NEW(1),
      DVT_DB_IMPORT_OVERWRITE(2),
      DVT_DB_IMPORT_MERGE(3),
      RVT_CAPTURE_IMG(1),
      RVT_TRANSFER_IMG(2),
      RVT_EXTRACT_TEMPLATE(3),
      RVT_TRANSFER_TEMPLATE(4),
      CNM_F1_DEFAULT(0),
      CNM_F1_SUPREMA_TEMPLATE(0),
      CNM_F1_ENC0_SUPREMA_TEMPLATE(16),
      CNM_F1_ENC2_SUPREMA_TEMPLATE(48),
      CNM_F1_ISO_TEMPLATE(1),
      CNM_F1_ENC0_ISO_TEMPLATE(17),
      CNM_F1_ENC2_ISO_TEMPLATE(49),
      CNM_F1_ANSI_TEMPLATE(2),
      CNM_F1_ENC0_ANSI_TEMPLATE(18),
      CNM_F1_ENC2_ANSI_TEMPLATE(50),
      CNM_F1_UNDEFINE_TEMPLATE(15),
      CNM_F1_UNDEFINE_ENC0_TEMPLATE(31),
      CNM_F1_UNDEFINE_ENC2_TEMPLATE(63),
      CNM_F2_LIVE_TEMPLATE(0),
      CNM_F2_TRANSFERED_TEMPLATE(1),
      CNM_F2_LAST_CAPTURE_DATA(2),
      CNM_F2_LAST_TEMPLATE(5),
      CVM_TRANSFERED_TEMPLATE(0),
      CVM_ENROLLED_TEMPLATE(1),
      CVM_ENROLLED_VS_TRANSFERED(2),
      CIM_LIVE_TEMPLATE(0),
      CIM_TRANSFERED_TEMPLATE(1),
      CIM_LAST_TEMPLATE(2),
      CUC_LOGOUT(0),
      CUC_LOGIN(1),
      CUC_CHANGE_PWD(2),
      CUC_CHECK_USER(3),
      CUC_ADD_USER(4),
      CUC_DELETE_USER(5),
      CUC_F1_ADD_NORMAL_USR(0),
      CUC_F1_ADD_POWER_USR(1),
      CUC_F2_ADD_USER(4),
      CUC_F1_DELETE_NA(0),
      CUC_F1_DELETE_SPECIFIC_USER(8),
      CUC_F1_DELETE_ADMIN(4),
      CUC_F1_DELETE_ALL_POWER_USER(2),
      CUC_F1_DELETE_ALL_NORMAL_USER(1),
      CUC_F2_DELETE_USER(5),
      CDI_DEV_INFO(0),
      CDI_MODEL_NAME(1),
      CDI_MODULE_SN(2),
      CDI_PRODUCT_SN(3),
      CEM_UNIQ_N_AES(257),
      CEM_UNIQ_N_RSA(258),
      CEM_EACH_N_AES(513),
      CEM_EACH_N_RSA(514),
      CEM_SYNC_N_AES(2049),
      CEM_SYNC_N_RSA(2050),
      CEM_NA(0),
      CBI_SUMMARY(1);

      private final int mValue;
      private final int mUserValue;

      private Sub(int value) {
         this.mValue = value;
         this.mUserValue = value;
      }

      private Sub(int value, int sec_value) {
         this.mValue = value;
         this.mUserValue = sec_value;
      }

      public int value() {
         return this.mValue;
      }

      public int userValue() {
         return this.mUserValue;
      }

      public int merge(Hid.Sub msb, Hid.Sub lsb) {
         return msb.value() << 8 & '\uff00' | lsb.value() & 255;
      }

      public int compose(int msb, int lsb) {
         return msb << 8 & '\uff00' | lsb & 255;
      }
   }

   public static enum Cmd {
      CMT_NA(0),
      CMT_CAPTURE(1),
      CMT_DEVICE_CTRL(2),
      CMT_EXTRACT(3),
      CMT_SEND_DATA(5),
      CMT_RECEIVE_DATA(6),
      CMT_USER_CTRL(16),
      CMT_SET(17),
      CMT_GET(18),
      CMT_VERIFY(32),
      CMT_ENROLL(33),
      CMT_IDENTIFY(34),
      DAT_NA(0),
      DAT_DATA_IN(16),
      DAT_DATA_OUT(32),
      VAT_NA(0),
      VAT_LED(1),
      VAT_ENCRYPT_OPT(2),
      VAT_IS_FINGER_ON(4),
      VAT_IS_TOUCH_ON(5),
      VAT_DEV_INFO(6),
      VAT_CAPTURE_OPT(7),
      VAT_TEMPLATE_OPT(8),
      VAT_VERIFIY_OPT(9),
      VAT_IMAGE_OPT(10),
      VAT_PARAM_RESET(12),
      VAT_IDENTIRY_OPT(13),
      VAT_IS_STREAM_UPDATE(14),
      VAT_RESET_DATA(15),
      VAT_USB_TYPE(17),
      VAT_HID_PROTOCOL_VER(19),
      VAT_DB_OPT(20),
      VAT_LAST_STATUS(21),
      VAT_DB_INFO(22),
      VAT_DB_IMPORT_TYPE(23),
      VAT_DB_ENCRYPT_KEY(24);

      private int mValue;

      private Cmd(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }
   }

   public static enum Pac {
      PAC_NA(0),
      PAC_CMD(1),
      PAC_SET(2),
      PAC_GET(3),
      PAC_DATA_IN(5),
      PAC_DATA_OUT(6),
      PAC_ECHO(128);

      private int mValue;

      private Pac(int value) {
         this.mValue = value;
      }

      public int value() {
         return this.mValue;
      }
   }
}
