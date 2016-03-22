package net.etalia.crepuscolo.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.etalia.crepuscolo.utils.Check;
import net.etalia.crepuscolo.utils.Strings;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class Caller<Serv> {

	protected Serv proxy = null;
	protected Class<Serv> interf = null;

	private String[] allFields;
	private Map<String,String> allHeaders;
	private String allAuthToken;

	public static enum HttpMethod {
		GET,
		PUT,
		POST,
		DELETE
	}

	public static class Invocation {
		private static ThreadLocal<Stack<Invocation>> invocations = new ThreadLocal<Stack<Invocation>>() {
			protected java.util.Stack<Invocation> initialValue() {
				return new Stack<Invocation>();
			};
		};

		public static Invocation getInvocation() {
			Stack<Invocation> stack = invocations.get();
			Invocation ret = null;
			if (stack.size() > 0) ret = stack.pop();
			if (stack.size() == 0) invocations.remove();
			return ret;
		}
		
		public static Invocation peekInvocation() {
			Stack<Invocation> stack = invocations.get();
			Invocation ret = null;
			if (stack.size() > 0) ret = stack.peek();
			return ret;
		}
		
		
		private Method method;
		private Object[] args;
		
		public Invocation(Method method, Object[] args) {
			this.method = method;
			this.args = args;
			invocations.get().push(this);
		}
		
		public Method getMethod() {
			return method;
		}
		public Object[] getArgs() {
			return args;
		}
		
		public Object getArgument(int index) {
			Check.illegalstate.assertTrue("There is no parameter " + index + " in the invocation of method " + this.method, index < args.length);
			return args[index];
		}
		
	}

	private static class CallInterceptor implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			new Invocation(method, args);
			if (method.getReturnType().isPrimitive()) {
				if (Void.TYPE.equals(method.getReturnType())) return null;
				if (Boolean.TYPE.equals(method.getReturnType())) return false;
				Class<? extends Number> clazz = (Class<? extends Number>)method.getReturnType();
				if (clazz == Byte.class || clazz == Byte.TYPE) return (byte)0;
				if (clazz == Short.class || clazz == Short.TYPE) return (short)0;
				if (clazz == Integer.class || clazz == Integer.TYPE) return (int)0;
				if (clazz == Long.class || clazz == Long.TYPE) return (long)0;
				if (clazz == Float.class || clazz == Float.TYPE) return (float)0;
				if (clazz == Double.class || clazz == Double.TYPE) return (double)0;

				throw new IllegalArgumentException("Unsupported primitive type " + clazz.getName());
			}
			return null;
		}
	}

	public Caller(Class<Serv> serviceInterface) {
		this.interf = serviceInterface;
	}

	public Serv service() {
		if (proxy == null) { 
			proxy = (Serv)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { interf }, new CallInterceptor());
		}
		return proxy;
	}

	public Call<Void> voidMethod() {
		return method((Void)null);
	}

	public <X> Call<X> method(X object) {
		Invocation inv = Invocation.getInvocation();
		if (inv == null) throw new IllegalStateException("Invocation is null, have you called method(mockService.something(arg,arg)) ??");
		
		// XXX this could be cached
		RequestMapping classrm = inv.getMethod().getDeclaringClass().getAnnotation(RequestMapping.class);
		RequestMapping methrm = inv.getMethod().getAnnotation(RequestMapping.class);
		if (classrm == null && methrm == null) throw new IllegalStateException("The method is not annotated for remote call, cannot compute path and parameters");
		
		HttpMethod httpMethod = null; 
		
		RequestMethod[] methods = methrm.method();
		if (methods.length == 0 && classrm != null) {
			methods = classrm.method();
		}
		if (methods.length == 0) {
			httpMethod = HttpMethod.GET;
		} else {
			httpMethod = HttpMethod.valueOf(methods[0].name());
		}
		
		String[] methvals = methrm.value();
		String[] classvals = classrm != null ? classrm.value() : null;
		
		List<String> paths = new ArrayList<String>();
		if (classvals != null && classvals.length > 0) {
			paths.add(classvals[0]);
		} 
		if (methvals.length > 0) {
			paths.add(methvals[0]);
		}
		
		// XXX this does not resolve relative vs non relative paths
		String path = Strings.pathConcat(paths);
		
		Call<?> call = createCall(inv.getMethod().getGenericReturnType(), httpMethod, path);
		
		Annotation[][] paranns = inv.getMethod().getParameterAnnotations();
		int paramCnt = 0;
		for (int i = 0; i < paranns.length; i++) {
			for (int j = 0; j < paranns[i].length; j++) {
				Annotation ann = paranns[i][j];
				if (ann instanceof PathVariable) {
					String paramName = ((PathVariable)ann).value();
					if (paramName == null || paramName.length() == 0) {
						int si = path.indexOf('{');
						int ei = path.indexOf('}', si);
						if (si == -1 || ei == -1) throw new IllegalStateException("@PathVariable used on parameter " + i + " of method " + inv.getMethod() + " but no {parameter} found in path");
						paramName = path.substring(si + 1, ei);
					}
					paramName = paramName.split(":")[0];
					call.setPathVariable(paramName, inv.getArgument(paramCnt++));
				} else if (ann instanceof RequestParam) {
					String paramName = ((RequestParam) ann).value();
					call.setParameter(paramName, inv.getArgument(paramCnt++));
				} else if (ann instanceof RequestBody || ann instanceof IdPathRequestBody) {
					call.setBody(inv.getArgument(paramCnt++));
				} else if (ann instanceof RequestHeader) {
					String headerName = ((RequestHeader)ann).value();
					call.setHeader(headerName, inv.getArgument(paramCnt++));
				} else {
					// TODO should handle headers and stuff like that
					paramCnt++;
				}
			}
		}
		
		return (Call<X>) initCall(call);
	}

	protected abstract <X> Call<X> createCall(Type clazz, HttpMethod method, String path);

	public Call<?> path(String path) {
		return initCall(createCall(Object.class, HttpMethod.GET, path));
	}

	public Call<?> path(HttpMethod method, String path) {
		return initCall(createCall(Object.class, method, path));
	}

	public void setAllAuthToken(String allAuthToken) {
		this.allAuthToken = allAuthToken;
	}
	public void setAllHeaders(Map<String, String> allHeaders) {
		this.allHeaders = allHeaders;
	}
	public void addAllHeader(String header, String value) {
		if (this.allHeaders == null) this.allHeaders = new HashMap<String, String>();
		this.allHeaders.put(header, value);
	}
	public void setAllFields(String... allFields) {
		this.allFields = allFields;
	}

	protected Call<?> initCall(Call<?> call) {
		if (allAuthToken != null) {
			call.authAsToken(allAuthToken);
		}
		if (allHeaders != null) {
			for (Map.Entry<String, String> entry : allHeaders.entrySet()) {
				call.setHeader(entry.getKey(), entry.getValue());
			}
		}
		if (allFields != null) {
			call.withFields(allFields);
		}
		return call;
	}

}
