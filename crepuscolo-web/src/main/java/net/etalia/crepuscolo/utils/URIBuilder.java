package net.etalia.crepuscolo.utils;

/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builder for {@link URI} instances.
 * <p>
 * Taken from HttpClient 4.2
 * </p>
 */
public class URIBuilder {

	public static class NameValuePair {
		public String name;
		public String value;

		public NameValuePair(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	/**
	 * Unreserved characters, i.e. alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
	 * <p>
	 * This list is the same as the {@code unreserved} list in <a
	 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
	 */
	private static final BitSet UNRESERVED = new BitSet(256);
	/**
	 * Punctuation characters: , ; : $ & + =
	 * <p>
	 * These are the additional characters allowed by userinfo.
	 */
	private static final BitSet PUNCT = new BitSet(256);
	/**
	 * Characters which are safe to use in userinfo, i.e. {@link #UNRESERVED}
	 * plus {@link #PUNCT}uation
	 */
	private static final BitSet USERINFO = new BitSet(256);
	/**
	 * Characters which are safe to use in a path, i.e. {@link #UNRESERVED} plus
	 * {@link #PUNCT}uation plus / @
	 */
	private static final BitSet PATHSAFE = new BitSet(256);
	/**
	 * Characters which are safe to use in a query or a fragment, i.e.
	 * {@link #RESERVED} plus {@link #UNRESERVED}
	 */
	private static final BitSet URIC = new BitSet(256);

	/**
	 * Reserved characters, i.e. {@code ;/?:@&=+$,[]}
	 * <p>
	 * This list is the same as the {@code reserved} list in <a
	 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a> as augmented by
	 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC 2732</a>
	 */
	private static final BitSet RESERVED = new BitSet(256);

	/**
	 * Safe characters for x-www-form-urlencoded data, as per
	 * java.net.URLEncoder and browser behaviour, i.e. alphanumeric plus
	 * {@code "-", "_", ".", "*"}
	 */
	private static final BitSet URLENCODER = new BitSet(256);

	static {
		// unreserved chars
		// alpha characters
		for (int i = 'a'; i <= 'z'; i++) {
			UNRESERVED.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			UNRESERVED.set(i);
		}
		// numeric characters
		for (int i = '0'; i <= '9'; i++) {
			UNRESERVED.set(i);
		}
		UNRESERVED.set('_'); // these are the charactes of the "mark" list
		UNRESERVED.set('-');
		UNRESERVED.set('.');
		UNRESERVED.set('*');
		URLENCODER.or(UNRESERVED); // skip remaining unreserved characters
		UNRESERVED.set('!');
		UNRESERVED.set('~');
		UNRESERVED.set('\'');
		UNRESERVED.set('(');
		UNRESERVED.set(')');
		// punct chars
		PUNCT.set(',');
		PUNCT.set(';');
		PUNCT.set(':');
		PUNCT.set('$');
		PUNCT.set('&');
		PUNCT.set('+');
		PUNCT.set('=');
		// Safe for userinfo
		USERINFO.or(UNRESERVED);
		USERINFO.or(PUNCT);

		// URL path safe
		PATHSAFE.or(UNRESERVED);
		PATHSAFE.set('/'); // segment separator
		PATHSAFE.set(';'); // param separator
		PATHSAFE.set(':'); // rest as per list in 2396, i.e. : @ & = + $ ,
		PATHSAFE.set('@');
		PATHSAFE.set('&');
		PATHSAFE.set('=');
		PATHSAFE.set('+');
		PATHSAFE.set('$');
		PATHSAFE.set(',');

		RESERVED.set(';');
		RESERVED.set('/');
		RESERVED.set('?');
		RESERVED.set(':');
		RESERVED.set('@');
		RESERVED.set('&');
		RESERVED.set('=');
		RESERVED.set('+');
		RESERVED.set('$');
		RESERVED.set(',');
		RESERVED.set('['); // added by RFC 2732
		RESERVED.set(']'); // added by RFC 2732

		URIC.or(RESERVED);
		URIC.or(UNRESERVED);
	}

	private String scheme;
	private String encodedSchemeSpecificPart;
	private String encodedAuthority;
	private String userInfo;
	private String encodedUserInfo;
	private String host;
	private int port;
	private String path;
	private String encodedPath;
	private String encodedQuery;
	private List<NameValuePair> queryParams;
	private String query;
	private String fragment;
	private String encodedFragment;

	/**
	 * Construct an instance from the string which must be a valid URI.
	 *
	 * @param string
	 *            a valid URI in string form
	 * @throws URISyntaxException
	 *             if the input is not a valid URI
	 */
	public URIBuilder(final String string) throws URISyntaxException {
		super();
		digestURI(new URI(string));
	}

	private void digestURI(final URI uri) {
		this.scheme = uri.getScheme();
		this.encodedSchemeSpecificPart = uri.getRawSchemeSpecificPart();
		this.encodedAuthority = uri.getRawAuthority();
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.encodedUserInfo = uri.getRawUserInfo();
		this.userInfo = uri.getUserInfo();
		this.encodedPath = uri.getRawPath();
		this.path = uri.getPath();
		this.encodedQuery = uri.getRawQuery();
		this.queryParams = null;
		this.encodedFragment = uri.getRawFragment();
		this.fragment = uri.getFragment();
	}

	/**
	 * Adds parameter to URI query. The parameter name and value are expected to
	 * be unescaped and may contain non ASCII characters.
	 * <p/>
	 * Please note query parameters and custom query component are mutually
	 * exclusive. This method will remove custom query if present.
	 */
	public URIBuilder addParameter(final String param, final String value) {
		if (this.queryParams == null) {
			this.queryParams = new ArrayList<NameValuePair>();
		}
		this.queryParams.add(new NameValuePair(param, value));
		this.encodedQuery = null;
		this.encodedSchemeSpecificPart = null;
		this.query = null;
		return this;
	}

	public URI build() throws URISyntaxException {
		return new URI(buildString());
	}

	private String buildString() {
		final StringBuilder sb = new StringBuilder();
		if (this.scheme != null) {
			sb.append(this.scheme).append(':');
		}
		if (this.encodedSchemeSpecificPart != null) {
			sb.append(this.encodedSchemeSpecificPart);
		} else {
			if (this.encodedAuthority != null) {
				sb.append("//").append(this.encodedAuthority);
			} else if (this.host != null) {
				sb.append("//");
				if (this.encodedUserInfo != null) {
					sb.append(this.encodedUserInfo).append("@");
				} else if (this.userInfo != null) {
					sb.append(encodeUserInfo(this.userInfo)).append("@");
				}
				if (isIPv6Address(this.host)) {
					sb.append("[").append(this.host).append("]");
				} else {
					sb.append(this.host);
				}
				if (this.port >= 0) {
					sb.append(":").append(this.port);
				}
			}
			if (this.encodedPath != null) {
				sb.append(normalizePath(this.encodedPath));
			} else if (this.path != null) {
				sb.append(encodePath(normalizePath(this.path)));
			}
			if (this.encodedQuery != null) {
				sb.append("?").append(this.encodedQuery);
			} else if (this.queryParams != null) {
				sb.append("?").append(encodeUrlForm(this.queryParams));
			} else if (this.query != null) {
				sb.append("?").append(encodeUric(this.query));
			}
		}
		if (this.encodedFragment != null) {
			sb.append("#").append(this.encodedFragment);
		} else if (this.fragment != null) {
			sb.append("#").append(encodeUric(this.fragment));
		}
		return sb.toString();
	}

	private String encodeUserInfo(final String userInfo) {
		return urlEncode(userInfo, UTF_8, USERINFO, false);
	}

	private String encodePath(final String path) {
		return urlEncode(path, UTF_8, PATHSAFE, false);
	}

	private String encodeUrlForm(final List<NameValuePair> params) {
		return format(params, '&', UTF_8);
	}

	private String encodeUric(final String fragment) {
		return urlEncode(fragment, UTF_8, URIC, false);
	}

	public static String format(
			final Iterable<? extends NameValuePair> parameters,
			final Charset charset) {
		return format(parameters, '&', charset);
	}

	public static String format(
			final Iterable<? extends NameValuePair> parameters,
			final char parameterSeparator, final Charset charset) {
		final StringBuilder result = new StringBuilder();
		for (final NameValuePair parameter : parameters) {
			final String encodedName = encodeFormFields(parameter.name, charset);
			final String encodedValue = encodeFormFields(parameter.value,
					charset);
			if (result.length() > 0) {
				result.append(parameterSeparator);
			}
			result.append(encodedName);
			if (encodedValue != null) {
				result.append('=');
				result.append(encodedValue);
			}
		}
		return result.toString();
	}

	private static String encodeFormFields(final String content,
			final Charset charset) {
		if (content == null) {
			return null;
		}
		return urlEncode(content, charset != null ? charset : UTF_8,
				URLENCODER, true);
	}

	private static String normalizePath(final String path) {
		String s = path;
		if (s == null) {
			return null;
		}
		int n = 0;
		for (; n < s.length(); n++) {
			if (s.charAt(n) != '/') {
				break;
			}
		}
		if (n > 1) {
			s = s.substring(n - 1);
		}
		return s;
	}

	private static String urlEncode(final String content,
			final Charset charset, final BitSet safechars,
			final boolean blankAsPlus) {
		if (content == null) {
			return null;
		}
		final StringBuilder buf = new StringBuilder();
		final ByteBuffer bb = charset.encode(content);
		while (bb.hasRemaining()) {
			final int b = bb.get() & 0xff;
			if (safechars.get(b)) {
				buf.append((char) b);
			} else if (blankAsPlus && b == ' ') {
				buf.append('+');
			} else {
				buf.append("%");
				final char hex1 = Character.toUpperCase(Character.forDigit(
						(b >> 4) & 0xF, 16));
				final char hex2 = Character.toUpperCase(Character.forDigit(
						b & 0xF, 16));
				buf.append(hex1);
				buf.append(hex2);
			}
		}
		return buf.toString();
	}

	private static final Pattern IPV6_STD_PATTERN = Pattern
			.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

	private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern
			.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

	/*
	 * The above pattern is not totally rigorous as it allows for more than 7
	 * hex fields in total
	 */
	private static final char COLON_CHAR = ':';

	// Must not have more than 7 colons (i.e. 8 fields)
	private static final int MAX_COLON_COUNT = 7;

	/**
	 * Checks whether the parameter is a valid standard (non-compressed) IPv6
	 * address
	 *
	 * @param input
	 *            the address string to check for validity
	 * @return true if the input parameter is a valid standard (non-compressed)
	 *         IPv6 address
	 */
	public static boolean isIPv6StdAddress(final String input) {
		return IPV6_STD_PATTERN.matcher(input).matches();
	}

	/**
	 * Checks whether the parameter is a valid compressed IPv6 address
	 *
	 * @param input
	 *            the address string to check for validity
	 * @return true if the input parameter is a valid compressed IPv6 address
	 */
	public static boolean isIPv6HexCompressedAddress(final String input) {
		int colonCount = 0;
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) == COLON_CHAR) {
				colonCount++;
			}
		}
		return colonCount <= MAX_COLON_COUNT
				&& IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
	}

	/**
	 * Checks whether the parameter is a valid IPv6 address (including
	 * compressed).
	 *
	 * @param input
	 *            the address string to check for validity
	 * @return true if the input parameter is a valid standard or compressed
	 *         IPv6 address
	 */
	public static boolean isIPv6Address(final String input) {
		return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
	}

}