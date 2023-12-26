package com.suprema.devices;

import android.os.SystemClock;
import android.util.Log;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;
import com.suprema.IUsbEventHandler;
import com.suprema.android.BioMiniJni;
import com.suprema.usb.IUsbHandler;
import java.util.Arrays;
import java.util.Locale;

public class BioMiniOC4 extends BioMiniBase {
   private int m_nExposureOP5;
   private int m_nXoffsetOP5;
   private int m_nYoffsetOP5;
   private boolean m_bTopIter;
   private int m_OC4ExpPre;
   private int m_OC4GainPre;
   private int m_nSensorVer;
   private int m_nTop;
   private ICaptureResponder mCaptureResponder = null;
   private final boolean SKIP_BROKEN_FRAME = true;
   private final int MAX_BULK_SIZE = 315904;
   private boolean bIwakeupYou = false;
   private boolean mIsUsbThreadRunning = false;
   private int g_bExtraDry = -1;
   private static final int IMG_XMAX_OC4 = 640;
   private static final int IMG_XMAX_OC4_EX = 656;
   private static final int IMG_YMAX_OC4 = 480;
   private static final int OC4_IMAGE_WIDTH = 288;
   private static final int OC4_IMAGE_HEIGHT = 320;
   private static final int OC4_EXP_PRE = 80;
   private static final int OC4_GAIN_PRE = 60;
   private static final int CAPTURE_BUFFER_SIZE = 12;
   private static final int DELAY_FOR_SUCCESSFUL_CAPTURE = 130;
   private long mLastNotification = 0L;
   private long mLastWait = 0L;
   private int mPacketsToSkipOC4 = 2;
   private int[] pVarOC4 = new int[]{300, 300, 275, 250, 225, 200, 150, 100};
   private int[] pCountOC4 = new int[]{1, 1, 2, 2, 2, 2, 3, 7};
   private int[] pStepsOC4 = new int[]{6, 5, 4, 4, 4, 4, 3, 3};
   private final int OPTIMAL_LOW_EXPO_PERCENTAGE = 59;
   private final int OPTIMAL_HIGH_EXPO_PERCENTAGE = 200;
   private long m_Start;
   private boolean m_bOC4Half = true;
   private long mProcessingCost = 0L;
   private static final int IMG_BUF_MAX = 314880;
   private static final int IMG_INT_BUF_MAX = 314880;
   private final byte[][] m_pFullBufferA = new byte[12][314889];
   private final byte[][] m_pFullBufferN = new byte[12][314889];
   private final byte[] m_Image = new byte[314880];
   private final byte[] m_ImageA = new byte[314880];
   private final byte[] m_ImageBG = new byte[314880];
   private byte[] m_ImagePrev = new byte[314880];
   private final byte[] m_ImageIntermediate = new byte[314880];
   private final byte[] m_ImageLast = new byte[314880];
   private byte[] m_ImageRawPrevN = new byte[314889];
   private boolean bIsAfterAbortCpaturing = true;
   private boolean bUSBisdominated = false;
   private int mSleepPlus = 0;
   private int mSleepVal = 20;
   private Runnable mLoop;
   private Runnable mSLoop;
   private BioMiniOC4.StartCapturingLoop mStartCapturingLoop;
   private Thread mUsbThread;
   private Thread mStartCapturingThread;
   private boolean bThreadFlag;
   private boolean bAbortCapturing = false;
   private boolean isBackGround = false;
   private int m_nCaptureMode;
   private int mHasPreviewBuffered;
   static int kk = 0;
   private static final int CMD_LED_CTRL = 194;
   private static final int CMD_SET_REG = 195;
   private static final int CMD_READ_FRAME = 198;
   private static final int CMD_GET_CID = 199;
   private static final int CMD_GET_SN = 201;
   private static final int CMD_CONNECT_INFO = 209;
   private static final int CMD_SET_OV_EEPROM = 216;
   private static final int CMD_GET_OV_EEPROM = 217;
   private static final int CMD_SET_EEPROM = 214;
   private static final int CMD_GET_EEPROM = 215;
   private static final int CMD_SENSOR_EEPROM_GET_ADDR = 222;
   private static final int CMD_SENSOR_EEPROM_GET_DATA = 223;
   private static final int CMD_EEPROM_WP_ENABLE = 201;
   private static final int CMD_SET_CIS_TIME = 192;
   private static final int OV_IIC_EEPROM_ADDR = 174;

   public BioMiniOC4() {
      this.TAG = "BioMiniOC4";
   }

   public int getMaxBulkSize() {
      return 315904;
   }

   private boolean setReg(int addr, int val) {
      return this.mUsbHandler.controlTx(this.GetCmd(195), new byte[]{(byte)addr, (byte)val}, 2);
   }

   public boolean captureSingle(final IBioMiniDevice.CaptureOption opt, final ICaptureResponder responder, boolean bAsync) {
      if (!this.isCapturing() && !this.mIsUsbThreadRunning) {
         this.mCaptureResponder = responder;
         if (bAsync) {
            Runnable captureObj = new Runnable() {
               public void run() {
                  BioMiniOC4.this._captureSingle(opt, responder);
               }
            };
            (new Thread(captureObj)).start();
            return true;
         } else {
            return this._captureSingle(opt, responder);
         }
      } else {
         responder.onCaptureError(responder, IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING.value(), "Capture is running.");
         return false;
      }
   }

   public int captureAuto(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED;
      return IBioMiniDevice.ErrorCode.ERR_NOT_SUPPORTED.value();
   }

   public byte[] getCapturedBuffer(IBioMiniDevice.ImageOptions options) {
      return null;
   }

   public byte[] getCaptureImageAsRAW_8() {
      if (!this.isCaptured) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_CAPTURED;
         return null;
      } else {
         return this.m_ImageLast;
      }
   }

   private boolean _captureSingle(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      int width = this.getImageWidth();
      int height = this.getImageHeight();
      byte[] capturedimage = new byte[width * height];
      this.mCurrentCaptureOption = opt;
      IBioMiniDevice.ErrorCode re = this.CaptureSingle(capturedimage);
      if (re == IBioMiniDevice.ErrorCode.OK) {
         return this.onCapture(responder, capturedimage, width, height, true);
      } else {
         if (responder != null) {
            responder.onCaptureError(this, re.value(), BioMiniJni.GetErrorString(re.value()));
         }

         return false;
      }
   }

   private IBioMiniDevice.ErrorCode CaptureSingle(byte[] pImage) {
      this.LogD("CaptureSingle : called...");
      if (this.mUsbHandler == null) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
         return IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
      } else if (this.mIsUsbThreadRunning) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
         return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
      } else {
         Arrays.fill(pImage, (byte)-1);
         int nRawWidth = this.getRawWidth();
         int nRawHeight = this.getRawHeight();
         int nIntWidth = this.getIntermediateWidth();
         int nIntHeight = this.getIntermediateHeight();
         int nTargetWidth = this.getTargetWidth();
         int nTargetHeight = this.getTargetHeight();
         int var10000 = nRawWidth * nRawHeight;
         if (this.bUSBisdominated) {
            this.LogD("CaptureSingle : handle busy");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
            return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
         } else {
            this.isBackGround = false;
            int res = this.CaptureFrameStart();
            this.LogD("CaptureSingle : CaptureFrameStart done (" + res + ")...");
            if (res == 0) {
               this.CaptureFrameStop();
               Log.e(this.TAG, "CaptureSingle : Cannot Start capturing properly. Stopping...");
               this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
               return IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
            } else {
               if (this.m_DetectFake > 0) {
                  this.m_nCaptureMode = 3;
               } else {
                  this.m_nCaptureMode = 1;
               }

               this.bThreadFlag = true;
               this.mLoop = new BioMiniOC4.UsbBulkLoopOC4(this);
               this.mUsbThread = new Thread(this.mLoop);
               this.mUsbThread.start();

               while(this.bThreadFlag) {
                  try {
                     if (this.m_nTop > -1) {
                        this.waitForCaptured();
                        int nTop = this.m_nTop;
                        if (!this.bIwakeupYou) {
                           break;
                        }

                        byte[] tmpBuf = new byte[nIntWidth * nIntHeight + 64];
                        Arrays.fill(tmpBuf, (byte)0);
                        BioMiniJni.ConvertImage2(this.m_pFullBufferN[nTop], tmpBuf, nIntWidth, nIntHeight);
                        if (!this.mUsbHandler.isNative()) {
                           int nCntBlacks = 0;
                           int nCntBlacksMax = 0;

                           for(int j = 0; j < nIntWidth * nIntHeight / 4; ++j) {
                              if (tmpBuf[j] == -2) {
                                 ++nCntBlacks;
                                 nCntBlacksMax = Math.max(nCntBlacksMax, nCntBlacks);
                              } else {
                                 nCntBlacks = 0;
                              }
                           }

                           if (nCntBlacksMax >= 1) {
                              this.mSleepPlus = 15;
                              this.LogE("StartCapturingLoop OC4 : Broken Frame");
                              continue;
                           }

                           this.LogD("StartCapturingLoop OC4 : Success Frame");
                           this.mSleepPlus = 0;
                        }

                        boolean bFingerTouch = BioMiniJni.IsOC4FingerTouch(this.m_pFullBufferN[nTop], 1, this.pVarOC4[this.m_nSensitivity], this.pCountOC4[this.m_nSensitivity], nIntWidth * nIntHeight + 64);
                        int[] gainStep = new int[1];
                        byte[] processedImg = new byte[nTargetWidth * nTargetHeight];
                        Arrays.fill(processedImg, (byte)-36);
                        BioMiniJni.CompOC4(tmpBuf, processedImg, nTargetWidth, nTargetHeight);
                        boolean bIsFingerOn = BioMiniJni.IsFingerOnOC4(this, processedImg, nTargetWidth, nTargetHeight, bFingerTouch, gainStep, this.pStepsOC4[this.m_nSensitivity]);
                        if (bIsFingerOn) {
                           byte[] output = new byte[nTargetWidth * nTargetHeight];
                           BioMiniJni.OC4PostProcessing(processedImg, this.m_pFullBufferN[nTop], output);
                           System.arraycopy(processedImg, 0, this.m_ImageIntermediate, 0, nTargetWidth * nTargetHeight);
                           byte[] buffer = new byte[64];
                           buffer[0] = 1;
                           this.mUsbHandler.controlTx(this.GetCmd(194), buffer, 1);
                           this.isCaptured = true;
                           System.arraycopy(this.m_ImageIntermediate, 0, this.m_ImageLast, 0, this.m_ImageIntermediate.length);
                           break;
                        }

                        if (this.m_TimeOut != 0L && System.currentTimeMillis() - this.m_Start > this.m_TimeOut) {
                           this.onCaptureError(this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - this.m_Start) + "/" + this.m_TimeOut + ")");
                           this.mIsTimeoutOccurred = true;
                           break;
                        }
                     } else {
                        SystemClock.sleep(50L);
                     }
                  } catch (NullPointerException var18) {
                     this.LogE("mUsbHandler missing");
                     break;
                  }
               }

               this.LogD("CaptureSingle : Process loop for OC4 finished");
               this.bThreadFlag = false;
               this.CaptureFrameStop();
               this.LogD("CaptureSingle : Done capturing a fingerprint");
               if (this.isCaptured) {
                  System.arraycopy(this.m_ImageLast, 0, pImage, 0, nTargetWidth * nTargetHeight);
                  this.m_LastError = IBioMiniDevice.ErrorCode.OK;
                  return IBioMiniDevice.ErrorCode.OK;
               } else {
                  this.LogD("CaptureSingle : No fingerprint captured");
                  if (this.mIsTimeoutOccurred) {
                     this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_TIMEOUT;
                  } else {
                     this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
                  }

                  this.mIsTimeoutOccurred = false;
                  return this.m_LastError;
               }
            }
         }
      }
   }

   public boolean isCapturing() {
      return this.bUSBisdominated;
   }

   public int abortCapturing() {
      if (!this.bIsAfterAbortCpaturing) {
         this.LogD("abortCapturing : Already done");
         this.m_LastError = IBioMiniDevice.ErrorCode.OK;
         return IBioMiniDevice.ErrorCode.OK.value();
      } else if (!this.isCapturing()) {
         Log.e(this.TAG, "abortCapturing : Nonsense because no capturing is on going...");
         this.m_LastError = IBioMiniDevice.ErrorCode.ERROR;
         return IBioMiniDevice.ErrorCode.ERROR.value();
      } else if (!this.bInitialized) {
         Log.e(this.TAG, "abortCapturing : Not initialized");
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
         return IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED.value();
      } else {
         this.bAbortCapturing = true;

         for(int cnt = 0; this.isCapturing() && cnt < 20; ++cnt) {
            SystemClock.sleep(100L);
         }

         if (this.mUsbThread != null) {
            this.LogD("abortCapturing : Interrupting transfer thread");
            this.mUsbThread.interrupt();
            this.mUsbThread = null;
         }

         this.bIsAfterAbortCpaturing = true;
         this.m_LastError = IBioMiniDevice.ErrorCode.OK;
         return IBioMiniDevice.ErrorCode.OK.value();
      }
   }

   public int startCapturing(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      this.LogD("Start capturing...");
      if (!this.isCapturing() && !this.mIsUsbThreadRunning) {
         if (!this.bInitialized) {
            Log.e(this.TAG, "UFA_StartCapturing : Not initialized");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
            return IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED.value();
         } else if (this.bUSBisdominated) {
            this.LogD("UFA_StartCapturing : USB Handle is busy");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
            return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING.value();
         } else {
            this.mCaptureResponder = responder;
            this.mCurrentCaptureOption = opt;
            this.isBackGround = false;
            int res = this.CaptureFrameStart();
            if (res == 0) {
               this.CaptureFrameStop();
               this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
               return IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED.value();
            } else {
               this.m_nCaptureMode = 2;
               this.bThreadFlag = true;
               this.mStartCapturingLoop = new BioMiniOC4.StartCapturingLoop(this);
               this.mStartCapturingThread = new Thread(this.mStartCapturingLoop);
               this.mSLoop = new BioMiniOC4.UsbBulkLoopOC4(this.mStartCapturingLoop);
               this.mUsbThread = new Thread(this.mSLoop);
               this.mUsbThread.start();
               this.mStartCapturingThread.start();
               this.m_LastError = IBioMiniDevice.ErrorCode.OK;
               return IBioMiniDevice.ErrorCode.OK.value();
            }
         }
      } else {
         this.LogD("UFA_StartCapturing : Cannot start capturing (another capturing processing is on going...)");
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
         return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING.value();
      }
   }

   public boolean isOnDestroying() {
      return false;
   }

   private int CaptureFrameStart() {
      this.bUSBisdominated = true;
      this.bAbortCapturing = false;
      this.isCaptured = false;
      int Ret = true;
      boolean re = false;
      int nWidth = this.getRawWidth();
      int nHeight = this.getRawHeight();
      this.mHasPreviewBuffered = 0;
      this.LogD(String.format(Locale.ENGLISH, "Timeout(%d) , injected timeout(%d)", this.m_TimeOut, this.mCurrentCaptureOption.captureTimeout));
      this.setTempCaptureOpts();
      this.LogD("Setting camera parameter...");
      if (this.m_bOC4Half) {
         this.LogD("oc4 Setting........");
         this.setReg(72, 199);
      }

      BioMiniJni.SetOC4IsFingerParams(true);
      if (this.m_nSensorVer == 1) {
         this.m_OC4ExpPre = Math.max(184, Math.min(216, this.m_nExposureOP5 - 235 + 200));
      } else {
         this.m_OC4ExpPre = 80;
      }

      this.m_OC4GainPre = 60;
      re = this.SetOC4IntegrationTime(this.m_OC4ExpPre, this.m_OC4GainPre);
      if (re) {
         re = this.mUsbHandler.controlTx(this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
      }

      this.m_Start = System.currentTimeMillis();
      return re ? 1 : 0;
   }

   private void CaptureFrameStop() {
      this.LogD("Stops capturing...");
      int tries = 0;

      for(byte MAX_TRIES = 50; this.mIsUsbThreadRunning && tries < MAX_TRIES; ++tries) {
         SystemClock.sleep(100L);
      }

      this.mStartCapturingLoop = null;
      this.mStartCapturingThread = null;
      if (this.mUsbThread != null) {
         this.mUsbThread.interrupt();
         this.mUsbThread = null;
      }

      this.mLoop = null;
      this.mSLoop = null;
      this.mUsbThread = null;
      BioMiniJni.SetOC4IsFingerParams(false);
      if (this.mUsbHandler != null) {
         this.mUsbHandler.controlTx(194, new byte[]{1, 0, 0, 0}, 1);
      }

      this.bUSBisdominated = false;
      this.LogD("Capture stopped");
      this.resetCaptureOpts();
   }

   private int GetCmd(int keyword) {
      switch(keyword) {
      case 194:
         return 194;
      case 195:
         return 195;
      case 196:
      case 197:
      case 199:
      case 200:
      default:
         return 0;
      case 198:
         return 198;
      case 201:
         return 201;
      }
   }

   private boolean SetOC4IntegrationTime(int Exp, int Gain) {
      this.OC4_SetRegister((byte)4, (byte)Exp);
      this.OC4_SetRegister((byte)-37, (byte)Gain);
      return this.OC4_SetRegister((byte)-36, (byte)Gain);
   }

   private boolean OC4_SetRegister(byte nAddr, byte nValue) {
      return this.mUsbHandler.controlTx(this.GetCmd(195), new byte[]{nAddr, nValue}, 2);
   }

   private byte[] OC4_Speed_Info() {
      byte[] buffer = new byte[4];
      return this.mUsbHandler.controlRx(209, buffer, 1) ? buffer : null;
   }

   private byte[] OC4_Get_OV_EEPROM_OP5(int sAddr, int Length) {
      byte[] Buf = new byte[256];
      byte[] BufferOut = new byte[256];
      if (sAddr <= 255 && Length <= 8 && sAddr + Length <= 256) {
         Buf[0] = 1;
         if (!this.mUsbHandler.controlTx(201, Buf, 1)) {
            return null;
         } else {
            Buf[0] = -82;
            Buf[1] = (byte)(sAddr >> 8 & 255);
            Buf[2] = (byte)(sAddr & 255);
            Buf[3] = (byte)Length;
            if (!this.mUsbHandler.controlTx(214, Buf, 4)) {
               return null;
            } else if (!this.mUsbHandler.controlRx(215, BufferOut, Length)) {
               return null;
            } else {
               Buf[0] = 0;
               return !this.mUsbHandler.controlTx(201, Buf, 1) ? null : BufferOut;
            }
         }
      } else {
         return null;
      }
   }

   public int Setting(int pid) {
      byte[] speed_value = this.OC4_Speed_Info();
      this.mUsbHandler.controlTx(225, speed_value, 1);
      byte[] bufRead = new byte[64];
      boolean re = this.mUsbHandler.readEEPROM(0, 2, bufRead);
      if (!re) {
         return 0;
      } else if (bufRead[0] == 17 && bufRead[1] == 5) {
         this.m_nSensorVer = 1;
         re = this.mUsbHandler.readEEPROM(8, 6, bufRead);
         if (!re) {
            return 0;
         } else {
            this.m_nXoffsetOP5 = bufRead[3] + (bufRead[2] << 8);
            this.m_nYoffsetOP5 = bufRead[5] + (bufRead[4] << 8);
            this.m_nExposureOP5 = bufRead[1] + (bufRead[0] << 8);
            return 0;
         }
      } else {
         this.mDeviceInfo.deviceName = this.TAG;
         this.mDeviceInfo.versionSDK = this.BASE_VERSION;
         this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI;
         Arrays.fill(bufRead, (byte)0);
         this.mUsbHandler.controlRx(this.GetCmd(201), bufRead, 32);
         this.mDeviceInfo.deviceSN = (new String(bufRead, 0, 32)).trim();
         return 1;
      }
   }

   public int getImageWidth() {
      return this.getTargetWidth();
   }

   public int getImageHeight() {
      return this.getTargetHeight();
   }

   private int getTargetWidth() {
      return 288;
   }

   private int getTargetHeight() {
      return 320;
   }

   private int getRawWidth() {
      return 656;
   }

   private int getRawHeight() {
      return 480;
   }

   private int getIntermediateWidth() {
      return 640;
   }

   private int getIntermediateHeight() {
      return 480;
   }

   public synchronized void captured() {
      this.LogD(" -- notify captured -- ");
      this.bIwakeupYou = true;
      this.notify();
      this.mLastNotification = SystemClock.uptimeMillis();
   }

   private synchronized void waitForCaptured() {
      try {
         this.bIwakeupYou = false;
         this.wait(1000L);
         this.mLastWait = SystemClock.uptimeMillis();
      } catch (InterruptedException var2) {
         var2.printStackTrace();
      }

   }

   private long getSafeDelay() {
      long delay = 130L - (this.mLastWait - this.mLastNotification);
      delay = Math.min(130L, Math.max(0L, delay));
      return delay;
   }

   public boolean clearCaptureImageBuffer() {
      if (this.isCaptured) {
         Arrays.fill(this.m_ImageIntermediate, 0, this.getTargetWidth() * this.getTargetHeight(), (byte)-1);
         this.isCaptured = false;
         this.LogD("set isCaptured false(UFA_ClearCaptureImageBuffer)");
         return true;
      } else {
         return false;
      }
   }

   public String getCompanyID() {
      byte[] buffer = new byte[64];
      return this.mUsbHandler.controlRx(199, buffer, 2) ? (new String(buffer)).trim() : "";
   }

   public boolean isAwake() {
      return true;
   }

   public boolean wakeUp() {
      return true;
   }

   public boolean hibernate() {
      return false;
   }

   protected void rotateImage() {
      byte[] m_ImageTemp = new byte[this.getImageHeight() * this.getImageWidth()];

      for(int i = 0; i < this.getImageHeight() * this.getImageWidth(); ++i) {
         m_ImageTemp[i] = this.m_ImageLast[this.getImageHeight() * this.getImageWidth() - 1 - i];
      }

      System.arraycopy(m_ImageTemp, 0, this.m_ImageLast, 0, this.getImageHeight() * this.getImageWidth());
   }

   public boolean deactivate(IUsbEventHandler.DisconnectionCause reason) {
      return false;
   }

   public byte[] getCaptureImageAs19794_4() {
      if (!this.isCaptured) {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_CAPTURED;
         return null;
      } else if (this.getImageHeight() > 0 && this.getImageHeight() > 0) {
         byte[] outImage = new byte[this.getImageWidth() * this.getImageHeight() + 1024];
         int[] outImageSize = new int[4];
         int nWidth = this.getImageWidth();
         int nHeight = this.getImageHeight();
         int res = BioMiniJniCommon.GetCaptureImageBufferTo197944ImageBuffer(this.m_ImageLast, nWidth, nHeight, outImage, outImageSize);
         this.m_LastError = IBioMiniDevice.ErrorCode.fromInt(res);
         if (res == IBioMiniDevice.ErrorCode.OK.value()) {
            this.LogD("19794_4 encoding successful : " + outImageSize[0]);
            byte[] p19794_4Image = new byte[outImageSize[0]];
            System.arraycopy(outImage, 0, p19794_4Image, 0, outImageSize[0]);
            return p19794_4Image;
         } else {
            Log.e(this.TAG, "19794_4 encoding failed");
            return null;
         }
      } else {
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
         return null;
      }
   }

   private class StartCapturingLoop implements Runnable {
      IBioMiniCyDevice pBioMiniAndroid;
      boolean bIwakeupYou = false;

      StartCapturingLoop(IBioMiniCyDevice pMyp) {
         this.pBioMiniAndroid = pMyp;
      }

      public synchronized void captured() {
         this.bIwakeupYou = true;
         this.notify();
      }

      public synchronized void iwait() {
         try {
            this.bIwakeupYou = false;
            this.wait(3000L);
         } catch (InterruptedException var2) {
            var2.printStackTrace();
         }

      }

      public void run() {
         int nTargetWidth = BioMiniOC4.this.getTargetWidth();
         int nTargetHeight = BioMiniOC4.this.getTargetHeight();
         int nIntWidth = BioMiniOC4.this.getIntermediateWidth();
         int nIntHeight = BioMiniOC4.this.getIntermediateHeight();
         int nRawWidth = BioMiniOC4.this.getRawWidth();
         int nRawHeight = BioMiniOC4.this.getRawHeight();
         BioMiniOC4.this.mProcessingCost = 0L;
         int bFingerOn = false;
         BioMiniOC4.this.mHasPreviewBuffered = 0;

         while(BioMiniOC4.this.bThreadFlag) {
            if (BioMiniOC4.this.m_nTop > -1) {
               this.iwait();
               BioMiniOC4.this.printTimeTag("StartCapturingLoop : Got captured notice");
               if (!this.bIwakeupYou || BioMiniOC4.this.bAbortCapturing) {
                  break;
               }

               int nTop = BioMiniOC4.this.m_nTop;
               long timerStart = SystemClock.currentThreadTimeMillis();
               byte[] tmpBuf = new byte[nIntWidth * nIntHeight + 64];
               int[] gainStep = new int[1];
               byte[] processedImg = new byte[nTargetWidth * nTargetHeight];
               byte[] output = new byte[nTargetWidth * nTargetHeight];
               Arrays.fill(tmpBuf, (byte)0);
               BioMiniJni.ConvertImage2(BioMiniOC4.this.m_pFullBufferN[nTop], tmpBuf, nIntWidth, nIntHeight);
               if (!BioMiniOC4.this.mUsbHandler.isNative()) {
                  int nCntBlacks = 0;
                  int nCntBlacksMax = 0;

                  for(int j = 0; j < nIntWidth * nIntHeight / 4; ++j) {
                     if (tmpBuf[j] == -2) {
                        ++nCntBlacks;
                        nCntBlacksMax = Math.max(nCntBlacksMax, nCntBlacks);
                     } else {
                        nCntBlacks = 0;
                     }
                  }

                  if (nCntBlacksMax >= 1) {
                     BioMiniOC4.this.mSleepPlus = 7;
                     BioMiniOC4.this.LogD("StartCapturingLoop OC4 : Broken Frame");
                     continue;
                  }

                  BioMiniOC4.this.LogD("StartCapturingLoop OC4 : Success Frame");
                  BioMiniOC4.this.mSleepPlus = 0;
               }

               boolean bFingerTouch = BioMiniJni.IsOC4FingerTouch(BioMiniOC4.this.m_pFullBufferN[nTop], 1, BioMiniOC4.this.pVarOC4[BioMiniOC4.this.m_nSensitivity], BioMiniOC4.this.pCountOC4[BioMiniOC4.this.m_nSensitivity], nIntWidth * nIntHeight + 64);
               BioMiniOC4.this.LogD("StartCapturingLoop OC4 : IsOC4FingerTouch : " + bFingerTouch);
               Arrays.fill(processedImg, (byte)-36);
               BioMiniJni.CompOC4(tmpBuf, processedImg, nTargetWidth, nTargetHeight);
               boolean bIsFingerOn = BioMiniJni.IsFingerOnOC4(this.pBioMiniAndroid, processedImg, nTargetWidth, nTargetHeight, bFingerTouch, gainStep, BioMiniOC4.this.pStepsOC4[BioMiniOC4.this.m_nSensitivity]);
               BioMiniOC4.this.LogD("StartCapturingLoop OC4 : IsFingerOnOC4 : " + bIsFingerOn);
               BioMiniJni.OC4PostProcessing(processedImg, BioMiniOC4.this.m_pFullBufferN[nTop], output);
               System.arraycopy(output, 0, BioMiniOC4.this.m_ImageIntermediate, 0, nTargetWidth * nTargetHeight);
               byte bFingerOnx;
               if (bIsFingerOn) {
                  bFingerOnx = 1;
               } else {
                  bFingerOnx = 0;
               }

               BioMiniOC4.this.isCaptured = true;
               BioMiniOC4.this.drawDebugMap(bFingerOnx, 0, 0, 0, (byte[])null, (byte[])null, (byte[])null, 0, 0, BioMiniOC4.this.m_ImageIntermediate, nTargetWidth, nTargetHeight);
               long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
               if (BioMiniOC4.this.mProcessingCost != 0L) {
                  BioMiniOC4.this.mProcessingCost = (long)((double)BioMiniOC4.this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
               } else {
                  BioMiniOC4.this.mProcessingCost = currentCost;
               }

               System.arraycopy(BioMiniOC4.this.m_ImageIntermediate, 0, BioMiniOC4.this.m_ImageLast, 0, BioMiniOC4.this.m_ImageIntermediate.length);
               BioMiniOC4.this.onCapture(BioMiniOC4.this.mCaptureResponder, BioMiniOC4.this.m_ImageLast, nTargetWidth, nTargetHeight, bFingerOnx == 1);
               if (BioMiniOC4.this.m_TimeOut != 0L && System.currentTimeMillis() - BioMiniOC4.this.m_Start > BioMiniOC4.this.m_TimeOut) {
                  BioMiniOC4.this.onCaptureError(BioMiniOC4.this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - BioMiniOC4.this.m_Start) + "/" + BioMiniOC4.this.m_TimeOut + ")");
                  break;
               }
            }
         }

         BioMiniOC4.this.bThreadFlag = false;
         BioMiniOC4.this.LogD("StartCapturingLoop : Capturing thread end");
         BioMiniOC4.this.CaptureFrameStop();
      }
   }

   private class UsbBulkLoopOC4 implements Runnable {
      BioMiniOC4 pBioMiniAndroid = null;
      BioMiniOC4.StartCapturingLoop StartCapturingLoop = null;
      byte fillExtra = -1;

      UsbBulkLoopOC4(BioMiniOC4 pMyp) {
         this.pBioMiniAndroid = pMyp;
      }

      UsbBulkLoopOC4(BioMiniOC4.StartCapturingLoop pMyp) {
         this.StartCapturingLoop = pMyp;
      }

      public void run() {
         BioMiniOC4.this.LogD(" -- UsbBulkLoopOC4 started... -- ");
         if (BioMiniOC4.this.mUsbHandler != null) {
            BioMiniOC4.this.mIsUsbThreadRunning = true;
            int Ret = false;
            int nRawWidth = BioMiniOC4.this.getRawWidth();
            int nRawHeight = BioMiniOC4.this.getRawHeight();
            int nIntWidth = BioMiniOC4.this.getIntermediateWidth();
            int nIntHeight = BioMiniOC4.this.getIntermediateHeight();
            int nBytesImage = nRawWidth * nRawHeight;
            int nBytesToRead = nBytesImage - nBytesImage % 16384;
            byte[] extra_64 = new byte[64];
            int num_errors = false;
            boolean useLibUsb = true;
            byte[] capBuffer;
            if (useLibUsb) {
               nBytesToRead = nBytesImage;
               capBuffer = new byte[nBytesImage];
               BioMiniOC4.this.mUsbHandler.setBulkRx(6);
               BioMiniOC4.this.mUsbHandler.setBulkTimeout(300);
            } else {
               capBuffer = new byte[nRawWidth * nRawHeight];
               Arrays.fill(capBuffer, nBytesToRead, nBytesImage, (byte)0);
               BioMiniOC4.this.mUsbHandler.setBulkTimeout(166);
            }

            int offset = 0;
            if (BioMiniOC4.this.m_bOC4Half) {
               offset = 1;
            }

            BioMiniOC4.this.mSleepVal = 20;
            this.fillExtra = -1;

            while(BioMiniOC4.this.bThreadFlag) {
               try {
                  int nexp = false;
                  int nTop = BioMiniOC4.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  if (BioMiniOC4.this.m_nSensorVer == 1) {
                     BioMiniOC4.this.m_OC4ExpPre = Math.max(184, Math.min(216, BioMiniOC4.this.m_nExposureOP5 - 235 + 200));
                  } else {
                     BioMiniOC4.this.m_OC4ExpPre = 80;
                  }

                  BioMiniOC4.this.m_OC4GainPre = 60;
                  BioMiniOC4.this.SetOC4IntegrationTime(BioMiniOC4.this.m_OC4ExpPre, BioMiniOC4.this.m_OC4GainPre);
                  BioMiniOC4.this.mUsbHandler.initRead(nBytesToRead, 0, false);
                  boolean re = BioMiniOC4.this.mUsbHandler.read(capBuffer, nBytesToRead, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniOC4.this.mUsbHandler.controlTx(BioMiniOC4.this.GetCmd(198), new byte[]{1}, 1);
                     }

                     public boolean afterRead() {
                        return true;
                     }
                  });
                  if (!re) {
                     BioMiniOC4.this.LogE("Bulk transfer error!");
                     break;
                  }

                  if (BioMiniOC4.this.mUsbHandler.isNative()) {
                     re = BioMiniOC4.this.mUsbHandler.read(extra_64, 64, 0, this.fillExtra);
                     if (!re) {
                        BioMiniOC4.this.LogE("Bulk transfer error!");
                        break;
                     }
                  }

                  int srcPos = 7 + offset;
                  int dstPos = 0;

                  for(int i = 0; i < nRawHeight; ++i) {
                     System.arraycopy(capBuffer, srcPos, BioMiniOC4.this.m_pFullBufferN[nTopNext], dstPos, nIntWidth);
                     srcPos += nRawWidth;
                     dstPos += nIntWidth;
                  }

                  System.arraycopy(BioMiniOC4.this.m_pFullBufferN[nTopNext], 0, BioMiniOC4.this.m_ImageRawPrevN, 0, nIntWidth * nIntHeight + 64);
                  if (nTop == 11) {
                     BioMiniOC4.this.m_bTopIter = true;
                  }

                  BioMiniOC4.this.m_nTop = nTopNext;
                  if (this.pBioMiniAndroid != null) {
                     this.pBioMiniAndroid.captured();
                  }

                  if (this.StartCapturingLoop != null) {
                     this.StartCapturingLoop.captured();
                  }

                  SystemClock.sleep((long)(BioMiniOC4.this.mSleepVal + BioMiniOC4.this.mSleepPlus));
               } catch (NullPointerException var20) {
                  BioMiniOC4.this.LogE("mUsbHandler missing");
                  break;
               }
            }

            BioMiniOC4.this.bThreadFlag = false;
            BioMiniOC4.this.mIsUsbThreadRunning = false;
            BioMiniOC4.this.LogD(" -- UsbBulkLoopOC4 finished... -- ");
         }
      }
   }
}
