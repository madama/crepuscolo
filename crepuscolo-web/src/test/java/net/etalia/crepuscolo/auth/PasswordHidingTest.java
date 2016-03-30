package net.etalia.crepuscolo.auth;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.etalia.crepuscolo.codec.Digester;
import net.etalia.crepuscolo.services.AuthService;

@RunWith(Theories.class)
public class PasswordHidingTest {

	@DataPoint
	public static String 
	pass1 = "ciao",
	pass2 = "1234!\"£$%&/()=:;,.",
	pass3 = "";
	
	@Theory
	public void hideAndVerify(String pass) throws Exception {
		AuthService as = new AuthServiceImpl();
		
		Digester dg = new Digester();
		String passmd5 = dg.md5(pass).toBase64UrlSafeNoPad();
		
		String hidden = as.hidePassword(passmd5);
		System.out.println(pass + " = " + hidden);
		Assert.assertTrue(as.verifyPassword(hidden, passmd5));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void invalidFormat() throws Exception {
		AuthService as = new AuthServiceImpl();
		as.verifyPassword("123", "123");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void invalidVersion() throws Exception {
		AuthService as = new AuthServiceImpl();
		as.verifyPassword("5/CU-sMqzzn4x4Rfj6yKL8llDP9CI:XWqQMbgIT5Xd2BPAqqnUx2pOohE", "123");
	}
	
	@Test
	public void dontRepeat() throws Exception {
		AuthService as = new AuthServiceImpl();
		Digester dg = new Digester();
		String passmd5 = dg.md5("test").toBase64UrlSafeNoPad();
		
		String hidden = as.hidePassword(passmd5);
		String hidden2 = as.hidePassword(hidden);
		
		Assert.assertEquals(hidden, hidden2);
	}
}
