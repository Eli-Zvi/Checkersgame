package checkersgame.common;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
/**
 * <p>This class is used to send the user's password in an encrypted form, providing confidentiality,
 * authentication, and integrity</p>
 *
 * <ol>
 *     <li>The class is used to encrypt the password with an AES key by one end</li>
 *     <li>The class is used to decrypt the encrypted password with the same AES key by the other end</li>
 * </ol>
 */
public class Password implements Serializable{

    private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding"; //provides authentication and integrity
    private static final int GCM_TAG_LENGTH = 128; //common tag length
    private static final int IV_LENGTH = 12;
    private final String encryptedPassword;
    private final byte[] iv;

    /**
     * Initializes a new instance of Password, encrypting the given password with the given AES key
     * @param password a password to be encrypted
     * @param key an AES key to encrypt with
     * @throws GeneralSecurityException if an error occurs during encryption
     */
    public Password(String password, SecretKey key) throws GeneralSecurityException {
        this.iv = generateIV(); //generate a new initialization vector
        this.encryptedPassword = encrypt(password, key);
    }

    /**
     * Attempts to decrypt the stored encrypted password with the given AES key
     * @param key the AES key to use for decryption
     * @return the password after decryption
     * @throws GeneralSecurityException if an error occurs during decryption, for example an invalid AES key
     */
    public String decrypt(SecretKey key) throws GeneralSecurityException{
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * Encrypts the given password with the given AES key
     * @param password password to be encrypted
     * @param key AES key to encrypt with
     * @return the base64 string representation of the encrypted password
     * @throws GeneralSecurityException if an error occurs during the encryption process
     */
    private String encrypt(String password, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] cipherText = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Generates a random IV of size IV_LENGTH
     * @return a byte array representation of the IV
     */
    private byte[] generateIV(){
        SecureRandom sr = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        sr.nextBytes(iv);
        return iv;
    }
}
