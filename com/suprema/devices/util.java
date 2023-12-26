package com.suprema.devices;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class util {
   public static Bitmap toBitmap(byte[] image, int width, int height) {
      Bitmap bm = Bitmap.createBitmap(width, height, Config.ARGB_8888);
      byte[] Bits = new byte[width * height * 4];

      for(int i = 0; i < width * height; ++i) {
         Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = image[i];
         Bits[i * 4 + 3] = -1;
      }

      bm.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
      return bm;
   }

   public static void saveImage(String filename, byte[] image, int w, int h) {
      FileOutputStream out = null;

      try {
         File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
         Log.v("saveImage", f.getAbsolutePath());
         out = new FileOutputStream(f);
         toBitmap(image, w, h).compress(CompressFormat.PNG, 100, out);
      } catch (Exception var14) {
         var14.printStackTrace();
      } finally {
         try {
            if (out != null) {
               out.close();
            }
         } catch (IOException var13) {
            var13.printStackTrace();
         }

      }

   }

   static void InvertClear(byte[] img, int width, int height, int cropx, int cropy) {
      int i = 0;

      for(int y = 0; y < height; ++y) {
         if (y <= cropy && y >= height - cropy) {
            for(int x = 0; x < width; ++i) {
               if (x <= cropx && x >= width - cropx) {
                  img[i] = -1;
               } else {
                  img[i] = (byte)(~img[i]);
               }

               ++x;
            }
         } else {
            i += width;
         }
      }

   }
}
