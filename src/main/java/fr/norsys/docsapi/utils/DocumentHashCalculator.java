package fr.norsys.docsapi.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class DocumentHashCalculator {

    public static String calculateChecksum(Path filePath) throws NoSuchAlgorithmException, IOException {
        return calculateHash(filePath);
    }

    public static String calculateHash(Path file) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[8192]; // 8KB buffer size
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            while ((count = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        byte[] hash = digest.digest();
        return bytesToHex(hash);
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
