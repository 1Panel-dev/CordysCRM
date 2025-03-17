package io.cordys.crm.utils;

import io.cordys.common.util.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileBaseUtils {

    public static byte[] getFileBytes(String filePath) {
        File file = new File(filePath);
        byte[] buffer = new byte[0];
        try (FileInputStream fi = new FileInputStream(file)) {
            buffer = new byte[(int) file.length()];
            int offset = 0;
            int numRead;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
        } catch (Exception ignore) {
        }
        return buffer;
    }

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }

            // 返回 MD5 字符串
            byte[] md5Bytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : md5Bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            // 使用标准日志框架记录异常信息
            LogUtils.error("getFileMD5 error", e);
            return null;
        }
    }

    public static String getFileMD5(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bytes, 0, bytes.length);
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16);
        } catch (Exception e) {
            return null;
        }
    }

}
