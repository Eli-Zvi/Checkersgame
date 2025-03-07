package checkersgame.common;


import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
//TODO ADD DOCUMENTATION
public class Password implements Serializable{
    private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding"; //provides authentication and integrity
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private final String encryptedPassword;
    private final byte[] iv;

    public Password(String password, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        this.iv = generateIV();
        this.encryptedPassword = encrypt(password, key);
    }

    public String decrypt(SecretKey key) throws InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(plainText, StandardCharsets.UTF_8);
    }

    private byte[] generateIV(){
        SecureRandom sr = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        sr.nextBytes(iv);
        return iv;
    }

    private String encrypt(String password, SecretKey key) throws InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] cipherText = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }
}
