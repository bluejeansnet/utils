/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.security.GeneralSecurityException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.owasp.encoder.esapi.ESAPIEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Meta utility.
 *
 * @author Dinesh Ilindra
 */
public class MetaUtil {

    /**
     * Generic meta data map
     */
    public static final Map<String, Object> META_MAP = new HashMap<>();

    /**
     * The simple return type list
     */
    public static final List<Class<?>> SIMPLE_RETURN_TYPE_LIST = new ArrayList<Class<?>>(
            Arrays.asList(Boolean.TYPE, Boolean.class, Integer.class, Integer.TYPE, Long.class, Long.TYPE, Short.class,
                    Short.TYPE, Double.TYPE, Double.class, Float.class, Float.TYPE, String.class));

    /**
     * Create a params map from given varargs.
     *
     * @param params
     *            the params
     * @return the param map
     */
    public static Map<String, Object> createParamMap(final Object... params) {
        final Map<String, Object> paramMap = new HashMap<String, Object>();
        if (params.length > 1) {
            for (int index = 0; index < params.length; index = index + 2) {
                paramMap.put(params[index].toString(), params[index + 1]);
            }
        }
        return paramMap;
    }

    /**
     * Get the resource as string.
     *
     * @param resourceName
     *            the resource name
     * @return the string value
     */
    public static String getResourceAsString(final String resourceName) {
        InputStream resourceStream = null;
        try {
            resourceStream = MetaUtil.class.getResourceAsStream(resourceName);
            return IOUtils.toString(resourceStream);
        } catch (final IOException e) {
            return "";
        } finally {
            if (resourceStream != null) {
                try {
                    resourceStream.close();
                } catch (final IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * Get the resource as bytes.
     *
     * @param resourceName
     *            the resource name
     * @return the bytes value
     */
    public static byte[] getResourceAsBytes(final String resourceName) {
        return getResourceAsBytes(MetaUtil.class, resourceName);
    }

    /**
     * Get the resource as bytes.
     *
     * @param resourceName
     *            the resource name
     * @param loader
     *            the class loader
     * @return the bytes value
     */
    public static byte[] getResourceAsBytes(final Class<?> loader, final String resourceName) {
        InputStream resourceStream = null;
        try {
            resourceStream = loader.getResourceAsStream(resourceName);
            return IOUtils.toByteArray(resourceStream);
        } catch (final IOException e) {
            return new byte[0];
        } finally {
            if (resourceStream != null) {
                try {
                    resourceStream.close();
                } catch (final IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * Register the instance as MBean.
     *
     * @param bean
     *            the bean
     * @throws JMException
     *             implicit
     */
    public static void registerAsMBean(final Object bean) throws JMException {
        try {
            MetaUtil.registerAsMBean(bean,
                    bean.getClass().getPackage().getName() + ":type=" + bean.getClass().getSimpleName());
        } catch (final InstanceAlreadyExistsException iaex) {
            MetaUtil.registerAsMBean(bean, bean.getClass().getPackage().getName() + ":type="
                    + bean.getClass().getSimpleName() + "-" + Integer.toHexString(bean.hashCode()));
        }
    }

    /**
     * Register the instance as MBean with given name.
     *
     * @param bean
     *            the bean
     * @param objectName
     *            the object name
     * @throws JMException
     *             implicit
     */
    public static void registerAsMBean(final Object bean, final String objectName) throws JMException {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName(objectName);
        mbs.registerMBean(bean, name);
    }

    /**
     * Finds the first method starting by this name or null if not found.
     *
     * @param clazz
     *            the class
     * @param methodName
     *            the method name
     * @return the first method found / null
     */
    public static Method findFirstMethodStartsWith(final Class<?> clazz, final String methodName) {
        for (final Method method : allMethodsOf(clazz)) {
            if (method.getName().startsWith(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the first method ending by this name and having this return type or null if not found.
     *
     * @param clazz
     *            the class
     * @param methodName
     *            the method name
     * @param returnType
     *            the return type
     * @return the first method found / null
     */
    public static Method findFirstMethodEndsWithAndReturns(final Class<?> clazz, final String methodName,
            final Class<?> returnType) {
        for (final Method method : allMethodsOf(clazz)) {
            if (method.getName().endsWith(methodName) && method.getReturnType() == returnType) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the first method by this name or null if not found.
     *
     * @param clazz
     *            the class
     * @param methodName
     *            the method name
     * @return the first method found / null
     */
    public static Method findFirstMethod(final Class<?> clazz, final String methodName) {
        for (final Method method : allMethodsOf(clazz)) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Find first method matching name and no. of arguments or null if none found.
     *
     * @param classz
     *            the class
     * @param methodName
     *            the method name
     * @param totalArguments
     *            the total no. of arguments
     * @return the first method found / null
     */
    public static Method findFirstMethod(final Class<?> classz, final String methodName, final int totalArguments) {
        for (final Method method : allMethodsOf(classz)) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == totalArguments) {
                return method;
            }
        }
        return null;
    }

    /**
     * All classes that the given class extends
     *
     * @param classz
     *            the class
     * @return the set of all classes that this extends
     */
    public static List<Class<?>> allExtendedClassesOf(final Class<?> classz) {
        final List<Class<?>> clazzList = new ArrayList<>();
        for (Class<?> c = classz; c != null; c = c.getSuperclass()) {
            clazzList.add(c);
        }
        return clazzList;
    }

    /**
     * All methods that can be invoked on an object of the given class
     *
     * @param classz
     *            the class
     * @return the list of all methods that can be invoked
     */
    public static List<Method> allMethodsOf(final Class<?> classz) {
        final List<Method> methods = new ArrayList<>();
        final List<Class<?>> clazzList = new ArrayList<>();
        for (Class<?> c = classz; c != null; c = c.getSuperclass()) {
            clazzList.add(c);
        }
        clazzList.addAll(Arrays.asList(classz.getInterfaces()));
        for (final Class<?> c : clazzList) {
            final Method[] declaredMethods = c.getDeclaredMethods();
            try {
                AccessibleObject.setAccessible(declaredMethods, true);
            } catch (final RuntimeException re) {
                // do nothing
            }
            methods.addAll(Arrays.asList(declaredMethods));
        }
        return methods;
    }

    /**
     * Finds the first field by this name or null if not found.
     *
     * @param clazz
     *            the class
     * @param fieldName
     *            the field name
     * @return the first field found / null
     */
    public static Field findFirstField(final Class<?> clazz, final String fieldName) {
        for (final Field field : allFieldsOf(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Finds the first super field by this name or null if not found.
     *
     * @param clazz
     *            the class
     * @param fieldName
     *            the field name
     * @return the first field found / null
     */
    public static Field findFirstSuperField(final Class<?> clazz, final String fieldName) {
        for (final Field field : allSuperFieldsOf(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Finds the first field by its type name or null if not found.
     *
     * @param clazz
     *            the class
     * @param typeName
     *            the field type simple name
     * @return the first field found / null
     */
    public static Field findFirstFieldByType(final Class<?> clazz, final String typeName) {
        for (final Field field : allFieldsOf(clazz)) {
            if (field.getType().getSimpleName().equals(typeName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * All fields that can be found on an object of the given class
     *
     * @param classz
     *            the class
     * @return the list of all fields that can be accessed
     */
    public static List<Field> allFieldsOf(final Class<?> classz) {
        final List<Field> fields = new ArrayList<>();
        for (Class<?> c = classz; c != null; c = c.getSuperclass()) {
            final Field[] declaredFields = c.getDeclaredFields();
            try {
                AccessibleObject.setAccessible(declaredFields, true);
            } catch (final RuntimeException re) {
                // do nothing
            }
            fields.addAll(Arrays.asList(declaredFields));
        }
        return fields;
    }

    /**
     * All fields that can be found on an object's hierarchy other than itself of the given class
     *
     * @param classz
     *            the class
     * @return the list of all fields that can be accessed
     */
    public static List<Field> allSuperFieldsOf(final Class<?> classz) {
        final List<Field> fields = new ArrayList<>();
        for (Class<?> c = classz.getSuperclass(); c != null; c = c.getSuperclass()) {
            final Field[] declaredFields = c.getDeclaredFields();
            try {
                AccessibleObject.setAccessible(declaredFields, true);
            } catch (final RuntimeException re) {
                // do nothing
            }
            fields.addAll(Arrays.asList(declaredFields));
        }
        return fields;
    }

    /**
     * Finds the setter method for the property or null if not found.
     *
     * @param classz
     *            the class
     * @param propertyName
     *            the property name
     * @return the setter
     */
    public static Method findSetter(final Class<?> classz, final String propertyName) {
        return MetaUtil.findFirstMethod(classz,
                "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1), 1);
    }

    /**
     * Sets the given property if exists with the given value.
     *
     * @param obj
     *            the object
     * @param propertyName
     *            the property name
     * @param propertyValue
     *            the property value
     */
    public static void setPropertyIfExists(final Object obj, final String propertyName, final Object propertyValue) {
        final Class<?> clazz = obj.getClass();
        final Method propSetter = MetaUtil.findSetter(clazz, propertyName);
        if (propSetter != null) {
            Class<?> paramType = propSetter.getParameterTypes()[0];
            if (paramType.isPrimitive()) {
                paramType = ClassUtils.primitiveToWrapper(paramType);
            }
            Object value = propertyValue;
            try {
                if (!propertyValue.getClass().equals(paramType)) {
                    value = paramType.getMethod("valueOf", propertyValue.getClass()).invoke(null, propertyValue);
                }
                propSetter.invoke(obj, value);
            } catch (final Exception e) {
                // e.printStackTrace();
            }
        }
    }

    /**
     * Get the map of all the return values of all the methods starting with given prefix with the
     * given return types of the given bean.
     *
     * @param target
     *            the target object
     * @param prependClassName
     *            the prepend class name
     * @param methodPrefix
     *            the method prefix
     * @param returnTypeList
     *            the return types list
     * @return the field data map
     */
    public static Map<String, Object> fieldDataMap(final Object target, final boolean prependClassName,
            final String methodPrefix, final List<Class<?>> returnTypeList) {
        final Map<String, Object> dataMap = new HashMap<String, Object>();
        final String className = target.getClass().getSimpleName();
        for (final Method method : allMethodsOf(target.getClass())) {
            if (method.getName().startsWith(methodPrefix) && method.getParameterTypes().length == 0
                    && returnTypeList.contains(method.getReturnType())) {
                try {
                    if (prependClassName) {
                        dataMap.put(className + "." + method.getName(), method.invoke(target));
                    } else {
                        dataMap.put(method.getName(), method.invoke(target));
                    }
                } catch (final Exception ex) {
                    // do nothing.
                }
            }
        }
        return dataMap;
    }

    /**
     * Get the map of all the return values of all the methods with no arguments with simple return
     * types of the given bean.
     *
     * @param target
     *            the target object
     * @param prependClassName
     *            the prepend class name
     * @return the field data map
     */
    public static Map<String, Object> simpleFieldDataMap(final Object target, final boolean prependClassName) {
        return MetaUtil.fieldDataMap(target, prependClassName, "", MetaUtil.SIMPLE_RETURN_TYPE_LIST);
    }

    /**
     * Returns a map containing the object's class name as key with the object itself as value.
     *
     * @param objects
     *            the objects
     * @return the name map
     */
    public static Map<String, Object> classNameObjectMap(final Object... objects) {
        final Map<String, Object> valueMap = new HashMap<String, Object>();
        int index = 0;
        for (final Object object : objects) {
            String key = object.getClass().getName();
            key = key.substring(key.lastIndexOf('.') + 1).replaceAll(";", "");
            key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
            if (valueMap.containsKey(key)) {
                valueMap.put(key + index++, object);
            } else {
                valueMap.put(key, object);
            }
        }
        return valueMap;
    }

    /**
     * Returns a new map which has single entry with the given key and value.
     *
     * @param key
     *            the key
     * @param value
     *            the value
     * @return the map
     */
    public static Map<String, Object> createMap(final String key, final Object value) {
        final Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(key, value);
        return valueMap;
    }

    /**
     * Create a property map from given varargs.
     *
     * @param args
     *            the arguments
     * @return the prop map
     */
    public static Map<String, String> createPropsMap(final String... args) {
        final Map<String, String> propMap = new HashMap<String, String>();
        String[] prop = null;
        for (final String arg : args) {
            prop = arg.split("=");
            propMap.put(prop[0], prop[1]);
        }
        return propMap;
    }

    /**
     * Shuffles the given array, if it is array.
     *
     * @param array
     *            the array to shuffle
     */
    public static void shuffleArray(final Object array) {
        if (!array.getClass().isArray()) {
            return;
        }
        int index;
        Object temp;
        final Random random = new Random();
        for (int i = Array.getLength(array) - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = Array.get(array, index);
            Array.set(array, index, Array.get(array, i));
            Array.set(array, i, temp);
        }
    }

    /**
     * Get available server port from the given port inclusive, until the max count.
     *
     * @param startPort
     *            the start port
     * @param maxCount
     *            the max count
     * @return the available port
     */
    public static int availablePort(final int startPort, final int maxCount) {
        int delta = 0;
        while (delta < maxCount) {
            try (Socket tmpSocket = new Socket("localhost", startPort + delta)) {
                delta++;
            } catch (final IOException ioe) {
                break;
            }
        }
        return startPort + delta;
    }

    /**
     * Run the nested method on the target object.
     *
     * @param target
     *            the target object
     * @param nestedMethod
     *            the nested method
     * @throws ScriptException
     *             when there is a problem in script
     * @throws ReflectiveOperationException
     *             when a problem in reflection
     */
    public static Object runNestedMethod(final Object target, final String nestedMethod)
            throws ReflectiveOperationException, ScriptException {
        return new URIInvoker(target).invokeNestedMethod(target, nestedMethod);
    }

    /**
     * Run the nested methods silently on the target object.
     *
     * @param target
     *            the target object
     * @param nestedMethods
     *            the nested methods separated by ;;
     */
    public static Map<String, Object> runNestedMethodsSilently(final Object target, final String nestedMethods) {
        final Map<String, Object> returnValues = new HashMap<String, Object>();
        final URIInvoker invoker = new URIInvoker(target);
        for (final String method : nestedMethods.split(";;")) {
            try {
                returnValues.put(method, invoker.invokeNestedMethod(target, method));
            } catch (ReflectiveOperationException | ScriptException | RuntimeException e) {
                returnValues.put(method, null);
            }
        }
        return returnValues;
    }

    /**
     * Disables SSL certificates checks
     *
     * @throws GeneralSecurityException
     *             if anything wrong happens
     */
    public static void disableSslCertificates() throws GeneralSecurityException {
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {
            }
        } };
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    /**
     * Computes a long hash for this string.
     *
     * @param string
     *            the string
     * @return the long hash
     */
    public static long hash(final String string) {
        long h = 1125899906842597L; // prime
        final int len = string.length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    /**
     * For each field that the subclass has, if superclasses also have it, then fetch that value
     * from hierarchy and set here.
     *
     * @param obj
     *            the object
     */
    public static void assignFromHierarchy(final Object obj) {
        final Class<?> dstClass = obj.getClass();
        final Field[] declaredFields = dstClass.getDeclaredFields();
        final List<Field> superFields = allSuperFieldsOf(obj.getClass());
        for (final Field field : declaredFields) {
            for (final Field superField : superFields) {
                if (superField.getName().equals(field.getName())) {
                    try {
                        AccessibleObject.setAccessible(new Field[] { field, superField }, true);
                    } catch (final RuntimeException re) {
                        // do nothing
                    }
                    try {
                        field.set(obj, superField.get(obj));
                    } catch (RuntimeException | ReflectiveOperationException ex) {
                        // do nothing
                    }
                    break;
                }
            }
        }
    }

    /**
     * For each field that the destination has, if source also have it, then fetch that value from
     * source and set in destination.
     *
     * @param dst
     *            the destination object
     * @param src
     *            the source object
     */
    public static void copyFields(final Object dst, final Object src) {
        final List<Field> dstFields = allFieldsOf(dst.getClass());
        final List<Field> srcFields = allFieldsOf(src.getClass());
        for (final Field dstField : dstFields) {
            for (final Field srcField : srcFields) {
                if (srcField.getName().equals(dstField.getName())) {
                    try {
                        AccessibleObject.setAccessible(new Field[] { dstField, srcField }, true);
                    } catch (final RuntimeException re) {
                        // do nothing
                    }
                    try {
                        dstField.set(dst, srcField.get(src));
                    } catch (RuntimeException | ReflectiveOperationException ex) {
                        // do nothing
                    }
                    break;
                }
            }
        }
    }

    /**
     * Enable JMX
     */
    public static int enableJmx() throws IOException {
        final int port = availablePort(9001, 100);
        enableJmx(port);
        return port;
    }

    /**
     * Enable JMX
     *
     * @throws IOException
     *             if anything goes wrong
     */
    public static void enableJmx(final int port) throws IOException {
        LocateRegistry.createRegistry(port);
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put("com.sun.management.jmxremote.authenticate", "false");
        env.put("com.sun.management.jmxremote.local.only", "false");
        env.put("com.sun.management.jmxremote.ssl", "false");
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi");
        final JMXConnectorServer svr = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        svr.start();
    }

    /**
     * Fetch the class definition bytes
     *
     * @param loader
     *            the loader
     * @param clazz
     *            the class
     * @return the bytes
     *
     * @throws IOException
     *             if problem
     */
    public static byte[] fetchClassDefinitionBytes(final Class<?> loader, final Class<?> clazz) throws IOException {
        return IOUtils.toByteArray(loader.getResourceAsStream("/" + clazz.getName().replace(".", "/") + ".class"));
    }

    /**
     * Fetch the class definition bytes
     *
     * @param clazz
     *            the class
     * @return the bytes
     *
     * @throws IOException
     *             if problem
     */
    public static byte[] fetchClassDefinitionBytes(final Class<?> clazz) throws IOException {
        return fetchClassDefinitionBytes(clazz, clazz);
    }

    /**
     * Write class definition to file
     *
     * @param loader
     *            the loader
     * @param clazz
     *            the class
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     * @return the file
     */
    public static File writeClassDefinition(final Class<?> loader, final String folder, final Class<?> clazz)
            throws IOException {
        final File parent = new File(folder + "/" + clazz.getPackage().getName().replace(".", "/"));
        parent.mkdirs();
        final File file = new File(parent, clazz.getSimpleName() + ".class");
        final FileOutputStream fos = new FileOutputStream(file);
        fos.write(fetchClassDefinitionBytes(loader, clazz));
        fos.flush();
        fos.close();
        return file;
    }

    /**
     * Write class definition to file
     *
     * @param clazz
     *            the class
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     * @return the file
     */
    public static File writeClassDefinition(final String folder, final Class<?> clazz) throws IOException {
        final File parent = new File(folder + "/" + clazz.getPackage().getName().replaceAll("\\.", "/"));
        parent.mkdirs();
        final File file = new File(parent, clazz.getSimpleName() + ".class");
        final FileOutputStream fos = new FileOutputStream(file);
        fos.write(fetchClassDefinitionBytes(clazz));
        fos.flush();
        fos.close();
        return file;
    }

    /**
     * Write class definition to file
     *
     * @param loader
     *            the loader
     * @param clazzes
     *            the classes
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     */
    public static void writeClassDefinitions(final Class<?> loader, final String folder, final List<Class<?>> clazzes)
            throws IOException {
        for (final Class<?> clazz : clazzes) {
            writeClassDefinition(loader, folder, clazz);
        }
    }

    /**
     * Write class definition to file
     *
     * @param loader
     *            the loader
     * @param clazzes
     *            the classes
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     */
    public static void writeClassDefinitions(final Class<?> loader, final String folder, final Class<?>... clazzes)
            throws IOException {
        for (final Class<?> clazz : clazzes) {
            writeClassDefinition(loader, folder, clazz);
        }
    }

    /**
     * Write class definition to file
     *
     * @param clazzes
     *            the classes
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     */
    public static void writeClassDefinitions(final String folder, final Class<?>... clazzes) throws IOException {
        for (final Class<?> clazz : clazzes) {
            writeClassDefinition(folder, clazz);
        }
    }

    /**
     * add source to jar stream
     *
     * @param source
     *            the source
     * @param jarStream
     *            the stream
     * @param prefix
     *            the prefix
     * @param jarName
     *            the jar name
     * @throws IOException
     *             if problem
     */
    public static void addSourceToJarStream(final String prefix, final File source, final JarOutputStream jarStream,
            final String jarName) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.endsWith("/")) {
                    name += "/";
                }
                name = name.replace(prefix, "");
                if (!name.isEmpty()) {
                    final JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    jarStream.putNextEntry(entry);
                    jarStream.closeEntry();
                }
                for (final File nestedFile : source.listFiles()) {
                    addSourceToJarStream(prefix, nestedFile, jarStream, jarName);
                }
                return;
            }
            final String name = source.getPath().replace("\\", "/").replace(prefix, "");
            if (!name.equals(jarName + ".jar") && !name.equals("META-INF/MANIFEST.MF")) {
                final JarEntry entry = new JarEntry(name);
                entry.setTime(source.lastModified());
                jarStream.putNextEntry(entry);
                in = new BufferedInputStream(new FileInputStream(source));
                final byte[] buffer = new byte[1024];
                while (true) {
                    final int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    jarStream.write(buffer, 0, count);
                }
                jarStream.closeEntry();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * create jar from source dir into destination dir
     *
     * @param srcDir
     *            the source
     * @param dstDir
     *            the dest
     * @param name
     *            the name
     * @param attributes
     *            the attributes
     * @return the file
     * @throws IOException
     *             if problem
     */
    public static File createJarFromDir(final String srcDir, final String dstDir, final String name,
            final Map<String, String> attributes) throws IOException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (attributes != null) {
            for (final String key : attributes.keySet()) {
                manifest.getMainAttributes().put(new Attributes.Name(key), attributes.get(key));
            }
        }
        final File parent = new File(dstDir);
        parent.mkdirs();
        final File jarFile = new File(dstDir, name + ".jar");
        final JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(jarFile), manifest);
        addSourceToJarStream(srcDir + "/", new File(srcDir), jarStream, name);
        jarStream.close();
        return jarFile;
    }

    /**
     * create jar from class definitions
     *
     * @param folder
     *            the folder
     * @param jarName
     *            the jar name
     * @param attributes
     *            the attributes
     * @param clazzes
     *            the classes
     * @return the file
     * @throws IOException
     *             if problem
     */
    public static File createJarFromClasses(final Class<?> loader, final String folder, final String jarName,
            final Map<String, String> attributes, final List<Class<?>> clazzes) throws IOException {
        writeClassDefinitions(loader, folder, clazzes);
        return createJarFromDir(folder, folder, jarName, attributes);
    }

    /**
     * create jar from class definitions
     *
     * @param folder
     *            the folder
     * @param jarName
     *            the jar name
     * @param attributes
     *            the attributes
     * @param clazzes
     *            the classes
     * @return the file
     * @throws IOException
     *             if problem
     */
    public static File createJarFromClasses(final Class<?> loader, final String folder, final String jarName,
            final Map<String, String> attributes, final Class<?>... clazzes) throws IOException {
        writeClassDefinitions(loader, folder, clazzes);
        return createJarFromDir(folder, folder, jarName, attributes);
    }

    /**
     * create jar from class definitions
     *
     * @param folder
     *            the folder
     * @param jarName
     *            the jar name
     * @param attributes
     *            the attributes
     * @param clazzes
     *            the classes
     * @return the file
     * @throws IOException
     *             if problem
     */
    public static File createJarFromClasses(final String folder, final String jarName,
            final Map<String, String> attributes, final Class<?>... clazzes) throws IOException {
        writeClassDefinitions(folder, clazzes);
        return createJarFromDir(folder, folder, jarName, attributes);
    }

    /**
     * upload to ftp
     *
     * @param ftpUrl
     *            the url
     * @param local
     *            the local
     * @param remote
     *            the remote
     * @return the status
     * @throws IOException
     *             if problem
     */
    public static boolean ftpUpload(final String ftpUrl, final String local, final String remote) throws IOException {
        return ftpUpload(ftpUrl, local, remote, false);
    }

    /**
     * upload to ftp
     *
     * @param ftpUrl
     *            the url
     * @param local
     *            the local
     * @param remote
     *            the remote
     * @param recursive
     *            if recursive
     * @return the status
     * @throws IOException
     *             if problem
     */
    public static boolean ftpUpload(final String ftpUrl, final String local, final String remote,
            final boolean recursive) throws IOException {
        boolean status = false;
        final FTPClient ftpClient = new FTPClient();
        try {
            final URL url = new URL(ftpUrl);
            ftpClient.connect(url.getHost(), url.getPort());
            final String[] userInfo = url.getUserInfo().split(":");
            ftpClient.login(userInfo[0], userInfo[1]);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            status = ftpUpload(ftpClient, new File(local), remote, recursive);
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
        return status;
    }

    /**
     * ftp upload recursive
     *
     * @param ftpClient
     *            the client
     * @param local
     *            the local
     * @param remote
     *            the remote
     * @param recursive
     *            if recursive
     * @return the status
     * @throws IOException
     *             if problem
     */
    public static boolean ftpUpload(final FTPClient ftpClient, final File local, final String remote,
            final boolean recursive) throws IOException {
        boolean status = false;
        if (local.isDirectory()) {
            final File[] files = local.listFiles();
            for (final File file : files) {
                if (file.isDirectory() && recursive || file.isFile()) {
                    status &= ftpUpload(ftpClient, file,
                            remote + (remote.endsWith("/") ? "" : "/") + local.getName() + "/", recursive);
                }
            }
        } else {
            final FileInputStream fis = new FileInputStream(local);
            status = ftpClient.storeFile(remote.endsWith("/") ? remote + local.getName() : remote, fis);
            fis.close();
        }
        return status;
    }

    /**
     * extract a resource
     *
     * @param loader
     *            the loader
     * @param resourceName
     *            the name
     * @param folder
     *            the folder
     * @throws IOException
     *             if problem
     */
    public static void extractResource(final Class<?> loader, final String resourceName, final String folder)
            throws IOException {
        final File parent = new File(folder);
        parent.mkdirs();
        final FileOutputStream fos = new FileOutputStream(new File(parent, resourceName));
        fos.write(getResourceAsBytes(loader, resourceName));
        fos.close();
    }

    /**
     * get loaded classes by name from instrumentation
     *
     * @param instr
     *            the instrumentation
     * @param className
     *            the name
     * @return the classes
     */
    public static List<Class<?>> getLoadedClassesByName(final Instrumentation instr, final String className) {
        final List<Class<?>> classList = new ArrayList<>();
        final Class<?>[] classes = instr.getAllLoadedClasses();
        for (final Class<?> clazz : classes) {
            if (clazz.getName().equals(className)) {
                classList.add(clazz);
            }
        }
        return classList;
    }

    /**
     * Logic transformer
     *
     * @author Dinesh Ilindra
     */
    public static class LogicTransformer implements ClassFileTransformer {

        private final String fqcn, methodName;
        private final int argLength;
        private final String mode, logic;

        /**
         * @param fqcn
         *            the class name
         * @param methodName
         *            the method name
         * @param argLength
         *            the arg length
         * @param mode
         *            the mode
         * @param logic
         *            the logic
         */
        public LogicTransformer(final String fqcn, final String methodName, final int argLength, final String mode,
                final String logic) {
            super();
            this.fqcn = fqcn;
            this.methodName = methodName;
            this.argLength = argLength;
            this.mode = mode;
            this.logic = logic;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
         * java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
         */
        @Override
        public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
                final ProtectionDomain protectionDomain, final byte[] classfileBuffer)
                throws IllegalClassFormatException {
            byte[] byteCode = classfileBuffer;
            if (className.equals(fqcn.replace('.', '/'))) {
                try {
                    final ClassPool classPool = ClassPool.getDefault();
                    final CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    final CtMethod[] methods = ctClass.getDeclaredMethods();
                    for (final CtMethod method : methods) {
                        if (method.getName().equals(methodName) && method.getParameterTypes().length == argLength) {
                            if ("prepend".equalsIgnoreCase(mode)) {
                                method.insertBefore(logic);
                            } else if ("append".equalsIgnoreCase(mode)) {
                                method.insertAfter(logic);
                            } else {
                                method.insertAt(Integer.parseInt(mode), logic);
                            }
                        }
                    }
                    byteCode = ctClass.toBytecode();
                    ctClass.detach();
                } catch (final Exception ex) {
                    // do nothing
                }
            }
            return byteCode;
        }
    }

    /**
     * @param instr
     *            the instrumentation
     * @param fqcn
     *            the class name
     * @param methodName
     *            the method names
     * @param argLength
     *            the arg lengths
     * @param mode
     *            the modes
     * @param logic
     *            the logics
     * @throws Exception
     *             if problem
     */
    public static void addLogic(final Instrumentation instr, final String fqcn, final String methodName,
            final String argLength, final String mode, final String logic) throws Exception {
        boolean methodMulti = false;
        if (methodName.contains(",")) {
            methodMulti = true;
        }
        final String[] methodInfo = methodName.split(",");
        final String[] argLengthInfo = argLength.split(",");
        final String[] modeInfo = mode.split(",");
        final String[] logicInfo = logic.split(",,");
        final List<ClassFileTransformer> transformers = new ArrayList<>();
        for (int index = 0; index < logicInfo.length; index++) {
            final ClassFileTransformer transformer = new LogicTransformer(fqcn,
                    methodMulti ? methodInfo[index] : methodName,
                    Integer.parseInt(methodMulti ? argLengthInfo[index] : argLength), modeInfo[index],
                    logicInfo[index]);
            instr.addTransformer(transformer, true);
            transformers.add(transformer);
        }
        instr.retransformClasses(Class.forName(fqcn));
        for (final ClassFileTransformer transformer : transformers) {
            instr.removeTransformer(transformer);
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package
     * and sub packages.
     *
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     *             if problem
     */
    public static List<Class<?>> getClasses(final String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        final String path = packageName.replace('.', '/');
        final Enumeration<URL> resources = classLoader.getResources(path);
        final List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        final List<Class<?>> classes = new ArrayList<>();
        for (final File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and sub dirs.
     *
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     *             if problem
     */
    public static List<Class<?>> findClasses(final File directory, final String packageName)
            throws ClassNotFoundException {
        final List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        final File[] files = directory.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(
                        Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    /**
     * Decompile class
     */
    public static String decompileClass(final String className) {
        final DecompilerSettings settings = DecompilerSettings.javaDefaults();
        final PlainTextOutput output = new PlainTextOutput();
        Decompiler.decompile(className, output, settings);
        return output.toString();
    }

    /**
     * Encode given text
     *
     * @param text
     *            to encode
     * @return encoded
     */
    public static String encodeForHTML(final String text) {
        return ESAPIEncoder.getInstance().encodeForHTML(text);
    }

    /**
     * Encode the json node text values for HTML
     *
     * @param node
     *            the node to be encoded
     * @return the encoded node
     */
    public static JsonNode encodeForHTML(final JsonNode node) {
        if (node.isArray()) {
            final ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, encodeForHTML(arr.get(i)));
            }
        } else if (node.isObject()) {
            final ObjectNode obj = (ObjectNode) node;
            for (final String key : Lists.newArrayList(obj.fieldNames())) {
                obj.set(key, encodeForHTML(obj.get(key)));
            }
        } else if (node.isTextual()) {
            return new TextNode(encodeForHTML(node.asText()));
        }
        return node;
    }

}
