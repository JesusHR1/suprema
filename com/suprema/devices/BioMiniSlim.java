package com.suprema.devices;

import android.os.SystemClock;
import android.util.Log;
import com.suprema.ABioMiniDevice;
import com.suprema.IBioMiniDevice;
import com.suprema.ICaptureResponder;
import com.suprema.IUsbStatusChangeListener;
import com.suprema.android.BioMiniJni;
import com.suprema.usb.IUsbHandler;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class BioMiniSlim extends BioMiniBase {
   private ICaptureResponder mCaptureResponder;
   final int MAX_BULK_SIZE;
   private boolean bIwakeupYou;
   private boolean mIsUsbThreadRunning;
   private int g_bExtraDry;
   private static final int IMG_XMAX_SLIM = 896;
   private static final int IMG_YMAX_SLIM = 432;
   private static final int IMG_XMAX_SLIM_256 = 768;
   private static final int IMG_YMAX_SLIM_256 = 336;
   private static final int SLIM_IMAGE_WIDTH_D = 352;
   private static final int SLIM_IMAGE_HEIGHT_D = 496;
   private static final int SLIM_IMAGE_WIDTH = 320;
   private static final int SLIM_IMAGE_HEIGHT = 480;
   private static final int SLIM_CROP_IMAGE_WIDTH = 320;
   private static final int SLIM_CROP_IMAGE_HEIGHT = 480;
   private static final int CAPTURE_BUFFER_SIZE = 12;
   private static final int DELAY_FOR_SUCCESSFUL_CAPTURE = 130;
   private long mLastNotification;
   private long mLastWait;
   private long m_Start;
   private double m_expoScaleFactor;
   private int m_NormalExposure;
   private int m_AdvancedExposure;
   private int m_UpperExposure;
   private int m_LowerExposure;
   private long mProcessingCost;
   private static final int IMG_BUF_MAX = 387072;
   private static final int IMG_INT_BUF_MAX = 174592;
   private final byte[][] m_pFullBufferA;
   private final byte[][] m_pFullBufferN;
   private final byte[] m_ImageBufferBG;
   private final byte[] m_Image;
   private final byte[] m_ImageA;
   private final byte[] m_ImageBG;
   private byte[] m_ImagePrev;
   private final byte[] m_ImageIntermediate;
   private final byte[] m_ImageLast;
   private final byte[] m_pOffAndIRImage;
   private final byte[] m_pOnAndIRImage;
   private final byte[] m_pLowerIRImage;
   private final byte[] m_pUpperIRImage;
   private byte[] m_ImageRawPrevN;
   private byte[] m_ImageRawPrevA;
   private byte[] tmpBuffer;
   private int[] m_LCH1;
   private int[] m_LCH2;
   private int m_LRCH1;
   private int m_LRCH2;
   private int m_LGCH1;
   private int m_LGCH2;
   private byte[] m_TouchBuffer;
   private boolean bIsAfterAbortCpaturing;
   private boolean bUSBisdominated;
   private int mSleepPlus;
   private int mSleepVal;
   private Runnable mLoop;
   private Runnable mSLoop;
   private BioMiniSlim.StartCapturingLoop mStartCapturingLoop;
   private Thread mUsbThread;
   private Thread mStartCapturingThread;
   private boolean bThreadFlag;
   private boolean bAbortCapturing;
   private boolean isBackGround;
   private int m_nCaptureMode;
   private int mHasPreviewBuffered;
   private int m_NEH;
   private int m_NGH;
   private int m_NHEH;
   private int m_NHGH;
   private int m_AEH;
   private int m_AGH;
   private int m_UEH;
   private int m_UGH;
   private int m_LEH;
   private int m_LGH;
   private int m_SEH;
   private int m_SGH;
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
   private boolean m_bLFDGet;
   private byte mPaddingValue;
   private int m_nOldStartPosX;
   private int m_nOldStartPoxY;
   private int m_nBGAvg;
   private boolean m256K_Mode;
   private Queue<BioMiniBase.MDRCapturedPair> mCapturedQueue;
   static int kk = 0;
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
   private static final int CMD_SET_CIS_TIME = 192;
   private static final int CMD_CIS_START_POS = 193;
   private static final int CMD_GET_SYSTEM_STATUS = 197;
   private static final int CMD_SET_SLEEPMODE = 204;
   private static final int CMD_MULTI_EXP_ENABLE = 195;
   private static final int CMD_READ_FRAME_LFD_U = 227;
   private static final int CMD_READ_FRAME_LFD_L = 228;
   private static final int CMD_READ_LFD_2ND = 232;
   private static final int CMD_READ_LFD_2ND_DONE = 233;
   private static final int CMD_READ_LFD_3RD = 232;
   private static final int CMD_READ_LFD_3RD_DONE = 233;
   private static final int CMD_READ_LFD_4TH = 236;
   private static final int CMD_READ_LFD_ALL_DONE = 238;
   private static final int CMD_SET_LED = 194;
   private static final int OV_IIC_EEPROM_ADDR = 174;

   public BioMiniSlim() {
      this.mCaptureResponder = null;
      this.MAX_BULK_SIZE = 387072;
      this.bIwakeupYou = false;
      this.mIsUsbThreadRunning = false;
      this.g_bExtraDry = -1;
      this.mLastNotification = 0L;
      this.mLastWait = 0L;
      this.m_expoScaleFactor = 1.0D;
      this.mProcessingCost = 0L;
      this.m_pFullBufferA = new byte[12][387081];
      this.m_pFullBufferN = new byte[12][387081];
      this.m_ImageBufferBG = new byte[387072];
      this.m_Image = new byte[174592];
      this.m_ImageA = new byte[174592];
      this.m_ImageBG = new byte[174592];
      this.m_ImagePrev = new byte[174592];
      this.m_ImageIntermediate = new byte[174592];
      this.m_ImageLast = new byte[174592];
      this.m_pOffAndIRImage = new byte[387072];
      this.m_pOnAndIRImage = new byte[387072];
      this.m_pLowerIRImage = new byte[387072];
      this.m_pUpperIRImage = new byte[387072];
      this.m_ImageRawPrevN = new byte[387081];
      this.m_ImageRawPrevA = new byte[387081];
      this.tmpBuffer = new byte[387072];
      this.m_LCH1 = new int[12];
      this.m_LCH2 = new int[12];
      this.m_TouchBuffer = new byte[6];
      this.bIsAfterAbortCpaturing = true;
      this.bUSBisdominated = false;
      this.mSleepPlus = 0;
      this.mSleepVal = 20;
      this.bAbortCapturing = false;
      this.isBackGround = false;
      this.mPaddingValue = -1;
      this.m_nBGAvg = -1;
      this.m256K_Mode = false;
      this.mCapturedQueue = new LinkedList();
      this.TAG = "BioMiniSlim";
   }

   public BioMiniSlim(IUsbStatusChangeListener eventLisenter) {
      this();
      this.m_UsbStatusChangeListener = eventLisenter;
   }

   public int getMaxBulkSize() {
      return 387072;
   }

   public boolean captureSingle(final IBioMiniDevice.CaptureOption opt, final ICaptureResponder responder, boolean bAsync) {
      if (!this.isCapturing() && !this.mIsUsbThreadRunning) {
         this.mCaptureResponder = responder;
         if (bAsync) {
            Runnable captureObj = new Runnable() {
               public void run() {
                  BioMiniSlim.this._captureSingle(opt, responder);
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
         int nBytesRaw = nRawWidth * nRawHeight;
         if (this.bUSBisdominated) {
            this.LogD("CaptureSingle : handle busy");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
            return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
         } else {
            this.isBackGround = false;
            if (this.mEnableAutoSleep) {
               this.wakeUp();
            }

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
               this.mLoop = new BioMiniSlim.UsbBulkLoopRev(this);
               this.mUsbThread = new Thread(this.mLoop);
               this.mUsbThread.start();
               int bFingerOn = false;
               int bcheckCorr = false;
               float gce_gain = 1.0F;
               float lce_gain = 0.4F;
               float de_gain = 0.03F;
               boolean bThreadFlagLocal = this.bThreadFlag;
               int var17 = 0;

               while(bThreadFlagLocal) {
                  if (this.m_nTop <= -1) {
                     SystemClock.sleep(50L);
                  } else {
                     this.LogD("CaptureSingle : Waiting for image transferred");
                     this.waitForCaptured();
                     bThreadFlagLocal = this.bThreadFlag;
                     if (this.bAbortCapturing) {
                        break;
                     }

                     int nTop = this.m_nTop;
                     long timerStart = SystemClock.currentThreadTimeMillis();
                     byte[] imageN = null;
                     byte[] imageA = null;
                     int MAX_POLLING_TRIES = 50;
                     int dequeingCnt = 0;

                     BioMiniBase.MDRCapturedPair cp;
                     for(cp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll(); cp == null && dequeingCnt < MAX_POLLING_TRIES; ++dequeingCnt) {
                        SystemClock.sleep(100L);
                        cp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll();
                     }

                     while(!this.mCapturedQueue.isEmpty()) {
                        BioMiniBase.MDRCapturedPair tmpCp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll();
                        if (tmpCp != null) {
                           cp = tmpCp;
                        }
                     }

                     if (cp != null) {
                        imageN = cp.MdrN.Image;
                        imageA = cp.MdrA.Image;
                     }

                     if (imageN != null && imageA != null) {
                        int bFingerOn = imageN[nBytesRaw + 8];
                        this.LogD("CaptureSingle : Got bFingerOn(" + bFingerOn + ", " + imageN[nBytesRaw + 8] + ") @" + nTop);
                        this.LogD("CaptureSingle : bIwakeupYou :" + this.bIwakeupYou);
                        if (!this.bIwakeupYou && bFingerOn == 0) {
                           this.LogD("break;");
                           break;
                        }

                        this.LogD("CaptureSingle : Compensating... " + this.m_Image + ", " + this.m_ImageA);
                        BioMiniJni.Comp(imageN, this.m_Image, bFingerOn);
                        BioMiniJni.Comp(imageA, this.m_ImageA, bFingerOn);
                        int bcheckCorr;
                        if (this.mHasPreviewBuffered == 0) {
                           bcheckCorr = BioMiniJni.CheckCorrelation(this.m_Image, this.m_Image, 60);
                           this.mHasPreviewBuffered = 1;
                        } else {
                           bcheckCorr = BioMiniJni.CheckCorrelation(this.m_ImagePrev, this.m_Image, 60);
                        }

                        this.LogD("CaptureSingle : CheckCorrelation done... (" + bcheckCorr + ")");
                        this.m_ImagePrev = Arrays.copyOf(this.m_Image, nIntWidth * nIntHeight);
                        this.LogD("CaptureSingle : Preprocessing... " + this.m_Image + ", " + this.m_ImageA);
                        if (bFingerOn == 1 && bcheckCorr == 1) {
                           BioMiniJni.GetPreprocessedImage(this.m_ImageBG, this.m_Image, this.m_ImageA, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                        } else if (bFingerOn == 1) {
                           BioMiniJni.GetPreprocessedImage(this.m_ImageBG, this.m_Image, this.m_Image, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                        } else {
                           BioMiniJni.GetPreprocessedImage(this.m_ImageBG, this.m_Image, this.m_Image, this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64);
                        }

                        ++var17;
                        long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
                        if (this.mProcessingCost != 0L) {
                           this.mProcessingCost = (long)((double)this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                        } else {
                           this.mProcessingCost = currentCost;
                        }

                        System.arraycopy(this.m_ImageIntermediate, 0, this.m_ImageLast, 0, this.m_ImageIntermediate.length);
                        if (this.m_TimeOut != 0L && System.currentTimeMillis() - this.m_Start > this.m_TimeOut) {
                           this.onCaptureError(this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - this.m_Start) + "/" + this.m_TimeOut + ")");
                           this.mIsTimeoutOccurred = true;
                           break;
                        }

                        ++var17;
                        if (bFingerOn == 1) {
                           this.isCaptured = true;
                           break;
                        }
                     } else {
                        this.LogE("CaptureSingle null image buffer");
                        bThreadFlagLocal = this.bThreadFlag;
                     }
                  }
               }

               this.LogD("CaptureSingle : Process loop finished");
               this.bThreadFlag = false;
               if (this.mEnableAutoSleep) {
                  this.hibernate();
               }

               this.CaptureFrameStop();
               if (this.m_nCaptureMode == 3 && this.m_bLFDGet) {
                  this.LogD("CaptureSingle : Got lfd result");
               }

               this.LogD("CaptureSingle : Done capturing a fingerprint");
               if (this.isCaptured) {
                  this.LogPublicProcessEnd("AutoCapture");
                  if (this.m_nCaptureMode == 3) {
                     this.LogProcessStart("LFD");
                     this.SlimLFDWorker();
                     this.LogProcessEnd("LFD");
                     if (this.mDetectedFake) {
                        this.LogD("CaptureSingle : Got lfd result(Fake Finger), Score(" + this.mDetectedFakeScore + ") mDetectFake : " + this.mDetectedFake);
                     } else {
                        this.LogD("CaptureSingle : Got lfd result(Live Finger), Score(" + this.mDetectedFakeScore + ") mDetectFake : " + this.mDetectedFake);
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
            Log.e(this.TAG, "startCapturing : Not initialized");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED;
            return IBioMiniDevice.ErrorCode.ERR_NOT_INITIALIZED.value();
         } else if (this.bUSBisdominated) {
            this.LogD("startCapturing : USB Handle is busy");
            this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
            return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING.value();
         } else {
            this.mCaptureResponder = responder;
            this.mCurrentCaptureOption = opt;
            this.isBackGround = false;
            if (this.mEnableAutoSleep) {
               this.wakeUp();
            }

            int res = this.CaptureFrameStart();
            if (res == 0) {
               this.CaptureFrameStop();
               this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED;
               return IBioMiniDevice.ErrorCode.ERR_CAPTURE_FAILED.value();
            } else {
               this.m_nCaptureMode = 2;
               this.bThreadFlag = true;
               boolean useNewUsbBulk = true;
               this.mStartCapturingLoop = new BioMiniSlim.StartCapturingLoop(this, true);
               this.mStartCapturingThread = new Thread(this.mStartCapturingLoop);
               this.mSLoop = new BioMiniSlim.UsbBulkLoopRev(this.mStartCapturingLoop);
               this.mUsbThread = new Thread(this.mSLoop);
               this.mUsbThread.start();
               this.mStartCapturingThread.start();
               this.m_LastError = IBioMiniDevice.ErrorCode.OK;
               return IBioMiniDevice.ErrorCode.OK.value();
            }
         }
      } else {
         this.LogD("startCapturing : Cannot start capturing (another capturing processing is on going...)");
         this.m_LastError = IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING;
         return IBioMiniDevice.ErrorCode.ERR_CAPTURE_RUNNING.value();
      }
   }

   public boolean isOnDestroying() {
      return false;
   }

   private int getBulkWidth() {
      return this.m256K_Mode ? 768 : this.getRawWidth();
   }

   private int getBulkHeight() {
      return this.m256K_Mode ? 336 : this.getRawHeight();
   }

   private void RescaleImage(byte[] pbDownScaleImage, int nBulkWidth, int nBulkHeight, int nRawWidth, int nRawHeight) {
      this.LogD("nBulkWidth :" + nBulkWidth + " , nBulkHeight:" + nBulkHeight + "  nRawWidth :" + nRawWidth + " , nRawHeight:" + nRawHeight);
      byte[] tmpBuffer = new byte[nRawHeight * nRawWidth];
      Arrays.fill(tmpBuffer, 0, nRawHeight * nRawWidth, this.mPaddingValue);
      int nW = nBulkWidth;
      int nOffX = (nRawWidth - nBulkWidth) / 2;
      int nOffY = (nRawHeight - nBulkHeight) / 2;
      this.LogD("nOffX: " + nOffX + " , nOffY:" + nOffY);
      this.LogD("BulkHeight :" + this.getBulkHeight() + " , BulkWidth:" + this.getBulkWidth());
      this.LogD("pbDownImage len :" + pbDownScaleImage.length);

      for(int a = 0; a < this.getBulkHeight(); ++a) {
         System.arraycopy(pbDownScaleImage, nW * a, tmpBuffer, nRawWidth * (a + nOffY) + nOffX, nW);
      }

      System.arraycopy(tmpBuffer, 0, pbDownScaleImage, 0, tmpBuffer.length);
   }

   private boolean Switch_256K_mode(boolean bFlag) {
      this.LogD("Switch_256K_mode / bFlag :" + bFlag);
      if (this.mUsbHandler == null) {
         return false;
      } else {
         boolean bRet = false;
         int nOff_x = false;
         int nOff_y = false;
         int nImageWidth = false;
         int nImageHeight = false;
         int nStartPosX = false;
         int nStartPosY = false;
         int nEndPosX = false;
         int nEndPosY = false;
         int nOff_x;
         int nOff_y;
         int nImageWidth;
         int nImageHeight;
         int nStartPosX;
         int nStartPosY;
         int nEndPosX;
         int nEndPosY;
         if (bFlag) {
            nOff_x = this.getRawWidth() - this.getBulkWidth() == 0 ? 0 : (this.getRawWidth() - this.getBulkWidth()) / 2;
            nOff_y = this.getRawHeight() - this.getBulkHeight() == 0 ? 0 : (this.getRawHeight() - this.getBulkHeight()) / 2;
            nImageWidth = this.getBulkWidth();
            nImageHeight = this.getBulkHeight();
            this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "  Start Position : pStartPos.x(" + this.m_nOldStartPosX + ") , pStartpos.y(" + this.m_nOldStartPoxY + ")");
            nStartPosX = this.m_nOldStartPosX + nOff_x;
            nStartPosY = this.m_nOldStartPoxY + nOff_y;
            int nAdditionalHeight = true;
            nEndPosX = nStartPosX + nImageWidth - 1;
            nEndPosY = nStartPosY + nImageHeight - 1;
         } else {
            nOff_x = 0;
            nOff_y = 0;
            nImageWidth = this.getBulkWidth();
            nImageHeight = this.getBulkHeight();
            this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "  Start Position : pStartPos.x(" + this.m_nOldStartPosX + ") , pStartpos.y(" + this.m_nOldStartPoxY + ")");
            nStartPosX = this.m_nOldStartPosX + nOff_x;
            nStartPosY = this.m_nOldStartPoxY + nOff_y;
            nEndPosX = nStartPosX + nImageWidth - 1;
            nEndPosY = nStartPosY + nImageHeight - 1;
         }

         this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "'s new Old Position : OLDx(" + this.m_nOldStartPosX + ") , OLD Y (" + this.m_nOldStartPoxY + ")");
         this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "'s new Start Position : StartPosX(" + nStartPosX + ") , StartPosY(" + nStartPosY + ")");
         this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "'s new  EndPosition : EndPosX(" + nEndPosX + ") , EndPosY(" + nEndPosY + ")");
         this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "'s new  Start Position : offsetX(" + nOff_x + ") , offSetY(" + nOff_y + ")");
         this.LogD("Biomini Slim Series(" + String.format("%4X", this.getProductId()) + "'s current Image size : " + nImageWidth + "x" + nImageHeight);
         byte[] b;
         if (bFlag) {
            b = new byte[]{0};
            bRet = this.mUsbHandler.controlTx(195, b, 1);
            if (!bRet) {
               this.LogD("Disable MDR fail;");
            }

            SystemClock.sleep(70L);
            bRet = this.setReg(3, 0);
            bRet = this.setReg(22, nStartPosX >> 8 & 255);
            this.getReg(22, b);
            this.LogD(String.format("0x16 : set(%x) , get(%x)", nStartPosX >> 8 & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x16 error");
            }

            bRet = this.setReg(23, nStartPosX & 255);
            this.getReg(23, b);
            this.LogD(String.format("0x17 : set(%x) , get(%x)", nStartPosX & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x17 error");
            }

            bRet = this.setReg(24, nStartPosY >> 8 & 255);
            this.getReg(24, b);
            this.LogD(String.format("0x18 : set(%x) , get(%x)", nStartPosY >> 8 & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x18 error");
            }

            bRet = this.setReg(25, nStartPosY & 255);
            this.getReg(25, b);
            this.LogD(String.format("0x19 : set(%x) , get(%x)", nStartPosY & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x19 error");
            }

            bRet = this.setReg(26, nEndPosX >> 8 & 255);
            this.getReg(26, b);
            this.LogD(String.format("0x1A : set(%x) , get(%x)", nEndPosX >> 8 & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x1A error");
            }

            bRet = this.setReg(27, nEndPosX & 255);
            this.getReg(27, b);
            this.LogD(String.format("0x1B : set(%x) , get(%x)", nEndPosX & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x1B error");
            }

            bRet = this.setReg(28, nEndPosY >> 8 & 255);
            this.getReg(28, b);
            this.LogD(String.format("0x1C : set(%x) , get(%x)", nEndPosY >> 8 & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x1C error");
            }

            bRet = this.setReg(29, nEndPosY & 255);
            this.getReg(29, b);
            this.LogD(String.format("0x1D : set(%x) , get(%x)", nEndPosY & 255, b[0] & 255));
            if (!bRet) {
               this.LogD("0x1D error");
            }

            b[0] = 0;
            bRet = this.mUsbHandler.controlTx(195, b, 1);
            if (!bRet) {
               this.LogD("Enable MDR fail;");
            }

            this.LogD("==============================SLIM=============");
            SystemClock.sleep(100L);
            this.getReg(22, b);
            this.LogD(String.format("--0x16 : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(23, b);
            this.LogD(String.format("--0x17 : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(24, b);
            this.LogD(String.format("--0x18 : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(25, b);
            this.LogD(String.format("--0x19 : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(26, b);
            this.LogD(String.format("--0x1A : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(27, b);
            this.LogD(String.format("--0x1B : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(28, b);
            this.LogD(String.format("--0x1C : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.getReg(29, b);
            this.LogD(String.format("--0x1D : set(%02x) , get(%02x)", 0, b[0] & 255));
            this.LogD("===========================================");
         } else {
            b = new byte[64];
            this.LogD("flag false revert crop setting");
            this.LogD("0x0409 Set Start Pos (revert)");
            b[0] = (byte)(nStartPosX >> 8 & 255);
            b[1] = (byte)(nStartPosX & 255);
            b[2] = (byte)(nStartPosY >> 8 & 255);
            b[3] = (byte)(nStartPosY & 255);
            this.LogD("set Start Pos - " + String.format("[0] %02x , [1] %02x , [2] %02x , [3] %02x ", b[0], b[1], b[2], b[3]));
            this.mUsbHandler.controlTx(193, b, 4);
         }

         return bRet;
      }
   }

   private int CaptureFrameStart() {
      this.bUSBisdominated = true;
      this.m_UsbStatusChangeListener.onStatusChangeListener(this.bUSBisdominated);
      this.bAbortCapturing = false;
      this.isCaptured = false;
      int Ret = true;
      boolean re = false;
      int nWidth = this.getRawWidth();
      int nHeight = this.getRawHeight();
      int nBulkWidth = this.getBulkWidth();
      int nBulkHeight = this.getBulkHeight();
      int nBulkLength = nBulkWidth * nBulkHeight;
      this.LogD(String.format(Locale.ENGLISH, "Timeout(%d) , injected timeout(%d)", this.m_TimeOut, this.mCurrentCaptureOption.captureTimeout));
      this.setTempCaptureOpts();
      this.mHasPreviewBuffered = 0;
      this.LogD("Setting camera parameter...");
      if (!this.mUsbHandler.controlTx(195, new byte[]{0}, 1)) {
         this.LogD("Disabling MDR failed");
      }

      if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.LOW) {
         this.LogD("SetFrameRate - LOW ");
         this.m_expoScaleFactor = 0.5D;
         this.setReg(4, 12);
         this.setReg(5, 14);
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.ELOW) {
         this.LogD("SetFrameRate - Extra LOW ");
         this.m256K_Mode = true;
         this.m_expoScaleFactor = 0.4D;
         this.setReg(4, 14);
         this.setReg(5, 188);
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SLOW) {
         this.LogD("SetFrameRate - Superior LOW ");
         this.m_expoScaleFactor = 0.4D;
         this.setReg(4, 14);
         this.setReg(5, 188);
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.MID) {
         this.LogD("SetFrameRate - MID ");
         this.m_expoScaleFactor = 0.67D;
         this.setReg(4, 8);
         this.setReg(5, 253);
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.HIGH) {
         this.LogD("SetFrameRate - HIGH ");
         this.m_expoScaleFactor = 0.8D;
         this.setReg(4, 7);
         this.setReg(5, 71);
      } else if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
         this.LogD("SetFrameRate - SHIGH ");
         this.m_expoScaleFactor = 1.0D;
         this.setReg(4, 6);
         this.setReg(5, 7);
      }

      this.Switch_256K_mode(this.m256K_Mode);
      re = this.SetIntegrationTime((int)((double)this.m_NHEH * this.m_expoScaleFactor), this.m_NHGH, (int)((double)this.m_AEH * this.m_expoScaleFactor), this.m_AGH, (int)((double)this.m_UEH * this.m_expoScaleFactor), this.m_UGH, (int)((double)this.m_LEH * this.m_expoScaleFactor), this.m_LGH, (int)((double)this.m_SEH * this.m_expoScaleFactor), this.m_SGH);
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
            this.mUsbHandler.initRead(nBulkLength, 0, false);
            byte[] cdata = new byte[64];
            byte fillExtra = -1;
            re = this.mUsbHandler.read(this.m_ImageBufferBG, nBulkLength, fillExtra, new IUsbHandler.IReadProcessor() {
               public boolean beforeRead() {
                  return BioMiniSlim.this.mUsbHandler.controlRx(225, new byte[]{0, 0, 0, 0, 0, 0}, 6);
               }

               public boolean afterRead() {
                  return BioMiniSlim.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
               }
            });
            if (!re) {
               return 0;
            } else {
               this.mPaddingValue = (byte)this.Boundary_Padding(this.m_ImageBufferBG, nBulkWidth, nBulkHeight);
               this.LogD("BioMini Slim / 256K mode / Boundary_padding value is:" + String.format(Locale.ENGLISH, "mPaddingValue (0x%02x)", this.mPaddingValue));
               if (this.m256K_Mode) {
                  this.RescaleImage(this.m_ImageBufferBG, nBulkWidth, nBulkHeight, nWidth, nHeight);
               }

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
   }

   private boolean ReadLFDFrameInBulk() {
      byte[] Buffer = new byte[64];
      boolean re = false;
      if (this.mEnableAutoSleep) {
         this.wakeUp();
      }

      int nWidth = this.getRawWidth();
      int nHeight = this.getRawHeight();
      int nBulkWidth = this.getBulkWidth();
      int nBulkHeight = this.getBulkHeight();
      int nBulkLength = nWidth * nHeight;
      if (this.m256K_Mode) {
         nBulkLength = nBulkWidth * nBulkHeight;
      }

      this.mUsbHandler.setBulkRx(2);
      this.mUsbHandler.resize(nBulkLength);
      if (!this.SetIntegrationTime((int)((double)this.m_NEH * this.m_expoScaleFactor), this.m_NGH, (int)((double)this.m_NEH * this.m_expoScaleFactor), this.m_NGH, (int)((double)this.m_UEH * this.m_expoScaleFactor), this.m_UGH, (int)((double)this.m_LEH * this.m_expoScaleFactor), this.m_LGH, (int)((double)this.m_SEH * this.m_expoScaleFactor), this.m_SGH)) {
         return false;
      } else {
         this.mUsbHandler.initRead(nBulkLength, 0, false);
         re = this.mUsbHandler.read(this.m_pUpperIRImage, nBulkLength, (byte)-1, new IUsbHandler.IReadProcessor() {
            public boolean beforeRead() {
               return BioMiniSlim.this.mUsbHandler.controlRx(227, new byte[]{0, 0, 0, 0, 0, 0}, 6);
            }

            public boolean afterRead() {
               return BioMiniSlim.this.mUsbHandler.controlTx(228, new byte[]{0, 0, 0, 0}, 1);
            }
         });
         if (!re) {
            return false;
         } else {
            this.mUsbHandler.initRead(nBulkLength, 0, false);
            re = this.mUsbHandler.read(this.m_pLowerIRImage, nBulkLength, (byte)-1, new IUsbHandler.IReadProcessor() {
               public boolean beforeRead() {
                  return BioMiniSlim.this.mUsbHandler.controlRx(232, new byte[]{0, 0, 0, 0, 0, 0}, 6);
               }

               public boolean afterRead() {
                  return BioMiniSlim.this.mUsbHandler.controlTx(233, new byte[]{0, 0, 0, 0}, 1);
               }
            });
            if (!re) {
               return false;
            } else {
               this.mUsbHandler.initRead(nBulkLength, 0, false);
               re = this.mUsbHandler.read(this.m_pOnAndIRImage, nBulkLength, (byte)-1, new IUsbHandler.IReadProcessor() {
                  public boolean beforeRead() {
                     return BioMiniSlim.this.mUsbHandler.controlRx(232, new byte[]{0, 0, 0, 0, 0, 0}, 6);
                  }

                  public boolean afterRead() {
                     return BioMiniSlim.this.mUsbHandler.controlTx(233, new byte[]{0, 0, 0, 0}, 1);
                  }
               });
               if (!re) {
                  return false;
               } else {
                  this.mUsbHandler.initRead(nBulkLength, 0, false);
                  re = this.mUsbHandler.read(this.m_pOffAndIRImage, nBulkLength, (byte)-1, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniSlim.this.mUsbHandler.controlRx(236, new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniSlim.this.mUsbHandler.controlTx(238, new byte[]{0, 0, 0, 0}, 1);
                     }
                  });
                  return re;
               }
            }
         }
      }
   }

   private void SlimLFDWorker() {
      int m_ImageLast_width = 320;
      int m_ImageLast_height = 480;
      int scanner_mode = 1;
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

      BioMiniSlim.SlimLFDWorkerLoop w1 = new BioMiniSlim.SlimLFDWorkerLoop(data1, scanner_mode, this.m_DetectFake, this.m_NHEH);
      BioMiniSlim.SlimLFDWorkerLoop w2 = new BioMiniSlim.SlimLFDWorkerLoop(data2, scanner_mode, this.m_DetectFake, this.m_NHEH);
      Thread th1 = new Thread(w1);
      th1.start();
      Thread th2 = new Thread(w2);
      th2.start();
      boolean var24 = false;

      int liveness;
      label142: {
         float[] scoretable;
         float nLFDLevel;
         int fake_level;
         label143: {
            try {
               var24 = true;
               th1.join();
               th2.join();
               var24 = false;
               break label143;
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
               break label142;
            }

            fakeDetected = true;
            liveness = score[0];
            break label142;
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
      this.LogD(String.format(Locale.ENGLISH, "DetectFakeScore(%d)", liveness));
   }

   private void CaptureFrameStop() {
      this.LogD("Stops capturing...");
      int tries = 0;

      for(byte MAX_TRIES = 50; this.mIsUsbThreadRunning && tries < MAX_TRIES; ++tries) {
         SystemClock.sleep(100L);
      }

      if (this.mUsbHandler == null) {
         this.bUSBisdominated = false;
         this.m_UsbStatusChangeListener.onStatusChangeListener(this.bUSBisdominated);
      } else {
         this.LogD("checks reading...");

         try {
            if (this.mUsbHandler.isReading()) {
               this.mUsbHandler.resetBulkPipe(false);
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
            this.mUsbHandler.controlTx(194, new byte[]{0, 0, 0, 0}, 1);
         } catch (NullPointerException var7) {
            var7.printStackTrace();
         } finally {
            this.mLoop = null;
            this.mSLoop = null;
            this.mUsbThread = null;
            this.bUSBisdominated = false;
            this.resetCaptureOpts();
            this.m_UsbStatusChangeListener.onStatusChangeListener(this.bUSBisdominated);
         }

         this.LogD("Capture stopped");
      }
   }

   private int Boundary_Padding(byte[] pbInput, int width, int height) {
      return this.m256K_Mode ? this.Boundary_Padding(pbInput, width, height, 0.98F) : 255;
   }

   private int Boundary_Padding(byte[] pbInput, int width, int height, float pos_ratio) {
      this.LogD(String.format(Locale.ENGLISH, " pbInput len(%d) , width(%d) , height(%d) , ", pbInput.length, width, height));
      int[] pnHist = new int[256];

      int nInLength;
      int i;
      for(int j = 0; j < height; ++j) {
         nInLength = j * width;

         for(i = 0; i < width; ++i) {
            ++pnHist[pbInput[nInLength + i] & 255];
         }
      }

      int[] pnCumulativeHist = new int[256];
      pnCumulativeHist[0] = pnHist[0];

      for(nInLength = 1; nInLength < 256; ++nInLength) {
         pnCumulativeHist[nInLength] = pnCumulativeHist[nInLength - 1] + pnHist[nInLength];
      }

      nInLength = width * height;

      for(i = 255; i >= 0; --i) {
         float f_v = (float)pnCumulativeHist[i] / (float)nInLength;
         if (f_v < pos_ratio) {
            return i;
         }
      }

      return 0;
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
      this.m_NormalExposure = nNormalExposure;
      this.m_AdvancedExposure = nAdvanceExposure;
      this.m_UpperExposure = nUpperExposure;
      this.m_LowerExposure = nLowerExposure;
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
            this.LogD("read EEPROM Start Pos - " + String.format(Locale.ENGLISH, "[0] %02x , [1] %02x , [2] %02x , [3] %02x ", bufRead[0], bufRead[1], bufRead[2], bufRead[3]));
            this.m_NEH = bufRead[4] << 8 | bufRead[5] & 255;
            this.m_NGH = bufRead[6];
            this.m_AEH = bufRead[8] << 8 | bufRead[9] & 255;
            this.m_AGH = bufRead[10];
            this.m_SEH = bufRead[12] << 8 | bufRead[13] & 255;
            this.m_SGH = bufRead[14];
            this.m_UEH = bufRead[16] << 8 | bufRead[17] & 255;
            this.m_UGH = bufRead[18];
            this.m_LEH = bufRead[20] << 8 | bufRead[21] & 255;
            this.m_LGH = bufRead[22];
            this.m_NHEH = bufRead[24] << 8 | bufRead[25] & 255;
            this.m_NHGH = bufRead[26];
            re = this.mUsbHandler.readEEPROM(80, 32, bufRead);
            if (!re) {
               return 0;
            } else {
               this.m_LRCH1 = bufRead[4] << 8 | bufRead[5] & 255;
               this.m_LRCH2 = bufRead[6] << 8 | bufRead[7] & 255;
               this.m_LGCH1 = bufRead[8] << 8 | bufRead[9] << 8 | bufRead[10] << 8 | bufRead[11] & 255;
               this.m_LGCH2 = bufRead[12] << 8 | bufRead[13] << 8 | bufRead[14] << 8 | bufRead[15] & 255;
               this.m_EW = bufRead[16] << 8 | bufRead[17] & 255;
               this.m_EH = bufRead[18] << 8 | bufRead[19] & 255;
               this.m_SOX = this.m_SOY = 0;
               this.m_sclFX = this.m_sclFY = 0;
               bufWrite[0] = 0;
               this.mUsbHandler.controlTx(201, bufWrite, 1);
               this.mDeviceInfo.deviceName = this.TAG;
               this.mDeviceInfo.versionSDK = this.BASE_VERSION;
               this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI_SLIM;
               Arrays.fill(bufRead, (byte)0);
               this.mUsbHandler.controlRx(this.GetCmd(201), bufRead, 32);
               this.mDeviceInfo.deviceSN = (new String(bufRead, 0, 32)).trim();
               if (this.getProductId() == 1031 || this.getProductId() == 1032) {
                  int[] p = this.getCurrentStartPos();
                  this.m_nOldStartPosX = p[0];
                  this.m_nOldStartPoxY = p[1];
                  this.LogD(String.format(Locale.ENGLISH, " m_nOldStartPosX (%d) , m_nOldStartPosY(%d) ", this.m_nOldStartPosX, this.m_nOldStartPoxY));
               }

               BioMiniJni.setESA(this.m_EW, this.m_EH, this.m_SOX, this.m_SOY, (float)this.m_sclFX, (float)this.m_sclFY);
               return 1;
            }
         }
      }
   }

   public int[] getCurrentStartPos() {
      int CMD_GET_SYSTEM_STATUS = true;
      byte[] cdata = new byte[24];
      boolean ret = this.mUsbHandler.controlRx(197, cdata, cdata.length);
      int nStartPosX = (cdata[5] & 255) * 256 + (cdata[6] & 255);
      int nStartPosY = (cdata[7] & 255) * 256 + (cdata[8] & 255);
      if (!ret) {
         this.LogD("CMD_GET_SYSTEM_STATUS return false");
         this.LogD(String.format(Locale.ENGLISH, "cdata : %s", Arrays.toString(cdata)));
      } else {
         this.LogD("CMD_GET_SYSTEM_STATUS return true");
         this.LogD(String.format(Locale.ENGLISH, "cdata : %s", Arrays.toString(cdata)));
      }

      return new int[]{nStartPosX, nStartPosY};
   }

   public int getImageWidth() {
      return this.getTargetWidth();
   }

   public int getImageHeight() {
      return this.getTargetHeight();
   }

   private int getTargetWidth() {
      return 320;
   }

   private int getTargetHeight() {
      return 480;
   }

   private int getRawWidth() {
      return 896;
   }

   private int getRawHeight() {
      return 432;
   }

   private int getIntermediateWidth() {
      return 352;
   }

   private int getIntermediateHeight() {
      return 496;
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
      byte[] cmd = new byte[64];
      if (this.mUsbHandler == null) {
         return false;
      } else {
         this.mUsbHandler.controlRx(197, cmd, 24);
         return (cmd[0] & 128) != 0;
      }
   }

   public boolean hibernate() {
      if (this.mUsbHandler != null) {
         byte[] cmd = new byte[64];
         this.mUsbHandler.controlRx(197, cmd, 24);
         int MAX_TRIES = true;

         for(int cnt = 0; cnt < 20; ++cnt) {
            if (!this.mUsbHandler.isReading()) {
               if ((cmd[0] & 128) != 0) {
                  cmd[0] = 1;
                  this.mUsbHandler.controlTx(204, cmd, 1);
                  SystemClock.sleep(100L);
                  Log.e(this.TAG, "DeviceSleep : set device to sleep...");
                  return true;
               }

               Log.e(this.TAG, "DeviceSleep : device sleeping already...");
               return false;
            }

            SystemClock.sleep(100L);
         }
      }

      Log.e(this.TAG, "DeviceSleep : failed...");
      return false;
   }

   public boolean wakeUp() {
      this.LogD("DeviceWakeup");
      boolean bRet = false;
      if (this.mUsbHandler != null) {
         byte[] cmd = new byte[64];
         Arrays.fill(cmd, (byte)-1);
         this.mUsbHandler.controlRx(197, cmd, 24);
         if ((cmd[0] & 128) == 0) {
            cmd[0] = 0;
            this.mUsbHandler.controlTx(204, cmd, 1);
            int MAX_TRIES = true;

            int cnt;
            for(cnt = 0; cnt < 50; ++cnt) {
               Arrays.fill(cmd, (byte)-1);
               this.mUsbHandler.controlRx(197, cmd, 24);
               if ((cmd[0] & 128) != 0) {
                  break;
               }
            }

            SystemClock.sleep(150L);
            if (cnt == 50) {
               Log.e(this.TAG, "DeviceWakeup failed...");
            }

            return cnt != 50;
         }

         Log.e(this.TAG, "DeviceWakeup : device waked up already...");
      }

      Log.e(this.TAG, "DeviceWakeup --> mUsbHandler: " + this.mUsbHandler);
      return false;
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

   private class SlimLFDWorkerLoop implements Runnable {
      byte[] fp_data;
      int m_mode;
      int m_DetectFake;
      int m_NormalExp;
      int[] mScore = new int[1];
      int re;

      SlimLFDWorkerLoop(byte[] _data, int _mode, int _detectFake, int _normalExposure) {
         this.fp_data = _data;
         this.m_mode = _mode;
         this.m_DetectFake = _detectFake;
         this.m_NormalExp = _normalExposure;
         this.re = 0;
      }

      public int Score() {
         BioMiniSlim.this.LogD(String.format(Locale.ENGLISH, "instance hash(%s) , Score(%d) called  ", this.toString(), this.mScore[0]));
         return this.mScore[0];
      }

      public void run() {
         this.re = BioMiniJni.GetLFDResult(this.fp_data, this.m_mode, this.m_DetectFake, this.m_NormalExp, this.mScore);
         BioMiniSlim.this.LogD(String.format(Locale.ENGLISH, " re(%d) , instance hash (%s) ... end of thread run()", this.re, this.toString()));
      }
   }

   private class StartCapturingLoop implements Runnable {
      BioMiniSlim pBioMiniAndroid;
      boolean bIwakeupYou = false;
      boolean IsUsingNewBulkThread = true;

      StartCapturingLoop(BioMiniSlim pMyp, boolean useNewBulkLoop) {
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
         int nTargetWidth = BioMiniSlim.this.getTargetWidth();
         int nTargetHeight = BioMiniSlim.this.getTargetHeight();
         int nIntWidth = BioMiniSlim.this.getIntermediateWidth();
         int nIntHeight = BioMiniSlim.this.getIntermediateHeight();
         int nRawWidth = BioMiniSlim.this.getRawWidth();
         int nRawHeight = BioMiniSlim.this.getRawHeight();
         int nBytesImage = nRawWidth * nRawHeight;
         float gce_gain = 1.0F;
         float lce_gain = 0.4F;
         float de_gain = 0.03F;
         BioMiniSlim.this.mProcessingCost = 0L;
         int bFingerOn = false;
         int bcheckCorr = false;
         BioMiniSlim.this.mHasPreviewBuffered = 0;
         if (BioMiniSlim.this.mEnableAutoSleep) {
            BioMiniSlim.this.wakeUp();
         }

         while(BioMiniSlim.this.bThreadFlag) {
            if (BioMiniSlim.this.m_nTop > -1) {
               this.iwait();
               BioMiniSlim.this.printTimeTag("StartCapturingLoop : Got captured notice");
               if (!this.bIwakeupYou || BioMiniSlim.this.bAbortCapturing) {
                  break;
               }

               int nTop = BioMiniSlim.this.m_nTop;
               long timerStart = SystemClock.currentThreadTimeMillis();
               byte[] imageN = null;
               byte[] imageA = null;
               if (this.IsUsingNewBulkThread) {
                  BioMiniBase.MDRCapturedPair cp = (BioMiniBase.MDRCapturedPair)BioMiniSlim.this.mCapturedQueue.poll();

                  for(BioMiniBase.MDRCapturedPair tmpCp = (BioMiniBase.MDRCapturedPair)BioMiniSlim.this.mCapturedQueue.poll(); tmpCp != null; tmpCp = (BioMiniBase.MDRCapturedPair)BioMiniSlim.this.mCapturedQueue.poll()) {
                     cp = tmpCp;
                  }

                  if (cp != null) {
                     imageN = cp.MdrN.Image;
                     imageA = cp.MdrA.Image;
                  }
               } else {
                  imageN = BioMiniSlim.this.m_pFullBufferN[nTop];
                  imageA = BioMiniSlim.this.m_pFullBufferA[nTop];
               }

               if (imageN != null && imageA != null) {
                  int bFingerOnx = imageN[nBytesImage + 8];
                  BioMiniJni.Comp(imageN, BioMiniSlim.this.m_Image, bFingerOnx);
                  BioMiniJni.Comp(imageA, BioMiniSlim.this.m_ImageA, bFingerOnx);
                  BioMiniSlim.this.printTimeTag("StartCapturingLoop : Compensation done");
                  int bcheckCorrx;
                  if (BioMiniSlim.this.mHasPreviewBuffered == 0) {
                     bcheckCorrx = BioMiniJni.CheckCorrelation(BioMiniSlim.this.m_Image, BioMiniSlim.this.m_Image, 60);
                     BioMiniSlim.this.mHasPreviewBuffered = 1;
                  } else {
                     bcheckCorrx = BioMiniJni.CheckCorrelation(BioMiniSlim.this.m_ImagePrev, BioMiniSlim.this.m_Image, 60);
                  }

                  BioMiniSlim.this.LogD("StartCapturingLoop : CheckCorrelation done... (" + bcheckCorrx + ")");
                  BioMiniSlim.this.m_ImagePrev = Arrays.copyOf(BioMiniSlim.this.m_Image, nIntWidth * nIntHeight);
                  if (bFingerOnx == 1 && bcheckCorrx == 1) {
                     BioMiniJni.GetPreprocessedImage(BioMiniSlim.this.m_ImageBG, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_ImageA, BioMiniSlim.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                  } else if (bFingerOnx == 1) {
                     BioMiniJni.GetPreprocessedImage(BioMiniSlim.this.m_ImageBG, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                  } else {
                     BioMiniJni.GetPreprocessedImage(BioMiniSlim.this.m_ImageBG, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64);
                  }

                  BioMiniSlim.this.printTimeTag("StartCapturingLoop : Preprocessing done");
                  BioMiniSlim.this.isCaptured = true;
                  BioMiniSlim.this.drawDebugMap(bFingerOnx, bcheckCorrx, BioMiniSlim.this.m_NHEH, BioMiniSlim.this.m_AEH, BioMiniSlim.this.m_Image, BioMiniSlim.this.m_ImageBG, BioMiniSlim.this.m_ImageA, nIntWidth, nIntHeight, BioMiniSlim.this.m_ImageIntermediate, nTargetWidth, nTargetHeight);
                  long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
                  if (BioMiniSlim.this.mProcessingCost != 0L) {
                     BioMiniSlim.this.mProcessingCost = (long)((double)BioMiniSlim.this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                  } else {
                     BioMiniSlim.this.mProcessingCost = currentCost;
                  }

                  System.arraycopy(BioMiniSlim.this.m_ImageIntermediate, 0, BioMiniSlim.this.m_ImageLast, 0, BioMiniSlim.this.m_ImageIntermediate.length);
                  BioMiniSlim.this.onCapture(BioMiniSlim.this.mCaptureResponder, BioMiniSlim.this.m_ImageLast, nTargetWidth, nTargetHeight, bFingerOnx == 1);
                  if (BioMiniSlim.this.m_TimeOut != 0L && System.currentTimeMillis() - BioMiniSlim.this.m_Start > BioMiniSlim.this.m_TimeOut) {
                     BioMiniSlim.this.onCaptureError(BioMiniSlim.this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - BioMiniSlim.this.m_Start) + "/" + BioMiniSlim.this.m_TimeOut + ")");
                     break;
                  }
               } else {
                  BioMiniSlim.this.LogE("CaptureSingle null image buffer");
               }
            }
         }

         BioMiniSlim.this.bThreadFlag = false;
         BioMiniSlim.this.LogD("StartCapturingLoop : Capturing thread end");
         if (BioMiniSlim.this.mEnableAutoSleep) {
            BioMiniSlim.this.hibernate();
         }

         BioMiniSlim.this.CaptureFrameStop();
      }
   }

   private class UsbBulkLoopRev implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniSlim.StartCapturingLoop mParentProcess = null;
      Queue<BioMiniBase.MDRImagePair> mImageQueueA = new LinkedList();
      Queue<BioMiniBase.MDRImagePair> mImageQueueN = new LinkedList();
      Queue<BioMiniBase.MDRExposurePair> mExposureQueue = new LinkedList();
      byte fillExtra = -1;
      boolean bTouchState = false;

      UsbBulkLoopRev(ABioMiniDevice pMyp) {
         this.mParentClass = pMyp;
      }

      UsbBulkLoopRev(BioMiniSlim.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniSlim.this.LogD(" -- UsbBulkLoop started... -- ");
         this.mImageQueueA.clear();
         this.mImageQueueN.clear();
         BioMiniSlim.this.mCapturedQueue.clear();
         this.mExposureQueue.clear();
         if (BioMiniSlim.this.mUsbHandler != null) {
            BioMiniSlim.this.mIsUsbThreadRunning = true;
            (new Thread(BioMiniSlim.this.new UsbBulkLoopCalc(this))).start();
            int nRawWidth = BioMiniSlim.this.getRawWidth();
            int nRawHeight = BioMiniSlim.this.getRawHeight();
            int nBulkWidth = BioMiniSlim.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            boolean updateAlways = false;
            byte[] frameA = new byte[387081];
            byte[] frameN = new byte[387081];
            BioMiniSlim.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(550);
            }

            updateAlways = false;
            BioMiniBase.MDRExposurePair expPrev = BioMiniSlim.this.new MDRExposurePair(BioMiniSlim.this.m_NHEH, BioMiniSlim.this.m_AEH);
            int cntLoop = 0;

            while(BioMiniSlim.this.bThreadFlag) {
               ++cntLoop;

               try {
                  BioMiniSlim.this.LogProcessStart("ReadA");
                  boolean re = BioMiniSlim.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     BioMiniSlim.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                     break;
                  }

                  int nTop = BioMiniSlim.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniSlim.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim.this.mUsbHandler.read(BioMiniSlim.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniSlim.this.mUsbHandler.controlRx(BioMiniSlim.this.GetCmd(226), BioMiniSlim.this.m_TouchBuffer, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniSlim.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim.this.LogProcessEnd("ReadA");
                  BioMiniSlim.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (!re) {
                     BioMiniSlim.this.LogE("UsbBulkLoopRev read (A) error");
                     continue;
                  }

                  System.arraycopy(BioMiniSlim.this.m_pFullBufferA[nTopNext], 0, frameA, 0, frameA.length);
                  if (BioMiniSlim.this.m256K_Mode) {
                     BioMiniSlim.this.LogD("bulk A Image , set 256 mode : upscale");
                     BioMiniSlim.this.RescaleImage(BioMiniSlim.this.m_pFullBufferA[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  BioMiniSlim.this.m_LCH1[BioMiniSlim.this.m_nLTop] = BioMiniSlim.this.m_TouchBuffer[3] << 8 | BioMiniSlim.this.m_TouchBuffer[2] & 255;
                  BioMiniSlim.this.m_LCH2[BioMiniSlim.this.m_nLTop] = BioMiniSlim.this.m_TouchBuffer[5] << 8 | BioMiniSlim.this.m_TouchBuffer[4] & 255;
                  SystemClock.sleep(5L);
                  this.mImageQueueA.add(BioMiniSlim.this.new MDRImagePair(nTopNext, BioMiniSlim.this.m_pFullBufferA[nTopNext], expPrev.ExposureA));
                  if (!BioMiniSlim.this.bThreadFlag) {
                     break;
                  }

                  BioMiniSlim.this.m_nLTop++;
                  if (BioMiniSlim.this.m_nLTop == 5) {
                     BioMiniSlim.this.m_bLTopIter = true;
                     BioMiniSlim.this.m_nLTop = 0;
                  }

                  int MAX_TRIES = true;
                  int cntPoll = 0;

                  BioMiniBase.MDRExposurePair exp;
                  for(exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll(); exp == null && cntPoll < 50; ++cntPoll) {
                     SystemClock.sleep(10L);
                     exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll();
                  }

                  if (exp == null || exp.ExposureA <= 0) {
                     BioMiniSlim.this.LogV("UsbBulkLoopRev error: could not get calculation result from UsbBulkLoopCalc @" + cntLoop);
                     break;
                  }

                  re = BioMiniSlim.this.SetIntegrationTime((int)((double)exp.ExposureN * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_NHGH, (int)((double)exp.ExposureA * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_AGH, (int)((double)BioMiniSlim.this.m_UEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_UGH, (int)((double)BioMiniSlim.this.m_LEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_LGH, (int)((double)BioMiniSlim.this.m_SEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_SGH);
                  BioMiniSlim.this.LogProcessEnd("Exposure");
                  BioMiniSlim.this.LogD("UsbBulkLoopRev : Setting Exposure m_NHEH: " + exp.ExposureN + " m_AEH: " + exp.ExposureA);
                  if (!re) {
                     BioMiniSlim.this.LogE("UsbBulkLoopRev : Command error");
                     break;
                  }

                  BioMiniSlim.this.LogProcessStart("ReadN");
                  re = BioMiniSlim.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     BioMiniSlim.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                     break;
                  }

                  BioMiniSlim.this.LogD("Before read m_pFullBufferN[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim.this.mUsbHandler.read(BioMiniSlim.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniSlim.this.mUsbHandler.controlRx(BioMiniSlim.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniSlim.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim.this.LogProcessEnd("ReadN");
                  BioMiniSlim.this.LogD("After read m_pFullBufferN[nTopNext]");
                  if (!re) {
                     this.mImageQueueN.add(BioMiniSlim.this.new MDRImagePair(nTopNext, BioMiniSlim.this.m_pFullBufferA[nTopNext], expPrev.ExposureN));
                     BioMiniSlim.this.LogE("UsbBulkLoopRev read (N) error");
                     continue;
                  }

                  System.arraycopy(BioMiniSlim.this.m_pFullBufferN[nTopNext], 0, frameN, 0, frameN.length);
                  if (BioMiniSlim.this.m256K_Mode) {
                     BioMiniSlim.this.RescaleImage(BioMiniSlim.this.m_pFullBufferN[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  SystemClock.sleep(5L);
                  this.mImageQueueN.add(BioMiniSlim.this.new MDRImagePair(nTopNext, BioMiniSlim.this.m_pFullBufferN[nTopNext], expPrev.ExposureN));
                  if (nTopNext >= 12) {
                     BioMiniSlim.this.m_nTop = 0;
                     BioMiniSlim.this.m_bTopIter = true;
                  } else {
                     BioMiniSlim.this.m_nTop = nTopNext;
                  }

                  expPrev = exp;
               } catch (NullPointerException var17) {
                  BioMiniSlim.this.LogE("mUsbHandler missing");
                  break;
               }

               if (Thread.currentThread().isInterrupted()) {
                  BioMiniSlim.this.LogD("mSubHandler interrupted, and changing running flag.");
                  BioMiniSlim.this.bThreadFlag = false;
               }
            }

            BioMiniSlim.this.LogD(" -- UsbBulkLoopRev finished... -- ");
            BioMiniSlim.this.bThreadFlag = false;
            BioMiniSlim.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }

   private class UsbBulkLoopCalc implements Runnable {
      BioMiniSlim.UsbBulkLoopRev mParent;
      private int[] avgArray = new int[4];
      private int avg = 0;
      private int avg_prev = 0;
      private int nblockw;
      private int nblockh;

      UsbBulkLoopCalc(BioMiniSlim.UsbBulkLoopRev parent) {
         this.mParent = parent;
      }

      public void run() {
         BioMiniSlim.this.LogD(" -- UsbBulkLoopCalc started... -- ");
         if (BioMiniSlim.this.mUsbHandler != null) {
            BioMiniSlim.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniSlim.this.getRawWidth();
            int nRawHeight = BioMiniSlim.this.getRawHeight();
            int nBytesRaw = nRawWidth * nRawHeight;
            int nBytesImage = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniSlim.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 100, 80};
            int[] pFingerOnThS = new int[]{4, 4, 3, 3, 3, 2, 2, 2};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniSlim.this.m_NHEH;
            int fingerOnPlusx = true;
            boolean updateAlways = false;
            int cntBulkErrorsx = 0;
            int MAX_BULK_ERRORS = true;
            BioMiniSlim.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlus = pFingerOnThS[BioMiniSlim.this.m_nSensitivity];
            updateAlways = false;
            byte[] frameA = new byte[387081];
            byte[] frameN = new byte[387081];
            BioMiniBase.MDRImagePair mdrA = null;
            BioMiniBase.MDRImagePair mdrN = null;
            boolean toggleHigh = true;
            int judge_count = pCountS[BioMiniSlim.this.m_nSensitivity];

            while(BioMiniSlim.this.bThreadFlag) {
               BioMiniSlim.this.LogD("UsbBulkLoopCalc *D* --------------->");

               try {
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
                     if (cntBulkErrorsx > 6) {
                        BioMiniSlim.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }
                  } else {
                     int cntBulkErrors = 0;
                     BioMiniSlim.this.LogProcessStart("AdjustRaw");
                     System.arraycopy(mdrA.Image, 0, frameA, 0, frameA.length);
                     if (BioMiniSlim.this.m256K_Mode) {
                        BioMiniSlim.this.LogD("UsbBulkLoopCalc bulk A Image , set 256 mode : upscale");
                        BioMiniSlim.this.RescaleImage(frameA, nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                     }

                     BioMiniSlim.this.LogProcessEnd("AdjustRaw");
                     if (!BioMiniSlim.this.bThreadFlag) {
                        break;
                     }

                     System.arraycopy(mdrA.Image, 0, BioMiniSlim.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                     BioMiniSlim.this.LogProcessStart("Exposure");
                     int nexp = false;
                     if (mdrPrevA != null && mdrN != null && nFingerCount > judge_count) {
                        int nexpx = BioMiniJni.GetOptimalExposureValue(mdrA.Image, mdrN.Image, mdrPrevA.Image, mdrN.Exposure, mdrA.Exposure, mdrPrevA.Exposure, BioMiniSlim.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniSlim.this.g_bExtraDry);
                        BioMiniSlim.this.m_AEH = nexpx;
                     } else {
                        mdrA.Image[nBytesImage + 8] = 0;
                        if (mdrN != null) {
                           mdrN.Image[nBytesImage + 8] = 0;
                        }
                     }

                     if (nFingerCount <= judge_count) {
                        int low_expo = 25;
                        int high_expo = 999;
                        int low_percentage = 20;
                        int high_percentage = 180;
                        if (toggleHigh) {
                           BioMiniSlim.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniSlim.this.m_NHEH + 50) / 100);
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc Max!!!!!!!!!! [" + BioMiniSlim.this.m_AEH + "]");
                        } else {
                           BioMiniSlim.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniSlim.this.m_NHEH + 50) / 100);
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc Min!!!!!!!!!! [" + BioMiniSlim.this.m_AEH + "]");
                        }

                        toggleHigh = !toggleHigh;
                     }

                     this.mParent.mExposureQueue.add(BioMiniSlim.this.new MDRExposurePair(BioMiniSlim.this.m_NHEH, BioMiniSlim.this.m_AEH));
                     BioMiniSlim.this.LogD("UsbBulkLoopCalc Exposure set : " + BioMiniSlim.this.m_NHEH + ", " + BioMiniSlim.this.m_AEH);
                     cntBulkRead = 0;

                     for(mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll(); mdrN == null && cntBulkRead < 50; ++cntBulkRead) {
                        SystemClock.sleep(10L);
                        mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll();
                     }

                     if (mdrN == null) {
                        cntBulkErrorsx = cntBulkErrors + 1;
                        if (cntBulkErrorsx > 6) {
                           BioMiniSlim.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                           break;
                        }
                     } else {
                        cntBulkErrorsx = 0;
                        BioMiniSlim.this.LogProcessStart("AdjustRaw");
                        System.arraycopy(mdrN.Image, 0, frameN, 0, frameN.length);
                        if (BioMiniSlim.this.m256K_Mode) {
                           BioMiniSlim.this.RescaleImage(frameN, nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                        }

                        BioMiniSlim.this.LogProcessEnd("AdjustRaw");
                        BioMiniSlim.this.LogProcessStart("CalcTouch");
                        int judge_value = false;
                        this.nblockw = 32;
                        this.nblockh = 64;
                        int judge_valuex = pVarS[BioMiniSlim.this.m_nSensitivity];
                        if (mdrPrevA != null && mdrPrevA.Exposure > 0) {
                           int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           if (BioMiniSlim.this.m_bExtTrigger == 1) {
                              bFingerOn1 = bFingerOn1 > 0 && this.mParent.bTouchState ? 1 : 0;
                              bFingerOn2 = bFingerOn2 > 0 && this.mParent.bTouchState ? 1 : 0;
                           }

                           BioMiniJni.GetAvg(this.avgArray);
                           this.avg = this.avgArray[0];
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                           bFingerOn = bFingerOn1 > 0 && bFingerOn2 > 0;
                           this.avg_prev = this.avg;
                        }

                        if (bFingerOn) {
                           if (nFingerCount == 0) {
                              BioMiniSlim.this.LogPublicProcessStart("AutoCapture");
                           }

                           ++nFingerCount;
                        } else {
                           nFingerCount = 0;
                        }

                        BioMiniSlim.this.LogD("UsbBulkLoopCalc : nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ")");
                        if (!BioMiniSlim.this.bThreadFlag) {
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                           break;
                        }

                        if (nFingerCount > judge_count + fingerOnPlus) {
                           mdrN.Image[nBytesImage + 8] = 1;
                           mdrA.Image[nBytesImage + 8] = 1;
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc : Tagging finger (1)");
                           if (BioMiniSlim.this.m_nCaptureMode == 1 || BioMiniSlim.this.m_nCaptureMode == 3) {
                              BioMiniSlim.this.mUsbHandler.controlTx(BioMiniSlim.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                              BioMiniSlim.this.LogD("UsbBulkLoopCalc : Capture successful at mode 1");
                              SystemClock.sleep(BioMiniSlim.this.getSafeDelay());
                              if (!BioMiniSlim.this.bThreadFlag) {
                                 BioMiniSlim.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                              } else {
                                 BioMiniSlim.this.mCapturedQueue.add(BioMiniSlim.this.new MDRCapturedPair(mdrA, mdrN));
                                 if (this.mParent.mParentClass != null) {
                                    this.mParent.mParentClass.captured();
                                 }
                              }
                              break;
                           }

                           if (BioMiniSlim.this.m_nCaptureMode == 2) {
                              BioMiniSlim.this.LogD("UsbBulkLoopCalc : Capture successful at mode 2");
                           } else {
                              BioMiniSlim.this.LogD("UsbBulkLoopCalc : Capture successful at mode unknown");
                           }
                        } else {
                           BioMiniSlim.this.LogD("UsbBulkLoopCalc : Tagging finger (0)");
                           mdrN.Image[nBytesImage + 8] = 0;
                           mdrN.Image[nBytesImage + 8] = 0;
                        }

                        BioMiniSlim.this.LogProcessEnd("CalcTouch");
                        BioMiniSlim.this.LogD(" -- UsbBulkLoopCalc : Notifying... -- ");
                        BioMiniSlim.this.mCapturedQueue.add(BioMiniSlim.this.new MDRCapturedPair(mdrA, mdrN));
                        if (this.mParent.mParentClass != null) {
                           this.mParent.mParentClass.captured();
                        }

                        if (this.mParent.mParentProcess != null) {
                           this.mParent.mParentProcess.captured();
                        }
                     }
                  }
               } catch (NullPointerException var33) {
                  BioMiniSlim.this.LogI("UsbBulkLoopCalc mUsbHandler missing");
                  break;
               }
            }

            this.mParent.mExposureQueue.add(BioMiniSlim.this.new MDRExposurePair(-1, -1));
            BioMiniSlim.this.LogD(" -- UsbBulkLoopCalc finished... -- ");
         }
      }
   }

   private class UsbBulkLoop implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniSlim.StartCapturingLoop mParentProcess = null;
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

      UsbBulkLoop(BioMiniSlim.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniSlim.this.LogD(" -- UsbBulkLoop started... -- ");
         if (BioMiniSlim.this.mUsbHandler != null) {
            BioMiniSlim.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniSlim.this.getRawWidth();
            int nRawHeight = BioMiniSlim.this.getRawHeight();
            int nBytesRaw = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniSlim.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 100, 80};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniSlim.this.m_NHEH;
            int sleepPostx = false;
            int sleepVal1 = BioMiniSlim.this.mSleepVal;
            int sleepVal2 = BioMiniSlim.this.mSleepVal;
            int fingerOnPlusx = true;
            boolean updateAlways = false;
            int Ret = false;
            boolean re = false;
            int nSkipPackets = true;
            int dividerFromProcessingCost = 6;
            BioMiniSlim.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(400);
            } else {
               dividerFromProcessingCost = 4;
               BioMiniSlim.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlus = 2;
            BioMiniSlim.this.mSleepPlus = 0;
            updateAlways = true;
            byte[] cdata = new byte[64];
            byte[] frameA = new byte[387081];
            byte[] frameN = new byte[387081];

            while(BioMiniSlim.this.bThreadFlag) {
               BioMiniSlim.this.LogD("UsbBulkLoop : in loop");

               try {
                  byte sleepPost;
                  if (BioMiniSlim.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SLOW) {
                     nSkipPackets = true;
                     if (BioMiniSlim.this.mProcessingCost != 0L) {
                        sleepPost = 0;
                        sleepVal1 = (int)(BioMiniSlim.this.mProcessingCost * 1L / (long)dividerFromProcessingCost);
                        sleepVal2 = (int)(BioMiniSlim.this.mProcessingCost * 3L / (long)dividerFromProcessingCost);
                     } else {
                        sleepPost = 0;
                        sleepVal1 = 0;
                        sleepVal2 = 33;
                     }
                  } else {
                     nSkipPackets = true;
                     sleepPost = 0;
                     sleepVal1 = 0;
                     sleepVal2 = 0;
                  }

                  sleepVal1 += BioMiniSlim.this.mSleepPlus;
                  sleepVal2 += BioMiniSlim.this.mSleepPlus;
                  re = BioMiniSlim.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniSlim.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  int nTop = BioMiniSlim.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniSlim.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim.this.mUsbHandler.read(BioMiniSlim.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniSlim.this.mUsbHandler.controlRx(BioMiniSlim.this.GetCmd(226), BioMiniSlim.this.m_TouchBuffer, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniSlim.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (sleepVal1 > 0) {
                     SystemClock.sleep((long)sleepVal1);
                  }

                  System.arraycopy(BioMiniSlim.this.m_pFullBufferA[nTopNext], 0, frameA, 0, frameA.length);
                  if (!re) {
                     Log.e(BioMiniSlim.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  if (BioMiniSlim.this.m256K_Mode) {
                     BioMiniSlim.this.LogD("bulk A Image , set 256 mode : upscale");
                     BioMiniSlim.this.RescaleImage(BioMiniSlim.this.m_pFullBufferA[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  if (!BioMiniSlim.this.bThreadFlag) {
                     break;
                  }

                  System.arraycopy(BioMiniSlim.this.m_pFullBufferA[nTopNext], 0, BioMiniSlim.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                  BioMiniSlim.this.m_LCH1[BioMiniSlim.this.m_nLTop] = BioMiniSlim.this.m_TouchBuffer[3] << 8 | BioMiniSlim.this.m_TouchBuffer[2] & 255;
                  BioMiniSlim.this.m_LCH2[BioMiniSlim.this.m_nLTop] = BioMiniSlim.this.m_TouchBuffer[5] << 8 | BioMiniSlim.this.m_TouchBuffer[4] & 255;
                  BioMiniSlim.this.m_nLTop++;
                  if (BioMiniSlim.this.m_nLTop == 5) {
                     BioMiniSlim.this.m_bLTopIter = true;
                     BioMiniSlim.this.m_nLTop = 0;
                  }

                  int nexp = false;
                  int judge_count = pCountS[BioMiniSlim.this.m_nSensitivity];
                  if ((nTop >= 1 || BioMiniSlim.this.m_bTopIter) && nFingerCount > judge_count) {
                     int nexpx = BioMiniJni.GetOptimalExposureValue(BioMiniSlim.this.m_pFullBufferA[nTopNext], BioMiniSlim.this.m_pFullBufferN[nTop], BioMiniSlim.this.m_pFullBufferA[nTop], BioMiniSlim.this.m_NHEH, BioMiniSlim.this.m_AEH, prev_exp, BioMiniSlim.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniSlim.this.g_bExtraDry);
                     prev_exp = BioMiniSlim.this.m_AEH;
                     BioMiniSlim.this.m_AEH = nexpx;
                  } else {
                     BioMiniSlim.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniSlim.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 0;
                  }

                  if (nFingerCount <= judge_count) {
                     int low_expo = 25;
                     int high_expo = 999;
                     int low_percentage = 20;
                     int high_percentage = 180;
                     if (nTop % 2 == 0) {
                        prev_exp = BioMiniSlim.this.m_AEH;
                        BioMiniSlim.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniSlim.this.m_NHEH + 50) / 100);
                        BioMiniSlim.this.LogD("Max!!!!!!!!!! [" + BioMiniSlim.this.m_AEH + "]");
                     } else {
                        prev_exp = BioMiniSlim.this.m_AEH;
                        BioMiniSlim.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniSlim.this.m_NHEH + 50) / 100);
                        BioMiniSlim.this.LogD("Min!!!!!!!!!! [" + BioMiniSlim.this.m_AEH + "]");
                     }
                  }

                  re = BioMiniSlim.this.SetIntegrationTime((int)((double)BioMiniSlim.this.m_NHEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_NHGH, (int)((double)BioMiniSlim.this.m_AEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_AGH, (int)((double)BioMiniSlim.this.m_UEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_UGH, (int)((double)BioMiniSlim.this.m_LEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_LGH, (int)((double)BioMiniSlim.this.m_SEH * BioMiniSlim.this.m_expoScaleFactor), BioMiniSlim.this.m_SGH);
                  BioMiniSlim.this.LogD("UsbBulkLoop : Setting Exposure m_NHEH: " + BioMiniSlim.this.m_NHEH + " m_AEH: " + BioMiniSlim.this.m_AEH);
                  if (!re) {
                     break;
                  }

                  re = BioMiniSlim.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniSlim.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  BioMiniSlim.this.LogD("Before read m_pFullBufferN[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim.this.mUsbHandler.read(BioMiniSlim.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessor() {
                     public boolean beforeRead() {
                        return BioMiniSlim.this.mUsbHandler.controlRx(BioMiniSlim.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public boolean afterRead() {
                        return BioMiniSlim.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim.this.LogD("After read m_pFullBufferN[nTopNext]");
                  if (sleepVal2 > 0) {
                     SystemClock.sleep((long)sleepVal2);
                  }

                  System.arraycopy(BioMiniSlim.this.m_pFullBufferN[nTopNext], 0, frameN, 0, frameN.length);
                  if (!re) {
                     Log.e(BioMiniSlim.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  if (BioMiniSlim.this.m256K_Mode) {
                     BioMiniSlim.this.RescaleImage(BioMiniSlim.this.m_pFullBufferN[nTopNext], nBulkWidth, nBulkHeight, nRawWidth, nRawHeight);
                  }

                  int judge_value = false;
                  this.nblockw = 32;
                  this.nblockh = 64;
                  int judge_valuex = pVarS[BioMiniSlim.this.m_nSensitivity];
                  if (nTop >= 2 || BioMiniSlim.this.m_bTopIter) {
                     int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     BioMiniJni.GetAvg(this.avgArray);
                     this.avg = this.avgArray[0];
                     BioMiniSlim.this.LogD("UsbBulkLoop : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                     bFingerOn = bFingerOn1 > 0 && bFingerOn2 > 0;
                  }

                  if (bFingerOn) {
                     ++nFingerCount;
                  } else {
                     nFingerCount = 0;
                  }

                  BioMiniSlim.this.LogD("UsbBulkLoop : nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ") @" + nTopNext + ")");
                  if (nFingerCount > judge_count + fingerOnPlus) {
                     BioMiniSlim.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 1;
                     BioMiniSlim.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 1;
                     BioMiniSlim.this.m_nTop = nTopNext;
                     BioMiniSlim.this.LogD("UsbBulkLoop : Tagging finger (1) @" + nTopNext);
                     if (BioMiniSlim.this.m_nCaptureMode == 1 || BioMiniSlim.this.m_nCaptureMode == 3) {
                        BioMiniSlim.this.mUsbHandler.controlTx(BioMiniSlim.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                        BioMiniSlim.this.LogD("UsbBulkLoop : Capture successful at mode 1");
                        SystemClock.sleep(BioMiniSlim.this.getSafeDelay());
                        this.mParentClass.captured();
                        break;
                     }

                     if (BioMiniSlim.this.m_nCaptureMode == 2) {
                        BioMiniSlim.this.LogD("UsbBulkLoop : Capture successful at mode 2");
                     } else {
                        BioMiniSlim.this.LogD("UsbBulkLoop : Capture successful at mode unknown");
                     }
                  } else {
                     BioMiniSlim.this.LogD("UsbBulkLoop : Tagging finger (0) @" + nTopNext);
                     BioMiniSlim.this.m_pFullBufferN[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniSlim.this.m_pFullBufferA[nTopNext][nBytesRaw + 8] = 0;
                     BioMiniSlim.this.m_nTop = nTopNext;
                  }

                  if (nTop == 11) {
                     BioMiniSlim.this.m_bTopIter = true;
                  }

                  BioMiniSlim.this.m_nTop = nTopNext;
                  BioMiniSlim.this.LogD(" -- UsbBulkLoop : Notifying... -- ");
                  if (this.mParentClass != null) {
                     this.mParentClass.captured();
                  }

                  if (this.mParentProcess != null) {
                     this.mParentProcess.captured();
                  }

                  if (sleepPost > 0) {
                     SystemClock.sleep((long)sleepPost);
                  }
               } catch (NullPointerException var32) {
                  BioMiniSlim.this.LogE("mUsbHandler missing");
                  break;
               }
            }

            BioMiniSlim.this.LogD(" -- UsbBulkLoop finished... -- ");
            BioMiniSlim.this.bThreadFlag = false;
            BioMiniSlim.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }
}
