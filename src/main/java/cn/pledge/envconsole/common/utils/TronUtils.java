package cn.pledge.envconsole.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import sun.misc.BASE64Decoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class TronUtils {
	static int ADDRESS_SIZE = 21;
	private static byte addressPreFixByte = (byte) 0x41; // 41 + address (byte) 0xa0; //a0 + address

	public static String toHexAddress(String tAddress) {
		return ByteArray.toHexString(decodeFromBase58Check(tAddress));
	}


	private static byte[] decodeFromBase58Check(String addressBase58) {
		if (StringUtils.isEmpty(addressBase58)) {
			return null;
		}
		byte[] address = decode58Check(addressBase58);
		if (!addressValid(address)) {
			return null;
		}
		return address;
	}

	public static byte[] decode58Check(String input) {
		byte[] decodeCheck = Base58.decode(input);
		if (decodeCheck.length <= 4) {
			return null;
		}
		byte[] decodeData = new byte[decodeCheck.length - 4];
		System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
		byte[] hash0 = hash(true, decodeData);
		byte[] hash1 = hash(true, hash0);
		if (hash1[0] == decodeCheck[decodeData.length] && hash1[1] == decodeCheck[decodeData.length + 1]
				&& hash1[2] == decodeCheck[decodeData.length + 2] && hash1[3] == decodeCheck[decodeData.length + 3]) {
			return decodeData;
		}
		return null;
	}


	public static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);  // Can't happen.
		}
	}
	public static SM3Digest newSM3Digest() {
		return new SM3Digest();
	}
	private static boolean addressValid(byte[] address) {
		if (ArrayUtils.isEmpty(address)) {
			return false;
		}
		if (address.length != ADDRESS_SIZE) {
			return false;
		}
		byte preFixbyte = address[0];
		return preFixbyte == getAddressPreFixByte();
		// Other rule;
	}

	private static byte getAddressPreFixByte() {
		return addressPreFixByte;
	}




//	public static void main(String args[]) throws Exception {
//	    String priv="BFE7VsiBzHtYpC2x2FJn8V81NQ54w6MUjMxX2aGruNySbsbz88v18yoyFHB2hvQyN7h8Cx97NeLUXd3vXtU6uQDc";
//	   // priv="909d3f912ab1f816a79e759547bbe33727f03ee7e6e8589d7e49f0092dbb97be";
//		//priv="AjWnDqkKBR3kimNMAzcZfYdhdPh2Nsj2uvYi1nLaoZkZ";
////		String luo="909d3f912ab1f816a79e759547bbe33727f03ee7e6e8589d7e49f0092dbb97be";
//		byte[] ddd=decode58Check(priv);
//        String priv1=Hex.toHexString(ddd);
//		System.out.println(kkkk);

//
//		//String sss = Base58.encode(priv.getBytes());
//
//		//byte[] decodeCheck = Base58.encode(priv.getBytes());
//
//		 priv=Hex.toHexString(priv.getBytes());


////
//		System.out.println(getAddressByPrivateKey(priv1));
//        System.out.println(toHexAddress(getAddressByPrivateKey(priv1)));
        //base58 AjWnDqkKBR3kimNMAzcZfYdhdPh2Nsj2uvYi1nLaoZkZ
//        priv="909d3f912ab1f816a79e759547bbe33727f03ee7e6e8589d7e49f0092dbb97be";
//        String base58=  Base58.encode(Hex.decode(priv));
//        System.out.println(base58);
//        byte[] eee= Base58.decode(base58);
//        priv=Hex.toHexString(eee);
//        System.out.println(priv);
//		System.out.println(getAddressByPrivateKey(priv));
//		System.out.println(toHexAddress(getAddressByPrivateKey(priv)));



        //自己地址转换
//		priv="";
//		System.out.println(getAddressByPrivateKey(priv));
//		System.out.println(toHexAddress(getAddressByPrivateKey(priv)));
//	}
    /**
     * BASE64解密
     * @throws Exception
     */
    public static byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }


    public static String getAddressByPrivateKey( byte[] privateBytes) {
        ECKey ecKey = ECKey.fromPrivate(privateBytes);
        byte[] from = ecKey.getAddress();
        return toViewAddress(Hex.toHexString(from));
    }

    	/**
	 * 根据私钥获取地址
	 *
	 * @param privateKey
	 * @return
	 */
	public static String getAddressByPrivateKey(String privateKey) {
		byte[] privateBytes = Hex.decode(privateKey);
		ECKey ecKey = ECKey.fromPrivate(privateBytes);
		byte[] from = ecKey.getAddress();
		return toViewAddress(Hex.toHexString(from));
	}

    /**
     * 转换成T开头的地址
     * @param hexAddress
     * @return
     */
    public static String toViewAddress(String hexAddress) {
        return encode58Check(org.tron.common.utils.ByteArray.fromHexString(hexAddress));
    }

    public static String encode58Check(byte[] input) {
        try {
            byte[] hash0 = hash(true, input);
            byte[] hash1 = hash(true, hash0);
            byte[] inputCheck = new byte[input.length + 4];
            System.arraycopy(input, 0, inputCheck, 0, input.length);
            System.arraycopy(hash1, 0, inputCheck, input.length, 4);
            return Base58.encode(inputCheck);
        } catch (Throwable t) {
            log.error(String.format("data error:%s", Hex.toHexString(input)), t);
        }
        return null;
    }
    /**
     * Calculates the SHA-256 hash of the given bytes.
     *
     * @param input the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(boolean isSha256, byte[] input) {
        return hash(isSha256, input, 0, input.length);
    }

	
    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param input  the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(boolean isSha256, byte[] input, int offset, int length) {
        if (isSha256) {
            MessageDigest digest = newDigest();
            digest.update(input, offset, length);
            return digest.digest();
        } else {
            SM3Digest digest = new SM3Digest();
            digest.update(input, offset, length);
            byte[] eHash = new byte[digest.getDigestSize()];
            digest.doFinal(eHash, 0);
            return eHash;
        }
    }



	/**
	 * 报装成transaction
	 *
	 * @param strTransaction
	 * @return
	 */


}