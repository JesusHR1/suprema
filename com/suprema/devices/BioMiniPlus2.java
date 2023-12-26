package com.suprema.devices;

import android.os.SystemClock;
import android.util.Log;
import com.suprema.ABioMiniDevice;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;
import com.suprema.android.BioMiniJni;
import com.suprema.usb.IUsbHandler;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class BioMiniPlus2 extends BioMiniBase {
   private ICaptureResponder mCaptureResponder = null;
   private final int MAX_BULK_SIZE = 308224;
   private final int mImageTrSize = 308224;
   private boolean bIwakeupYou = false;
   private boolean mIsUsbThreadRunning = false;
   private int g_bExtraDry = -1;
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
   private static final int CAPTURE_BUFFER_SIZE = 12;
   private static final int DELAY_FOR_SUCCESSFUL_CAPTURE = 130;
   private long mLastNotification = 0L;
   private long mLastWait = 0L;
   private final int OPTIMAL_LOW_EXPO_PERCENTAGE = 59;
   private final int OPTIMAL_HIGH_EXPO_PERCENTAGE = 200;
   private long m_Start;
   private double m_expoScaleFactor = 1.0D;
   private int m_NormalExposure;
   private int m_NormalGain;
   private int m_AdaptiveExposure;
   private int m_AdaptiveGain;
   private long mProcessingCost = 0L;
   private static final int IMG_BUF_MAX = 307200;
   private static final int IMG_INT_BUF_MAX = 111510;
   private final byte[][] m_pFullBufferA = new byte[12][307209];
   private final byte[][] m_pFullBufferN = new byte[12][307209];
   private final byte[] m_ImageBufferBG = new byte[307200];
   private final byte[] m_Image = new byte[111510];
   private final byte[] m_ImageA = new byte[111510];
   private final byte[] m_ImageBG = new byte[111510];
   private byte[] m_ImagePrev = new byte[111510];
   private final byte[] m_ImageIntermediate = new byte[111510];
   private final byte[] m_ImageLast = new byte[111510];
   private byte[] m_ImageRawPrevA = new byte[307209];
   private byte[] tmpBuffer = new byte[307200];
   private int[] m_LCH1 = new int[12];
   private int[] m_LCH2 = new int[12];
   private byte[] m_TouchBuffer = new byte[6];
   private boolean bIsAfterAbortCpaturing = true;
   private boolean bUSBisdominated = false;
   private int mSleepPlus = 0;
   private int mSleepVal = 20;
   private Runnable mLoop;
   private Runnable mSLoop;
   private BioMiniPlus2.StartCapturingLoop mStartCapturingLoop;
   private Thread mUsbThread;
   private Thread mStartCapturingThread;
   private boolean bThreadFlag;
   private boolean bAbortCapturing = false;
   private boolean isBackGround = false;
   private int m_nCaptureMode;
   private int mHasPreviewBuffered;
   private int m_NHEH;
   private int m_NHGH;
   private int m_AEH;
   private int m_AGH;
   private int m_EW;
   private int m_EH;
   private int m_SOX;
   private int m_SOY;
   private int m_sclFX;
   private int m_sclFY;
   private int m_nTop;
   private int m_nLTop;
   private boolean m_bTopIter;
   private boolean m_bLTopIter;
   private int mPLUS2_StartPosX = 0;
   private int mPLUS2_StartPosY = 0;
   private byte mPaddingValue = -1;
   private int m_nBGAvg = -1;
   private boolean m256K_Mode = true;
   private Queue<BioMiniBase.MDRCapturedPair> mCapturedQueue = new LinkedList();
   static int kk = 0;
   private static final int CMD_LED_CTRL = 194;
   private static final int CMD_SET_REG = 195;
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
   private static final int CMD_SET_CIS_TIME = 192;
   private static final int CMD_ANDROID_256K_MODE = 234;
   private static final int CMD_ANDROID_256K_SXSY = 235;
   private static final int OV_IIC_EEPROM_ADDR = 174;

   public BioMiniPlus2() {
      this.TAG = "BioMiniPlus2";
   }

   private boolean isSmallBulkMode() {
      return this.mUsbHandler != null && this.m256K_Mode;
   }

   public int getMaxBulkSize() {
      return 308224;
   }

   public boolean captureSingle(final IBioMiniDevice.CaptureOption opt, final ICaptureResponder responder, boolean bAsync) {
      if (!this.isCapturing() && !this.mIsUsbThreadRunning) {
         this.mCaptureResponder = responder;
         if (bAsync) {
            Runnable captureObj = new Runnable() {
               public void run() {
                  BioMiniPlus2.this._captureSingle(opt, responder);
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

   private boolean setReg(int addr, int val) {
      return this.mUsbHandler.controlTx(this.GetCmd(195), new byte[]{(byte)addr, (byte)val}, 2);
   }

   private boolean getReg(int addr, byte[] data) {
      return this.mUsbHandler.controlTx(210, new byte[]{(byte)addr}, 1) ? this.mUsbHandler.controlRx(211, data, 1) : false;
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
         int nBytesImage = nRawWidth * nRawHeight;
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
               boolean useNewUsbBulk = true;
               this.mLoop = new BioMiniPlus2.UsbBulkLoopRev(this);
               this.mUsbThread = new Thread(this.mLoop);
               this.mUsbThread.start();
               int bFingerOn = false;
               int bcheckCorr = false;
               float gce_gain = 1.0F;
               float lce_gain = 0.4F;
               float de_gain = 0.03F;
               if (this.g_bExtraDry == 1) {
                  gce_gain = 0.0F;
                  lce_gain = 0.0F;
                  de_gain = 0.0F;
               } else {
                  gce_gain = 0.8F;
                  lce_gain = 0.4F;
                  de_gain = 0.02F;
               }

               boolean bThreadFlagLocal = this.bThreadFlag;
               int var18 = 0;

               while(bThreadFlagLocal) {
                  if (this.m_nTop <= -1) {
                     SystemClock.sleep(50L);
                  } else {
                     this.LogD("CaptureSingle : Waiting for image transferred");
                     this.LogProcessStart("CapturingWait");
                     this.waitForCaptured();
                     this.LogProcessEnd("CapturingWait");
                     bThreadFlagLocal = this.bThreadFlag;
                     if (this.bAbortCapturing) {
                        break;
                     }

                     int nTop = this.m_nTop;
                     long timerStart = SystemClock.uptimeMillis();
                     byte[] imageN = null;
                     byte[] imageA = null;
                     int MAX_POLLING_TRIES = 50;
                     int dequeingCnt = 0;

                     BioMiniBase.MDRCapturedPair tmpImageBuffer;
                     for(tmpImageBuffer = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll(); tmpImageBuffer == null && dequeingCnt < MAX_POLLING_TRIES; ++dequeingCnt) {
                        SystemClock.sleep(100L);
                        tmpImageBuffer = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll();
                     }

                     while(!this.mCapturedQueue.isEmpty()) {
                        BioMiniBase.MDRCapturedPair tmpCp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll();
                        if (tmpCp != null) {
                           tmpImageBuffer = tmpCp;
                        }
                     }

                     if (tmpImageBuffer != null) {
                        imageN = tmpImageBuffer.MdrN.Image;
                        imageA = tmpImageBuffer.MdrA.Image;
                     }

                     if (imageN != null && imageA != null) {
                        int bFingerOn = imageN[nBytesImage + 8];
                        this.LogD("CaptureSingle : Got bFingerOn(" + bFingerOn + ", " + imageN[nBytesImage + 8] + ") @" + nTop);
                        this.LogD("CaptureSingle : bIwakeupYou :" + this.bIwakeupYou);
                        if (!this.bIwakeupYou && bFingerOn == 0) {
                           this.LogD("break;");
                           break;
                        }

                        this.LogD("CaptureSingle : Compensating... " + this.m_Image + ", " + this.m_ImageA);
                        this.LogProcessStart("Comp");
                        BioMiniJni.Comp(imageN, this.m_Image, bFingerOn);
                        BioMiniJni.Comp(imageA, this.m_ImageA, bFingerOn);
                        this.LogProcessEnd("Comp");
                        this.LogProcessStart("Corr");
                        int bcheckCorr;
                        if (this.mHasPreviewBuffered == 0) {
                           bcheckCorr = BioMiniJni.CheckCorrelation(this.m_Image, this.m_Image, 60);
                           this.mHasPreviewBuffered = 1;
                        } else {
                           bcheckCorr = BioMiniJni.CheckCorrelation(this.m_ImagePrev, this.m_Image, 60);
                        }

                        this.LogProcessEnd("Corr");
                        this.LogD("CaptureSingle : CheckCorrelation done... (" + bcheckCorr + ")");
                        this.m_ImagePrev = Arrays.copyOf(this.m_Image, nIntWidth * nIntHeight);
                        this.LogD("CaptureSingle : Preprocessing... " + this.m_Image + ", " + this.m_ImageA);
                        this.LogProcessStart("Preprocessing");
                        if (bFingerOn == 1 && bcheckCorr == 1) {
                           BioMiniJni.GetPreprocessedImage(this.m_ImageBG, this.m_Image, this.m_ImageA, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                        } else if (bFingerOn == 1) {
                           BioMiniJni.GetPreprocessedImageEx(this.m_ImageBG, this.m_Image, this.m_Image, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64, !this.isBackGround);
                        } else {
                           BioMiniJni.GetPreprocessedImageEx(this.m_ImageBG, this.m_Image, this.m_Image, this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64, !this.isBackGround);
                        }

                        this.LogProcessEnd("Preprocessing");
                        ++var18;
                        long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
                        if (this.mProcessingCost != 0L) {
                           this.mProcessingCost = (long)((double)this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                        } else {
                           this.mProcessingCost = currentCost;
                        }

                        tmpImageBuffer = null;
                        if (this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_CROP) {
                           byte[] tmpImageBuffer = new byte[97920];
                           int off_x = (int)Math.ceil(13.0D);
                           int off_y = 14;

                           for(int i = 0; i < 340; ++i) {
                              System.arraycopy(this.m_ImageIntermediate, 315 * (i + off_y) + off_x, tmpImageBuffer, 288 * i, 288);
                           }

                           System.arraycopy(tmpImageBuffer, 0, this.m_ImageIntermediate, 0, 97920);
                        }

                        System.arraycopy(this.m_ImageIntermediate, 0, this.m_ImageLast, 0, this.m_ImageIntermediate.length);
                        if (this.m_TimeOut != 0L && System.currentTimeMillis() - this.m_Start > this.m_TimeOut) {
                           this.LogD("Capture timeout occurred");
                           this.onCaptureError(this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - this.m_Start) + "/" + this.m_TimeOut + ")");
                           this.mIsTimeoutOccurred = true;
                           break;
                        }

                        ++var18;
                        if (bFingerOn == 1) {
                           this.LogD("CaptureSingle : isCaptured is true");
                           this.isCaptured = true;
                           break;
                        }
                     } else {
                        this.LogE("CaptureSingle null image buffer");
                     }
                  }
               }

               this.LogD("CaptureSingle : Process loop finished");
               this.bThreadFlag = false;
               this.CaptureFrameStop();
               this.LogPublicProcessEnd("AutoCapture");
               this.LogD("CaptureSingle : Done capturing a fingerprint");
               if (this.isCaptured) {
                  if (this.m_nCaptureMode == 3) {
                     this.LogProcessStart("LFD");
                     this.Plus2LFDWorker();
                     this.LogProcessEnd("LFD");
                     if (this.mDetectedFake) {
                        this.LogD("CaptureSingle : Got lfd result(Fake Finger), Score(" + this.mDetectedFakeScore + ")");
                     } else {
                        this.LogD("CaptureSingle : Got lfd result(Live Finger), Score(" + this.mDetectedFakeScore + ")");
                     }

                     this.m_LastError = this.mDetectedFake ? IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER : IBioMiniDevice.ErrorCode.OK;
                  } else {
                     this.m_LastError = IBioMiniDevice.ErrorCode.OK;
                  }

                  if (this.m_ImageLast != null && pImage != null) {
                     System.arraycopy(this.m_ImageLast, 0, pImage, 0, nTargetWidth * nTargetHeight);
                  }
               } else {
                  this.LogD("CaptureSingle : No fingerprint captured");
                  if (this.mIsTimeoutOccurred) {
                     this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_TIMEOUT;
                  } else {
                     this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
                  }

                  this.mIsTimeoutOccurred = false;
               }

               return this.m_LastError;
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
               boolean useNewUsbBulk = true;
               this.mStartCapturingLoop = new BioMiniPlus2.StartCapturingLoop(this, true);
               this.mStartCapturingThread = new Thread(this.mStartCapturingLoop);
               this.mSLoop = new BioMiniPlus2.UsbBulkLoopRev(this.mStartCapturingLoop);
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

   private int getBulkWidth() {
      return this.isSmallBulkMode() ? 576 : this.getRawWidth();
   }

   private int getBulkHeight() {
      return this.isSmallBulkMode() ? 448 : this.getRawHeight();
   }

   private boolean Switch_256K_mode(boolean bFlag) {
      this.LogD("Switch_256K_mode / bFlag :" + bFlag);
      boolean bRet = false;
      byte[] cmd;
      if (bFlag) {
         cmd = new byte[64];
         cmd[0] = 1;
         this.mUsbHandler.controlTx(234, cmd, 1);
         this.LogD("0x0409:: set 256K_mode return " + cmd[0]);
         int oStartPosX = this.m_SOX - 10 + 32;
         int oStartPosY = this.m_SOY + 16;
         this.LogD(String.format(Locale.ENGLISH, "origin PlusStartPosX (%d) , origin PlustStartPosY(%d) ", oStartPosX, oStartPosY));
         this.mPLUS2_StartPosX = Math.max(0, Math.min(oStartPosX, 64));
         this.mPLUS2_StartPosY = Math.max(0, Math.min(oStartPosY, 40));
         this.LogD(String.format(Locale.ENGLISH, " mPlus2_StartPOS X(%d) , mPlus2_StartPos Y (%d) ", this.mPLUS2_StartPosX, this.mPLUS2_StartPosY));
         cmd[0] = (byte)(this.mPLUS2_StartPosX & 255);
         cmd[1] = (byte)(this.mPLUS2_StartPosY & 255);
         this.LogD(String.format(Locale.ENGLISH, " StartPosX (%d) , StartPosY (%d) ", cmd[0], cmd[1]));
         boolean b = this.mUsbHandler.controlTx(235, cmd, 2);
         if (b) {
            this.LogD("Set command SX SY return true");
         } else {
            this.LogD("Set command SX SY return false");
         }
      } else {
         cmd = new byte[64];
         cmd[0] = 0;
         this.mUsbHandler.controlTx(234, cmd, 1);
         this.LogD("0x0409:: set off 256K_mode return " + cmd[0]);
      }

      return bRet;
   }

   private void RescaleImage(byte[] pbDownScaleImage, int nBulkWidth, int nBulkHeight, int nRawWidth, int nRawHeight) {
      this.LogD("nBulkWidth :" + nBulkWidth + " , nBulkHeight:" + nBulkHeight + "  nRawWidth :" + nRawWidth + " , nRawHeight:" + nRawHeight);
      this.LogD("RescaleImage, ");
      byte[] tmpBuffer = new byte[nRawWidth * nRawHeight];
      Arrays.fill(tmpBuffer, (byte)0);
      int offy = 16;
      int offx = 32;

      for(int h = 0; h < nBulkHeight && h + this.mPLUS2_StartPosY < nRawHeight; ++h) {
         System.arraycopy(pbDownScaleImage, h * nBulkWidth, tmpBuffer, (h + offy) * nRawWidth + offx, nBulkWidth);
      }

      System.arraycopy(tmpBuffer, 0, pbDownScaleImage, 0, nRawWidth * nRawHeight);
   }

   private void Plus2LFDWorker() {
      int m_ImageLast_width = 288;
      int m_ImageLast_height = 340;
      int scanner_mode = 1;
      if (this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_FULL) {
         m_ImageLast_width = 315;
         m_ImageLast_height = 354;
         scanner_mode = 0;
      }

      int liveness = false;
      boolean fakeDetected = false;
      int[] score = new int[1];
      int split_height = m_ImageLast_height * 3 / 4;
      int off_y = m_ImageLast_height - split_height;
      byte[] data1 = new byte[m_ImageLast_width * split_height];
      byte[] data2 = new byte[m_ImageLast_width * split_height];

      for(int yy = 0; yy < split_height; ++yy) {
         for(int xx = 0; xx < m_ImageLast_width; ++xx) {
            data1[yy * m_ImageLast_width + xx] = this.m_ImageLast[yy * m_ImageLast_width + xx];
            data2[yy * m_ImageLast_width + xx] = this.m_ImageLast[(yy + off_y) * m_ImageLast_width + xx];
         }
      }

      BioMiniPlus2.Plus2LFDWorkerLoop w1 = new BioMiniPlus2.Plus2LFDWorkerLoop(data1, scanner_mode, this.m_DetectFake, this.m_NHEH);
      BioMiniPlus2.Plus2LFDWorkerLoop w2 = new BioMiniPlus2.Plus2LFDWorkerLoop(data2, scanner_mode, this.m_DetectFake, this.m_NHEH);
      Thread th1 = new Thread(w1);
      th1.start();
      Thread th2 = new Thread(w2);
      th2.start();
      boolean var24 = false;

      int liveness;
      label152: {
         float[] scoretable;
         float nLFDLevel;
         int fake_level;
         label153: {
            try {
               var24 = true;
               th1.join();
               th2.join();
               var24 = false;
               break label153;
            } catch (InterruptedException var25) {
               var25.printStackTrace();
               var24 = false;
            } finally {
               if (var24) {
                  score[0] = (w1.Score() + w2.Score()) / 2;
                  float[] scoretable = new float[]{0.0F, 38.0F, 48.0F, 50.0F, 54.0F, 58.0F};
                  float nLFDLevel = (float)score[0] / 10.0F;
                  int fake_level = Math.min(5, Math.max(0, this.m_DetectFake));
                  if (!(nLFDLevel < scoretable[fake_level]) && w1.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value() && w2.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value()) {
                     liveness = score[0];
                  } else {
                     fakeDetected = true;
                     liveness = score[0];
                  }

               }
            }

            score[0] = (w1.Score() + w2.Score()) / 2;
            scoretable = new float[]{0.0F, 38.0F, 48.0F, 50.0F, 54.0F, 58.0F};
            nLFDLevel = (float)score[0] / 10.0F;
            fake_level = Math.min(5, Math.max(0, this.m_DetectFake));
            if (!(nLFDLevel < scoretable[fake_level]) && w1.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value() && w2.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value()) {
               liveness = score[0];
               break label152;
            }

            fakeDetected = true;
            liveness = score[0];
            break label152;
         }

         score[0] = (w1.Score() + w2.Score()) / 2;
         scoretable = new float[]{0.0F, 38.0F, 48.0F, 50.0F, 54.0F, 58.0F};
         nLFDLevel = (float)score[0] / 10.0F;
         fake_level = Math.min(5, Math.max(0, this.m_DetectFake));
         if (!(nLFDLevel < scoretable[fake_level]) && w1.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value() && w2.re != IBioMiniDevice.ErrorCode.ERR_FAKE_FINGER.value()) {
            liveness = score[0];
         } else {
            fakeDetected = true;
            liveness = score[0];
         }
      }

      this.mDetectedFake = fakeDetected;
      this.mDetectedFakeScore = liveness;
   }

   private int CaptureFrameStart() {
      this.bUSBisdominated = true;
      this.bAbortCapturing = false;
      this.isCaptured = false;
      boolean re = false;
      int nWidth = this.getRawWidth();
      int nHeight = this.getRawHeight();
      int nBulkWidth = this.getBulkWidth();
      int nBulkHeight = this.getBulkHeight();
      int nBulkLength = nBulkWidth * nBulkHeight;
      this.mHasPreviewBuffered = 0;
      this.LogD(String.format(Locale.ENGLISH, "Timeout(%d) , injected timeout(%d)", this.m_TimeOut, this.mCurrentCaptureOption.captureTimeout));
      this.setTempCaptureOpts();
      this.LogD("Setting camera parameter...");
      if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.ELOW) {
         this.setReg(32, 48);
         this.m_expoScaleFactor = 0.72D;
         this.LogD("SetFrameRate - ELOW ");
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.LOW) {
         this.setReg(32, 48);
         this.m_expoScaleFactor = 0.5D;
         this.LogD("SetFrameRate - LOW ");
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.MID) {
         this.setReg(32, 32);
         this.m_expoScaleFactor = 0.67D;
         this.LogD("SetFrameRate - MID ");
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.HIGH) {
         this.setReg(32, 16);
         this.m_expoScaleFactor = 0.8D;
         this.LogD("SetFrameRate - HIGH ");
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
         this.setReg(32, 0);
         this.m_expoScaleFactor = 1.0D;
         this.LogD("SetFrameRate - SHIGH ");
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SLOW) {
         this.setReg(4, 4);
         this.m_expoScaleFactor = 0.5D;
         this.LogD("SetFrameRate - SLOW adjust reg(CLK_DIV)");
      }

      this.Switch_256K_mode(this.isSmallBulkMode());
      this.m_NHEH = (int)((double)this.m_NormalExposure * this.m_expoScaleFactor);
      this.m_NHGH = this.m_NormalGain;
      this.m_AEH = (int)((double)this.m_AdaptiveExposure * this.m_expoScaleFactor);
      this.m_AGH = this.m_AdaptiveGain;
      re = this.SetIntegrationTime(this.m_NHEH, this.m_NHGH, this.m_AEH, this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
      if (!re) {
         Log.e(this.TAG, "Setting camera parameter failed");
         return 0;
      } else {
         SystemClock.sleep(70L);
         this.m_Start = System.currentTimeMillis();
         this.LogD("Turning on LED...");
         re = this.mUsbHandler.controlTx(this.GetCmd(194), new byte[]{4, 0, 0, 0}, 1);
         if (!re) {
            Arrays.fill(this.m_ImageBufferBG, (byte)-1);
            Arrays.fill(this.m_ImageBG, (byte)-1);
            Log.e(this.TAG, "LED control failed");
            return 0;
         } else {
            this.LogD("Capturing BG...");
            this.m_nTop = -1;
            this.m_bTopIter = false;
            this.m_nLTop = 0;
            this.m_bLTopIter = false;
            int bFingerOn = false;
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 100, 80};
            this.mUsbHandler.setBulkRx(2);
            if (this.mUsbHandler.isNative()) {
               this.mUsbHandler.setBulkTimeout(500);
            }

            this.mUsbHandler.initRead(nBulkLength, 0, false);
            byte fillExtra = 0;
            this.mUsbHandler.read(this.m_ImageBufferBG, nBulkLength, fillExtra, new IUsbHandler.IReadProcessor() {
               public boolean beforeRead() {
                  return BioMiniPlus2.this.mUsbHandler.controlRx(BioMiniPlus2.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
               }

               public boolean afterRead() {
                  return BioMiniPlus2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
               }
            });
            if (this.isSmallBulkMode()) {
               this.RescaleImage(this.m_ImageBufferBG, nBulkWidth, nBulkHeight, nWidth, nHeight);
            }

            BioMiniJni.AdjustRaw(this.m_ImageBufferBG);
            this.LogD("Detecting FingerprintArea");
            int nblockw = 32;
            int nblockh = 64;
            int bFingerOn = BioMiniJni.DetectFingerprintArea_For_Background(this.m_ImageBufferBG, this.m_ImageBufferBG, nblockw, nblockh, nWidth, nHeight, pVarS[this.m_nSensitivity]);
            int[] avgArray = new int[4];
            BioMiniJni.GetAvg(avgArray);
            this.m_nBGAvg = avgArray[0];
            if (bFingerOn == 1) {
               if (!this.isBackGround) {
                  this.LogD("Generating BG...");
                  byte[] tmpBuffer2 = Arrays.copyOf(this.m_ImageBufferBG, nWidth * nHeight);
                  BioMiniJni.BKCompensateSelf2D(tmpBuffer2, this.m_ImageBufferBG, this.tmpBuffer, nWidth, nHeight);
                  BioMiniJni.Comp(this.m_ImageBufferBG, this.m_ImageBG, 0);
                  this.LogD("Generating BG done...");
               }
            } else {
               this.LogD("Compensating BG...");
               this.isBackGround = true;
               BioMiniJni.Comp(this.m_ImageBufferBG, this.m_ImageBG, 0);
               this.LogD("Compensation done...");
            }

            return 1;
         }
      }
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

      this.LogD("checks reading...");
      if (this.mUsbHandler != null && this.mUsbHandler.isReading()) {
         this.mUsbHandler.resetBulkPipe(false);
      }

      this.mLoop = null;
      this.mSLoop = null;
      this.mUsbThread = null;
      if (this.mUsbHandler != null) {
         this.mUsbHandler.controlTx(194, new byte[]{0, 0, 0, 0}, 1);
      }

      this.bUSBisdominated = false;
      this.resetCaptureOpts();
      this.LogD("Capture stopped");
   }

   private int GetCmd(int keyword) {
      switch(keyword) {
      case 194:
         return 194;
      case 195:
         return 209;
      case 201:
         return 219;
      case 225:
         return 225;
      case 226:
         return 226;
      default:
         return 0;
      }
   }

   private boolean SetIntegrationTime(int nNormalExposure, int nNormalGain, int nAdvanceExposure, int nAdvanceGain, int nUpperExposure, int nUpperGain, int nLowerExposure, int nLowerGain, int nSpecialExposure, int nSpecialGain) {
      byte[] Buffer = new byte[64];
      Buffer[0] = (byte)(nNormalExposure >> 8 & 255);
      Buffer[1] = (byte)(nNormalExposure & 255);
      Buffer[2] = (byte)(nNormalGain & 255);
      Buffer[3] = (byte)(nAdvanceExposure >> 8 & 255);
      Buffer[4] = (byte)(nAdvanceExposure & 255);
      Buffer[5] = (byte)(nAdvanceGain & 255);
      Buffer[6] = (byte)(nUpperExposure >> 8 & 255);
      Buffer[7] = (byte)(nUpperExposure & 255);
      Buffer[8] = (byte)(nUpperGain & 255);
      Buffer[9] = (byte)(nLowerExposure >> 8 & 255);
      Buffer[10] = (byte)(nLowerExposure & 255);
      Buffer[11] = (byte)(nLowerGain & 255);
      Buffer[12] = (byte)(nSpecialExposure >> 8 & 255);
      Buffer[13] = (byte)(nSpecialExposure & 255);
      Buffer[14] = (byte)(nSpecialGain & 255);
      return this.mUsbHandler.controlTx(192, Buffer, 15);
   }

   public int Setting(int pid) {
      byte[] bufWrite = new byte[64];
      byte[] bufRead = new byte[64];
      BioMiniJni.SwitchScanningMode(this.m_nScanningMode.value());
      this.mDeviceInfo.deviceName = this.TAG;
      this.mDeviceInfo.versionSDK = this.BASE_VERSION;
      this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI_PLUS2;
      Arrays.fill(bufRead, (byte)0);
      this.mUsbHandler.controlRx(this.GetCmd(201), bufRead, 32);
      this.mDeviceInfo.deviceSN = (new String(bufRead, 0, 32)).trim();
      bufWrite[0] = 1;
      this.mUsbHandler.controlTx(201, bufWrite, 1);
      boolean re = this.mUsbHandler.readEEPROM(4, 4, bufRead);
      if (!re) {
         return 0;
      } else {
         re = this.mUsbHandler.readEEPROM(48, 32, bufRead);
         if (!re) {
            return 0;
         } else {
            this.m_AdaptiveExposure = bufRead[8] << 8 | bufRead[9] & 255;
            this.m_AdaptiveGain = bufRead[10];
            this.m_NormalExposure = bufRead[24] << 8 | bufRead[25] & 255;
            this.m_NormalGain = bufRead[26];
            this.mUsbHandler.readSensorEEPROM(96, 3, bufRead);
            this.m_NormalExposure = (bufRead[0] << 8) + (bufRead[1] & 255);
            this.m_NormalGain = bufRead[2];
            this.m_AdaptiveExposure = 80;
            this.m_AdaptiveGain = 0;
            String BMP2 = "BIOMINIPLUS2";
            String SFU = "SFU-550";
            String BMP2ES = "BMP2ES";
            String BMP2PS = "BMP2PS";
            int refDate = 201704;
            String _SN = this.mDeviceInfo.deviceSN;
            this.LogD("_SN :" + _SN);
            if (!_SN.contains("BIOMINIPLUS2") && !_SN.contains("SFU-550")) {
               if (_SN.contains("BMP2ES") || _SN.contains("BMP2PS")) {
                  this.m_NHEH = this.m_NHEH / 2 + 50;
               }
            } else {
               String str_containText = _SN.contains("BIOMINIPLUS2") ? "BIOMINIPLUS2" : "SFU-550";
               String str_date = _SN.subSequence(_SN.indexOf(str_containText) + str_containText.length(), _SN.length()).subSequence(0, 6).toString();
               this.LogD("str_date : " + str_date);

               try {
                  int _ndate = Integer.parseInt(str_date);
                  if (_ndate < 201704) {
                     this.m_NHEH = this.m_NHEH / 2 + 50;
                  }
               } catch (Exception var14) {
                  var14.printStackTrace();
               }
            }

            re = this.mUsbHandler.readEEPROM(80, 32, bufRead);
            if (!re) {
               return 0;
            } else {
               this.m_EW = 315;
               this.m_EH = 354;
               this.m_EW = this.m_EW * 315 / 315;
               this.m_EH = this.m_EH * 354 / 354;
               this.m_SOX = 10;
               this.m_SOY = 20;
               this.m_sclFX = 312;
               this.m_sclFY = 308;
               re = this.mUsbHandler.readSensorEEPROM(112, 4, bufRead);
               if (!re) {
                  return 0;
               } else {
                  this.LogD("Setting #4");
                  if (bufRead[0] == 255 && bufRead[1] == 255) {
                     this.m_SOX = 0;
                     this.m_SOY = 0;
                  } else if (bufRead[2] == 0 && bufRead[3] == 0) {
                     this.m_SOX = (bufRead[0] & 255) - 128 + 10;
                     this.m_SOY = (bufRead[1] & 255) - 128 + 20;
                  } else {
                     this.m_SOX = (bufRead[0] & 255) - 128 + (bufRead[2] & 255);
                     this.m_SOY = (bufRead[1] & 255) - 128 + (bufRead[3] & 255);
                  }

                  this.LogD("off-x = " + this.m_SOX + ", off-y = " + this.m_SOY);
                  re = this.mUsbHandler.readSensorEEPROM(160, 4, bufRead);
                  if (!re) {
                     return 0;
                  } else {
                     this.LogD("Setting #5");
                     if (bufRead[0] == 0 && bufRead[1] == 0) {
                        this.m_sclFX = 312;
                        this.m_sclFY = 308;
                     } else {
                        this.m_sclFX = ((bufRead[0] & 255) << 8) + bufRead[1];
                        this.m_sclFY = ((bufRead[2] & 255) << 8) + bufRead[3];
                     }

                     this.LogD("m_sclFX = " + this.m_sclFX + ", m_sclFY= " + this.m_sclFY);
                     bufWrite[0] = 0;
                     this.mUsbHandler.controlTx(201, bufWrite, 1);
                     BioMiniJni.setESA(this.m_EW, this.m_EH, this.m_SOX, this.m_SOY, (float)this.m_sclFX, (float)this.m_sclFY);
                     return 1;
                  }
               }
            }
         }
      }
   }

   public int getImageWidth() {
      return this.getTargetWidth();
   }

   public int getImageHeight() {
      return this.getTargetHeight();
   }

   private int getTargetWidth() {
      if (this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_FULL) {
         return 315;
      } else {
         return this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_CROP ? 288 : -1;
      }
   }

   private int getTargetHeight() {
      if (this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_FULL) {
         return 354;
      } else {
         return this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_CROP ? 340 : -1;
      }
   }

   private int getRawWidth() {
      return 640;
   }

   private int getRawHeight() {
      return 480;
   }

   private int getIntermediateWidth() {
      return 315;
   }

   private int getIntermediateHeight() {
      return 354;
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
      return this.mUsbHandler.controlRx(217, buffer, 2) ? (new String(buffer)).trim() : "";
   }

   public boolean isAwake() {
      return true;
   }

   public boolean hibernate() {
      return false;
   }

   public boolean wakeUp() {
      return true;
   }

   protected void rotateImage() {
      byte[] m_ImageTemp = new byte[this.getImageHeight() * this.getImageWidth()];

      for(int i = 0; i < this.getImageHeight() * this.getImageWidth(); ++i) {
         m_ImageTemp[i] = this.m_ImageLast[this.getImageHeight() * this.getImageWidth() - 1 - i];
      }

      System.arraycopy(m_ImageTemp, 0, this.m_ImageLast, 0, this.getImageHeight() * this.getImageWidth());
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

   private class Plus2LFDWorkerLoop implements Runnable {
      byte[] fp_data;
      int m_mode;
      int m_DetectFake;
      int m_NormalExp;
      int[] mScore = new int[1];
      int re;

      Plus2LFDWorkerLoop(byte[] _data, int _mode, int _detectFake, int _normalExposure) {
         this.fp_data = _data;
         this.m_mode = _mode;
         this.m_DetectFake = _detectFake;
         Log.e(BioMiniPlus2.this.TAG, "m_DetectFake : " + this.m_DetectFake);
         this.m_NormalExp = _normalExposure;
         this.re = 0;
      }

      public int Score() {
         return this.mScore[0];
      }

      public void run() {
         this.re = BioMiniJni.GetLFDResult(this.fp_data, this.m_mode, this.m_DetectFake, this.m_NormalExp, this.mScore);
         Log.e(BioMiniPlus2.this.TAG, " GetLFDResult :" + this.re);
      }
   }

   private class StartCapturingLoop implements Runnable {
      BioMiniPlus2 pBioMiniAndroid;
      boolean bIwakeupYou = false;
      boolean IsUsingNewBulkThread = true;

      StartCapturingLoop(BioMiniPlus2 pMyp, boolean useNewBulkLoop) {
         this.pBioMiniAndroid = pMyp;
         this.IsUsingNewBulkThread = useNewBulkLoop;
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
         int nTargetWidth = BioMiniPlus2.this.getTargetWidth();
         int nTargetHeight = BioMiniPlus2.this.getTargetHeight();
         int nIntWidth = BioMiniPlus2.this.getIntermediateWidth();
         int nIntHeight = BioMiniPlus2.this.getIntermediateHeight();
         int nRawWidth = BioMiniPlus2.this.getRawWidth();
         int nRawHeight = BioMiniPlus2.this.getRawHeight();
         int nBytesImage = nRawWidth * nRawHeight;
         float gce_gain = 1.0F;
         float lce_gain = 0.4F;
         float de_gain = 0.03F;
         if (BioMiniPlus2.this.g_bExtraDry == 1) {
            gce_gain = 0.0F;
            lce_gain = 0.0F;
            de_gain = 0.0F;
         } else {
            gce_gain = 0.8F;
            lce_gain = 0.4F;
            de_gain = 0.02F;
         }

         BioMiniPlus2.this.mProcessingCost = 0L;
         int bFingerOnx = false;
         int bcheckCorrx = false;
         BioMiniPlus2.this.mHasPreviewBuffered = 0;

         while(BioMiniPlus2.this.bThreadFlag) {
            if (BioMiniPlus2.this.m_nTop > -1) {
               this.iwait();
               BioMiniPlus2.this.printTimeTag("StartCapturingLoop : Got captured notice");
               if (!this.bIwakeupYou || BioMiniPlus2.this.bAbortCapturing) {
                  break;
               }

               int nTop = BioMiniPlus2.this.m_nTop;
               long timerStart = SystemClock.currentThreadTimeMillis();
               byte[] imageN = null;
               byte[] imageA = null;
               BioMiniBase.MDRCapturedPair tmpImageBufferx;
               if (this.IsUsingNewBulkThread) {
                  BioMiniBase.MDRCapturedPair cp = (BioMiniBase.MDRCapturedPair)BioMiniPlus2.this.mCapturedQueue.poll();

                  for(tmpImageBufferx = (BioMiniBase.MDRCapturedPair)BioMiniPlus2.this.mCapturedQueue.poll(); tmpImageBufferx != null; tmpImageBufferx = (BioMiniBase.MDRCapturedPair)BioMiniPlus2.this.mCapturedQueue.poll()) {
                     cp = tmpImageBufferx;
                  }

                  if (cp != null) {
                     imageN = cp.MdrN.Image;
                     imageA = cp.MdrA.Image;
                  }
               } else {
                  imageN = BioMiniPlus2.this.m_pFullBufferN[nTop];
                  imageA = BioMiniPlus2.this.m_pFullBufferA[nTop];
               }

               if (imageN != null && imageA != null) {
                  int bFingerOn = imageN[nBytesImage + 8];
                  BioMiniJni.Comp(imageN, BioMiniPlus2.this.m_Image, bFingerOn);
                  BioMiniJni.Comp(imageA, BioMiniPlus2.this.m_ImageA, bFingerOn);
                  BioMiniPlus2.this.printTimeTag("StartCapturingLoop : Compensation done");
                  int bcheckCorr;
                  if (BioMiniPlus2.this.mHasPreviewBuffered == 0) {
                     bcheckCorr = BioMiniJni.CheckCorrelation(BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_Image, 60);
                     BioMiniPlus2.this.mHasPreviewBuffered = 1;
                  } else {
                     bcheckCorr = BioMiniJni.CheckCorrelation(BioMiniPlus2.this.m_ImagePrev, BioMiniPlus2.this.m_Image, 60);
                  }

                  BioMiniPlus2.this.LogD("StartCapturingLoop : CheckCorrelation done... (" + bcheckCorr + ")");
                  BioMiniPlus2.this.m_ImagePrev = Arrays.copyOf(BioMiniPlus2.this.m_Image, nIntWidth * nIntHeight);
                  if (bFingerOn == 1 && bcheckCorr == 1) {
                     BioMiniJni.GetPreprocessedImage(BioMiniPlus2.this.m_ImageBG, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_ImageA, BioMiniPlus2.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                  } else if (bFingerOn == 1) {
                     BioMiniJni.GetPreprocessedImageEx(BioMiniPlus2.this.m_ImageBG, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64, !BioMiniPlus2.this.isBackGround);
                  } else {
                     BioMiniJni.GetPreprocessedImageEx(BioMiniPlus2.this.m_ImageBG, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64, !BioMiniPlus2.this.isBackGround);
                  }

                  BioMiniPlus2.this.printTimeTag("StartCapturingLoop : Preprocessing done");
                  BioMiniPlus2.this.isCaptured = true;
                  tmpImageBufferx = null;
                  if (BioMiniPlus2.this.getProductId() == 1033 && BioMiniPlus2.this.m_nScanningMode == IBioMiniDevice.ScanningMode.SCANNING_MODE_CROP) {
                     byte[] tmpImageBuffer = new byte[97920];
                     int off_x = (int)Math.ceil(13.0D);
                     int off_y = 14;

                     for(int i = 0; i < 340; ++i) {
                        System.arraycopy(BioMiniPlus2.this.m_ImageIntermediate, 315 * (i + off_y) + off_x, tmpImageBuffer, 288 * i, 288);
                     }

                     System.arraycopy(tmpImageBuffer, 0, BioMiniPlus2.this.m_ImageIntermediate, 0, 97920);
                  }

                  BioMiniPlus2.this.drawDebugMap(bFingerOn, bcheckCorr, BioMiniPlus2.this.m_NHEH, BioMiniPlus2.this.m_AEH, BioMiniPlus2.this.m_Image, BioMiniPlus2.this.m_ImageBG, BioMiniPlus2.this.m_ImageA, nIntWidth, nIntHeight, BioMiniPlus2.this.m_ImageIntermediate, nTargetWidth, nTargetHeight);
                  long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
                  if (BioMiniPlus2.this.mProcessingCost != 0L) {
                     BioMiniPlus2.this.mProcessingCost = (long)((double)BioMiniPlus2.this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                  } else {
                     BioMiniPlus2.this.mProcessingCost = currentCost;
                  }

                  System.arraycopy(BioMiniPlus2.this.m_ImageIntermediate, 0, BioMiniPlus2.this.m_ImageLast, 0, BioMiniPlus2.this.m_ImageIntermediate.length);
                  BioMiniPlus2.this.onCapture(BioMiniPlus2.this.mCaptureResponder, BioMiniPlus2.this.m_ImageLast, nTargetWidth, nTargetHeight, bFingerOn == 1);
                  if (BioMiniPlus2.this.m_TimeOut != 0L && System.currentTimeMillis() - BioMiniPlus2.this.m_Start > BioMiniPlus2.this.m_TimeOut) {
                     BioMiniPlus2.this.onCaptureError(BioMiniPlus2.this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - BioMiniPlus2.this.m_Start) + "/" + BioMiniPlus2.this.m_TimeOut + ")");
                     break;
                  }
               } else {
                  BioMiniPlus2.this.LogE("CaptureSingle null image buffer");
               }
            }
         }

         BioMiniPlus2.this.bThreadFlag = false;
         BioMiniPlus2.this.LogD("StartCapturingLoop : Capturing thread end");
         BioMiniPlus2.this.CaptureFrameStop();
      }
   }

   private class UsbBulkLoopRev implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniPlus2.StartCapturingLoop mParentProcess = null;
      Queue<BioMiniBase.MDRImagePair> mImageQueueA = new LinkedList();
      Queue<BioMiniBase.MDRImagePair> mImageQueueN = new LinkedList();
      Queue<BioMiniBase.MDRExposurePair> mExposureQueue = new LinkedList();
      byte fillExtra = 0;
      boolean bTouchState = false;

      UsbBulkLoopRev(ABioMiniDevice pMyp) {
         this.mParentClass = pMyp;
      }

      UsbBulkLoopRev(BioMiniPlus2.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniPlus2.this.LogD(" -- UsbBulkLoop started... -- ");
         this.mImageQueueA.clear();
         this.mImageQueueN.clear();
         BioMiniPlus2.this.mCapturedQueue.clear();
         this.mExposureQueue.clear();
         if (BioMiniPlus2.this.mUsbHandler != null) {
            BioMiniPlus2.this.mIsUsbThreadRunning = true;
            (new Thread(BioMiniPlus2.this.new UsbBulkLoopCalc(this))).start();
            int nBulkWidth = BioMiniPlus2.this.getBulkWidth();
            int nBulkHeight = BioMiniPlus2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int nTouchPrevState = 0;
            int nTouchCurrState = false;
            int nTouchTrigger = false;
            boolean updateAlways = false;
            int MAX_BULK_ERRORS = true;
            int cntBulkErrorsx = 0;
            BioMiniPlus2.this.mUsbHandler.setBulkRx(2);
            BioMiniPlus2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniPlus2.this.mUsbHandler.isNative()) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(500);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(550);
            }

            updateAlways = false;
            BioMiniBase.MDRExposurePair expPrev = BioMiniPlus2.this.new MDRExposurePair(BioMiniPlus2.this.m_NHEH, BioMiniPlus2.this.m_AEH);
            int cntLoop = 0;

            while(BioMiniPlus2.this.bThreadFlag) {
               ++cntLoop;

               try {
                  BioMiniPlus2.this.LogProcessStart("ReadA");
                  boolean re = BioMiniPlus2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     BioMiniPlus2.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                     break;
                  }

                  int nTop = BioMiniPlus2.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniPlus2.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniPlus2.this.mUsbHandler.read(BioMiniPlus2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniPlus2.this.mUsbHandler.controlRx(BioMiniPlus2.this.GetCmd(226), BioMiniPlus2.this.m_TouchBuffer, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniPlus2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniPlus2.this.LogProcessEnd("ReadA");
                  BioMiniPlus2.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (!re) {
                     ++cntBulkErrorsx;
                     if (cntBulkErrorsx > 10) {
                        BioMiniPlus2.this.LogE("Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }

                     this.mImageQueueA.add(BioMiniPlus2.this.new MDRImagePair(nTopNext, BioMiniPlus2.this.m_pFullBufferA[nTopNext], expPrev.ExposureA));
                     BioMiniPlus2.this.LogE("UsbBulkLoopRev read (A) error");
                     continue;
                  }

                  int cntBulkErrors = 0;
                  int nTouchCurrStatex = BioMiniPlus2.this.m_TouchBuffer[3] & 1;
                  byte nTouchTriggerx;
                  if (nTouchCurrStatex == 1) {
                     if (nTouchPrevState == 1) {
                        nTouchTriggerx = 0;
                     } else {
                        nTouchTriggerx = 1;
                     }
                  } else {
                     nTouchTriggerx = -1;
                  }

                  nTouchPrevState = nTouchCurrStatex;
                  if (nTouchTriggerx == 1) {
                     this.bTouchState = true;
                  } else if (nTouchTriggerx == -1) {
                     this.bTouchState = false;
                  }

                  SystemClock.sleep(5L);
                  this.mImageQueueA.add(BioMiniPlus2.this.new MDRImagePair(nTopNext, BioMiniPlus2.this.m_pFullBufferA[nTopNext], expPrev.ExposureA));
                  if (!BioMiniPlus2.this.bThreadFlag) {
                     break;
                  }

                  BioMiniPlus2.this.m_nLTop++;
                  if (BioMiniPlus2.this.m_nLTop == 5) {
                     BioMiniPlus2.this.m_bLTopIter = true;
                     BioMiniPlus2.this.m_nLTop = 0;
                  }

                  int MAX_TRIES = true;
                  int cntPoll = 0;

                  BioMiniBase.MDRExposurePair exp;
                  for(exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll(); exp == null && cntPoll < 50; ++cntPoll) {
                     SystemClock.sleep(10L);
                     exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll();
                  }

                  for(BioMiniBase.MDRExposurePair expTmp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll(); expTmp != null; expTmp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll()) {
                     exp = expTmp;
                  }

                  if (exp == null || exp.ExposureA <= 0) {
                     if (exp == null) {
                        BioMiniPlus2.this.LogV("UsbBulkLoopRev error: could not get calculation result from UsbBulkLoopCalc @" + cntLoop);
                     }

                     BioMiniPlus2.this.LogD(String.format(Locale.ENGLISH, "UsbBulkLoopRev exp(%s) , exp.ExposureA(%s)", exp == null ? "null" : exp, exp == null ? "null" : exp.ExposureA + ""));
                     break;
                  }

                  re = BioMiniPlus2.this.SetIntegrationTime(exp.ExposureN, BioMiniPlus2.this.m_NHGH, exp.ExposureA, BioMiniPlus2.this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
                  BioMiniPlus2.this.LogProcessEnd("Exposure");
                  BioMiniPlus2.this.LogD("UsbBulkLoopRev *D* Setting Exposure exp.ExposureN: " + exp.ExposureN + " exp.ExposureA: " + exp.ExposureA);
                  if (!re) {
                     BioMiniPlus2.this.LogE("UsbBulkLoopRev : Command error");
                     break;
                  }

                  BioMiniPlus2.this.LogProcessStart("ReadN");
                  re = BioMiniPlus2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     BioMiniPlus2.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                     break;
                  }

                  BioMiniPlus2.this.LogD("Before read m_pFullBufferN[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniPlus2.this.mUsbHandler.read(BioMiniPlus2.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniPlus2.this.mUsbHandler.controlRx(BioMiniPlus2.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniPlus2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniPlus2.this.LogProcessEnd("ReadN");
                  BioMiniPlus2.this.LogD("After read m_pFullBufferN[nTopNext]");
                  if (!re) {
                     cntBulkErrorsx = cntBulkErrors + 1;
                     if (cntBulkErrorsx > 10) {
                        BioMiniPlus2.this.LogE("Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }

                     this.mImageQueueN.add(BioMiniPlus2.this.new MDRImagePair(nTopNext, BioMiniPlus2.this.m_pFullBufferN[nTopNext], expPrev.ExposureN));
                     BioMiniPlus2.this.LogE("UsbBulkLoopRev read (N) error");
                     continue;
                  }

                  cntBulkErrorsx = 0;
                  SystemClock.sleep(5L);
                  this.mImageQueueN.add(BioMiniPlus2.this.new MDRImagePair(nTopNext, BioMiniPlus2.this.m_pFullBufferN[nTopNext], expPrev.ExposureN));
                  if (nTopNext >= 12) {
                     BioMiniPlus2.this.m_nTop = 0;
                     BioMiniPlus2.this.m_bTopIter = true;
                  } else {
                     BioMiniPlus2.this.m_nTop = nTopNext;
                  }

                  expPrev = exp;
               } catch (NullPointerException var19) {
                  BioMiniPlus2.this.LogE("mUsbHandler missing");
                  break;
               }

               if (Thread.currentThread().isInterrupted()) {
                  BioMiniPlus2.this.LogD("mSubHandler interrupted, and changing running flag.");
                  BioMiniPlus2.this.bThreadFlag = false;
               }
            }

            BioMiniPlus2.this.LogD(" -- UsbBulkLoopRev finished... -- ");
            BioMiniPlus2.this.bThreadFlag = false;
            BioMiniPlus2.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }

   private class UsbBulkLoopCalc implements Runnable {
      BioMiniPlus2.UsbBulkLoopRev mParent;
      private int[] avgArray = new int[4];
      private int avg = 0;
      private int avg_prev = 0;
      private int nblockw;
      private int nblockh;

      UsbBulkLoopCalc(BioMiniPlus2.UsbBulkLoopRev parent) {
         this.mParent = parent;
      }

      public void run() {
         BioMiniPlus2.this.LogD(" -- UsbBulkLoopCalc started... -- ");
         if (BioMiniPlus2.this.mUsbHandler != null) {
            BioMiniPlus2.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniPlus2.this.getRawWidth();
            int nRawHeight = BioMiniPlus2.this.getRawHeight();
            int nBytesRaw = nRawWidth * nRawHeight;
            int nBytesImage = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniPlus2.this.getBulkWidth();
            int nBulkHeight = BioMiniPlus2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 100, 80};
            int[] pFingerOnThS = new int[]{4, 4, 3, 3, 3, 2, 2, 2};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniPlus2.this.m_NHEH;
            int fingerOnPlusx = true;
            boolean updateAlways = false;
            int cntBulkErrorsx = 0;
            int MAX_BULK_ERRORS = true;
            BioMiniPlus2.this.mUsbHandler.setBulkRx(2);
            BioMiniPlus2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniPlus2.this.mUsbHandler.isNative()) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(500);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlus = pFingerOnThS[BioMiniPlus2.this.m_nSensitivity];
            updateAlways = false;
            byte[] cdata = new byte[64];
            byte[] frameA = new byte[307209];
            byte[] frameN = new byte[307209];
            BioMiniBase.MDRImagePair mdrA = null;
            BioMiniBase.MDRImagePair mdrN = null;
            boolean toggleHigh = true;
            int judge_count = pCountS[BioMiniPlus2.this.m_nSensitivity];

            while(BioMiniPlus2.this.bThreadFlag) {
               try {
                  BioMiniPlus2.this.LogD("UsbBulkLoopCalc *D* --------------->");
                  int MAX_TRIES = true;
                  int cntBulkRead = 0;
                  int offsetToCurrent = false;
                  BioMiniBase.MDRImagePair mdrPrevA = mdrA;
                  mdrA = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueA.poll();
                  if (mdrA == null) {
                     do {
                        SystemClock.sleep(10L);
                        mdrA = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueA.poll();
                        ++cntBulkRead;
                     } while(mdrA == null && cntBulkRead < 50);
                  }

                  if (mdrA == null) {
                     ++cntBulkErrorsx;
                     if (cntBulkErrorsx > 4) {
                        BioMiniPlus2.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }
                  } else {
                     int cntBulkErrors = 0;
                     BioMiniPlus2.this.LogProcessStart("AdjustRaw");
                     if (BioMiniPlus2.this.isSmallBulkMode()) {
                        BioMiniPlus2.this.LogD("UsbBulkLoopCalc bulk A Image , set 256 mode : upscale");
                        BioMiniPlus2.this.RescaleImage(mdrA.Image, nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                     }

                     System.arraycopy(mdrA.Image, 0, frameA, 0, frameA.length);
                     util.InvertClear(frameA, nRawWidth, nRawHeight, 90, 60);
                     BioMiniJni.AdjustRaw(mdrA.Image);
                     BioMiniPlus2.this.LogProcessEnd("AdjustRaw");
                     if (!BioMiniPlus2.this.bThreadFlag) {
                        break;
                     }

                     System.arraycopy(mdrA.Image, 0, BioMiniPlus2.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                     BioMiniPlus2.this.m_nLTop++;
                     if (BioMiniPlus2.this.m_nLTop == 5) {
                        BioMiniPlus2.this.m_bLTopIter = true;
                        BioMiniPlus2.this.m_nLTop = 0;
                     }

                     BioMiniPlus2.this.LogProcessStart("Exposure");
                     int nexp = false;
                     if (mdrPrevA != null && mdrN != null && nFingerCount > judge_count) {
                        int nexpx = BioMiniJni.GetOptimalExposureValue(mdrA.Image, mdrN.Image, mdrPrevA.Image, mdrN.Exposure, mdrA.Exposure, mdrPrevA.Exposure, BioMiniPlus2.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniPlus2.this.g_bExtraDry);
                        BioMiniPlus2.this.m_AEH = nexpx;
                     } else {
                        mdrA.Image[nBytesImage + 8] = 0;
                        if (mdrN != null) {
                           mdrN.Image[nBytesImage + 8] = 0;
                        }
                     }

                     if (nFingerCount <= judge_count) {
                        int low_expo = 25;
                        int high_expo = 492;
                        int low_percentage = 59;
                        int high_percentage = 200;
                        if (toggleHigh) {
                           BioMiniPlus2.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniPlus2.this.m_NHEH + 50) / 100);
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc Max!!!!!!!!!! [" + BioMiniPlus2.this.m_AEH + "]");
                        } else {
                           BioMiniPlus2.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniPlus2.this.m_NHEH + 50) / 100);
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc Min!!!!!!!!!! [" + BioMiniPlus2.this.m_AEH + "]");
                        }

                        toggleHigh = !toggleHigh;
                     }

                     this.mParent.mExposureQueue.add(BioMiniPlus2.this.new MDRExposurePair(BioMiniPlus2.this.m_NHEH, BioMiniPlus2.this.m_AEH));
                     BioMiniPlus2.this.LogD("UsbBulkLoopCalc Exposure set : " + BioMiniPlus2.this.m_NHEH + ", " + BioMiniPlus2.this.m_AEH);
                     cntBulkRead = 0;

                     for(mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll(); mdrN == null && cntBulkRead < 50; ++cntBulkRead) {
                        SystemClock.sleep(10L);
                        mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll();
                     }

                     if (mdrN == null) {
                        cntBulkErrorsx = cntBulkErrors + 1;
                        if (cntBulkErrorsx > 4) {
                           BioMiniPlus2.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                           break;
                        }
                     } else {
                        cntBulkErrorsx = 0;
                        if (BioMiniPlus2.this.isSmallBulkMode()) {
                           BioMiniPlus2.this.RescaleImage(mdrN.Image, nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                        }

                        BioMiniPlus2.this.LogProcessStart("AdjustRaw");
                        System.arraycopy(mdrN.Image, 0, frameN, 0, frameN.length);
                        util.InvertClear(frameN, nRawWidth, nRawHeight, 90, 60);
                        BioMiniJni.AdjustRaw(mdrN.Image);
                        BioMiniPlus2.this.LogProcessEnd("AdjustRaw");
                        BioMiniPlus2.this.LogProcessStart("CalcTouch");
                        int judge_value = false;
                        this.nblockw = 32;
                        this.nblockh = 64;
                        int judge_valuex = pVarS[BioMiniPlus2.this.m_nSensitivity];
                        if (mdrPrevA != null && mdrPrevA.Exposure > 0) {
                           int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           if (BioMiniPlus2.this.m_bExtTrigger == 1) {
                              bFingerOn1 = bFingerOn1 > 0 && this.mParent.bTouchState ? 1 : 0;
                              bFingerOn2 = bFingerOn2 > 0 && this.mParent.bTouchState ? 1 : 0;
                           }

                           BioMiniJni.GetAvg(this.avgArray);
                           this.avg = this.avgArray[0];
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                           if (bFingerOn1 > 0 && bFingerOn2 > 0) {
                              if (nFingerCount <= judge_count && this.avg == BioMiniPlus2.this.m_nBGAvg) {
                                 bFingerOn = false;
                                 nFingerCount = 0;
                              } else {
                                 bFingerOn = true;
                                 BioMiniPlus2.this.LogProcessStart("AutoCapture");
                              }
                           } else {
                              bFingerOn = false;
                           }

                           this.avg_prev = this.avg;
                        }

                        if (bFingerOn) {
                           if (nFingerCount == 0) {
                              BioMiniPlus2.this.LogPublicProcessStart("AutoCapture");
                           }

                           ++nFingerCount;
                        } else {
                           nFingerCount = 0;
                        }

                        BioMiniPlus2.this.LogD("UsbBulkLoopCalc : nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ")");
                        if (!BioMiniPlus2.this.bThreadFlag) {
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                           break;
                        }

                        if (nFingerCount > judge_count + fingerOnPlus) {
                           mdrN.Image[nBytesImage + 8] = 1;
                           mdrA.Image[nBytesImage + 8] = 1;
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Tagging finger (1)");
                           if (BioMiniPlus2.this.m_nCaptureMode == 1) {
                              BioMiniPlus2.this.mUsbHandler.controlTx(BioMiniPlus2.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                              BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Capture successful at mode 1");
                              SystemClock.sleep(BioMiniPlus2.this.getSafeDelay());
                              if (!BioMiniPlus2.this.bThreadFlag) {
                                 BioMiniPlus2.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                              } else {
                                 BioMiniPlus2.this.mCapturedQueue.add(BioMiniPlus2.this.new MDRCapturedPair(mdrA, mdrN));
                                 if (this.mParent.mParentClass != null) {
                                    this.mParent.mParentClass.captured();
                                 }
                              }
                              break;
                           }

                           if (BioMiniPlus2.this.m_nCaptureMode == 2) {
                              BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Capture successful at mode 2");
                           } else {
                              BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Capture successful at mode unknown");
                           }
                        } else {
                           BioMiniPlus2.this.LogD("UsbBulkLoopCalc : Tagging finger (0)");
                           mdrN.Image[nBytesImage + 8] = 0;
                           mdrN.Image[nBytesImage + 8] = 0;
                        }

                        BioMiniPlus2.this.LogProcessEnd("CalcTouch");
                        BioMiniPlus2.this.LogD(" -- UsbBulkLoopCalc : Notifying... -- ");
                        BioMiniPlus2.this.mCapturedQueue.add(BioMiniPlus2.this.new MDRCapturedPair(mdrA, mdrN));
                        if (this.mParent.mParentClass != null) {
                           this.mParent.mParentClass.captured();
                        }

                        if (this.mParent.mParentProcess != null) {
                           this.mParent.mParentProcess.captured();
                        }
                     }
                  }
               } catch (NullPointerException var34) {
                  BioMiniPlus2.this.LogI("UsbBulkLoopCalc mUsbHandler missing");
                  break;
               }
            }

            this.mParent.mExposureQueue.add(BioMiniPlus2.this.new MDRExposurePair(-1, -1));
            BioMiniPlus2.this.LogD(" -- UsbBulkLoopCalc finished... -- ");
         }
      }
   }

   private class UsbBulkLoop implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniPlus2.StartCapturingLoop mParentProcess = null;
      private int[] avgArray = new int[4];
      private int avg = 0;
      private int avg_prev = 0;
      private int avg_prev2 = 0;
      byte fillExtra = -1;
      private int nblockw;
      private int nblockh;

      UsbBulkLoop(ABioMiniDevice pMyp) {
         this.mParentClass = pMyp;
      }

      UsbBulkLoop(BioMiniPlus2.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniPlus2.this.LogD(" -- UsbBulkLoop started... -- ");
         if (BioMiniPlus2.this.mUsbHandler != null) {
            BioMiniPlus2.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniPlus2.this.getRawWidth();
            int nRawHeight = BioMiniPlus2.this.getRawHeight();
            int nBytesRaw = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniPlus2.this.getBulkWidth();
            int nBulkHeight = BioMiniPlus2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 100, 80};
            int[] pFingerOnThS = new int[]{4, 4, 3, 3, 3, 2, 2, 2};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniPlus2.this.m_NHEH;
            int sleepPost = false;
            int sleepVal1 = BioMiniPlus2.this.mSleepVal;
            int sleepVal2 = BioMiniPlus2.this.mSleepVal;
            int fingerOnPlusx = true;
            boolean updateAlways = false;
            int Ret = false;
            boolean re = false;
            int nSkipPackets = true;
            int dividerFromProcessingCost = 6;
            BioMiniPlus2.this.mUsbHandler.setBulkRx(2);
            BioMiniPlus2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniPlus2.this.mUsbHandler.isNative()) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(500);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniPlus2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               dividerFromProcessingCost = 3;
               BioMiniPlus2.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlus = pFingerOnThS[BioMiniPlus2.this.m_nSensitivity];
            updateAlways = false;
            byte[] cdata = new byte[64];
            byte[] frameA = new byte[307209];
            byte[] frameN = new byte[307209];

            while(BioMiniPlus2.this.bThreadFlag) {
               BioMiniPlus2.this.LogD("UsbBulkLoop : in loop");

               try {
                  int sleepPostx = 0;
                  nSkipPackets = true;
                  BioMiniPlus2.this.mSleepPlus = 15;
                  if (BioMiniPlus2.this.mProcessingCost != 0L) {
                     BioMiniPlus2.this.mSleepVal = sleepVal1 = sleepVal2 = (int)(BioMiniPlus2.this.mProcessingCost / (long)dividerFromProcessingCost);
                  } else {
                     sleepVal2 = 33;
                     sleepVal1 = 33;
                     BioMiniPlus2.this.mSleepVal = 33;
                  }

                  sleepVal1 += BioMiniPlus2.this.mSleepPlus;
                  sleepVal2 += BioMiniPlus2.this.mSleepPlus;
                  re = BioMiniPlus2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniPlus2.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  long starttime = System.currentTimeMillis();
                  BioMiniPlus2.this.LogD("get controlRx(CMD_READ_FRAME_A - 0xe1) --> Start Bulk Frame A");
                  int nTop = BioMiniPlus2.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniPlus2.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = 0;
                  re = BioMiniPlus2.this.mUsbHandler.read(BioMiniPlus2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniPlus2.this.bThreadFlag && BioMiniPlus2.this.mUsbHandler.controlRx(BioMiniPlus2.this.GetCmd(226), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniPlus2.this.bThreadFlag && BioMiniPlus2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniPlus2.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (BioMiniPlus2.this.isSmallBulkMode()) {
                     BioMiniPlus2.this.LogD("bulk A Image , set 256 mode : upscale");
                     BioMiniPlus2.this.RescaleImage(BioMiniPlus2.this.m_pFullBufferA[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  if (sleepVal1 > 0) {
                     SystemClock.sleep((long)sleepVal1);
                  }

                  System.arraycopy(BioMiniPlus2.this.m_pFullBufferA[nTopNext], 0, frameA, 0, frameA.length);
                  util.InvertClear(frameA, nRawWidth, nRawHeight, 90, 60);
                  BioMiniJni.AdjustRaw(BioMiniPlus2.this.m_pFullBufferA[nTopNext]);
                  if (!re) {
                     Log.e(BioMiniPlus2.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  if (!BioMiniPlus2.this.bThreadFlag) {
                     break;
                  }

                  System.arraycopy(BioMiniPlus2.this.m_pFullBufferA[nTopNext], 0, BioMiniPlus2.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                  BioMiniPlus2.this.m_LCH1[BioMiniPlus2.this.m_nLTop] = cdata[3] << 8 | cdata[2] & 255;
                  BioMiniPlus2.this.m_LCH2[BioMiniPlus2.this.m_nLTop] = cdata[5] << 8 | cdata[4] & 255;
                  BioMiniPlus2.this.m_nLTop++;
                  if (BioMiniPlus2.this.m_nLTop == 5) {
                     BioMiniPlus2.this.m_bLTopIter = true;
                     BioMiniPlus2.this.m_nLTop = 0;
                  }

                  int nexp = false;
                  int judge_count = pCountS[BioMiniPlus2.this.m_nSensitivity];
                  if ((nTop >= 1 || BioMiniPlus2.this.m_bTopIter) && nFingerCount > judge_count) {
                     int nexpx = BioMiniJni.GetOptimalExposureValue(BioMiniPlus2.this.m_pFullBufferA[nTopNext], BioMiniPlus2.this.m_pFullBufferN[nTop], BioMiniPlus2.this.m_pFullBufferA[nTop], BioMiniPlus2.this.m_NHEH, BioMiniPlus2.this.m_AEH, prev_exp, BioMiniPlus2.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniPlus2.this.g_bExtraDry);
                     prev_exp = BioMiniPlus2.this.m_AEH;
                     BioMiniPlus2.this.m_AEH = nexpx;
                  } else {
                     BioMiniPlus2.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniPlus2.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 0;
                  }

                  if (nFingerCount <= judge_count) {
                     int low_expo = 25;
                     int high_expo = 492;
                     int low_percentage = 59;
                     int high_percentage = 200;
                     if (nTop % 2 == 0) {
                        prev_exp = BioMiniPlus2.this.m_AEH;
                        BioMiniPlus2.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniPlus2.this.m_NHEH + 50) / 100);
                        BioMiniPlus2.this.LogD("Max!!!!!!!!!! [" + BioMiniPlus2.this.m_AEH + "]");
                     } else {
                        prev_exp = BioMiniPlus2.this.m_AEH;
                        BioMiniPlus2.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniPlus2.this.m_NHEH + 50) / 100);
                        BioMiniPlus2.this.LogD("Min!!!!!!!!!! [" + BioMiniPlus2.this.m_AEH + "]");
                     }
                  }

                  re = BioMiniPlus2.this.SetIntegrationTime(BioMiniPlus2.this.m_NHEH, BioMiniPlus2.this.m_NHGH, BioMiniPlus2.this.m_AEH, BioMiniPlus2.this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
                  BioMiniPlus2.this.LogD("UsbBulkLoop : Setting Exposure m_NHEH: " + BioMiniPlus2.this.m_NHEH + " m_AEH: " + BioMiniPlus2.this.m_AEH);
                  if (!re) {
                     break;
                  }

                  re = BioMiniPlus2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniPlus2.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  BioMiniPlus2.this.LogD("Before read m_pFullBufferN[nTopNext]");
                  this.fillExtra = 0;
                  re = BioMiniPlus2.this.mUsbHandler.read(BioMiniPlus2.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniPlus2.this.bThreadFlag && BioMiniPlus2.this.mUsbHandler.controlRx(BioMiniPlus2.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniPlus2.this.bThreadFlag && BioMiniPlus2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniPlus2.this.LogD("After read m_pFullBufferN[nTopNext]");
                  if (BioMiniPlus2.this.isSmallBulkMode()) {
                     BioMiniPlus2.this.RescaleImage(BioMiniPlus2.this.m_pFullBufferN[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  if (sleepVal2 > 0) {
                     SystemClock.sleep((long)sleepVal2);
                  }

                  System.arraycopy(BioMiniPlus2.this.m_pFullBufferN[nTopNext], 0, frameN, 0, frameN.length);
                  util.InvertClear(frameN, nRawWidth, nRawHeight, 90, 60);
                  BioMiniJni.AdjustRaw(BioMiniPlus2.this.m_pFullBufferN[nTopNext]);
                  if (!re) {
                     Log.e(BioMiniPlus2.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  int judge_value = false;
                  this.nblockw = 22;
                  this.nblockh = 80;
                  int judge_valuex = pVarS[BioMiniPlus2.this.m_nSensitivity];
                  if (nTop >= 2 || BioMiniPlus2.this.m_bTopIter) {
                     int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     BioMiniJni.GetAvg(this.avgArray);
                     this.avg = this.avgArray[0];
                     BioMiniPlus2.this.LogD("UsbBulkLoop : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                     if (bFingerOn1 > 0 && bFingerOn2 > 0) {
                        if (nFingerCount <= judge_count && this.avg == BioMiniPlus2.this.m_nBGAvg) {
                           bFingerOn = false;
                           nFingerCount = 0;
                        } else {
                           bFingerOn = true;
                        }
                     } else {
                        bFingerOn = false;
                     }

                     this.avg_prev2 = this.avg_prev;
                     this.avg_prev = this.avg;
                  }

                  if (bFingerOn) {
                     ++nFingerCount;
                  } else {
                     nFingerCount = 0;
                  }

                  BioMiniPlus2.this.LogD("UsbBulkLoop : nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ") @" + nTopNext + ")");
                  if (nFingerCount > judge_count + fingerOnPlus) {
                     BioMiniPlus2.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 1;
                     BioMiniPlus2.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 1;
                     BioMiniPlus2.this.m_nTop = nTopNext;
                     BioMiniPlus2.this.LogD("UsbBulkLoop : Tagging finger (1) @" + nTopNext);
                     if (BioMiniPlus2.this.m_nCaptureMode == 1) {
                        BioMiniPlus2.this.mUsbHandler.controlTx(BioMiniPlus2.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                        BioMiniPlus2.this.LogD("UsbBulkLoop : Capture successful at mode 1");
                        SystemClock.sleep(BioMiniPlus2.this.getSafeDelay());
                        this.mParentClass.captured();
                        break;
                     }

                     if (BioMiniPlus2.this.m_nCaptureMode == 2) {
                        BioMiniPlus2.this.LogD("UsbBulkLoop : Capture successful at mode 2");
                     } else {
                        BioMiniPlus2.this.LogD("UsbBulkLoop : Capture successful at mode unknown");
                     }
                  } else {
                     BioMiniPlus2.this.LogD("UsbBulkLoop : Tagging finger (0) @" + nTopNext);
                     BioMiniPlus2.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniPlus2.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniPlus2.this.m_nTop = nTopNext;
                  }

                  if (nTop == 11) {
                     BioMiniPlus2.this.m_bTopIter = true;
                  }

                  BioMiniPlus2.this.m_nTop = nTopNext;
                  BioMiniPlus2.this.LogD(" -- UsbBulkLoop : Notifying... -- ");
                  if (this.mParentClass != null) {
                     this.mParentClass.captured();
                  }

                  if (this.mParentProcess != null) {
                     this.mParentProcess.captured();
                  }

                  if (sleepPostx > 0) {
                     SystemClock.sleep((long)sleepPostx);
                  }
               } catch (NullPointerException var35) {
                  BioMiniPlus2.this.LogE("mUsbHandler missing");
                  break;
               }
            }

            BioMiniPlus2.this.LogD(" -- UsbBulkLoop finished... -- ");
            BioMiniPlus2.this.bThreadFlag = false;
            BioMiniPlus2.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }
}
