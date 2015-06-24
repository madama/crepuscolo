package net.etalia.crepuscolo.codec;

import java.util.Arrays;

import net.etalia.crepuscolo.codec.Digester.Digest;
import net.etalia.crepuscolo.utils.Strings;

/**
 * Implements a simple, two way, token system.
 * 
 * The token is composed of a number of elements, that are encoded in either base 64 or hex or similar,
 * that can be read back, followed by an md5 signature using one or more salts.
 *  
 */
public class Token {

	public static class TokenWriter {
		private boolean firstSep = false;
		private StringBuilder builder = new StringBuilder();
		private StringBuilder salts = new StringBuilder();
		
		private TokenWriter() {}
		
		private void addSep() {
			if (firstSep) builder.append(':');
			firstSep = true;
		}
		
		public TokenWriter addLong(long value) {
			addSep();
			builder.append(Long.toString(value, 36));
			return this;
		}
		
		public TokenWriter addString(String value) {
			addSep();
			if (value == null) {
				value = "_NULL_";
			}
			builder.append(new Base64Codec().encodeUrlSafeNoPad(value.getBytes(Strings.UTF8)));
			return this;
		}
		
		public TokenWriter addSalt(String... salt) {
			for (String s : salt) {
				salts.append(s);
			}
			return this;
		}
		
		public TokenWriter fork() {
			TokenWriter ret = new TokenWriter();
			ret.builder.append(this.builder);
			ret.salts.append(this.salts);
			ret.firstSep = this.firstSep;
			return ret;
		}
		
		public String encode() {
			Digester digester = new Digester();
			Digest md5 = digester.md5(builder.toString() + "-" + salts.toString());
			addSep();
			builder.append(md5.toBase64UrlSafeNoPad());
			return builder.toString();
		}
	}

	public static class TokenReader {
		
		private static boolean acceptTest;
		
		private boolean isTest = false;
		private String token;
		private String[] split;
		private int actoken;
		
		private TokenReader(String token) {
			this.token = token;
			this.split = token.split(":");
			isTest = (acceptTest && this.split[0].equals("__TEST__"));
			if (isTest) { 
				this.split = Arrays.copyOfRange(this.split, 1, this.split.length);
			} else {
				this.split = Arrays.copyOfRange(this.split, 0, this.split.length - 1);
			}
		}
		
		public Long getLong() {
			if (actoken >= split.length) return null;
			return Long.parseLong(split[actoken++], isTest ? 10 : 36);
		}
		
		public String getString() {
			if (actoken >= split.length) return null;
			if (isTest) return split[actoken++];
			String ret =  new String(new Base64Codec().decodeNoGzip(split[actoken++]), Strings.UTF8);
			if (ret.equals("_NULL_")) return null;
			return ret;
		}
		
		public boolean checkValid(String... salts) {
			// Accept fake signatures for tests
			if (isTest) return true;
			// Find the two parts
			int li = token.lastIndexOf(':');
			String openpart = token.substring(0, li);
			String matchpart = token.substring(li + 1);
			Digester digester = new Digester();
			StringBuilder allsalts = new StringBuilder();
			for (String salt : salts) {
				allsalts.append(salt);
			}
			Digest md5 = digester.md5(openpart + "-" + allsalts.toString());
			String signature = md5.toBase64UrlSafeNoPad();
			return matchpart.equals(signature);
		}
	}

	public static TokenWriter create() {
		return new TokenWriter();
	}

	public static TokenReader parse(String token) {
		return new TokenReader(token);
	}

	public static void acceptTestTokens(boolean accept) {
		TokenReader.acceptTest = accept;
	}

}
