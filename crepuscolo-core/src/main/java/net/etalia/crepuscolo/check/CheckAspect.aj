package net.etalia.crepuscolo.check;

import java.lang.reflect.Method;
import java.util.Stack;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.AuthService.Verification;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Configurable
public aspect CheckAspect {

	pointcut checkMethod() : 
		(
		execution(@(@CheckerAnnotation *) * *.*(..))
		||
		execution(* *.*(@(@CheckerAnnotation *) (*),..))
		||
		execution(* *.*(*, @(@CheckerAnnotation *) (*),..))
		||
		execution(* *.*(*, *, @(@CheckerAnnotation *) (*),..))
		||
		execution(* *.*(*, *, *, @(@CheckerAnnotation *) (*),..))
		||
		execution(@CheckerAnnotation * *.*(..))
		)
		&& !execution(* (@(Entity || MappedSuperclass) *).get*());

	@Autowired(required=true)
	private AuthService authService;
	
	private CheckerFactory checkerFactory;
	
	public void setAuthService(AuthService authService) {
		this.authService = authService;
	}
	
	public void setCheckerFactory(CheckerFactory cf) {
		this.checkerFactory = cf;
	}

	private ThreadLocal<Boolean> probing = new ThreadLocal<Boolean>();
	private ThreadLocal<Boolean> skip = new ThreadLocal<Boolean>();
	private ThreadLocal<Boolean> nullAuth = new ThreadLocal<Boolean>();
	private ThreadLocal<Stack<String>> mode = new ThreadLocal<Stack<String>>();

	public class JoinPointCheckPoint implements CheckPoint {
	
		private JoinPoint jp;
		
		public JoinPointCheckPoint(JoinPoint jp) {
			this.jp = jp;
		}
	
		@Override
		@Transactional(propagation=Propagation.REQUIRES_NEW)
		public String getAuthenticableId(Verification level) {
			if (nullAuth.get() == Boolean.TRUE) return null;
			return authService.getPrincipalUserId(level);
		}
		
		@Override
		@Transactional(propagation=Propagation.REQUIRES_NEW)
		public Authenticable getAuthenticable(Verification level) {
			if (nullAuth.get() == Boolean.TRUE) return null;
			return authService.getPrincipalUser(level);
		}
	
		@Override
		public Object getInstance() {
			return jp.getThis();
		}
	
		@Override
		public Method getMethod() {
			return ((MethodSignature)jp.getSignature()).getMethod();
		}
	
		@Override
		public Object[] getParameters() {
			return jp.getArgs();
		}

		@Override
		public boolean isInMode(String testMode) {
			return CheckAspect.this.isInMode(testMode);
		}
		
	}

	before() : 
		checkMethod() 
		&& !cflowbelow(checkMethod())
	{
		if (checkerFactory == null) return;
		if (skip.get() == Boolean.TRUE) return;
		
		Checker checker = checkerFactory.getFor(((MethodSignature)thisJoinPointStaticPart.getSignature()).getMethod());
		
		int code = checker.check(new JoinPointCheckPoint(thisJoinPoint));
		if (code != 0) {
			throw new AuthException().statusCode(code);
		}
		if (this.probing.get() == Boolean.TRUE) {
			throw new AuthException().statusCode(100);
		}
	}
	
	public void probing(boolean set) {
		if (set) {
			this.probing.set(true);
		} else {
			this.probing.remove();
		}
	}
	
	public void nullAuth(boolean set) {
		if (set) {
			this.nullAuth.set(true);
		} else {
			this.nullAuth.remove();
		}
	}
	
	public void skip(boolean set) {
		if (set) {
			this.skip.set(true);
		} else {
			this.skip.remove();
		}
	}
	
	public boolean isSkipping() {
		return this.skip.get() == Boolean.TRUE;
	}
	public boolean isProbing() {
		return this.probing.get() == Boolean.TRUE;
	}
	public boolean isNullAuth() {
		return this.nullAuth.get() == Boolean.TRUE;
	}
	
	public void enterMode(String pmode) {
		Stack<String> stack = mode.get();
		if (stack == null) {
			stack = new Stack<String>();
			mode.set(stack);
		}
		stack.add(pmode);
	}
	
	public void exitMode() {
		Stack<String> stack = mode.get();
		if (stack == null) return;
		stack.pop();
	}
	
	public boolean isInMode(String testMode) {
		Stack<String> stack = mode.get();
		if (stack == null || stack.size() == 0) return testMode == null;
		return stack.contains(testMode);		
	}
	
	Object around() :
		execution (@CheckNoAuth * *.*(..))
	{
		boolean prenullauth = this.nullAuth.get() == Boolean.TRUE;
		try {
			nullAuth(true);
			return proceed();
		} finally {
			nullAuth(prenullauth);
		}
	}
	
	Object around() :
		execution (@CheckNoChecks * *.*(..))
	{
		boolean preskip = this.skip.get() == Boolean.TRUE;
		try {
			skip(true);
			return proceed();
		} finally {
			skip(preskip);
		}
	}

	before(String setMode) :
		execution (@CheckMode * (@Controller *).*(..))
		&& @annotation(CheckMode(setMode))
	{
		enterMode(setMode);
	}
	
	Object around(String setMode) :
		execution (@CheckMode * *.*(..))
		&& @annotation(CheckMode(setMode))
	{
		enterMode(setMode);
		try {
			return proceed(setMode);
		} finally {
			exitMode();
		}
	}
	
	public void clear() {
		this.mode.remove();
		this.nullAuth.remove();
		this.probing.remove();
		this.skip.remove();
	}
}
