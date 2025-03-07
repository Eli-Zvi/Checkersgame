package checkersgame.server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
//TODO ADD DOCUMENTATION
public class PasswordUtils {
    private static final String HASHING_ALGO = "PBKDF2WithHmacSHA256"; //derives key from a password but also allows us to hash password
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 60 * 8;
    private static final int SALT_SIZE = 16;

    public static String generateSalt(){
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static byte[] hashPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //derive key from password and return it
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASHING_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }

    public static boolean verifyPassword(String password, byte[] hashedPassword, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] userPassHash = hashPassword(password, salt);
        return Arrays.equals(hashedPassword, userPassHash);
    }
}
