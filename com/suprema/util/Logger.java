package com.suprema.util;

import android.os.SystemClock;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Logger {
   static Map<String, ProcessingCost> mListCosts = new HashMap();

   public static void LogD(String TAG, Object msg) {
   }

   public static void LogI(String TAG, Object msg) {
   }

   public static void LogW(String TAG, Object msg) {
   }

   public static void LogV(String TAG, Object msg) {
   }

   public static void LogE(String TAG, Object msg) {
      Log.e(String.format("%s%s", TAG, ""), msg.toString());
   }

   public static void StartProcessingLog(String name) {
      if (mListCosts.get(name) == null) {
         mListCosts.put(name, new ProcessingCost());
      }

      mListCosts.put(name, ((ProcessingCost)mListCosts.get(name)).Start(SystemClock.uptimeMillis()));
   }

   public static void EndProcessingLog(String name) {
      if (mListCosts.get(name) != null) {
         mListCosts.put(name, ((ProcessingCost)mListCosts.get(name)).End(SystemClock.uptimeMillis()));
      }
   }

   public static void ClearProcessingLog() {
      mListCosts.clear();
   }

   public static String GetProcessingLog() {
      String log = "";
      Iterator it = mListCosts.entrySet().iterator();

      while(it.hasNext()) {
         Entry pair = (Entry)it.next();
         ProcessingCost val = (ProcessingCost)pair.getValue();
         log = log + pair.getKey() + " = " + (float)val.Cost / (float)val.Count + "(" + val.Count + ")\n";
         it.remove();
      }

      return log;
   }
}
