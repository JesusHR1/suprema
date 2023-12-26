package com.suprema.util;

class ProcessingCost {
   public long Cost = 0L;
   public int Count = 0;
   private long LastMillis = 0L;

   ProcessingCost() {
      this.Cost = 0L;
      this.Count = 0;
   }

   ProcessingCost Start(long currentMillis) {
      this.LastMillis = currentMillis;
      return this;
   }

   ProcessingCost End(long currentMillis) {
      this.Cost += currentMillis - this.LastMillis;
      ++this.Count;
      return this;
   }
}
