package net.gierach.githubsummary.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityHelper {

    private static final String TAG = "SecurityHelper";

    private static SecurityHelper sInstance = null;

    public static synchronized SecurityHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SecurityHelper(context);
        }

        return sInstance;
    }

    private static final int KEY_SIZE = 128;
    private static final Charset CHAR_SET = Charset.forName("UTF-8");


    private final Context mContext;
    private final SecretKeySpec mKey;

    private SecurityHelper(Context context) {
        mContext = context.getApplicationContext();

        SharedPreferences preferences = context.getSharedPreferences("SecurityHelper", Context.MODE_PRIVATE);

        mKey = loadKey(preferences);
    }

    private SecretKeySpec loadKey(SharedPreferences preferences) {
        String base64 = preferences.getString("PRIVACY_MATTERS", null);
        final byte[] binary;
        if (!TextUtils.isEmpty(base64)) {
            binary = Base64.decode(base64, Base64.NO_WRAP | Base64.URL_SAFE);
        } else {
            binary = new byte[KEY_SIZE / 8];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(binary);
            base64 = Base64.encodeToString(binary, Base64.NO_WRAP | Base64.URL_SAFE);
            preferences.edit().putString("PRIVACY_MATTERS", base64).apply();
        }

        return new SecretKeySpec(binary, "AES");
    }

    public byte[] encryptPassword(String username, String password) {
        if (!TextUtils.isEmpty(password)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("md5");
                byte[] ivBytes = digest.digest(username.getBytes(CHAR_SET));

                IvParameterSpec iv = new IvParameterSpec(ivBytes);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, mKey, iv);

                return cipher.doFinal(password.getBytes(CHAR_SET));
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
                Log.e(TAG, "Error encrypting password: " + e, e);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                Log.e(TAG, "Error encrypting password: " + e, e);
            }
        }

        return null;
    }

    public String decryptPassword(String username, byte[] passwordEnc) {
        if (passwordEnc != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("md5");
                byte[] ivBytes = digest.digest(username.getBytes(CHAR_SET));

                IvParameterSpec iv = new IvParameterSpec(ivBytes);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, mKey, iv);

                return new String(cipher.doFinal(passwordEnc), CHAR_SET);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
                Log.e(TAG, "Error decrypting password: " + e, e);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                Log.e(TAG, "Error decrypting password: " + e, e);
            }
        }

        return null;
    }
}
