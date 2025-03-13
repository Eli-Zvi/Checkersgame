package checkersgame.server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
/**
 * <p>This class consists of password hashing, and verification methods</p>
 */
public class PasswordUtils {
    private static final String HASHING_ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536; //65536 iterations of HMAC SHA-256
    private static final int KEY_LENGTH = 60 * 8; //Key of length 60 bytes
    private static final int SALT_SIZE = 16; // Salt of size 16 bytes

    /**
     * Generates a base64 string representation of a generated Salt
     * @return the generated base64 string salt
     */
    public static String generateSalt(){
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * <p>Salts and hashes password</p>
     * @param password password to be salted and hashed
     * @param salt salt to use for hashing
     * @return a salted and hashed password
     */
    public static byte[] hashPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //derive key from password and return it
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASHING_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * <p>Hashes the given password and compares with the given hashed password</p>
     * @param password the plain password used for verification
     * @param hashedPassword the salted and hashed password
     * @param salt the salt used for the original password before hashing
     * @return true if the password hashes are equal false otherwise
     */
    public static boolean verifyPassword(String password, byte[] hashedPassword, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] userPassHash = hashPassword(password, salt);
        return Arrays.equals(hashedPassword, userPassHash);
    }
}
