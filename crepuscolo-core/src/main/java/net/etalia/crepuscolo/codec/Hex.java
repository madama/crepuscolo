package net.etalia.crepuscolo.codec;

import java.math.BigInteger;

public class Hex {

	public static String encode(byte[] msg) {
		return new BigInteger(1, msg).toString(16);
	}

}
