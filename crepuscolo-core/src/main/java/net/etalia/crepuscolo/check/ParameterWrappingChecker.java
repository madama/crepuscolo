package net.etalia.crepuscolo.check;

import java.lang.reflect.Method;

import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.services.AuthService.Verification;

/**
 * Wraps a normal {@link Checker} when used on a parameter instead of a method. 
 * <p>
 * Thanks to this abstraction there is no difference between a checker that works on a method
 * interpreting the "this" instance (for example on a getter) or a checker that works
 * on a parameter of a method (for example on a service method).
 * <p>
 * <p>
 * Examples :
 * <pre>{@code 
 *   public class User {
 *     
 *     {@literal @}ChSelf // A user can only modify his own password 
 *     public void setPassword(String pwd) {
 *       //...
 *     }
 *   }
 *
 *   
 *   public class ServiceAPIImpl {
 *   
 *     // A user can only spam himself
 *     // In this case ChSelfChecker will run wrapped to act on the parameter
 *     public void spamUser({@literal @}ChSelf String userid) {
 *   	//...  
 *     }
 *   }
 * }</pre>
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
public class ParameterWrappingChecker implements Checker {

	private Checker underlying;
	private int paramIndex;

	public ParameterWrappingChecker(Checker ch, int paramIndex) {
		this.underlying = ch;
		this.paramIndex = paramIndex;
	}

	public class WrappedCheckPoint implements CheckPoint {

		private CheckPoint delegate;
		
		private WrappedCheckPoint(CheckPoint delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getAuthenticableId(Verification level) {
			return delegate.getAuthenticableId(level);
		}

		@Override
		public Authenticable getAuthenticable(Verification level) {
			return delegate.getAuthenticable(level);
		}

		@Override
		public Object getInstance() {
			return delegate.getParameters()[paramIndex];
		}

		@Override
		public Method getMethod() {
			return delegate.getMethod();
		}

		@Override
		public Object[] getParameters() {
			return delegate.getParameters();
		}
		
		@Override
		public boolean isInMode(String mode) {
			return delegate.isInMode(mode);
		}
		
	}

	@Override
	public int check(CheckPoint p) {
		return underlying.check(new WrappedCheckPoint(p));
	}

	public Checker getUnderlying() {
		return underlying;
	}

	public int getParamIndex() {
		return paramIndex;
	}

}
