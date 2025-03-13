package checkersgame.common;

import javax.crypto.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
/**
 * <p>The class consists of AES and RSA key generation, encryption, and decryption methods</p>
 *
 * <p>The class is used for sending an AES key encrypted with an RSA key</p>
 *
 * @author Ilay Zvi
 */
public class KeyUtils {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;

    /**
     * Generates a new RSA keypair
     * @return an RSA keypair
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.genKeyPair();
    }

    /**
     * Generates a new AES key
     * @return an AES key
     */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Encrypts the given AES key with the given RSA public key
     * @param aesKey an AES key to be encrypted
     * @param publicKey an RSA public key to encrypt with
     * @return an encrypted AES key with the given RSA key
     * @throws GeneralSecurityException if there is error during encryption
     */
    public static String rsaEncrypt(SecretKey aesKey, PublicKey publicKey) throws GeneralSecurityException {
        //RSA + OAEP SHA-256 + MGF1 PADDING
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(aesKey.getEncoded()));
    }

    /**
     * Decrypts the encrypted AES key with the RSA private key
     * @param encryptedAESKey encrypted AES key
     * @param privateKey an RSA private key to decrypt with
     * @return the AES key
     * @throws GeneralSecurityException if there is error during decryption
     */
    public static SecretKey rsaDecrypt(String encryptedAESKey, PrivateKey privateKey) throws GeneralSecurityException {
        //RSA + OAEP SHA-256 + MGF1 PADDING
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new javax.crypto.spec.SecretKeySpec(cipher.doFinal(Base64.getDecoder().decode(encryptedAESKey)), "AES");
    }

    /**
     * Serializes the given RSA public key by encoding it into a Base64 string
     *
     * @param publicKey an RSA public key to be encoded
     * @return a Base64-encoded string representation of the RSA public key
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Deserializes the given base64-encoded string representation of the RSA public key
     * @param base64PubKey a Base64-encoded string representation of the RSA public key
     * @return the deserialized RSA public key
     */
    public static PublicKey base64ToPublicKey(String base64PubKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(base64PubKey);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }
}
