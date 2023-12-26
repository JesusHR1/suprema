package com.suprema.usb;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.os.SystemClock;
import android.util.Log;
import com.suprema.IBioMiniDevice;
import com.suprema.IBioMiniInterops;
import com.suprema.hid.Hid;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UsbHandlerAndroidHid implements IUsbHandlerAndroidHid {
   public static int BMSS_DEFAULT_PACKET_SIZE = 512;
   public static int BMS2S_DEFAULT_PACKET_SIZE = 64;
   public static final int HID_CMD_PACKET_SIZE;
   public static final int HID_DATA_PACKET_SIZE;
   public static final int HID_DATA_PACKET_SIZE_R;
   public static final int HID_HEADER_SIZE = 5;
   public static final int HID_POS_PACCODE = 1;
   public static final int HID_POS_PACCODE_R = 1;
   static final int HID_POS_CMDCODE = 2;
   static final int HID_POS_SUBCODE = 3;
   static final int HID_POS_ERRCODE = 5;
   static final int HID_POS_DATA = 6;
   static final int HID_POS_CMDCODE_R = 2;
   static final int HID_POS_SUBCODE_R = 3;
   static final int HID_POS_ERRCODE_R = 5;
   static final int HID_POS_DATA_R = 6;
   static final int HID_REPORT_ID_ECHO = 1;
   static final int HID_REPORT_ID_CMD = 2;
   static final int HID_REPORT_ID_DATAIN = 3;
   static final int HID_REPORT_ID_DATAOUT = 4;
   private String TAG = "BioMiniSDK";
   private int TRANSFER_TIMEOUT = 140;
   private IBioMiniInterops bioMini;
   private UsbRequest[] request = null;
   private UsbRequest requestWrite = null;
   private UsbRequest requestRead = null;
   private ByteBuffer[] dataQ = null;
   private int maxTimesToRead = 0;
   private int maxBytesToRead = 0;
   private int dstOffset = 0;
   private int hidPacketType;
   private int hidMaxTransferData;
   private final boolean useReservedQueue;
   private byte[] bufQ;
   private byte[] hidPacket;
   private byte[] hidPacketFrag;
   public byte[] hidPacketEcho;
   public byte[] hidPacketEchoData;
   private UsbDeviceConnection usbConnection;
   private UsbEndpoint epIn;
   private UsbEndpoint epOut;
   private int mInitializedRequests;
   private boolean mReading;
   public boolean flagToFix;
   private static final int CMD_LED_CTRL = 194;
   private static final int CMD_READ_FRAME_A = 226;
   private static final int CMD_READ_FRAME_DONE = 239;
   private static final int CMD_SET_EEPROM = 214;
   private static final int CMD_GET_EEPROM = 215;
   private static final int CMD_SENSOR_EEPROM_GET_ADDR = 222;
   private static final int CMD_SENSOR_EEPROM_GET_DATA = 223;
   private static final int CMD_EEPROM_WP_ENABLE = 201;
   public static final int OV_IIC_EEPROM_ADDR = 174;

   public UsbHandlerAndroidHid(IBioMiniInterops bioMiniInterface, UsbDeviceConnection conn, UsbEndpoint ep_in, UsbEndpoint ep_out, int maxBulkSize) {
      this.hidPacketType = Hid.Extra.USB_PACKET_64.value();
      this.useReservedQueue = true;
      this.bufQ = new byte['耀'];
      this.usbConnection = null;
      this.epIn = null;
      this.epOut = null;
      this.mInitializedRequests = 0;
      this.mReading = false;
      this.flagToFix = false;
      this.maxTimesToRead = (maxBulkSize + 16383) / 16384;
      this.maxBytesToRead = this.maxTimesToRead * 16384;
      this.request = new UsbRequest[this.maxTimesToRead];
      this.requestWrite = new UsbRequest();
      this.requestRead = new UsbRequest();
      this.dataQ = new ByteBuffer[this.maxTimesToRead];
      this.epIn = ep_in;
      this.epOut = ep_out;
      this.usbConnection = conn;

      int i;
      for(i = 0; i < this.mInitializedRequests; ++i) {
         this.request[i].close();
      }

      this.mInitializedRequests = 0;

      for(i = 0; i < this.maxTimesToRead; ++i) {
         this.dataQ[i] = ByteBuffer.allocate(16384);
         this.request[i] = new UsbRequest();
      }

      try {
         this.requestWrite.initialize(this.usbConnection, this.epOut);
         this.requestRead.initialize(this.usbConnection, this.epIn);
      } catch (Exception var8) {
         Log.e(this.TAG, "BioMini SDK Device initializing..... " + var8.getCause() + " / " + var8.getMessage());
      }

      byte[] cmd_packet = new byte[Hid.Extra.USB_PACKET_64.value()];
      byte[] echo_packet = new byte[Hid.Extra.USB_PACKET_64.value()];
      Arrays.fill(cmd_packet, 0, Hid.Extra.USB_PACKET_64.value(), (byte)0);
      cmd_packet[0] = 2;
      cmd_packet[1] = (byte)Hid.Pac.PAC_GET.value();
      cmd_packet[2] = (byte)Hid.Cmd.VAT_USB_TYPE.value();
      if (this.write(cmd_packet, Hid.Extra.USB_PACKET_64.value()) && this.readSmall(echo_packet, Hid.Extra.USB_PACKET_64.value()) && cmd_packet[1] == (echo_packet[1] & 127) && cmd_packet[2] == echo_packet[2] && echo_packet[5] == IBioMiniDevice.ErrorCode.OK.value()) {
         if (echo_packet[6] == Hid.Sub.UVT_USB_FULLSPEED.value()) {
            this.hidPacketType = 64;
         } else {
            this.hidPacketType = 512;
         }
      }

      this.hidPacket = new byte[this.hidPacketType];
      this.hidPacketFrag = new byte[this.hidPacketType];
      this.hidPacketEcho = new byte[this.hidPacketType];
      this.hidPacketEchoData = new byte[this.hidPacketType - 5 - 1];
      this.hidMaxTransferData = (this.hidPacketType - 5 - 1) * 16383;
   }

   public void resize(int maxBulkSize) {
      int _maxTimesToRead = (maxBulkSize + 16383) / 16384;
      if (_maxTimesToRead > this.maxTimesToRead) {
         this.maxTimesToRead = _maxTimesToRead;
         this.maxBytesToRead = this.maxTimesToRead * 16384;

         int i;
         for(i = 0; i < this.mInitializedRequests; ++i) {
            this.request[i].close();
         }

         this.mInitializedRequests = 0;
         this.usbConnection.close();
         this.request = new UsbRequest[this.maxTimesToRead];
         this.dataQ = new ByteBuffer[this.maxTimesToRead];

         for(i = 0; i < this.maxTimesToRead; ++i) {
            this.dataQ[i] = ByteBuffer.allocate(16384);
            this.request[i] = new UsbRequest();
         }
      }

   }

   public boolean enumerate() {
      return false;
   }

   public int getDeviceCount() {
      return 0;
   }

   public void setCommandTimeout(int _timeout) {
   }

   public boolean open(int idx) {
      return false;
   }

   public void close() {
      if (this.mReading) {
         Log.e(this.TAG, "Ooops!! Some reading thread would be corrupted!!");
      }

      for(int i = 0; i < this.mInitializedRequests; ++i) {
         this.request[i].close();
      }

      this.usbConnection.close();
      this.usbConnection = null;
      this.mInitializedRequests = 0;
   }

   public String getPath() {
      return null;
   }

   public boolean isEqual(Object dev) {
      return false;
   }

   public boolean writeHid(byte[] buf, byte id, int len) {
      return false;
   }

   public void resetBulkPipe() {
      for(int i = 0; i < this.mInitializedRequests; ++i) {
         boolean var2 = this.request[i].cancel();
      }

   }

   public void setDstOffset(int offset) {
      this.dstOffset = offset;
   }

   public boolean initRead(int len, int skipPackets, boolean updateAlways) {
      boolean reqres = false;
      int nTimesToRead = (len + 16383) / 16384;
      nTimesToRead -= skipPackets;
      if (updateAlways || this.mInitializedRequests != nTimesToRead) {
         int i;
         for(i = 0; i < this.mInitializedRequests; ++i) {
            this.request[i].close();
         }

         if (nTimesToRead > this.request.length) {
            return false;
         }

         for(i = 0; i < nTimesToRead; ++i) {
            this.request[i] = new UsbRequest();
            reqres = this.request[i].initialize(this.usbConnection, this.epIn);
            if (!reqres) {
               return false;
            }

            this.mInitializedRequests = i + 1;
         }
      }

      return true;
   }

   public synchronized boolean read(byte[] buf, int len, int skipPackets, byte fillExtra) {
      this.mReading = true;
      int nBytesToRead = len;
      int nRemainingBytes = false;
      int nToRead = (len + 16383) / 16384;
      int nToEnqueue = len / 16384;
      boolean reqres = false;
      long CHECKING_TIME = 0L;
      int nRemainingBytes = len % 16384;
      nToRead -= skipPackets;
      if (skipPackets != 0) {
         nRemainingBytes = 0;
         nBytesToRead = nToRead * 16384;
      }

      nToEnqueue = nToRead;
      CHECKING_TIME = System.currentTimeMillis();

      int i;
      for(i = 0; i < nToEnqueue; ++i) {
         this.request[i].setClientData(this);
         this.dataQ[i].position(0);
         if (i == nToEnqueue - 1 && nRemainingBytes > 0) {
            reqres = this.request[i].queue(this.dataQ[i], nRemainingBytes);
         } else {
            reqres = this.request[i].queue(this.dataQ[i], 16384);
         }

         if (!reqres) {
            Log.e(this.TAG, "UsbHandlerAndroid result of USB request  : " + reqres);
            this.mReading = false;
            return false;
         }
      }

      for(i = 0; i < nToRead; ++i) {
         if (this.usbConnection != null) {
            try {
               this.usbConnection.requestWait();
            } catch (Exception var15) {
               var15.printStackTrace();
            }
         }
      }

      for(i = 0; i < nBytesToRead; i += 16384) {
         int posBuf = this.dataQ[i / 16384].position();
         this.dataQ[i / 16384].position(0);
         this.dataQ[i / 16384].get(this.bufQ, 0, posBuf);
         System.arraycopy(this.bufQ, 0, buf, i + this.dstOffset, posBuf);
      }

      Arrays.fill(buf, nBytesToRead, len, fillExtra);
      this.mReading = false;
      return true;
   }

   @SuppressLint({"DefaultLocale"})
   public boolean write(byte[] buf, int len) {
      this.requestWrite.setClientData(this);
      if (!this.requestWrite.queue(ByteBuffer.wrap(buf), len)) {
         return false;
      } else {
         this.usbConnection.requestWait();
         return true;
      }
   }

   @SuppressLint({"DefaultLocale"})
   public boolean readSmall(byte[] buf, int len) {
      this.requestRead.setClientData(this);
      if (!this.requestRead.queue(ByteBuffer.wrap(buf), len)) {
         return false;
      } else {
         UsbRequest request = this.usbConnection.requestWait();
         return request != null || !this.flagToFix;
      }
   }

   public boolean controlRx(int cmd, byte[] ctrlBuf, int len) {
      int re = this.usbConnection.controlTransfer(128, cmd, 0, 0, ctrlBuf, len, this.TRANSFER_TIMEOUT);
      return re == len;
   }

   public boolean controlTx(int cmd, byte[] ctrlBuf, int len) {
      int re = this.usbConnection.controlTransfer(0, cmd, 0, 0, ctrlBuf, len, this.TRANSFER_TIMEOUT);
      return re != -1;
   }

   public boolean readEEPROM(int addr, int len, byte[] buffer) {
      byte[] buf = new byte[64];
      buf[0] = 1;
      int r = this.usbConnection.controlTransfer(0, 201, 0, 0, buf, 1, 0);
      if (r == -1) {
         return false;
      } else {
         buf[0] = -82;
         buf[1] = (byte)(addr >> 8 & 255);
         buf[2] = (byte)(addr & 255);
         buf[3] = (byte)len;
         r = this.usbConnection.controlTransfer(0, 214, 0, 0, buf, 4, 0);
         if (r != -1) {
            r = this.usbConnection.controlTransfer(128, 215, 0, 0, buffer, len, 0);
         }

         if (r == -1) {
            return false;
         } else {
            buf[0] = 0;
            r = this.usbConnection.controlTransfer(0, 201, 0, 0, buf, 1, 0);
            return r != -1;
         }
      }
   }

   private boolean readSensorEEPROMOneByte(int addr, byte[] buffer) {
      boolean ret = false;
      byte[] buf = new byte[64];
      buf[0] = (byte)addr;
      int len = 64;
      if (!this.controlTx(222, buf, 1) && this.controlRx(223, buf, len)) {
         buffer[0] = buf[0];
         ret = true;
      }

      SystemClock.sleep(10L);
      return ret;
   }

   public boolean readSensorEEPROM(int addr, int len, byte[] buffer) {
      boolean ret = false;
      byte[] buf = new byte[2];
      if (len > 64) {
         ret = false;
      }

      for(int i = 0; i < len; ++i) {
         if (!this.readSensorEEPROMOneByte(addr + i, buf)) {
            ret = false;
            break;
         }

         buffer[i] = buf[0];
         ret = true;
      }

      return ret;
   }

   public int hidCommand(Hid.Pac pac, Hid.Cmd cmd, Hid.Sub sub) {
      return this.hidCommand(pac.value(), cmd.value(), sub.value(), (byte[])null);
   }

   public int hidCommand(int pac, int cmd, int sub) {
      return this.hidCommand(pac, cmd, sub, (byte[])null);
   }

   public int hidCommand(int pac, int cmd, int sub_1, int sub_2) {
      return this.hidCommand(pac, cmd, (sub_1 & 255) << 8 | sub_2 & 255, (byte[])null);
   }

   public int hidCommand(Hid.Pac pac, Hid.Cmd cmd, Hid.Sub sub, byte[] extra) {
      return this.hidCommand(pac.value(), cmd.value(), sub.value(), extra);
   }

   public int hidCommand(Hid.Pac pac, Hid.Cmd cmd, int sub_1, int sub_2, byte[] extra) {
      return this.hidCommand(pac.value(), cmd.value(), sub_1, sub_2, extra);
   }

   public int hidCommand(int pac, int cmd, int sub_1, int sub_2, byte[] extra) {
      return this.hidCommand(pac, cmd, (sub_1 & 255) << 8 | sub_2 & 255, extra);
   }

   public int hidCommand(int pac, int cmd, int sub, byte[] extra) {
      Arrays.fill(this.hidPacket, 0, this.hidPacketType, (byte)0);
      this.hidPacket[0] = 2;
      this.hidPacket[1] = (byte)pac;
      this.hidPacket[2] = (byte)cmd;
      Hid.putShort(this.hidPacket, 3, sub);
      if (extra != null) {
         System.arraycopy(extra, 0, this.hidPacket, 6, Math.min(extra.length, this.hidPacket.length - 6));
      }

      Log.d(this.TAG, String.format("Write : pac = %x, cmd = %x, sub = %x", pac, cmd, sub));
      if (this.write(this.hidPacket, this.hidPacketType)) {
         if (pac == Hid.Pac.PAC_CMD.value() && cmd == Hid.Cmd.CMT_CAPTURE.value() && sub == Hid.Sub.CCT_STOP.value()) {
            return this.echoAbortCapture(pac, cmd, sub);
         } else {
            return pac == Hid.Pac.PAC_CMD.value() && cmd == Hid.Cmd.CMT_CAPTURE.value() && sub == Hid.Sub.CCT_SINGLE.value() ? this.echoSingleCapture(pac, cmd, sub) : this.echo(pac, cmd, sub);
         }
      } else {
         return -1;
      }
   }

   public int echo(Hid.Pac pac, Hid.Cmd cmd, Hid.Sub sub) {
      return this.echo(pac.value(), cmd.value(), sub.value());
   }

   @SuppressLint({"DefaultLocale"})
   public int echo(int pac, int cmd, int sub) {
      Arrays.fill(this.hidPacketEcho, (byte)0);
      if (this.readSmall(this.hidPacketEcho, this.hidPacketType)) {
         Log.d(this.TAG, String.format("Capture Single Echo : pac = %x/%x, cmd = %x/%x, sub = %x/%x, err = %d", pac, this.hidPacketEcho[1], cmd, this.hidPacketEcho[2], sub, this.read2BytesAsInt(this.hidPacketEcho, 3), this.hidPacketEcho[5]));
         if (pac == (this.hidPacketEcho[1] & 127) && cmd == this.hidPacketEcho[2]) {
            return this.hidPacketEcho[5];
         }

         if (this.flagToFix) {
            return -47;
         }
      } else if (this.flagToFix) {
         return -47;
      }

      return -1;
   }

   public int read2BytesAsInt(byte[] buf, int startIndex) {
      int result = 0;
      int shift = 1;

      for(int i = 0; i < 2; ++i) {
         result |= (buf[startIndex + i] & 255) << 8 * (shift - i);
      }

      return result;
   }

   public boolean cancelWaitToAbort() {
      if (this.requestRead != null) {
         this.flagToFix = true;
         return this.requestRead.cancel();
      } else {
         return false;
      }
   }

   public int echoSingleCapture(int pac, int cmd, int sub) {
      int readCount = false;
      boolean bContinue = true;

      while(bContinue) {
         int readCount = this.usbConnection.bulkTransfer(this.epIn, this.hidPacketEcho, this.hidPacketEcho.length, 10);
         if (this.flagToFix) {
            return -47;
         }

         if (readCount > 0) {
            break;
         }

         SystemClock.sleep(10L);
      }

      return this.hidPacketEcho[5];
   }

   public int echoAbortCapture(int pac, int cmd, int sub) {
      int result = -1;
      int rpac = false;
      int rcmd = false;
      int rsub = false;
      int rerr = false;
      this.usbConnection.bulkTransfer(this.epIn, this.hidPacketEcho, this.hidPacketEcho.length, 500);
      Log.d(this.TAG, String.format("Abort Echo : pac = %x/%x, cmd = %x/%x, sub = %x/%x, err = %d", pac, this.hidPacketEcho[1], cmd, this.hidPacketEcho[2], sub, this.read2BytesAsInt(this.hidPacketEcho, 3), this.hidPacketEcho[5]));
      int rpac = this.hidPacketEcho[1];
      int rcmd = this.hidPacketEcho[2];
      int rsub = this.read2BytesAsInt(this.hidPacketEcho, 3);
      int rerr = this.hidPacketEcho[5];
      if (rpac == Hid.Pac.PAC_CMD.value() && rcmd == Hid.Cmd.CMT_CAPTURE.value() && rsub == Hid.Sub.CCT_STOP.value()) {
         result = rerr;
      }

      this.usbConnection.bulkTransfer(this.epIn, this.hidPacketEcho, this.hidPacketEcho.length, 500);
      Log.d(this.TAG, String.format("Abort Echo : pac = %x/%x, cmd = %x/%x, sub = %x/%x, err = %d", pac, this.hidPacketEcho[1], cmd, this.hidPacketEcho[2], sub, this.read2BytesAsInt(this.hidPacketEcho, 3), this.hidPacketEcho[5]));
      rpac = this.hidPacketEcho[1];
      rcmd = this.hidPacketEcho[2];
      rsub = this.read2BytesAsInt(this.hidPacketEcho, 3);
      rerr = this.hidPacketEcho[5];
      if (rpac == Hid.Pac.PAC_CMD.value() && rcmd == Hid.Cmd.CMT_CAPTURE.value() && rsub == Hid.Sub.CCT_STOP.value()) {
         result = rerr;
      }

      return result;
   }

   public byte[] getLastEcho() {
      return this.hidPacketEcho;
   }

   public byte[] getLastEchoData() {
      System.arraycopy(this.hidPacketEcho, 6, this.hidPacketEchoData, 0, this.hidPacketEchoData.length);
      return this.hidPacketEchoData;
   }

   public int hidReceiveSize() {
      int re = Hid.retrieveInt(this.hidPacketEcho, 6);
      if (re < 0) {
         re = 0;
      }

      if (re > this.hidMaxTransferData) {
         re = this.hidMaxTransferData;
      }

      return re;
   }

   @SuppressLint({"DefaultLocale"})
   public boolean hidReceive(byte[] buf) {
      int offset = 0;
      int data_len = Hid.retrieveInt(this.hidPacketEcho, 6);
      int fragSize = this.hidPacketType - 1;
      int fragDataSize = fragSize - 5;
      if (buf.length >= data_len && fragDataSize > 0 && data_len > 0) {
         boolean lastPacket;
         do {
            this.readSmall(this.hidPacketFrag, this.hidPacketType);
            System.arraycopy(this.hidPacketFrag, 6, buf, offset, Math.min(fragDataSize, data_len));
            offset += fragDataSize;
            int data_idx = Hid.retrieveInt(this.hidPacketFrag, 3);
            lastPacket = (data_idx & 1073741824) != 0;
            boolean incompleteTransfer = (data_idx & Integer.MIN_VALUE) != 0;
            data_len -= fragDataSize;
         } while(this.hidPacketFrag[5] == 0 && !lastPacket && data_len > 0);

         return lastPacket && data_len <= 0;
      } else {
         return false;
      }
   }

   @SuppressLint({"DefaultLocale"})
   public synchronized byte[] hidReceive() {
      int offset = 0;
      int data_len = Hid.retrieveInt(this.hidPacketEcho, 6);
      int fragSize = this.hidPacketType - 1;
      int fragDataSize = fragSize - 5;
      byte[] buf = new byte[data_len];
      if (fragDataSize > 0 && data_len > 0) {
         boolean lastPacket;
         do {
            this.readSmall(this.hidPacketFrag, this.hidPacketType);
            System.arraycopy(this.hidPacketFrag, 6, buf, offset, Math.min(fragDataSize, data_len));
            offset += fragDataSize;
            int data_idx = Hid.retrieveInt(this.hidPacketFrag, 3);
            lastPacket = (data_idx & 1073741824) != 0;
            boolean incompleteTransfer = (data_idx & Integer.MIN_VALUE) != 0;
            data_len -= fragDataSize;
         } while(this.hidPacketFrag[5] == 0 && !lastPacket && data_len > 0);

         return lastPacket && data_len <= 0 ? buf : null;
      } else {
         return null;
      }
   }

   @SuppressLint({"DefaultLocale"})
   public boolean hidSend(byte[] buf, int data_len) {
      int offset = 0;
      int data_idx = 0;
      boolean lastPacket = false;
      int fragSize = this.hidPacketType - 1;
      int fragDataSize = fragSize - 5;
      int transferSize = fragDataSize;
      if (fragDataSize > 0 && data_len > 0) {
         do {
            this.hidPacket[0] = 4;
            this.hidPacket[1] = (byte)Hid.Pac.PAC_DATA_OUT.value();
            this.hidPacket[2] = 0;
            if (offset + fragDataSize >= data_len) {
               data_idx |= 16384;
               lastPacket = true;
               transferSize = data_len - offset;
            }

            Hid.putShort(this.hidPacket, 3, data_idx);
            System.arraycopy(buf, offset, this.hidPacket, 6, transferSize);
            if (!this.write(this.hidPacket, this.hidPacketType)) {
               break;
            }

            ++data_idx;
            offset += transferSize;
         } while(data_idx < 16384);

         return lastPacket && data_len == offset;
      } else {
         return false;
      }
   }

   public int setUSBPacketMode(int packetMode) {
      return this.hidCommand(Hid.Pac.PAC_SET.value(), Hid.Cmd.VAT_USB_TYPE.value(), packetMode);
   }

   public int resetDevice() {
      return this.hidCommand(23, 0, 0);
   }

   static {
      HID_CMD_PACKET_SIZE = BMSS_DEFAULT_PACKET_SIZE;
      HID_DATA_PACKET_SIZE = BMSS_DEFAULT_PACKET_SIZE;
      HID_DATA_PACKET_SIZE_R = BMSS_DEFAULT_PACKET_SIZE;
   }
}
