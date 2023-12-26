package com.suprema.usb;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.SystemClock;
import android.os.Build.VERSION;
import android.util.Log;
import com.suprema.IBioMiniInterops;
import com.suprema.util.Logger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UsbHandlerAndroidCyDevice implements IUsbHandlerAndroidCyDevice {
   private static boolean mBLoadLibrary = false;
   private static final String USBTAG = "SupremaUSB";
   private static final String[] SUPPORT_CPU_ARCH = new String[]{"arm64-v8a", "armeabi", "armeabi-v7a", "mips", "mips64", "x86", "x86_64"};
   private static final String[] EXCLUDE_MODEL = new String[]{"TPS360(TPS360)", "TPS360", "TPS360C", "TPS450", "TPS470", "ZTE Grand S II", "BioWolf 8n", "ATF80", "Ruggbo 20 Lite", "BioWolf 8n", "Ruggbo 20", "AF20 Lite", "COMET-10", "ATF80", "COMET-20", "BioWolf 8N"};
   private String TAG = "UsbHandlerAndroidCyDevice";
   public static final int HID_CMD_PACKET_SIZE = 512;
   public static final int HID_DATA_PACKET_SIZE = 512;
   public static final int HID_DATA_PACKET_SIZE_R = 512;
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
   static final int MAX_TRANSFER_DATA = 8289798;
   private int BULK_RX_PORT = 2;
   private int BULK_TX_PORT = 1;
   private int TRANSFER_TIMEOUT = 140;
   public static final int BULK_PACKET_LENGTH = 16384;
   public int BULK_TIMEOUT = 400;
   private IBioMiniInterops bioMini;
   private UsbRequest[] request = null;
   private UsbRequest requestWrite = null;
   private UsbRequest requestRead = null;
   private ByteBuffer[] dataQ = null;
   private int maxTimesToRead = 0;
   private int maxBytesToRead = 0;
   private int dstOffset = 0;
   private final boolean useReservedQueue = true;
   private byte[] bufQ = new byte['耀'];
   private final byte[] hidPacket = new byte[512];
   private final byte[] hidPacketFrag = new byte[512];
   public byte[] hidPacketEcho = new byte[512];
   public byte[] hidPacketEchoData = new byte[506];
   private UsbManager usbManager;
   private UsbDevice usbDevice;
   private UsbDeviceConnection usbConnection = null;
   private UsbEndpoint epIn = null;
   private UsbEndpoint epOut = null;
   private boolean isUninitSet = false;
   private boolean isInterruptEndpointDetected = false;
   private boolean isIntSuccessful = false;
   private boolean useLibUsb = false;
   private long handleLibUsb = -1L;
   private int mInitializedRequests = 0;
   private boolean mReading = false;
   private final Object syncReading = new Object();
   private final Object syncCheckBulkTimeout = new Object();
   private boolean mTimeoutOccurred = false;
   private static final int CMD_LED_CTRL = 194;
   private static final int CMD_READ_FRAME_A = 226;
   private static final int CMD_READ_FRAME_DONE = 239;
   private static final int CMD_SET_EEPROM = 214;
   private static final int CMD_GET_EEPROM = 215;
   private static final int CMD_SENSOR_EEPROM_GET_ADDR = 222;
   private static final int CMD_SENSOR_EEPROM_GET_DATA = 223;
   private static final int CMD_EEPROM_WP_ENABLE = 201;
   public static final int OV_IIC_EEPROM_ADDR = 174;

   public boolean hasInterruptEndpoint() {
      return this.isInterruptEndpointDetected;
   }

   public boolean isValid() {
      return this.isIntSuccessful;
   }

   public void setBulkRx(int port) {
      this.BULK_RX_PORT = port;
   }

   public void setBulkTx(int port) {
      this.BULK_TX_PORT = port;
   }

   public boolean isNative() {
      return this.useLibUsb;
   }

   public UsbHandlerAndroidCyDevice(IBioMiniInterops bioMiniInterface, UsbManager usbManager, UsbDevice usbDevice, int maxBulkSize, boolean bUseLibUsb) {
      this.useLibUsb = bUseLibUsb;
      this.isIntSuccessful = false;
      this.handleLibUsb = -1L;
      this.usbManager = usbManager;
      this.usbDevice = usbDevice;
      if (this.useLibUsb) {
         Log.i("SupremaUSB", "L.B.U");
         if (mBLoadLibrary) {
            Log.i("SupremaUSB", "Load externalLib success.");
            this.libusbInit();
         } else {
            Log.e("SupremaUSB", "Load externalLib fail. LoadLibrary(" + mBLoadLibrary + ") , usbLibrary(" + this.useLibUsb + ")");
            this.useLibUsb = false;
         }
      } else {
         Log.i("SupremaUSB", "A.B.U");
      }

      try {
         this.usbConnection = usbManager.openDevice(usbDevice);
      } catch (IllegalArgumentException var9) {
         this.LogW("activate failed : " + var9);
         return;
      }

      if (this.usbConnection == null) {
         this.LogW("activate failed conn null");
      } else {
         if (this.useLibUsb) {
            this.handleLibUsb = this.libusbOpen(this.usbConnection.getFileDescriptor());
         }

         if (this.handleLibUsb == -1L) {
            if (!this.usbConnection.claimInterface(usbDevice.getInterface(0), true)) {
               this.LogE("activate failed while claimInterface");
               return;
            }

            if (usbDevice.getInterfaceCount() <= 0) {
               this.LogE("activate failed no connection interface");
               return;
            }

            UsbInterface usbIf = usbDevice.getInterface(0);
            this.LogD("USB interface count: " + usbIf.getEndpointCount());

            int i;
            for(i = 0; i < usbIf.getEndpointCount(); ++i) {
               if (usbIf.getEndpoint(i).getType() == 2) {
                  this.LogD("EndPoint No : " + usbIf.getEndpoint(i).getEndpointNumber());
                  if (usbIf.getEndpoint(i).getDirection() == 128) {
                     this.epIn = usbIf.getEndpoint(i);
                  } else {
                     this.epOut = usbIf.getEndpoint(i);
                  }
               } else if (usbIf.getEndpoint(i).getType() == 3) {
                  this.LogD("EndPoint No (interrupt) : " + usbIf.getEndpoint(i).getEndpointNumber() + ", " + usbIf.getEndpoint(i));
                  this.isInterruptEndpointDetected = true;
                  if (usbIf.getEndpoint(i).getDirection() == 128) {
                     this.epIn = usbIf.getEndpoint(i);
                  } else {
                     this.epOut = usbIf.getEndpoint(i);
                  }
               }
            }

            this.maxTimesToRead = (maxBulkSize + 16384 - 1) / 16384;
            this.maxBytesToRead = this.maxTimesToRead * 16384;
            this.request = new UsbRequest[this.maxTimesToRead];
            this.requestWrite = new UsbRequest();
            this.requestRead = new UsbRequest();
            this.dataQ = new ByteBuffer[this.maxTimesToRead];

            for(i = 0; i < this.mInitializedRequests; ++i) {
               this.request[i].close();
            }

            this.mInitializedRequests = 0;

            for(i = 0; i < this.maxTimesToRead; ++i) {
               this.dataQ[i] = ByteBuffer.allocate(16384);
               this.request[i] = new UsbRequest();
            }

            try {
               if (this.epOut != null) {
                  this.requestWrite.initialize(this.usbConnection, this.epOut);
               }

               if (this.epIn != null) {
                  this.requestRead.initialize(this.usbConnection, this.epIn);
               }
            } catch (Exception var8) {
               Log.e(this.TAG, "BioMini SDK Device initializing..... " + var8.getCause() + " / " + var8.getMessage());
               return;
            }
         }

         this.isIntSuccessful = true;
      }
   }

   public void setBulkTimeout(int timeout) {
      this.BULK_TIMEOUT = timeout;
   }

   public int getBulkTimeout() {
      return this.BULK_TIMEOUT;
   }

   public void resize(int maxBulkSize) {
      if (!this.useLibUsb) {
         int _maxTimesToRead = (maxBulkSize + 16384 - 1) / 16384;
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

   }

   void LogD(Object msg) {
      Logger.LogD(this.TAG, msg);
   }

   void LogI(Object msg) {
      Logger.LogI(this.TAG, msg);
   }

   void LogE(Object msg) {
      Logger.LogE(this.TAG, msg);
   }

   void LogW(Object msg) {
      Logger.LogW(this.TAG, msg);
   }

   void LogV(Object msg) {
      Logger.LogV(this.TAG, msg);
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

      if (this.useLibUsb && this.handleLibUsb != -1L) {
         this.libusbClose(this.handleLibUsb);
         this.handleLibUsb = -1L;
      } else {
         for(int i = 0; i < this.mInitializedRequests; ++i) {
            this.request[i].close();
         }
      }

      this.usbConnection.close();
      this.usbConnection = null;
      this.mInitializedRequests = 0;
      if (this.useLibUsb) {
         this.libusbExit();
      }

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

   public void resetBulkPipe(boolean setReadingStatus) {
      this.LogD("resetBulkPipe()=======================>");
      if (this.useLibUsb && this.handleLibUsb != -1L) {
         this.libusbResetPipe(this.handleLibUsb, this.BULK_RX_PORT);
      } else {
         for(int i = 0; i < this.mInitializedRequests; ++i) {
            boolean var3 = this.request[i].cancel();
         }
      }

      this.setReading(setReadingStatus);
      this.LogD("<=======================resetBulkPipe()");
   }

   public void setDstOffset(int offset) {
      this.dstOffset = offset;
   }

   public boolean initRead(int len, int skipPackets, boolean updateAlways) {
      if (this.useLibUsb && this.handleLibUsb != -1L) {
         return true;
      } else {
         boolean reqres = false;
         int nTimesToRead = (len + 16384 - 1) / 16384;
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
   }

   private synchronized boolean tryLibUsbRead(byte[] buf, int len, IUsbHandler.IReadProcessor readProcessor, int retries) {
      if (this.useLibUsb && this.handleLibUsb != -1L) {
         int retryInterval = 10;

         while(true) {
            while(retries-- > 0) {
               if (readProcessor != null && !readProcessor.beforeRead()) {
                  this.LogD("tryLibUsbRead beforeRead failed");
               } else {
                  int re = this.libusbBulkRead(this.handleLibUsb, this.BULK_RX_PORT, buf, len, this.getBulkTimeout());
                  if (readProcessor != null && !readProcessor.afterRead()) {
                     this.LogD("tryLibUsbRead afterRead failed");
                  } else {
                     if (re >= 0) {
                        return true;
                     }

                     if (re == -1) {
                        this.libusbResetDevice(this.handleLibUsb);
                     } else {
                        this.libusbResetPipe(this.handleLibUsb, this.BULK_RX_PORT | 128);
                     }

                     SystemClock.sleep((long)retryInterval);
                     retryInterval *= 2;
                     this.LogD("tryLibUsbRead failed (" + retries + ")");
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public synchronized boolean read(byte[] buf, int len, int skipPackets, byte fillExtra) {
      if (this.useLibUsb && this.handleLibUsb != -1L) {
         return this.tryLibUsbRead(buf, len, (IUsbHandler.IReadProcessor)null, 3);
      } else {
         this.mReading = true;
         int nBytesToRead = len;
         int nRemainingBytes = false;
         int nToRead = (len + 16384 - 1) / 16384;
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
               Log.e(this.TAG, "UsbHandlerAndroidCyDevice result of USB request  : " + reqres);
               this.mReading = false;
               return false;
            }
         }

         this.LogD("nToRead : " + nToRead);

         for(i = 0; i < nToRead; ++i) {
            if (this.usbConnection != null) {
               try {
                  this.usbConnection.requestWait();
               } catch (Exception var14) {
                  var14.printStackTrace();
               }
            }
         }

         this.LogD("UsbHandlerAndroidCyDevice USB transfer time : " + (System.currentTimeMillis() - CHECKING_TIME));
         this.LogD("len = " + len + ", nBytesToRead = " + nBytesToRead);

         for(i = 0; i < nBytesToRead; i += 16384) {
            int posBuf = this.dataQ[i / 16384].position();
            this.dataQ[i / 16384].position(0);
            this.dataQ[i / 16384].get(this.bufQ, 0, posBuf);
            this.LogD("UsbHandlerAndroidCyDevice Data in queue : " + posBuf);
            this.LogD("i + dstOffset = " + i + this.dstOffset);
            System.arraycopy(this.bufQ, 0, buf, i + this.dstOffset, posBuf);
         }

         Arrays.fill(buf, nBytesToRead, len, fillExtra);
         this.mReading = false;
         return true;
      }
   }

   public boolean isReading() {
      synchronized(this.syncReading) {
         boolean re = this.mReading;
         return re;
      }
   }

   private void setReading(boolean isReading) {
      synchronized(this.syncReading) {
         this.mReading = isReading;
      }
   }

   private boolean hadBulkTimeout() {
      synchronized(this.syncCheckBulkTimeout) {
         return this.mTimeoutOccurred;
      }
   }

   private void setBulkTimeoutStatus(boolean timeoutOccurred) {
      synchronized(this.syncCheckBulkTimeout) {
         this.mTimeoutOccurred = timeoutOccurred;
      }
   }

   private void watchTimeout(int milliseconds) {
      this.setBulkTimeoutStatus(false);
      (new Thread(new UsbHandlerAndroidCyDevice.WatchTimeoutThread(milliseconds))).start();
   }

   public synchronized boolean read(byte[] buf, int len, byte fillExtra, final IUsbHandler.IReadProcessor readProcessor) {
      return this.read(buf, len, fillExtra, new IUsbHandler.IReadProcessorAdv() {
         public boolean beforeRead() {
            return readProcessor.beforeRead();
         }

         public void firstBulkReceived(long delay) {
         }

         public boolean afterRead() {
            return readProcessor.afterRead();
         }
      }, 5);
   }

   public synchronized boolean read(byte[] buf, int len, byte fillExtra, IUsbHandler.IReadProcessorAdv readProcessor) {
      return this.read(buf, len, fillExtra, readProcessor, 5);
   }

   public synchronized boolean read(byte[] buf, int len, byte fillExtra, IUsbHandler.IReadProcessorAdv readProcessor, int retries) {
      if (this.useLibUsb && this.handleLibUsb != -1L) {
         return this.tryLibUsbRead(buf, len, readProcessor, retries);
      } else {
         int nMinBulkUnitSize = 1024;
         int nBytesToRead = len;
         int nRemainingBytes = false;
         int nToRead = (len + 16384 - 1) / 16384;
         int nRemainingBytes = len % 16384;
         this.LogD("nRemainingBytes :" + nRemainingBytes);
         int fAddtionalLen = 0;
         if (nRemainingBytes % nMinBulkUnitSize != 0) {
            fAddtionalLen = nMinBulkUnitSize - nRemainingBytes % nMinBulkUnitSize;
         }

         nRemainingBytes += fAddtionalLen;
         int nToEnqueue = nToRead;

         int _retries;
         for(_retries = 0; _retries < nToEnqueue; ++_retries) {
            this.request[_retries].setClientData(this);
         }

         _retries = retries;

         while(true) {
            while(retries > 0) {
               if (_retries > retries) {
                  this.LogD("Retrying... " + (_retries - retries));
               }

               boolean re = true;
               this.setReading(true);
               --retries;
               boolean reqres = false;
               long CHECKING_TIME = 0L;
               CHECKING_TIME = SystemClock.uptimeMillis();
               this.LogD("BioMiniUsbHandler number of USB reads : " + nToRead + ", available USB request handles : " + this.request.length);

               int i;
               try {
                  for(i = 0; i < nToEnqueue; ++i) {
                     this.dataQ[i].position(0);
                     if (i == nToEnqueue - 1 && nRemainingBytes > 0) {
                        reqres = this.request[i].queue(this.dataQ[i], nRemainingBytes);
                     } else {
                        reqres = this.request[i].queue(this.dataQ[i], 16384);
                     }

                     if (!reqres || this.isUninitSet) {
                        this.LogE("BioMiniUsbHandler result of USB request  : " + reqres);
                        re = false;
                        break;
                     }
                  }
               } catch (Exception var19) {
                  this.LogE(var19.toString());
                  return false;
               }

               if (!re) {
                  this.setReading(false);
                  SystemClock.sleep(5L);
               } else if (readProcessor != null && !readProcessor.beforeRead()) {
                  this.LogE("beforeRead failed: canceling bulk transfer...");

                  for(i = 0; i < nToEnqueue; ++i) {
                     this.request[i].cancel();
                  }

                  SystemClock.sleep(5L);
               } else {
                  this.watchTimeout(this.BULK_TIMEOUT);
                  this.LogD("BioMiniUsbHandler requestWait loop (" + nToRead + ")");

                  int posBuf;
                  try {
                     boolean firstPacket = true;

                     for(posBuf = 0; posBuf < nToRead; ++posBuf) {
                        if (this.usbConnection == null) {
                           this.setReading(false);
                           break;
                        }

                        this.usbConnection.requestWait();
                        if (this.mTimeoutOccurred) {
                           this.LogE("BioMiniUsbHandler requestWait (" + posBuf + ") canceled");
                        } else if (firstPacket) {
                           firstPacket = false;
                           readProcessor.firstBulkReceived(SystemClock.uptimeMillis() - CHECKING_TIME);
                        }
                     }
                  } catch (Exception var20) {
                     var20.printStackTrace();
                  }

                  this.LogD("BioMiniUsbHandler USB transfer time : " + (SystemClock.uptimeMillis() - CHECKING_TIME));
                  if (readProcessor == null || readProcessor.afterRead()) {
                     if (!this.hadBulkTimeout()) {
                        for(i = 0; i < nBytesToRead; i += 16384) {
                           if (this.isUninitSet) {
                              this.LogD("isUninitSet false;");
                              this.setReading(false);
                              return false;
                           }

                           int posBuf = false;
                           if (i + 16384 > nBytesToRead && this.dataQ[i / 16384].position() - fAddtionalLen >= 0) {
                              posBuf = this.dataQ[i / 16384].position() - fAddtionalLen;
                              this.LogD("condition1 position : " + posBuf);
                           } else {
                              posBuf = this.dataQ[i / 16384].position();
                              this.LogD("condition2 position : " + posBuf);
                           }

                           this.dataQ[i / 16384].position(0);
                           this.dataQ[i / 16384].get(this.bufQ, 0, posBuf);
                           if (posBuf == 0) {
                              this.LogD("posBuf is zero.");
                              this.setReading(false);
                              return false;
                           }

                           System.arraycopy(this.bufQ, 0, buf, i + this.dstOffset, posBuf);
                        }

                        Arrays.fill(buf, nBytesToRead, len, fillExtra);
                        this.LogD("BioMiniUsbHandler read done");
                        this.setReading(false);
                        return true;
                     }

                     this.setBulkTimeoutStatus(false);
                     this.setReading(false);
                     SystemClock.sleep(5L);
                  }
               }
            }

            this.setReading(false);
            return false;
         }
      }
   }

   public boolean readSync(byte[] buf, int len, int timeout) {
      try {
         if (VERSION.SDK_INT >= 18) {
            int fraction_size = 16384;
            int received = 0;
            int incoming = this.usbConnection.bulkTransfer(this.epIn, buf, 0, fraction_size, timeout);

            int received;
            for(received = received + incoming; incoming > 0 && received < len; received += incoming) {
               if (fraction_size > len - received) {
                  fraction_size = len - received;
               }

               incoming = this.usbConnection.bulkTransfer(this.epIn, buf, received, fraction_size, 5);
               if (incoming < 0) {
                  this.LogE("Critical Error readSync: " + incoming + "/" + received + "/" + len);
                  return true;
               }
            }

            if (received < 0) {
               this.LogE("Critical Error readSync: " + received);
               return false;
            }

            if (received < len) {
               this.LogW("Bulk transfer incomplete: " + received + "/" + len);
            }

            return received == len;
         }
      } catch (Exception var7) {
         this.LogE("readSyc failed: " + var7.toString());
      }

      return false;
   }

   public boolean readSync(byte[] buf, int len, byte fillExtra, IUsbHandler.IReadProcessorAdv readProcessor) {
      return this.readSync(buf, len, fillExtra, readProcessor, 5);
   }

   public boolean readSync(byte[] buf, int len, byte fillExtra, IUsbHandler.IReadProcessorAdv readProcessor, int retries) {
      Arrays.fill(buf, fillExtra);

      for(int i = 0; i < retries; ++i) {
         if (readProcessor.beforeRead()) {
            boolean re = this.readSync(buf, len, this.BULK_TIMEOUT);
            if (readProcessor.afterRead() && re) {
               return re;
            }
         }
      }

      return false;
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
         this.usbConnection.requestWait();
         return true;
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
      int len = 1;
      if (this.controlTx(222, buf, 1) && this.controlRx(223, buf, len)) {
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
            this.LogD("readSensorEEPROM #2 " + i);
            ret = false;
            break;
         }

         buffer[i] = buf[0];
         ret = true;
      }

      return ret;
   }

   private boolean initUsbInterface() {
      UsbInterface usbIf = this.usbDevice.getInterface(0);

      for(int i = 0; i < usbIf.getEndpointCount(); ++i) {
         if (usbIf.getEndpoint(i).getType() == 2) {
            this.LogD("EndPoint No : " + usbIf.getEndpoint(i).getEndpointNumber());
            if (usbIf.getEndpoint(i).getDirection() == 128) {
               this.epIn = usbIf.getEndpoint(i);
            } else {
               this.epOut = usbIf.getEndpoint(i);
            }
         } else if (usbIf.getEndpoint(i).getType() == 3) {
            this.LogD("EndPoint No (interrupt) : " + usbIf.getEndpoint(i).getEndpointNumber() + ", " + usbIf.getEndpoint(i));
            this.isInterruptEndpointDetected = true;
            if (usbIf.getEndpoint(i).getDirection() == 128) {
               this.epIn = usbIf.getEndpoint(i);
            } else {
               this.epOut = usbIf.getEndpoint(i);
            }
         }
      }

      if (this.epOut != null) {
         this.requestWrite.initialize(this.usbConnection, this.epOut);
      }

      if (this.epIn != null) {
         this.requestRead.initialize(this.usbConnection, this.epIn);
      }

      return true;
   }

   private native String hellolibusb();

   private native void libusbInit();

   private native void libusbExit();

   private native long libusbOpen(int var1);

   private native long libusbReopen(long var1, int var3);

   private native void libusbClose(long var1);

   private native int libusbBulkRead(long var1, int var3, byte[] var4, int var5, int var6);

   private native void libusbResetDevice(long var1);

   private native void libusbResetPipe(long var1, int var3);

   private native int libusbControlTransfer(long var1, int var3, int var4, int var5, int var6, byte[] var7, int var8, int var9);

   static {
      boolean bSupportARCH = false;
      boolean bSupportMode = true;
      String cpu_arch = Build.CPU_ABI;
      String device_model = Build.MODEL;
      Log.i("SupremaUSB", "Powered device model : [" + Build.MODEL + "]");
      if (Build.MODEL == null) {
         Log.e("SupremaUSB", "Device Model is null");
      }

      String[] var4 = EXCLUDE_MODEL;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String _model = var4[var6];
         if (_model.equals(Build.MODEL)) {
            Log.i("SupremaUSB", "Excluded device model. ");
            bSupportMode = false;
         }
      }

      Log.i("SupremaUSB", " Current Architecture [" + Build.CPU_ABI + "]");

      for(int i = 0; i < SUPPORT_CPU_ARCH.length; ++i) {
         if (cpu_arch.equals(SUPPORT_CPU_ARCH[i])) {
            bSupportARCH = true;
            break;
         }
      }

      try {
         Log.i("SupremaUSB", "Is supported (MODE2) Architecture? : " + String.valueOf(bSupportARCH));
         Log.i("SupremaUSB", "Is supported (MODE2) Model ? : " + String.valueOf(bSupportMode));
         if (bSupportARCH && bSupportMode) {
            Log.i("SupremaUSB", "Trying to load mode2 module.");
            System.loadLibrary("suprema_libusb");
            mBLoadLibrary = true;
         } else {
            Log.i("SupremaUSB", "Not trying to load mode2 module.");
         }
      } catch (Exception var8) {
         Log.e("SupremaUSB", "Fail to extlibrary.");
         var8.printStackTrace();
         mBLoadLibrary = false;
      }

   }

   private class WatchTimeoutThread implements Runnable {
      private int _timeout;

      WatchTimeoutThread(int timeout) {
         this._timeout = timeout;
      }

      public void run() {
         int cnt = this._timeout / 5;

         while(cnt > 0) {
            SystemClock.sleep(5L);
            --cnt;
            if (!UsbHandlerAndroidCyDevice.this.isReading()) {
               UsbHandlerAndroidCyDevice.this.LogD("watching timeout done...");
               break;
            }
         }

         if (UsbHandlerAndroidCyDevice.this.isReading()) {
            UsbHandlerAndroidCyDevice.this.LogE("USB timeout... Resetting Bulk pipe...");
            UsbHandlerAndroidCyDevice.this.setBulkTimeoutStatus(true);
            UsbHandlerAndroidCyDevice.this.resetBulkPipe(true);
         }

      }
   }
}
