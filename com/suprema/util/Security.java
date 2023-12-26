package com.suprema.util;

import android.util.Base64;
import android.util.Log;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Security {
   private static String uniqueData = null;
   private static SecretKey mainKey = null;
   private static String ivKey = "SupremaID";
   private static final int AES_ENCRYPT_128 = 128;
   private static String hashedData = null;
   private static String publicStr = null;
   private static String privateStr = null;

   public Security() {
   }

   public Security(String uData) {
      uniqueData = uData;
   }

   public void generateRSAKey() {
      KeyPair keyPair = null;

      try {
         KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
         keyGen.initialize(2048);
         keyPair = keyGen.generateKeyPair();
      } catch (GeneralSecurityException var4) {
         Log.w("Security", "RSA Key Generation Failed...");
      }

      byte[] bytePubKey = keyPair.getPublic().getEncoded();
      byte[] bytePrivKey = keyPair.getPrivate().getEncoded();
      publicStr = Base64.encodeToString(bytePubKey, 0);
      privateStr = Base64.encodeToString(bytePrivKey, 0);
   }

   public void setPublicStr(String pubKey) {
      publicStr = pubKey;
   }

   public String getPublicStr() {
      return publicStr;
   }

   public void setPrivateStr(String privKey) {
      privateStr = privKey;
   }

   public String getPrivateStr() {
      return privateStr;
   }

   public byte[] RSAEncrypt(byte[] data) {
      try {
         KeyFactory factory = KeyFactory.getInstance("RSA");
         X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.decode(publicStr, 0));
         Key encryptionKey = factory.generatePublic(pubSpec);
         Cipher rsa = Cipher.getInstance("RSA/None/PKCS1Padding");
         rsa.init(1, encryptionKey);
         return rsa.doFinal(data);
      } catch (Exception var6) {
         Log.w("Security", "RSAEncrypt Failed");
         return null;
      }
   }

   public byte[] RSADecrypt(byte[] data) {
      try {
         KeyFactory factory = KeyFactory.getInstance("RSA");
         PKCS8EncodedKeySpec prvSpec = new PKCS8EncodedKeySpec(Base64.decode(privateStr, 0));
         Key decryptionKey = factory.generatePrivate(prvSpec);
         Cipher rsa = Cipher.getInstance("RSA/None/PKCS1Padding");
         rsa.init(2, decryptionKey);
         return rsa.doFinal(data);
      } catch (Exception var6) {
         Log.w("Security", "RSADecrypt Failed");
         return null;
      }
   }

   public boolean generateAESKey() {
      if (uniqueData == null) {
         Log.w("Security", "Data is empty: Terminated Key Generation.");
         return false;
      } else {
         try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(uniqueData.getBytes());
            String encryptedString = Base64.encodeToString(messageDigest.digest(), 2);
            hashedData = encryptedString;
            byte[] tmp = encryptedString.getBytes();
            tmp = Arrays.copyOf(tmp, 16);
            mainKey = new SecretKeySpec(tmp, "AES");
            return true;
         } catch (Exception var5) {
            Log.w("Exception", "Security.java: generateKey(): No such algorithm exception");
            return false;
         }
      }
   }

   public String getHashedData() {
      return hashedData;
   }

   public byte[] encryptData(byte[] iData) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         byte[] iv = Arrays.copyOf(ivKey.getBytes(), 16);
         IvParameterSpec paramSpec = new IvParameterSpec(iv);
         cipher.init(1, mainKey, paramSpec);
         byte[] cipherText = cipher.doFinal(iData);
         return cipherText;
      } catch (Exception var6) {
         Log.w("Exception", "Security.java: RSACryptedKey(): No such algorithm exception");
         return null;
      }
   }

   public byte[] decryptData(byte[] iData) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         byte[] iv = Arrays.copyOf(ivKey.getBytes(), 16);
         IvParameterSpec paramSpec = new IvParameterSpec(iv);
         cipher.init(2, mainKey, paramSpec);
         byte[] plainText = cipher.doFinal(iData);
         return plainText;
      } catch (Exception var6) {
         Log.w("Exception", "Security.java: RSACryptedKey(): No such algorithm exception");
         return null;
      }
   }
}
