package us.cubk.openajs.api.fv;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public final class FvCrypto {

    private FvCrypto() {
    }

    public static PublicKey serverPublicKey() {
        try {
            BigInteger n = new BigInteger("141601187905926909444644730286409666755495300050721564398199769080746616339148556683219048017120018748016428099359153205651362664001287890722535799466700786136519566714160932079339659299698611914846217596226121056961277268141789905486380539961102053398435632146710731361922836526658451793921073088741138357721");
            BigInteger e = BigInteger.valueOf(65537);
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static byte[] rsaOaepEncrypt(byte[] plain) {
        try {
            String hash = System.getProperty("fv.oaep", "SHA-1");
            MGF1ParameterSpec mgf = switch (hash) {
                case "SHA-1" -> MGF1ParameterSpec.SHA1;
                case "SHA-224" -> MGF1ParameterSpec.SHA224;
                case "SHA-384" -> MGF1ParameterSpec.SHA384;
                case "SHA-512" -> MGF1ParameterSpec.SHA512;
                default -> MGF1ParameterSpec.SHA256;
            };
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec spec = new OAEPParameterSpec(hash, "MGF1", mgf, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey(), spec);
            return cipher.doFinal(plain);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static byte[] aesCtr(byte[] key, byte[] iv, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
