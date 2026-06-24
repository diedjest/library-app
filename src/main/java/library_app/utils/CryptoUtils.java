package library_app.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {

    // Метод шифрования строки алгоритмом MD5
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : array) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Критическая ошибка: алгоритм хэширования не найден.", e);
        }
    }
}