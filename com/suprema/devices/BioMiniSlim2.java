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

public class BioMiniSlim2 extends BioMiniBase {
   private ICaptureResponder mCaptureResponder = null;
   private final boolean DEBUG_USB_BULK = false;
   private final boolean DEBUG_MDR = true;
   final int MAX_BULK_SIZE = 524288;
   final int mImageTrSize = 524288;
   private boolean bIwakeupYou = false;
   private boolean mIsUsbThreadRunning = false;
   private boolean g_bSlim2NewBgProcess = false;
   private int g_bExtraDry = -1;
   private static final int IMG_XMAX_SLIM2 = 1240;
   private static final int IMG_YMAX_SLIM2 = 422;
   private static final int IMG_XMAX_SLIM2_256 = 640;
   private static final int IMG_YMAX_SLIM2_256 = 408;
   private static final int SLIM2_IMAGE_WIDTH_D = 300;
   private static final int SLIM2_IMAGE_HEIGHT_D = 433;
   private static final int SLIM2_IMAGE_WIDTH = 300;
   private static final int SLIM2_IMAGE_HEIGHT = 433;
   private static final int SLIM2_CROP_IMAGE_WIDTH = 300;
   private static final int SLIM2_CROP_IMAGE_HEIGHT = 400;
   private static final int CAPTURE_BUFFER_SIZE = 12;
   private static final int DELAY_FOR_SUCCESSFUL_CAPTURE = 130;
   private long mLastNotification = 0L;
   private long mLastWait = 0L;
   private final int OPTIMAL_LOW_EXPO_PERCENTAGE = 60;
   private final int OPTIMAL_HIGH_EXPO_PERCENTAGE = 200;
   private long m_Start;
   private double m_expoScaleFactor = 1.0D;
   private int m_NormalExposure;
   private int m_NormalGain;
   private int m_AdaptiveExposure;
   private int m_AdaptiveGain;
   private long mProcessingCost = 0L;
   private static final int IMG_BUF_MAX = 523280;
   private static final int IMG_INT_BUF_MAX = 129900;
   private final byte[][] m_pFullBufferA = new byte[12][524297];
   private final byte[][] m_pFullBufferN = new byte[12][524297];
   private final byte[] m_ImageBufferBG = new byte[524288];
   private final byte[] m_Image = new byte[129900];
   private final byte[] m_ImageA = new byte[129900];
   private final byte[] m_ImageBG = new byte[129900];
   private byte[] m_ImagePrev = new byte[129900];
   private final byte[] m_ImageIntermediate = new byte[129900];
   private final byte[] m_ImageLast = new byte[129900];
   private byte[] m_ImageRawPrevA = new byte[524297];
   private byte[] tmpBuffer = new byte[523280];
   private byte[] m_TouchBuffer = new byte[6];
   private boolean bIsAfterAbortCpaturing = true;
   private boolean bUSBisdominated = false;
   private int mSleepPlus = 0;
   private int mSleepVal = 20;
   private Runnable mLoop;
   private Runnable mSLoop;
   private BioMiniSlim2.StartCapturingLoop mStartCapturingLoop;
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
   private int m_nBGAvg = -1;
   private boolean m256K_Mode = true;
   private Queue<BioMiniBase.MDRCapturedPair> mCapturedQueue = new LinkedList();
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
   private static final int CMD_READ_LFD_4TH = 236;
   private static final int CMD_READ_LFD_ALL_DONE = 238;
   private static final int CMD_SET_LED = 194;
   private static final int CMD_ANDROID_256K_MODE = 234;
   private static final int OV_IIC_EEPROM_ADDR = 174;

   public BioMiniSlim2() {
      this.TAG = "BioMiniSlim2";
   }

   public int getMaxBulkSize() {
      return 524288;
   }

   public boolean captureSingle(final IBioMiniDevice.CaptureOption opt, final ICaptureResponder responder, boolean bAsync) {
      if (!this.isCapturing() && !this.mIsUsbThreadRunning) {
         this.mCaptureResponder = responder;
         if (bAsync) {
            Runnable captureObj = new Runnable() {
               public void run() {
                  BioMiniSlim2.this._captureSingle(opt, responder);
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

   boolean onCaptureDebug(ICaptureResponder responder, byte[] imageData, int width, int height, boolean isFingerOn) {
      int nIntWidth = this.getIntermediateWidth();
      int nIntHeight = this.getIntermediateHeight();
      int nRawWidth = this.getRawWidth();
      int nRawHeight = this.getRawHeight();
      boolean showFrameN = true;
      boolean showFrameA = true;
      boolean showFrameBG = false;
      boolean showBufferBG = false;
      boolean showHistogram = true;
      int width_debug = width;
      if (showFrameN) {
         width_debug = width + nIntWidth;
      }

      if (showFrameA) {
         width_debug += nIntWidth;
      }

      if (showFrameBG) {
         width_debug += nIntWidth;
      }

      if (showBufferBG) {
         width_debug += nRawWidth;
      }

      int height_debug = height;
      if (showFrameN || showFrameA || showFrameBG) {
         height_debug = height > nIntHeight ? height : nIntHeight;
      }

      if (showBufferBG) {
         height_debug = height_debug > nRawHeight ? height_debug : nRawHeight;
      }

      if (showHistogram) {
         height_debug += 110;
      }

      byte[] debugImage = new byte[width_debug * height_debug];
      Arrays.fill(debugImage, (byte)-64);

      int width_hist;
      for(width_hist = 0; width_hist < height; ++width_hist) {
         System.arraycopy(imageData, width_hist * width, debugImage, width_hist * width_debug, width);
      }

      for(width_hist = 0; width_hist < nIntHeight; ++width_hist) {
         if (showFrameN) {
            System.arraycopy(this.m_Image, width_hist * nIntWidth, debugImage, width_hist * width_debug + width, nIntWidth);
         }

         if (showFrameA) {
            System.arraycopy(this.m_ImageA, width_hist * nIntWidth, debugImage, width_hist * width_debug + width + nIntWidth, nIntWidth);
         }

         if (showFrameBG) {
            System.arraycopy(this.m_ImageBG, width_hist * nIntWidth, debugImage, width_hist * width_debug + width + nIntWidth * 2, nIntWidth);
         }
      }

      for(width_hist = 0; width_hist < nRawHeight; ++width_hist) {
         if (showBufferBG) {
            System.arraycopy(this.m_ImageBufferBG, width_hist * nRawWidth, debugImage, width_hist * width_debug + width + nIntWidth * 3, nRawWidth);
         }
      }

      if (showHistogram) {
         width_hist = width_debug / 2;
         this.LogD("*************** 1 ****************");
         float[] h1 = this.getHistogram(this.m_Image, this.m_ImageBG, 20).normHist(100.0F);
         float[] h2 = this.getHistogram(this.m_ImageA, this.m_ImageBG, 20).normHist(100.0F);
         this.LogD("*************** 2 ****************");

         for(int x = 0; x < width_hist; ++x) {
            int v = x * 256 / width_hist;
            int h = (int)h1[Math.min(255, v)];

            int y;
            for(y = 0; y < h; ++y) {
               debugImage[(height_debug - y - 1) * width_debug + x] = 0;
            }

            h = (int)h2[Math.min(255, v)];

            for(y = 0; y < h; ++y) {
               debugImage[(height_debug - y - 1) * width_debug + x + width_hist] = 64;
            }
         }

         this.LogD("*************** 3 ****************");
      }

      return this.onCapture(responder, debugImage, width_debug, height_debug, isFingerOn);
   }

   private BioMiniSlim2.HistogramContainer getHistogram(byte[] image) {
      BioMiniSlim2.HistogramContainer hist = new BioMiniSlim2.HistogramContainer();
      byte[] var3 = image;
      int var4 = image.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         byte v = var3[var5];
         hist.add(v);
      }

      return hist;
   }

   private BioMiniSlim2.HistogramContainer getHistogram(byte[] image, byte[] bg, int diff) {
      BioMiniSlim2.HistogramContainer hist = new BioMiniSlim2.HistogramContainer();

      for(int i = 0; i < image.length; ++i) {
         int a = image[i] & 255;
         int b = bg[i] & 255;
         if (Math.abs(a - b) > diff) {
            hist.add(image[i]);
         }
      }

      return hist;
   }

   private boolean _captureSingle(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      int width = this.getImageWidth();
      int height = this.getImageHeight();
      byte[] capturedimage = new byte[width * height];
      this.mCurrentCaptureOption = opt;
      IBioMiniDevice.ErrorCode re = this.CaptureSingle(capturedimage);
      this.LogD("CaptureSingle return : " + re.toString());
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
               this.mLoop = new BioMiniSlim2.UsbBulkLoopRev(this);
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
               byte[] tmpImageBuffer = new byte[120000];
               int idx = 0;

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
                     int normalExp = false;
                     int adaptiveExp = false;
                     int normalTag = -1;
                     int adaptiveTag = -1;
                     BioMiniBase.MDRCapturedPair cp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll();

                     for(BioMiniBase.MDRCapturedPair tmpCp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll(); tmpCp != null; tmpCp = (BioMiniBase.MDRCapturedPair)this.mCapturedQueue.poll()) {
                        cp = tmpCp;
                     }

                     if (cp != null) {
                        normalTag = cp.MdrN.TagIndex;
                        adaptiveTag = cp.MdrN.TagIndex;
                        imageN = cp.MdrN.Image;
                        imageA = cp.MdrA.Image;
                        int normalExp = cp.MdrN.Exposure;
                        int var37 = cp.MdrA.Exposure;
                     }

                     if (imageN != null && imageA != null) {
                        int bFingerOn = imageN[nBytesImage + 8];
                        this.LogD("CaptureSingle : Got bFingerOn(" + bFingerOn + ", " + imageN[nBytesImage + 8] + ") @" + nTop);
                        this.LogD("CaptureSingle : bIwakeupYou :" + this.bIwakeupYou);
                        if (!this.bIwakeupYou && bFingerOn == 0) {
                           this.LogD("break;");
                           break;
                        }

                        this.LogD("CaptureSingle : Compensating...  " + this.m_Image + ", " + this.m_ImageA);
                        this.LogD("CaptureSingle : Tagging(" + this.m_nTop + "), normalTag(" + normalTag + "), adaptiveTag(" + adaptiveTag + ")");
                        this.LogProcessStart("Comp");
                        BioMiniJni.Comp(imageN, this.m_Image, bFingerOn);
                        BioMiniJni.Comp(imageA, this.m_ImageA, bFingerOn);
                        this.LogProcessEnd("Comp");
                        this.LogD("CaptureSingle : Count( " + idx + " )");
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
                           this.LogD("GetPreprocessedImage Final");
                           BioMiniJni.GetPreprocessedImage(this.m_ImageBG, this.m_Image, this.m_ImageA, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                        } else if (bFingerOn == 1) {
                           this.LogD("GetPreprocessedImageEx - 1");
                           BioMiniJni.GetPreprocessedImageEx(this.m_ImageBG, this.m_Image, this.m_ImageA, this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64, !this.isBackGround);
                        } else {
                           this.LogD("GetPreprocessedImageEx - 2");
                           BioMiniJni.GetPreprocessedImageEx(this.m_ImageBG, this.m_Image, this.m_ImageA, this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64, !this.isBackGround);
                        }

                        this.LogProcessEnd("Preprocessing");
                        ++idx;
                        long currentCost = SystemClock.uptimeMillis() - timerStart;
                        if (this.mProcessingCost != 0L) {
                           this.mProcessingCost = (long)((double)this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                        } else {
                           this.mProcessingCost = currentCost;
                        }

                        int off_x = (int)Math.ceil(0.0D);
                        int off_y = (int)Math.ceil(16.0D);

                        for(int i = 0; i < 400; ++i) {
                           System.arraycopy(this.m_ImageIntermediate, 300 * (i + off_y) + off_x, tmpImageBuffer, 300 * i, 300);
                        }

                        System.arraycopy(tmpImageBuffer, 0, this.m_ImageIntermediate, 0, 120000);
                        System.arraycopy(this.m_ImageIntermediate, 0, this.m_ImageLast, 0, this.m_ImageIntermediate.length);
                        if (this.m_TimeOut != 0L && System.currentTimeMillis() - this.m_Start > this.m_TimeOut) {
                           this.LogD("Capture timeout occurred");
                           this.onCaptureError(this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - this.m_Start) + "/" + this.m_TimeOut + ")");
                           this.mIsTimeoutOccurred = true;
                           break;
                        }

                        ++idx;
                        if (bFingerOn == 1) {
                           this.LogD("CaptureSingle : isCaptured is true");
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
               this.LogPublicProcessEnd("AutoCapture");
               this.LogD("CaptureSingle : Done capturing a fingerprint");
               if (this.isCaptured) {
                  if (this.m_nCaptureMode == 3) {
                     this.LogProcessStart("LFD");
                     this.Slim2LFDWorker();
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
               this.mStartCapturingLoop = new BioMiniSlim2.StartCapturingLoop(this, true);
               this.mStartCapturingThread = new Thread(this.mStartCapturingLoop);
               this.mSLoop = new BioMiniSlim2.UsbBulkLoopRev(this.mStartCapturingLoop);
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

   public int startCapturingDebug(IBioMiniDevice.CaptureOption opt, ICaptureResponder responder) {
      this.LogD("Start capturing debug...");
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
            this.m_nCaptureMode = 2;
            this.bThreadFlag = true;
            this.mStartCapturingLoop = null;
            this.mStartCapturingThread = null;
            this.mSLoop = new BioMiniSlim2.UsbBulkLoopDebug();
            this.mUsbThread = new Thread(this.mSLoop);
            this.mUsbThread.start();
            this.m_LastError = IBioMiniDevice.ErrorCode.OK;
            return IBioMiniDevice.ErrorCode.OK.value();
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
      return this.m256K_Mode ? 640 : this.getRawWidth();
   }

   private int getBulkHeight() {
      return this.m256K_Mode ? 408 : this.getRawHeight();
   }

   private boolean Switch_256K_mode(boolean bFlag) {
      this.LogD("Switch_256K_mode / bFlag :" + bFlag);
      boolean bRet = false;
      byte[] cmd = new byte[64];
      this.mUsbHandler.controlRx(234, cmd, 1);
      this.LogD("Set 256K_mode return " + cmd[0]);
      return bRet;
   }

   private int CaptureFrameStart() {
      this.bUSBisdominated = true;
      this.bAbortCapturing = false;
      this.isCaptured = false;
      int Ret = true;
      boolean re = false;
      int nWidth = this.getRawWidth();
      int nHeight = this.getRawHeight();
      int nBulkWidth = this.getBulkWidth();
      int nBulkHeight = this.getBulkHeight();
      int nBulkLength = nBulkWidth * nBulkHeight;
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
      } else if (this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
         if (this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.MID) {
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
      } else {
         this.LogD("SetFrameRate - Extra LOW ");
         this.m_expoScaleFactor = 0.4D;
         this.setReg(4, 14);
         this.setReg(5, 188);
      }

      this.Switch_256K_mode(this.m256K_Mode);
      this.m_NHEH = (int)((double)this.m_NormalExposure * this.m_expoScaleFactor);
      this.m_NHGH = this.m_NormalGain;
      this.m_AEH = (int)((double)this.m_AdaptiveExposure * this.m_expoScaleFactor);
      this.m_AGH = this.m_AdaptiveGain;
      this.LogD("Setting camera parameter - set nr exp(eeprom) : " + this.m_NormalExposure + ", ad exp(eeprom) : " + this.m_AdaptiveExposure);
      this.LogD("Setting camera parameter - set nr exp(calc) : " + this.m_NHEH + ", ad exp(calc) : " + this.m_AEH);
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
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 60, 30};
            this.mUsbHandler.setBulkRx(2);
            re = this.mUsbHandler.initRead(nBulkLength, 0, false);
            if (!re) {
               return 0;
            } else {
               byte[] cdata = new byte[64];
               byte fillExtra = -1;
               re = this.mUsbHandler.read(this.m_ImageBufferBG, nBulkLength, fillExtra, new IUsbHandler.IReadProcessorAdv() {
                  public boolean beforeRead() {
                     return BioMiniSlim2.this.mUsbHandler.controlRx(225, new byte[]{0, 0, 0, 0, 0, 0}, 6);
                  }

                  public void firstBulkReceived(long delay) {
                  }

                  public boolean afterRead() {
                     return BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                  }
               });
               if (!re) {
                  this.LogE("CaptureFrameStart read (BG) error");
                  return 0;
               } else {
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
   }

   private void Slim2LFDWorker() {
      int m_ImageLast_width = 300;
      int m_ImageLast_height = 400;
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

      BioMiniSlim2.Slim2LFDWorkerLoop w1 = new BioMiniSlim2.Slim2LFDWorkerLoop(data1, scanner_mode, this.m_DetectFake, this.m_NHEH);
      BioMiniSlim2.Slim2LFDWorkerLoop w2 = new BioMiniSlim2.Slim2LFDWorkerLoop(data2, scanner_mode, this.m_DetectFake, this.m_NHEH);
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
   }

   private void CaptureFrameStop() {
      this.LogD("Stops capturing...");
      int tries = 0;

      for(byte MAX_TRIES = 50; this.mIsUsbThreadRunning && tries < MAX_TRIES; ++tries) {
         SystemClock.sleep(100L);
      }

      this.LogD("checks reading...");
      if (this.mUsbHandler != null && this.mUsbHandler.isReading()) {
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
      if (this.mUsbHandler != null) {
         this.mUsbHandler.controlTx(194, new byte[]{0, 0, 0, 0}, 1);
      }

      this.resetCaptureOpts();
      this.bUSBisdominated = false;
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
      byte[] bufPos = new byte[4];
      BioMiniJni.SwitchScanningMode(this.m_nScanningMode.value());
      bufWrite[0] = 1;
      this.mUsbHandler.controlTx(201, bufWrite, 1);
      boolean re = this.mUsbHandler.readEEPROM(4, 4, bufRead);
      if (!re) {
         return 0;
      } else {
         this.m_EW = 300;
         this.m_EH = 433;
         this.m_SOX = 10;
         this.m_SOY = 20;
         this.m_sclFX = 312;
         this.m_sclFY = 308;
         this.m_AdaptiveExposure = 80;
         this.m_AdaptiveGain = 0;
         re = this.mUsbHandler.readEEPROM(48, 32, bufRead);
         if (!re) {
            return 0;
         } else {
            if (bufRead[0] != -1 && bufRead[1] != -1) {
               bufPos = Arrays.copyOf(bufRead, 4);
               this.m_NormalExposure = ((bufRead[4] & 255) << 8) + bufRead[5];
               this.m_NormalExposure += 100;
               this.m_NormalGain = bufRead[6];
            } else {
               bufPos[0] = 0;
               bufPos[1] = -106;
               bufPos[2] = 0;
               bufPos[3] = -106;
               this.m_NormalExposure = 380;
               this.m_NormalGain = 0;
            }

            this.LogD("Original Normal Exposure = " + this.m_NormalExposure + ", Origianl Gain = " + this.m_NormalGain);
            this.LogD("Original Adaptive Exposure = " + this.m_AdaptiveExposure + ", Origianl Adatptive Gain = " + this.m_AdaptiveGain);
            re = this.mUsbHandler.readEEPROM(160, 4, bufRead);
            if (!re) {
               return 0;
            } else {
               if (bufRead[0] != -1 && bufRead[1] != -1) {
                  this.m_SOX = ((bufRead[0] & 255) << 8) + (bufRead[1] & 255);
                  this.m_SOY = ((bufRead[2] & 255) << 8) + (bufRead[3] & 255);
                  if (this.m_SOX >= 32768) {
                     this.m_SOX ^= 65535;
                     this.m_SOX = -this.m_SOX - 1;
                  }

                  if (this.m_SOY >= 32768) {
                     this.m_SOY ^= 65535;
                     this.m_SOY = -this.m_SOY - 1;
                  }
               } else {
                  this.m_SOX = 0;
                  this.m_SOY = 13;
               }

               this.LogD("off-x = " + this.m_SOX + ", off-y = " + this.m_SOY);
               re = this.mUsbHandler.readEEPROM(96, 4, bufRead);
               if (!re) {
                  return 0;
               } else {
                  if (bufRead[0] == 0 && bufRead[1] == 0) {
                     this.m_sclFX = 312;
                     this.m_sclFY = 312;
                  } else {
                     this.m_sclFX = ((bufRead[0] & 255) << 8) + (bufRead[1] & 255);
                     this.m_sclFY = ((bufRead[2] & 255) << 8) + (bufRead[3] & 255);
                  }

                  this.LogD("m_sclFX = " + this.m_sclFX + ", m_sclFY= " + this.m_sclFY);
                  bufWrite[0] = 0;
                  re = this.mUsbHandler.controlTx(201, bufWrite, 1);
                  if (!re) {
                     return 0;
                  } else {
                     re = this.mUsbHandler.controlTx(193, bufPos, 4);
                     if (!re) {
                        return 0;
                     } else {
                        this.mDeviceInfo.deviceName = this.TAG;
                        this.mDeviceInfo.versionSDK = this.BASE_VERSION;
                        this.mDeviceInfo.scannerType = IBioMiniDevice.ScannerType.BIOMINI_SLIM2;
                        Arrays.fill(bufRead, (byte)0);
                        re = this.mUsbHandler.controlRx(this.GetCmd(201), bufRead, 32);
                        if (!re) {
                           return 0;
                        } else {
                           this.mDeviceInfo.deviceSN = (new String(bufRead, 0, 32)).trim().replaceAll("[^\\x00-\\x7F]", "");
                           BioMiniJni.setESA(this.m_EW, this.m_EH, this.m_SOX, this.m_SOY, (float)this.m_sclFX, (float)this.m_sclFY);
                           return 1;
                        }
                     }
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
      return 300;
   }

   private int getTargetHeight() {
      return 400;
   }

   private int getRawWidth() {
      return this.m256K_Mode ? 640 : 1240;
   }

   private int getRawHeight() {
      return this.m256K_Mode ? 408 : 422;
   }

   private int getIntermediateWidth() {
      return 300;
   }

   private int getIntermediateHeight() {
      return 433;
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

   private class Slim2LFDWorkerLoop implements Runnable {
      byte[] fp_data;
      int m_mode;
      int m_DetectFake;
      int m_NormalExp;
      int[] mScore = new int[1];
      int re;

      Slim2LFDWorkerLoop(byte[] _data, int _mode, int _detectFake, int _normalExposure) {
         this.fp_data = _data;
         this.m_mode = _mode;
         this.m_DetectFake = _detectFake;
         this.m_NormalExp = _normalExposure;
         this.re = 0;
      }

      public int Score() {
         return this.mScore[0];
      }

      public void run() {
         this.re = BioMiniJni.GetLFDResult(this.fp_data, this.m_mode, this.m_DetectFake, this.m_NormalExp, this.mScore);
      }
   }

   private class StartCapturingLoop implements Runnable {
      BioMiniSlim2 pBioMiniAndroid;
      boolean bIwakeupYou = false;
      boolean IsUsingNewBulkThread = false;

      StartCapturingLoop(BioMiniSlim2 pMyp, boolean useNewBulkLoop) {
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
         int nTargetWidth = BioMiniSlim2.this.getTargetWidth();
         int nTargetHeight = BioMiniSlim2.this.getTargetHeight();
         int nIntWidth = BioMiniSlim2.this.getIntermediateWidth();
         int nIntHeight = BioMiniSlim2.this.getIntermediateHeight();
         int nRawWidth = BioMiniSlim2.this.getRawWidth();
         int nRawHeight = BioMiniSlim2.this.getRawHeight();
         int nBytesImage = nRawWidth * nRawHeight;
         float gce_gain = 1.0F;
         float lce_gain = 0.4F;
         float de_gain = 0.03F;
         if (BioMiniSlim2.this.g_bExtraDry == 1) {
            gce_gain = 0.0F;
            lce_gain = 0.0F;
            de_gain = 0.0F;
         } else {
            gce_gain = 0.8F;
            lce_gain = 0.4F;
            de_gain = 0.02F;
         }

         BioMiniSlim2.this.mProcessingCost = 0L;
         int bFingerOnx = false;
         int bcheckCorrx = false;
         BioMiniSlim2.this.mHasPreviewBuffered = 0;
         if (BioMiniSlim2.this.mEnableAutoSleep) {
            BioMiniSlim2.this.wakeUp();
         }

         byte[] tmpImageBuffer = new byte[120000];

         while(BioMiniSlim2.this.bThreadFlag) {
            if (BioMiniSlim2.this.m_nTop > -1) {
               this.iwait();
               BioMiniSlim2.this.printTimeTag("StartCapturingLoop : Got captured notice");
               if (!this.bIwakeupYou || BioMiniSlim2.this.bAbortCapturing) {
                  break;
               }

               int nTop = BioMiniSlim2.this.m_nTop;
               long timerStart = SystemClock.currentThreadTimeMillis();
               byte[] imageN = null;
               byte[] imageA = null;
               int off_y;
               if (this.IsUsingNewBulkThread) {
                  int MAX_POLLING_TRIES = 50;
                  off_y = 0;

                  BioMiniBase.MDRCapturedPair cp;
                  for(cp = (BioMiniBase.MDRCapturedPair)BioMiniSlim2.this.mCapturedQueue.poll(); cp == null && off_y < MAX_POLLING_TRIES; ++off_y) {
                     SystemClock.sleep(100L);
                     cp = (BioMiniBase.MDRCapturedPair)BioMiniSlim2.this.mCapturedQueue.poll();
                  }

                  while(!BioMiniSlim2.this.mCapturedQueue.isEmpty()) {
                     BioMiniBase.MDRCapturedPair tmpCp = (BioMiniBase.MDRCapturedPair)BioMiniSlim2.this.mCapturedQueue.poll();
                     if (tmpCp != null) {
                        cp = tmpCp;
                     }
                  }

                  if (cp != null) {
                     imageN = cp.MdrN.Image;
                     imageA = cp.MdrA.Image;
                  }
               } else {
                  imageN = BioMiniSlim2.this.m_pFullBufferN[nTop];
                  imageA = BioMiniSlim2.this.m_pFullBufferA[nTop];
               }

               if (imageN != null && imageA != null) {
                  int bFingerOn = imageN[nBytesImage + 8];
                  BioMiniJni.Comp(imageN, BioMiniSlim2.this.m_Image, bFingerOn);
                  BioMiniJni.Comp(imageA, BioMiniSlim2.this.m_ImageA, bFingerOn);
                  BioMiniSlim2.this.printTimeTag("StartCapturingLoop : Compensation done");
                  int bcheckCorr;
                  if (BioMiniSlim2.this.mHasPreviewBuffered == 0) {
                     bcheckCorr = BioMiniJni.CheckCorrelation(BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_Image, 60);
                     BioMiniSlim2.this.mHasPreviewBuffered = 1;
                  } else {
                     bcheckCorr = BioMiniJni.CheckCorrelation(BioMiniSlim2.this.m_ImagePrev, BioMiniSlim2.this.m_Image, 60);
                  }

                  BioMiniSlim2.this.LogD("StartCapturingLoop : CheckCorrelation done... (" + bcheckCorr + ")");
                  BioMiniSlim2.this.m_ImagePrev = Arrays.copyOf(BioMiniSlim2.this.m_Image, nIntWidth * nIntHeight);
                  if (bFingerOn == 1 && bcheckCorr == 1) {
                     BioMiniSlim2.this.LogD("GetPreprocessedImage Final");
                     BioMiniJni.GetPreprocessedImage(BioMiniSlim2.this.m_ImageBG, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_ImageA, BioMiniSlim2.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64);
                  } else if (bFingerOn == 1) {
                     BioMiniSlim2.this.LogD("GetPreprocessedImageEx - 1");
                     BioMiniJni.GetPreprocessedImageEx(BioMiniSlim2.this.m_ImageBG, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_ImageIntermediate, gce_gain, lce_gain, de_gain, 1, 0, nIntWidth / 2, 64, !BioMiniSlim2.this.isBackGround);
                  } else {
                     BioMiniSlim2.this.LogD("GetPreprocessedImageEx - 2");
                     BioMiniJni.GetPreprocessedImageEx(BioMiniSlim2.this.m_ImageBG, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_ImageIntermediate, 0.0F, 0.0F, 0.0F, 0, 0, nIntWidth / 2, 64, !BioMiniSlim2.this.isBackGround);
                  }

                  BioMiniSlim2.this.printTimeTag("StartCapturingLoop : Preprocessing done");
                  int off_x = (int)Math.ceil(0.0D);
                  off_y = (int)Math.ceil(16.0D);

                  for(int i = 0; i < 400; ++i) {
                     System.arraycopy(BioMiniSlim2.this.m_ImageIntermediate, 300 * (i + off_y) + off_x, tmpImageBuffer, 300 * i, 300);
                  }

                  System.arraycopy(tmpImageBuffer, 0, BioMiniSlim2.this.m_ImageIntermediate, 0, 120000);
                  BioMiniSlim2.this.drawDebugMap(bFingerOn, bcheckCorr, BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_AEH, BioMiniSlim2.this.m_Image, BioMiniSlim2.this.m_ImageBG, BioMiniSlim2.this.m_ImageA, nIntWidth, nIntHeight, BioMiniSlim2.this.m_ImageIntermediate, nTargetWidth, nTargetHeight);
                  long currentCost = SystemClock.currentThreadTimeMillis() - timerStart;
                  if (BioMiniSlim2.this.mProcessingCost != 0L) {
                     BioMiniSlim2.this.mProcessingCost = (long)((double)BioMiniSlim2.this.mProcessingCost * 0.8D + (double)currentCost * 0.2D);
                  } else {
                     BioMiniSlim2.this.mProcessingCost = currentCost;
                  }

                  System.arraycopy(BioMiniSlim2.this.m_ImageIntermediate, 0, BioMiniSlim2.this.m_ImageLast, 0, BioMiniSlim2.this.m_ImageIntermediate.length);
                  BioMiniSlim2.this.onCapture(BioMiniSlim2.this.mCaptureResponder, BioMiniSlim2.this.m_ImageLast, nTargetWidth, nTargetHeight, bFingerOn == 1);
                  if (bFingerOn == 1) {
                     BioMiniSlim2.this.isCaptured = true;
                  }

                  if (BioMiniSlim2.this.m_TimeOut != 0L && System.currentTimeMillis() - BioMiniSlim2.this.m_Start > BioMiniSlim2.this.m_TimeOut) {
                     BioMiniSlim2.this.onCaptureError(BioMiniSlim2.this.mCaptureResponder, -11, "Capture Timeout (" + (System.currentTimeMillis() - BioMiniSlim2.this.m_Start) + "/" + BioMiniSlim2.this.m_TimeOut + ")");
                     break;
                  }
               } else {
                  BioMiniSlim2.this.LogE("StartCapturingLoop null image buffer");
               }
            }
         }

         BioMiniSlim2.this.bThreadFlag = false;
         BioMiniSlim2.this.LogD("StartCapturingLoop : Capturing thread end");
         if (BioMiniSlim2.this.mEnableAutoSleep) {
            BioMiniSlim2.this.hibernate();
         }

         BioMiniSlim2.this.CaptureFrameStop();
      }
   }

   private class FrameSyncTimer {
      LinkedList<BioMiniSlim2.FrameSyncTimer.Signal> mSignal = new LinkedList();
      boolean mIsLive;
      float mPeriod;
      long mAnchorMillis;
      int mSegments;
      Thread mTimerThread = new Thread(new Runnable() {
         public void run() {
            FrameSyncTimer.this.mAnchorMillis = SystemClock.uptimeMillis();

            for(int idxLastProcessed = -1; FrameSyncTimer.this.mIsLive; SystemClock.sleep(5L)) {
               long gap = SystemClock.uptimeMillis() - FrameSyncTimer.this.mAnchorMillis;
               int idxAbsolute = (int)((float)gap / FrameSyncTimer.this.mPeriod);
               if (gap <= 5L) {
                  idxLastProcessed = 0;
                  idxAbsolute = 0;
               }

               if (idxLastProcessed != idxAbsolute) {
                  idxLastProcessed = idxAbsolute;
                  int idxRelative = idxAbsolute % FrameSyncTimer.this.mSegments;
                  FrameSyncTimer.this.mSignal.add(FrameSyncTimer.this.new Signal(idxRelative));
                  BioMiniSlim2.this.LogE("* timer set @" + gap + " - " + idxRelative);
                  SystemClock.sleep(10L);
                  FrameSyncTimer.this.mSignal.poll();
               }
            }

         }
      });

      public FrameSyncTimer(float period, int segments) {
         this.mPeriod = period;
         this.mSegments = segments;
         this.mAnchorMillis = -1L;
         this.mIsLive = true;
         this.mTimerThread.start();
      }

      public void setSync(long delta) {
         this.mAnchorMillis = SystemClock.uptimeMillis() + delta;
      }

      public void stopAsync() {
         this.mIsLive = false;
      }

      public void stop() throws InterruptedException {
         this.mIsLive = false;
         this.mTimerThread.join();
      }

      public boolean waitNext(int idx) {
         BioMiniSlim2.FrameSyncTimer.Signal sig = (BioMiniSlim2.FrameSyncTimer.Signal)this.mSignal.poll();
         boolean re = sig != null && sig.mIndex == idx;

         while(sig == null && this.mIsLive) {
            sig = (BioMiniSlim2.FrameSyncTimer.Signal)this.mSignal.poll();
            if (sig != null && sig.mIndex == idx) {
               re = true;
               break;
            }

            SystemClock.sleep(5L);
         }

         return re;
      }

      class Signal {
         int mIndex;

         public Signal(int idx) {
            this.mIndex = idx;
         }
      }
   }

   private class UsbBulkLoopDebug implements Runnable {
      Queue<BioMiniBase.RawImageItem> mImageQueueAny = new LinkedList();
      byte fillExtra = -1;
      BioMiniSlim2.FrameSyncTimer mFrameSyncTimer;

      UsbBulkLoopDebug() {
      }

      public void run() {
         BioMiniSlim2.this.LogD(" -- UsbBulkLoopDebug started... -- ");
         this.mImageQueueAny.clear();
         BioMiniSlim2.this.mCapturedQueue.clear();
         BioMiniSlim2.this.LogD("UsbBulkLoopDebug : Setting Exposure m_NHEH: " + BioMiniSlim2.this.m_NHEH + " m_AEH: " + BioMiniSlim2.this.m_AEH);
         if (BioMiniSlim2.this.mUsbHandler != null) {
            BioMiniSlim2.this.mIsUsbThreadRunning = true;
            (new Thread(BioMiniSlim2.this.new UsbBulkLoopShow(this))).start();
            int nBulkWidth = BioMiniSlim2.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            boolean updateAlways = false;
            BioMiniSlim2.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim2.this.mUsbHandler.resize(nBulkLength);
            float fr = 10.0F;
            if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               fr = 15.0F;
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.LOW) {
               fr = 7.5F;
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.HIGH) {
                  fr = 12.0F;
               } else {
                  fr = 10.0F;
               }
            } else {
               fr = 6.0F;
            }

            BioMiniSlim2.this.mUsbHandler.setBulkTimeout((int)(1500.0F / fr));
            updateAlways = false;
            int[] toggleCmd = new int[]{226, 225};
            int cntLoop = 0;
            long milliBulkStart = SystemClock.uptimeMillis();
            long milliBulkEnd = milliBulkStart;
            int NUM_REPEATS = true;
            boolean TEST_SPAN = false;
            boolean TEST_OFFSET = true;
            boolean re;
            int[] timesSuccessful2;
            int var10002;
            final int cmdCur;
            int idxTest;
            long timerSpanCur;
            int nTop;
            int nTopNext;
            final long lastBulkCost;
            if (TEST_SPAN) {
               long[] timerSpanTries = new long[]{(long)(1000.0F / fr), (long)(1500.0F / fr), (long)(2000.0F / fr), (long)(2500.0F / fr), (long)(3000.0F / fr), (long)(3000.0F / fr), (long)(2500.0F / fr), (long)(2000.0F / fr), (long)(1500.0F / fr), (long)(1000.0F / fr)};
               timesSuccessful2 = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
               boolean lastResult = true;

               while(BioMiniSlim2.this.bThreadFlag && cntLoop < timerSpanTries.length * 20) {
                  try {
                     cmdCur = toggleCmd[cntLoop % 2];
                     idxTest = cntLoop / 20;
                     timerSpanCur = timerSpanTries[idxTest];
                     ++cntLoop;
                     BioMiniSlim2.this.LogProcessStart("ReadX");
                     re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopDebug : mUsbHandler initRead error");
                        break;
                     }

                     nTop = BioMiniSlim2.this.m_nTop;
                     nTopNext = (nTop + 1) % 12;
                     BioMiniSlim2.this.LogD("Before read m_pFullBufferX[nTopNext]");
                     this.fillExtra = -1;
                     BioMiniSlim2.this.LogD("Trying interval " + timerSpanCur);
                     lastBulkCost = milliBulkEnd - milliBulkStart;
                     if (milliBulkEnd - milliBulkStart > 0L && timerSpanCur > lastBulkCost) {
                        if (!lastResult) {
                           SystemClock.sleep(timerSpanCur - lastBulkCost + 7L);
                        } else {
                           SystemClock.sleep(timerSpanCur - lastBulkCost);
                        }
                     }

                     milliBulkStart = SystemClock.uptimeMillis();
                     lastResult = re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                        public boolean beforeRead() {
                           return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(cmdCur), BioMiniSlim2.this.m_TouchBuffer, 6);
                        }

                        public void firstBulkReceived(long delay) {
                        }

                        public boolean afterRead() {
                           return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                        }
                     }, 1);
                     if (re) {
                        var10002 = timesSuccessful2[idxTest]++;
                     }

                     milliBulkEnd = SystemClock.uptimeMillis();
                     BioMiniSlim2.this.LogProcessEnd("ReadX");
                     BioMiniSlim2.this.LogD("After read m_pFullBufferX[nTopNext]");
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopDebug read (X) error");
                     } else {
                        SystemClock.sleep(5L);
                        BioMiniSlim2.this.LogD("UsbBulkLoopDebug image queue (X) add : (" + nTopNext + ")");
                        this.mImageQueueAny.add(BioMiniSlim2.this.new RawImageItem(BioMiniSlim2.this.m_pFullBufferA[nTopNext], ""));
                        if (!BioMiniSlim2.this.bThreadFlag) {
                           break;
                        }
                     }
                  } catch (NullPointerException var28) {
                     BioMiniSlim2.this.LogE("mUsbHandler missing");
                     break;
                  }
               }

               BioMiniSlim2.this.LogD(" -- UsbBulkLoopDebug span test finished... -- ");

               for(cmdCur = 0; cmdCur < timesSuccessful2.length; ++cmdCur) {
                  BioMiniSlim2.this.LogD("" + timerSpanTries[cmdCur] + " Successful: " + timesSuccessful2[cmdCur]);
               }
            }

            if (TEST_OFFSET) {
               cntLoop = 0;
               this.mFrameSyncTimer = BioMiniSlim2.this.new FrameSyncTimer(1070.0F / fr, 2);
               int[] offsetTries = new int[]{0, 5, 11, 22, 33, 44, 56};
               timesSuccessful2 = new int[]{0, 0, 0, 0, 0, 0, 0};

               final int i;
               while(BioMiniSlim2.this.bThreadFlag && cntLoop < offsetTries.length * 20) {
                  try {
                     i = cntLoop % 2;
                     cmdCur = toggleCmd[i];
                     idxTest = cntLoop / 20;
                     timerSpanCur = 67L;
                     ++cntLoop;
                     BioMiniSlim2.this.mUsbHandler.setBulkTimeout((int)(3000.0F / fr) + offsetTries[idxTest]);
                     BioMiniSlim2.this.LogProcessStart("ReadX");
                     re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopDebug : mUsbHandler initRead error");
                        break;
                     }

                     nTop = BioMiniSlim2.this.m_nTop;
                     nTopNext = (nTop + 1) % 12;
                     BioMiniSlim2.this.LogD("Before read m_pFullBufferX[nTopNext]");
                     this.fillExtra = -1;
                     lastBulkCost = (long)offsetTries[idxTest];
                     BioMiniSlim2.this.LogD("Trying offset " + lastBulkCost);
                     this.mFrameSyncTimer.waitNext(i);
                     re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                        public boolean beforeRead() {
                           return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(cmdCur), BioMiniSlim2.this.m_TouchBuffer, 6);
                        }

                        public void firstBulkReceived(long delay) {
                           BioMiniSlim2.this.LogD("** got 1st packet! delay " + delay);
                           if (delay > lastBulkCost + 10L && i == 0) {
                              BioMiniSlim2.this.LogD("Adjusting timer @" + SystemClock.uptimeMillis());
                              UsbBulkLoopDebug.this.mFrameSyncTimer.setSync(-lastBulkCost);
                           }

                        }

                        public boolean afterRead() {
                           return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                        }
                     }, 1);
                     if (re) {
                        var10002 = timesSuccessful2[idxTest]++;
                     }

                     BioMiniSlim2.this.LogProcessEnd("ReadX");
                     BioMiniSlim2.this.LogD("After read m_pFullBufferX[nTopNext]");
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopDebug read (X) error");
                     } else {
                        SystemClock.sleep(5L);
                        BioMiniSlim2.this.LogD("UsbBulkLoopDebug image queue (X) add : (" + nTopNext + ")");
                        this.mImageQueueAny.add(BioMiniSlim2.this.new RawImageItem(BioMiniSlim2.this.m_pFullBufferA[nTopNext], ""));
                        if (!BioMiniSlim2.this.bThreadFlag) {
                           break;
                        }
                     }
                  } catch (NullPointerException var27) {
                     BioMiniSlim2.this.LogE("mUsbHandler missing");
                     break;
                  }
               }

               this.mFrameSyncTimer.stopAsync();
               BioMiniSlim2.this.LogD(" -- UsbBulkLoopDebug offset test finished... -- ");

               for(i = 0; i < timesSuccessful2.length; ++i) {
                  BioMiniSlim2.this.LogD("" + offsetTries[i] + " Successful: " + timesSuccessful2[i]);
               }
            }

            BioMiniSlim2.this.bThreadFlag = false;
            BioMiniSlim2.this.mIsUsbThreadRunning = false;
            BioMiniSlim2.this.CaptureFrameStop();
         }
      }
   }

   private class UsbBulkLoopShow implements Runnable {
      BioMiniSlim2.UsbBulkLoopDebug mParent;

      UsbBulkLoopShow(BioMiniSlim2.UsbBulkLoopDebug parent) {
         this.mParent = parent;
      }

      public void run() {
         BioMiniSlim2.this.LogD(" -- UsbBulkLoopShow started... -- ");
         if (BioMiniSlim2.this.mUsbHandler != null) {
            BioMiniSlim2.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniSlim2.this.getRawWidth();
            int nRawHeight = BioMiniSlim2.this.getRawHeight();
            int nBytesRaw = 524288;
            int var10000 = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniSlim2.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int cntBulkErrors = 0;
            int MAX_BULK_ERRORS = true;
            BioMiniSlim2.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(550);
            }

            BioMiniBase.RawImageItem rawX = null;

            while(BioMiniSlim2.this.bThreadFlag) {
               BioMiniSlim2.this.LogD("UsbBulkLoopShow : in loop");
               int MAX_TRIES = true;
               int cntBulkRead = 0;
               int offsetToCurrent = 0;
               rawX = (BioMiniBase.RawImageItem)this.mParent.mImageQueueAny.poll();
               if (rawX == null) {
                  do {
                     SystemClock.sleep(10L);
                     rawX = (BioMiniBase.RawImageItem)this.mParent.mImageQueueAny.poll();
                     ++cntBulkRead;
                  } while(rawX == null && cntBulkRead < 50);
               } else {
                  for(BioMiniBase.RawImageItem tmpA = (BioMiniBase.RawImageItem)this.mParent.mImageQueueAny.poll(); tmpA != null; ++offsetToCurrent) {
                     rawX = tmpA;
                  }
               }

               if (rawX == null) {
                  ++cntBulkErrors;
                  if (cntBulkErrors > 4) {
                     BioMiniSlim2.this.LogE("Bulk Transfer is unstable. Canceling capture process...");
                     break;
                  }
               } else {
                  cntBulkErrors = 0;
                  BioMiniSlim2.this.LogD(" -- UsbBulkLoopShow : Draw callback... -- ");
                  BioMiniSlim2.this.onCapture(BioMiniSlim2.this.mCaptureResponder, rawX.imageData, BioMiniSlim2.this.getRawWidth(), BioMiniSlim2.this.getRawHeight(), false);
               }
            }

            BioMiniSlim2.this.LogD(" -- UsbBulkLoopShow finished... -- ");
         }
      }
   }

   private class UsbBulkLoopRev implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniSlim2.StartCapturingLoop mParentProcess = null;
      Queue<BioMiniBase.MDRImagePair> mImageQueueA = new LinkedList();
      Queue<BioMiniBase.MDRImagePair> mImageQueueN = new LinkedList();
      Queue<BioMiniBase.MDRExposurePair> mExposureQueue = new LinkedList();
      byte fillExtra = -1;
      boolean bTouchState = false;

      UsbBulkLoopRev(ABioMiniDevice pMyp) {
         this.mParentClass = pMyp;
      }

      UsbBulkLoopRev(BioMiniSlim2.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniSlim2.this.LogD(" -- UsbBulkLoop started... -- ");
         this.mImageQueueA.clear();
         this.mImageQueueN.clear();
         BioMiniSlim2.this.mCapturedQueue.clear();
         this.mExposureQueue.clear();
         if (BioMiniSlim2.this.mUsbHandler != null) {
            BioMiniSlim2.this.mIsUsbThreadRunning = true;
            (new Thread(BioMiniSlim2.this.new UsbBulkLoopCalc(this))).start();
            int nBulkWidth = BioMiniSlim2.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int nTouchPrevState = 0;
            int nTouchCurrState = false;
            int nTouchTrigger = false;
            boolean updateAlways = false;
            int MAX_BULK_ERRORS = true;
            int cntBulkErrorsx = 0;
            BioMiniSlim2.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(550);
            }

            updateAlways = false;
            BioMiniBase.MDRExposurePair expPrev = BioMiniSlim2.this.new MDRExposurePair(BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_AEH);
            BioMiniSlim2.this.SetIntegrationTime(BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_NHGH, BioMiniSlim2.this.m_AEH, BioMiniSlim2.this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
            int cntLoop = 0;

            while(BioMiniSlim2.this.bThreadFlag) {
               ++cntLoop;

               try {
                  BioMiniSlim2.this.LogProcessStart("ReadA");
                  boolean re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     BioMiniSlim2.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                     break;
                  }

                  int nTop = BioMiniSlim2.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniSlim2.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                     public boolean beforeRead() {
                        return BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(226), BioMiniSlim2.this.m_TouchBuffer, 6);
                     }

                     public void firstBulkReceived(long delay) {
                     }

                     public boolean afterRead() {
                        return BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim2.this.LogProcessEnd("ReadA");
                  BioMiniSlim2.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (!re) {
                     ++cntBulkErrorsx;
                     if (cntBulkErrorsx > 10) {
                        BioMiniSlim2.this.LogE("Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }

                     this.mImageQueueA.add(BioMiniSlim2.this.new MDRImagePair(nTopNext, BioMiniSlim2.this.m_pFullBufferA[nTopNext], expPrev.ExposureA));
                     BioMiniSlim2.this.LogE("UsbBulkLoopRev read (A) error");
                  } else {
                     int cntBulkErrors = 0;
                     int nTouchCurrStatex = BioMiniSlim2.this.m_TouchBuffer[3] & 1;
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
                     BioMiniSlim2.this.LogD("UsbBulkLoopRev *D* image queue (A) enqueue : (" + nTopNext + ", " + expPrev.ExposureA + ")");
                     this.mImageQueueA.add(BioMiniSlim2.this.new MDRImagePair(nTopNext, BioMiniSlim2.this.m_pFullBufferA[nTopNext], expPrev.ExposureA));
                     BioMiniSlim2.this.ImageLogD(String.format(Locale.ENGLISH, "%03d_A[exp_%s].png", nTopNext, expPrev.ExposureA), BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkWidth, nBulkHeight);
                     if (!BioMiniSlim2.this.bThreadFlag) {
                        break;
                     }

                     BioMiniSlim2.this.m_nLTop++;
                     if (BioMiniSlim2.this.m_nLTop == 5) {
                        BioMiniSlim2.this.m_bLTopIter = true;
                        BioMiniSlim2.this.m_nLTop = 0;
                     }

                     int MAX_TRIES = true;
                     int cntPoll = 0;
                     BioMiniBase.MDRExposurePair exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll();
                     BioMiniSlim2.this.LogD("mExposureQueue's size " + this.mExposureQueue.size());

                     while(exp == null && cntPoll < 50) {
                        SystemClock.sleep(10L);
                        exp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll();
                        ++cntPoll;
                     }

                     for(BioMiniBase.MDRExposurePair expTmp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll(); expTmp != null; expTmp = (BioMiniBase.MDRExposurePair)this.mExposureQueue.poll()) {
                        exp = expTmp;
                     }

                     if (exp == null || exp.ExposureA <= 0) {
                        if (exp == null) {
                           BioMiniSlim2.this.LogV("UsbBulkLoopRev error: could not get calculation result from UsbBulkLoopCalc @" + cntLoop);
                        }

                        BioMiniSlim2.this.LogD(String.format(Locale.ENGLISH, "UsbBulkLoopRev exp(%s) , exp.ExposureA(%s)", exp == null ? "null" : exp, exp == null ? "null" : exp.ExposureA + ""));
                        break;
                     }

                     re = BioMiniSlim2.this.SetIntegrationTime(exp.ExposureN, BioMiniSlim2.this.m_NHGH, exp.ExposureA, BioMiniSlim2.this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
                     BioMiniSlim2.this.LogD("UsbBulkLoopRev *D* Setting Exposure exp.ExposureN: " + exp.ExposureN + " exp.ExposureA: " + exp.ExposureA);
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopRev : Command error");
                        break;
                     }

                     BioMiniSlim2.this.LogProcessStart("ReadN");
                     re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                     if (!re) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopRev : mUsbHandler initRead error");
                        break;
                     }

                     BioMiniSlim2.this.LogD("Before read m_pFullBufferN[nTopNext]");
                     this.fillExtra = -1;
                     re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                        public boolean beforeRead() {
                           return BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                        }

                        public void firstBulkReceived(long delay) {
                        }

                        public boolean afterRead() {
                           return BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                        }
                     });
                     BioMiniSlim2.this.LogProcessEnd("ReadN");
                     BioMiniSlim2.this.LogD("After read m_pFullBufferN[nTopNext]");
                     if (!re) {
                        cntBulkErrorsx = cntBulkErrors + 1;
                        if (cntBulkErrorsx > 10) {
                           BioMiniSlim2.this.LogE("Bulk Transfer is unstable. Canceling capture process...");
                           break;
                        }

                        this.mImageQueueN.add(BioMiniSlim2.this.new MDRImagePair(nTopNext, BioMiniSlim2.this.m_pFullBufferN[nTopNext], expPrev.ExposureN));
                        BioMiniSlim2.this.LogE("UsbBulkLoopRev read (N) error");
                     } else {
                        cntBulkErrorsx = 0;
                        SystemClock.sleep(5L);
                        BioMiniSlim2.this.LogD("UsbBulkLoopRev *D* image queue (N) enqueue : (" + nTopNext + ")");
                        BioMiniSlim2.this.ImageLogD(String.format(Locale.ENGLISH, "%03d_N[exp_%s].png", nTopNext, expPrev.ExposureN), BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkWidth, nBulkHeight);
                        this.mImageQueueN.add(BioMiniSlim2.this.new MDRImagePair(nTopNext, BioMiniSlim2.this.m_pFullBufferN[nTopNext], expPrev.ExposureN));
                        if (nTopNext >= 12) {
                           BioMiniSlim2.this.m_nTop = 0;
                           BioMiniSlim2.this.m_bTopIter = true;
                        } else {
                           BioMiniSlim2.this.m_nTop = nTopNext;
                        }

                        expPrev = exp;
                     }
                  }
               } catch (NullPointerException var19) {
                  BioMiniSlim2.this.LogE("mUsbHandler missing");
                  break;
               }
            }

            BioMiniSlim2.this.LogD(" -- UsbBulkLoopRev finished... -- ");
            BioMiniSlim2.this.bThreadFlag = false;
            BioMiniSlim2.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }

   private class UsbBulkLoopCalc implements Runnable {
      BioMiniSlim2.UsbBulkLoopRev mParent;
      private int[] avgArray = new int[4];
      private int avg = 0;
      private int avg_prev = 0;
      private int avg_prev2 = 0;
      private int nblockw;
      private int nblockh;

      UsbBulkLoopCalc(BioMiniSlim2.UsbBulkLoopRev parent) {
         this.mParent = parent;
      }

      public void run() {
         BioMiniSlim2.this.LogD(" -- UsbBulkLoopCalc started... -- ");
         if (BioMiniSlim2.this.mUsbHandler != null) {
            BioMiniSlim2.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniSlim2.this.getRawWidth();
            int nRawHeight = BioMiniSlim2.this.getRawHeight();
            int nBytesRaw = 524288;
            int nBytesImage = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniSlim2.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 60, 30};
            int[] pFingerOnThS = new int[]{4, 4, 3, 3, 3, 2, 2, 2};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniSlim2.this.m_NHEH;
            int fingerOnPlus = true;
            boolean updateAlways = false;
            int cntBulkErrorsx = 0;
            int MAX_BULK_ERRORS = true;
            BioMiniSlim2.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlusx = pFingerOnThS[BioMiniSlim2.this.m_nSensitivity];
            updateAlways = false;
            byte[] cdata = new byte[64];
            byte[] frameA = new byte[523289];
            byte[] frameN = new byte[523289];
            BioMiniBase.MDRImagePair mdrA = null;
            BioMiniBase.MDRImagePair mdrN = null;
            boolean toggleHigh = true;
            int judge_count = pCountS[BioMiniSlim2.this.m_nSensitivity];

            while(BioMiniSlim2.this.bThreadFlag) {
               BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* --------------->");

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
                     if (cntBulkErrorsx > 4) {
                        BioMiniSlim2.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                        break;
                     }
                  } else {
                     int cntBulkErrors = 0;
                     BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* mdrA: " + mdrA.Exposure + ", m_AEH = " + BioMiniSlim2.this.m_AEH);
                     BioMiniSlim2.this.LogProcessStart("AdjustRaw");
                     System.arraycopy(mdrA.Image, 0, frameA, 0, frameA.length);
                     util.InvertClear(frameA, nRawWidth, nRawHeight, 90, 60);
                     BioMiniSlim2.this.LogProcessEnd("AdjustRaw");
                     if (!BioMiniSlim2.this.bThreadFlag) {
                        break;
                     }

                     System.arraycopy(mdrA.Image, 0, BioMiniSlim2.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                     BioMiniSlim2.this.m_nLTop++;
                     if (BioMiniSlim2.this.m_nLTop == 5) {
                        BioMiniSlim2.this.m_bLTopIter = true;
                        BioMiniSlim2.this.m_nLTop = 0;
                     }

                     int nexp = false;
                     if (mdrPrevA != null && mdrN != null && nFingerCount >= judge_count) {
                        BioMiniSlim2.this.LogProcessStart("Exposure");

                        int nexpx;
                        try {
                           nexpx = BioMiniJni.GetOptimalExposureValue(mdrA.Image, mdrN.Image, mdrPrevA.Image, mdrN.Exposure, mdrA.Exposure, mdrPrevA.Exposure, BioMiniSlim2.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniSlim2.this.g_bExtraDry);
                        } catch (Exception var34) {
                           BioMiniSlim2.this.LogE(var34.toString());
                           break;
                        }

                        BioMiniSlim2.this.LogProcessEnd("Exposure");
                        BioMiniSlim2.this.m_AEH = nexpx;
                        BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* GetOptimalExposureValue : " + BioMiniSlim2.this.m_AEH + " from (" + mdrPrevA.Exposure + ", " + mdrA.Exposure + ")");
                     } else {
                        mdrA.Image[nBytesImage + 8] = 0;
                        if (mdrN != null) {
                           mdrN.Image[nBytesImage + 8] = 0;
                        }
                     }

                     if (nFingerCount <= judge_count) {
                        int low_expo = 25;
                        int high_expo = 999;
                        int low_percentage = 60;
                        int high_percentage = 200;
                        if (toggleHigh) {
                           BioMiniSlim2.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniSlim2.this.m_NHEH + 50) / 100);
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc Max!!!!!!!!!! [" + BioMiniSlim2.this.m_AEH + "], fingerCount [" + nFingerCount + "]");
                        } else {
                           BioMiniSlim2.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniSlim2.this.m_NHEH + 50) / 100);
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc Min!!!!!!!!!! [" + BioMiniSlim2.this.m_AEH + "], fingerCount [" + nFingerCount + "]");
                        }

                        toggleHigh = !toggleHigh;
                     }

                     this.mParent.mExposureQueue.add(BioMiniSlim2.this.new MDRExposurePair(BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_AEH));
                     BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* Exposure enqueue : " + BioMiniSlim2.this.m_NHEH + ", " + BioMiniSlim2.this.m_AEH);
                     cntBulkRead = 0;

                     for(mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll(); mdrN == null && cntBulkRead < 50; ++cntBulkRead) {
                        SystemClock.sleep(10L);
                        mdrN = (BioMiniBase.MDRImagePair)this.mParent.mImageQueueN.poll();
                     }

                     if (mdrN == null) {
                        cntBulkErrorsx = cntBulkErrors + 1;
                        if (cntBulkErrorsx > 4) {
                           BioMiniSlim2.this.LogE("UsbBulkLoopCalc Bulk Transfer is unstable. Canceling capture process...");
                           break;
                        }
                     } else {
                        cntBulkErrorsx = 0;
                        BioMiniSlim2.this.LogProcessStart("AdjustRaw");
                        System.arraycopy(mdrN.Image, 0, frameN, 0, frameN.length);
                        util.InvertClear(frameN, nRawWidth, nRawHeight, 90, 60);
                        BioMiniSlim2.this.LogProcessEnd("AdjustRaw");
                        int judge_value = false;
                        this.nblockw = 32;
                        this.nblockh = 64;
                        int judge_valuex = pVarS[BioMiniSlim2.this.m_nSensitivity];
                        if (mdrPrevA != null && mdrPrevA.Exposure > 0) {
                           BioMiniSlim2.this.LogProcessStart("CalcTouch");
                           int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                           if (BioMiniSlim2.this.m_bExtTrigger == 1) {
                              BioMiniSlim2.this.LogD("UsbBulkLoopCalc : touch state( " + this.mParent.bTouchState + " )");
                              bFingerOn1 = bFingerOn1 > 0 && this.mParent.bTouchState ? 1 : 0;
                              bFingerOn2 = bFingerOn2 > 0 && this.mParent.bTouchState ? 1 : 0;
                           }

                           BioMiniJni.GetAvg(this.avgArray);
                           this.avg = this.avgArray[0];
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                           if (bFingerOn1 > 0 && bFingerOn2 > 0) {
                              if (nFingerCount <= judge_count && this.avg == this.avg_prev2) {
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
                           BioMiniSlim2.this.LogProcessEnd("CalcTouch");
                        }

                        if (bFingerOn) {
                           if (nFingerCount == 0) {
                              BioMiniSlim2.this.LogPublicProcessStart("AutoCapture");
                           }

                           ++nFingerCount;
                        } else {
                           nFingerCount = 0;
                        }

                        BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ")");
                        if (!BioMiniSlim2.this.bThreadFlag) {
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                           break;
                        }

                        if (nFingerCount > judge_count + fingerOnPlusx) {
                           mdrN.Image[nBytesImage + 8] = 1;
                           mdrA.Image[nBytesImage + 8] = 1;
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Tagging finger (1)");
                           if (BioMiniSlim2.this.m_nCaptureMode == 1) {
                              BioMiniSlim2.this.mUsbHandler.controlTx(BioMiniSlim2.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                              BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Capture successful at mode 1");
                              SystemClock.sleep(BioMiniSlim2.this.getSafeDelay());
                              if (!BioMiniSlim2.this.bThreadFlag) {
                                 BioMiniSlim2.this.LogD("UsbBulkLoopCalc breaking with stop signal");
                              } else {
                                 BioMiniSlim2.this.LogD("UsbBulkLoopCalc *D* enqueue captured image exp=" + mdrA.Exposure);
                                 BioMiniSlim2.this.mCapturedQueue.add(BioMiniSlim2.this.new MDRCapturedPair(mdrA, mdrN));
                                 if (this.mParent.mParentClass != null) {
                                    this.mParent.mParentClass.captured();
                                 }
                              }
                              break;
                           }

                           if (BioMiniSlim2.this.m_nCaptureMode == 2) {
                              BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Capture successful at mode 2");
                           } else {
                              BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Capture successful at mode unknown");
                           }
                        } else {
                           BioMiniSlim2.this.LogD("UsbBulkLoopCalc : Tagging finger (0)");
                           mdrN.Image[nBytesImage + 8] = 0;
                           mdrA.Image[nBytesImage + 8] = 0;
                        }

                        BioMiniSlim2.this.LogD(" -- UsbBulkLoopCalc : Notifying... -- ");
                        BioMiniSlim2.this.mCapturedQueue.add(BioMiniSlim2.this.new MDRCapturedPair(mdrA, mdrN));
                        if (this.mParent.mParentClass != null) {
                           this.mParent.mParentClass.captured();
                        }

                        if (this.mParent.mParentProcess != null) {
                           this.mParent.mParentProcess.captured();
                        }
                     }
                  }
               } catch (NullPointerException var35) {
                  BioMiniSlim2.this.LogI("UsbBulkLoopCalc mUsbHandler missing");
                  break;
               }
            }

            this.mParent.mExposureQueue.add(BioMiniSlim2.this.new MDRExposurePair(-1, -1));
            BioMiniSlim2.this.LogD(" -- UsbBulkLoopCalc finished... -- ");
         }
      }
   }

   private class UsbBulkLoop implements Runnable {
      ABioMiniDevice mParentClass = null;
      BioMiniSlim2.StartCapturingLoop mParentProcess = null;
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

      UsbBulkLoop(BioMiniSlim2.StartCapturingLoop pMyp) {
         this.mParentProcess = pMyp;
      }

      public void run() {
         BioMiniSlim2.this.LogD(" -- UsbBulkLoop started... -- ");
         if (BioMiniSlim2.this.mUsbHandler != null) {
            BioMiniSlim2.this.mIsUsbThreadRunning = true;
            int nRawWidth = BioMiniSlim2.this.getRawWidth();
            int nRawHeight = BioMiniSlim2.this.getRawHeight();
            int nBytesRaw = 524288;
            int nBytesImage = nRawWidth * nRawHeight;
            int nBulkWidth = BioMiniSlim2.this.getBulkWidth();
            int nBulkHeight = BioMiniSlim2.this.getBulkHeight();
            int nBulkLength = nBulkWidth * nBulkHeight;
            int[] pCountS = new int[]{4, 3, 3, 2, 2, 2, 1, 1};
            int[] pVarS = new int[]{200, 180, 160, 140, 120, 100, 80, 60};
            int[] pFingerOnThS = new int[]{4, 4, 3, 3, 3, 2, 2, 2};
            boolean bFingerOn = false;
            int nFingerCount = 0;
            int prev_exp = BioMiniSlim2.this.m_NHEH;
            int sleepPostx = false;
            int sleepVal1 = BioMiniSlim2.this.mSleepVal;
            int sleepVal2 = BioMiniSlim2.this.mSleepVal;
            int fingerOnPlusx = true;
            boolean updateAlways = false;
            int nSkipPackets = true;
            int dividerFromProcessingCost = 6;
            BioMiniSlim2.this.mUsbHandler.setBulkRx(2);
            BioMiniSlim2.this.mUsbHandler.resize(nBulkLength);
            if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate == IBioMiniDevice.FrameRate.SHIGH) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(200);
            } else if (BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.LOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.ELOW && BioMiniSlim2.this.mCurrentCaptureOption.frameRate != IBioMiniDevice.FrameRate.SLOW) {
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(400);
            } else {
               dividerFromProcessingCost = 3;
               BioMiniSlim2.this.mUsbHandler.setBulkTimeout(550);
            }

            int fingerOnPlus = pFingerOnThS[BioMiniSlim2.this.m_nSensitivity];
            updateAlways = false;
            byte[] cdata = new byte[64];
            byte[] frameA = new byte[523289];
            byte[] frameN = new byte[523289];

            while(BioMiniSlim2.this.bThreadFlag) {
               BioMiniSlim2.this.LogD("UsbBulkLoop : in loop");

               try {
                  int sleepPost = 0;
                  BioMiniSlim2.this.mSleepPlus = 15;
                  if (BioMiniSlim2.this.mProcessingCost != 0L) {
                     BioMiniSlim2.this.mSleepVal = sleepVal1 = sleepVal2 = (int)(BioMiniSlim2.this.mProcessingCost / (long)dividerFromProcessingCost);
                  } else {
                     sleepVal2 = 33;
                     sleepVal1 = 33;
                     BioMiniSlim2.this.mSleepVal = 33;
                  }

                  sleepVal1 += BioMiniSlim2.this.mSleepPlus;
                  sleepVal2 += BioMiniSlim2.this.mSleepPlus;
                  BioMiniSlim2.this.LogProcessStart("ReadA");
                  boolean re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniSlim2.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  int nTop = BioMiniSlim2.this.m_nTop;
                  int nTopNext = (nTop + 1) % 12;
                  BioMiniSlim2.this.LogD("Before read m_pFullBufferA[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferA[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                     public boolean beforeRead() {
                        return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(226), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public void firstBulkReceived(long delay) {
                     }

                     public boolean afterRead() {
                        return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim2.this.LogProcessEnd("ReadA");
                  BioMiniSlim2.this.LogD("After read m_pFullBufferA[nTopNext]");
                  if (sleepVal1 > 0) {
                     SystemClock.sleep((long)sleepVal1);
                  }

                  BioMiniSlim2.this.LogProcessStart("AdjustRaw");
                  System.arraycopy(BioMiniSlim2.this.m_pFullBufferA[nTopNext], 0, frameA, 0, frameA.length);
                  util.InvertClear(frameA, nRawWidth, nRawHeight, 90, 60);
                  BioMiniSlim2.this.LogProcessEnd("AdjustRaw");
                  if (!re) {
                     Log.e(BioMiniSlim2.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  if (!BioMiniSlim2.this.bThreadFlag) {
                     break;
                  }

                  System.arraycopy(BioMiniSlim2.this.m_pFullBufferA[nTopNext], 0, BioMiniSlim2.this.m_ImageRawPrevA, 0, nBytesRaw + 9);
                  BioMiniSlim2.this.m_nLTop++;
                  if (BioMiniSlim2.this.m_nLTop == 5) {
                     BioMiniSlim2.this.m_bLTopIter = true;
                     BioMiniSlim2.this.m_nLTop = 0;
                  }

                  BioMiniSlim2.this.LogProcessStart("Exposure");
                  int nexp = false;
                  int judge_count = pCountS[BioMiniSlim2.this.m_nSensitivity];
                  if ((nTop >= 1 || BioMiniSlim2.this.m_bTopIter) && nFingerCount > judge_count) {
                     int nexpx = BioMiniJni.GetOptimalExposureValue(BioMiniSlim2.this.m_pFullBufferA[nTopNext], BioMiniSlim2.this.m_pFullBufferN[nTop], BioMiniSlim2.this.m_pFullBufferA[nTop], BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_AEH, prev_exp, BioMiniSlim2.this.m_expoScaleFactor, nRawWidth, nRawHeight, nFingerCount, judge_count + 1, BioMiniSlim2.this.g_bExtraDry);
                     prev_exp = BioMiniSlim2.this.m_AEH;
                     BioMiniSlim2.this.m_AEH = nexpx;
                  } else {
                     BioMiniSlim2.this.m_pFullBufferA[nTopNext][nBytesImage + 8] = 0;
                     BioMiniSlim2.this.m_pFullBufferN[nTopNext][nBytesImage + 8] = 0;
                  }

                  if (nFingerCount <= judge_count) {
                     int low_expo = 25;
                     int high_expo = 999;
                     int low_percentage = 60;
                     int high_percentage = 200;
                     if (nTop % 2 == 0) {
                        prev_exp = BioMiniSlim2.this.m_AEH;
                        BioMiniSlim2.this.m_AEH = Math.max(low_expo, (low_percentage * BioMiniSlim2.this.m_NHEH + 50) / 100);
                        BioMiniSlim2.this.LogD("Max!!!!!!!!!! [" + BioMiniSlim2.this.m_AEH + "]");
                     } else {
                        prev_exp = BioMiniSlim2.this.m_AEH;
                        BioMiniSlim2.this.m_AEH = Math.min(high_expo, (high_percentage * BioMiniSlim2.this.m_NHEH + 50) / 100);
                        BioMiniSlim2.this.LogD("Min!!!!!!!!!! [" + BioMiniSlim2.this.m_AEH + "]");
                     }
                  }

                  re = BioMiniSlim2.this.SetIntegrationTime(BioMiniSlim2.this.m_NHEH, BioMiniSlim2.this.m_NHGH, BioMiniSlim2.this.m_AEH, BioMiniSlim2.this.m_AGH, 1024, 32, 1024, 8, 1023, 0);
                  BioMiniSlim2.this.LogProcessEnd("Exposure");
                  BioMiniSlim2.this.LogD("UsbBulkLoop : Setting Exposure m_NHEH: " + BioMiniSlim2.this.m_NHEH + " m_AEH: " + BioMiniSlim2.this.m_AEH);
                  if (!re) {
                     break;
                  }

                  BioMiniSlim2.this.LogProcessStart("ReadN");
                  re = BioMiniSlim2.this.mUsbHandler.initRead(nBulkLength, 0, updateAlways);
                  if (!re) {
                     Log.e(BioMiniSlim2.this.TAG, "UsbBulkLoop : mUsbHandler initRead error");
                     break;
                  }

                  BioMiniSlim2.this.LogD("Before read m_pFullBufferN[nTopNext]");
                  this.fillExtra = -1;
                  re = BioMiniSlim2.this.mUsbHandler.read(BioMiniSlim2.this.m_pFullBufferN[nTopNext], nBulkLength, this.fillExtra, new IUsbHandler.IReadProcessorAdv() {
                     public boolean beforeRead() {
                        return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlRx(BioMiniSlim2.this.GetCmd(225), new byte[]{0, 0, 0, 0, 0, 0}, 6);
                     }

                     public void firstBulkReceived(long delay) {
                     }

                     public boolean afterRead() {
                        return BioMiniSlim2.this.bThreadFlag && BioMiniSlim2.this.mUsbHandler.controlTx(239, new byte[]{0}, 1);
                     }
                  });
                  BioMiniSlim2.this.LogProcessEnd("ReadN");
                  BioMiniSlim2.this.LogD("After read m_pFullBufferN[nTopNext]");
                  if (sleepVal2 > 0) {
                     SystemClock.sleep((long)sleepVal2);
                  }

                  BioMiniSlim2.this.LogProcessStart("AdjustRaw");
                  System.arraycopy(BioMiniSlim2.this.m_pFullBufferN[nTopNext], 0, frameN, 0, frameN.length);
                  util.InvertClear(frameN, nRawWidth, nRawHeight, 90, 60);
                  BioMiniSlim2.this.LogProcessEnd("AdjustRaw");
                  if (!re) {
                     Log.e(BioMiniSlim2.this.TAG, "UsbBulkLoop : mUsbHandler read error");
                     break;
                  }

                  BioMiniSlim2.this.LogProcessStart("CalcTouch");
                  int judge_value = false;
                  this.nblockw = 32;
                  this.nblockh = 64;
                  int judge_valuex = pVarS[BioMiniSlim2.this.m_nSensitivity];
                  if (nTop >= 2 || BioMiniSlim2.this.m_bTopIter) {
                     int bFingerOn1 = BioMiniJni.DetectFingerprintArea(frameN, frameN, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     int bFingerOn2 = BioMiniJni.DetectFingerprintArea(frameA, frameA, this.nblockw, this.nblockh, nRawWidth, nRawHeight, judge_valuex);
                     BioMiniJni.GetAvg(this.avgArray);
                     this.avg = this.avgArray[0];
                     BioMiniSlim2.this.LogD("UsbBulkLoop : Avg = " + this.avg + " fingeron(" + bFingerOn1 + " ," + bFingerOn2 + ")");
                     if (bFingerOn1 > 0 && bFingerOn2 > 0) {
                        if (nFingerCount <= judge_count && this.avg == this.avg_prev2) {
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

                  BioMiniSlim2.this.LogD("UsbBulkLoop : nFingerCount(" + nFingerCount + "), bFingerOn(" + bFingerOn + ") @" + nTopNext + ")");
                  if (nFingerCount > judge_count + fingerOnPlus) {
                     BioMiniSlim2.this.m_pFullBufferN[nTopNext][nBytesImage + 8] = 1;
                     BioMiniSlim2.this.m_pFullBufferA[nTopNext][nBytesImage + 8] = 1;
                     BioMiniSlim2.this.m_nTop = nTopNext;
                     BioMiniSlim2.this.LogD("UsbBulkLoop : Tagging finger (1) @" + nTopNext);
                     if (BioMiniSlim2.this.m_nCaptureMode == 1) {
                        BioMiniSlim2.this.mUsbHandler.controlTx(BioMiniSlim2.this.GetCmd(194), new byte[]{0, 0, 0, 0}, 1);
                        BioMiniSlim2.this.LogD("UsbBulkLoop : Capture successful at mode 1");
                        SystemClock.sleep(BioMiniSlim2.this.getSafeDelay());
                        this.mParentClass.captured();
                        break;
                     }

                     if (BioMiniSlim2.this.m_nCaptureMode == 2) {
                        BioMiniSlim2.this.LogD("UsbBulkLoop : Capture successful at mode 2");
                     } else {
                        BioMiniSlim2.this.LogD("UsbBulkLoop : Capture successful at mode unknown");
                     }
                  } else {
                     BioMiniSlim2.this.LogD("UsbBulkLoop : Tagging finger (0) @" + nTopNext);
                     BioMiniSlim2.this.m_pFullBufferN[nTopNext][nBytesImage + 8] = 0;
                     BioMiniSlim2.this.m_pFullBufferA[nTopNext][nBytesImage + 8] = 0;
                     BioMiniSlim2.this.m_nTop = nTopNext;
                  }

                  BioMiniSlim2.this.LogProcessEnd("CalcTouch");
                  if (nTop == 11) {
                     BioMiniSlim2.this.m_bTopIter = true;
                  }

                  BioMiniSlim2.this.m_nTop = nTopNext;
                  BioMiniSlim2.this.LogD(" -- UsbBulkLoop : Notifying... -- ");
                  if (this.mParentClass != null) {
                     this.mParentClass.captured();
                  }

                  if (this.mParentProcess != null) {
                     this.mParentProcess.captured();
                  }

                  if (sleepPost > 0) {
                     SystemClock.sleep((long)sleepPost);
                  }
               } catch (NullPointerException var33) {
                  BioMiniSlim2.this.LogE("mUsbHandler missing");
                  break;
               }
            }

            BioMiniSlim2.this.LogD(" -- UsbBulkLoop finished... -- ");
            BioMiniSlim2.this.bThreadFlag = false;
            BioMiniSlim2.this.mIsUsbThreadRunning = false;
            this.mParentClass = null;
            this.mParentProcess = null;
         }
      }
   }

   private class HistogramContainer {
      int[] _hist = new int[256];
      int _max_count;

      HistogramContainer() {
         Arrays.fill(this._hist, 0);
         this._max_count = 0;
      }

      public void add(byte v) {
         int vv = 255 & v;
         int var10002 = this._hist[vv]++;
         this._max_count = Math.max(this._hist[vv], this._max_count);
      }

      public int[] hist() {
         return this._hist;
      }

      public float[] normHist(float norm) {
         float[] re = new float[256];
         if (this._max_count != 0) {
            for(int i = 0; i < 256; ++i) {
               re[i] = (float)this._hist[i] * norm / (float)this._max_count;
            }
         } else {
            Arrays.fill(re, 0.0F);
         }

         return re;
      }
   }
}
