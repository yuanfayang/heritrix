package org.archive.crawler.restui.microframework;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;

public class RestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected List<Class<? extends RestController>> controllers = new LinkedList<Class<? extends RestController>>();
    
    protected Map<Class<? extends Annotation>, Map<Pattern, Method>> methodTables = new HashMap<Class<? extends Annotation>, Map<Pattern, Method>>();
    protected Map<String, Class<? extends Annotation>> annotationsByName = new HashMap<String, Class<? extends Annotation>>();
    
    public RestServlet() {
        registerHttpMethod("GET", GET.class);
        registerHttpMethod("PUT", PUT.class);
        registerHttpMethod("POST", POST.class);
        registerHttpMethod("DELETE", DELETE.class);
    }
    
    private void registerHttpMethod(String name, Class<? extends Annotation> annotation) {
        methodTables.put(annotation, new HashMap<Pattern, Method>());
        annotationsByName.put(name, annotation);
    }
    
    /**
     * Return the value of the given http method annotation.
     * 
     * FIXME: This is an ugly hack to get around java not allowing superclasses
     * of annotation interfaces. Find a nicer way of doing it.
     * 
     * @param anno
     * @return
     */
    private String valueForAnnotation(Annotation anno) {
        if (anno instanceof GET) {
            return ((GET)anno).value();
        } else if (anno instanceof POST) {
            return ((POST)anno).value();
        } else if (anno instanceof PUT) {
            return ((PUT)anno).value();
        } else if (anno instanceof DELETE) {
            return ((DELETE)anno).value();
        }
        throw new NotImplementedException("Unhandled annotation type");
    }
    
    /**
     * Register all the methods in the given controller.
     * @param controller
     */
    public void register(Class<? extends RestController> controller) {
        controllers.add(controller);
        for (Method method: controller.getMethods()) {
            register(method);
        }
    }

    /**
     * Register an individual method.  It should have one or more GET, PUT, POST or DELETE annotations.
     * @param method
     */
    public void register(Method method) {
        for (Class<? extends Annotation> annoClass: methodTables.keySet()) {
            if (method.isAnnotationPresent(annoClass)) {
                Annotation anno = method.getAnnotation(annoClass);
                Map<Pattern, Method> methodTable = methodTables.get(annoClass);
                methodTable.put(Pattern.compile("^" + valueForAnnotation(anno) + "$"), method);
            }                
        }
    }

    public boolean isRegistered(Class<? extends RestController> controller) {
        return controllers.contains(controller);
    }
    
    public Method getMethodForPath(Class<? extends Annotation> requestMethod, String path) {
        Map<Pattern, Method> methodTable = methodTables.get(requestMethod);
        for (Pattern pattern: methodTable.keySet()) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                return methodTable.get(pattern);
            }
        }
        return null;
    }
    
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<Pattern, Method> methodTable = methodTables.get(annotationsByName.get(req.getMethod()));
        for (Pattern pattern: methodTable.keySet()) {
            Matcher matcher = pattern.matcher(req.getContextPath());
            if (matcher.matches()) {
                Method method = methodTable.get(pattern);
                Object[] args = argsFromGroups(method, matcher);
                RestController controller;
                
                // instantiate the controller
                try {
                    Object obj = method.getClass().newInstance();
                    controller = (RestController) obj;
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                
                controller.setRequest(req);
                controller.setResponse(resp);
                
                // invoke the requested method
                try {
                    method.invoke(controller, args);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }                
            }
        }

        super.service(req, resp);
    }

    /**
     * Parse the groups in a given regex matcher into arguments for the given method.
     * 
     * @param method
     * @param matcher
     * @return
     */
    private Object[] argsFromGroups(Method method, Matcher matcher) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        
        if (paramTypes.length != matcher.groupCount()) {
            throw new RuntimeException("Argument count mismatch: method " + method.getClass().getName() + "." + method.getName() + " expected " + paramTypes.length + " regex matched " + matcher.groupCount());
        }
        
        for (int i=0; i < paramTypes.length; i++) {
            String group = matcher.group(i + 1);
            
            if (paramTypes[i] == int.class || paramTypes[i] == Integer.class) {
                args[i] = new Integer(group);
            } else if (paramTypes[i] == long.class || paramTypes[i] == Long.class) {
                args[i] = new Long(group);                        
            } else if (paramTypes[i] == float.class || paramTypes[i] == Float.class) {
                args[i] = new Float(group);
            } else if (paramTypes[i] == double.class || paramTypes[i] == Double.class) {
                args[i] = new Double(group);                        
            } else if (paramTypes[i] == String.class) {
                args[i] = group;                        
            } else {
                throw new RuntimeException("Unhandled type in http method " + method.getClass().getName() + "." + method.getName() + " parameter " + i + " of type " + paramTypes[i]);
            }
        }
        return args;
    }        
}
