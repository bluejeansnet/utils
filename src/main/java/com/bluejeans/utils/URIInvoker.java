/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;

import com.bluejeans.utils.InvokerMXBean.Invoker;
import com.google.gson.internal.Primitives;

/**
 * Invoker based on URI strings.
 *
 * @author Dinesh Ilindra
 */
public class URIInvoker extends Invoker {

    /**
     * The size step suffixes
     */
    public static final String[] SIZE_STEPS = new String[] { "B", "KB", "MB", "GB" };

    private final Map<String, Object> targetMap;

    private final SystemInfo system;

    private final MetaUtil metaUtil;

    private final ConvertUtilsBean convertUtil;

    private ScriptEngine jsEngine;

    /**
     * @param target
     *            the target object
     */
    public URIInvoker(final Object target) {
        this(MetaUtil.createMap("target", target));
    }

    /**
     * @param targets
     *            the targets objects
     */
    public URIInvoker(final Object... targets) {
        this(MetaUtil.classNameObjectMap(targets));
    }

    /**
     * @param targetMap
     *            the target map
     */
    public URIInvoker(final Map<String, Object> targetMap) {
        system = new SystemInfo();
        metaUtil = new MetaUtil();
        this.targetMap = new HashMap<String, Object>();
        this.targetMap.put("system", system);
        this.targetMap.put("metaUtil", metaUtil);
        convertUtil = new ConvertUtilsBean();
        resetJsEngine();
    }

    /**
     * Reset the JS Engine.
     */
    public void resetJsEngine() {
        resetJsEngine("JavaScript");
    }

    /**
     * Reset the JS Engine with given one.
     *
     * @param engineName
     *            the engine name
     */
    public void resetJsEngine(final String engineName) {
        jsEngine = new ScriptEngineManager().getEngineByName(engineName);
        addTargets(targetMap);
    }

    /**
     * Set the target with name.
     *
     * @param name
     *            the taget name
     * @param target
     *            the target object
     */
    public void setTarget(final String name, final Object target) {
        targetMap.put(name, target);
        jsEngine.put(name, target);
    }

    /**
     * Set the target.
     *
     * @param target
     *            the target object
     */
    public void setTarget(final Object target) {
        setTarget("target", target);
    }

    /**
     * Add more targets.
     *
     * @param targetMap
     *            the targets map
     */
    public void addTargets(final Map<String, Object> targetMap) {
        if (targetMap != this.targetMap) {
            this.targetMap.putAll(targetMap);
        }
        for (final Entry<String, Object> targetEntry : this.targetMap.entrySet()) {
            jsEngine.put(targetEntry.getKey(), targetEntry.getValue());
        }
    }

    /**
     * Add more targets.
     *
     * @param targets
     *            the target objects
     */
    public void addTargets(final Object... targets) {
        addTargets(MetaUtil.classNameObjectMap(targets));
    }

    /**
     * Add one target
     *
     * @param targetName
     *            the name
     * @param targetObject
     *            the object
     */
    public void addTarget(final String targetName, final Object targetObject) {
        addTargets(MetaUtil.createMap(targetName, targetObject));
    }

    /**
     * @return the targetMap
     */
    public Map<String, Object> getTargetMap() {
        return targetMap;
    }

    /**
     * @return the convertUtil
     */
    public ConvertUtilsBean getConvertUtil() {
        return convertUtil;
    }

    /**
     * @return the jsEngine
     */
    public ScriptEngine getJsEngine() {
        return jsEngine;
    }

    /**
     * Evaluates the content of the js from given link.
     *
     * @param link
     *            the link
     * @return the output
     * @throws IOException
     *             implicit
     * @throws ScriptException
     *             implicit
     */
    public Object runLink(final String link) throws ScriptException, IOException {
        String url = link;
        if (!link.contains(":/")) {
            url = "http://" + link;
        }
        return run(IOUtils.toString(new URL(url).openStream()));
    }

    /**
     * Evaluates the given script.
     *
     * @param script
     *            the script
     * @return the output
     * @throws ScriptException
     *             implicit
     */
    public Object run(final String script) throws ScriptException {
        return jsEngine.eval(script);
    }

    /**
     * Appends the run value of the given input.
     *
     * @param value
     *            the value
     * @param runLink
     *            the run link
     */
    public void appendRunLink(final Appendable value, final String runLink) {
        try {
            appendValue(value, runLink(runLink));
        } catch (final IOException ioe) {
            appendError(value, ioe);
        } catch (final ScriptException se) {
            appendError(value, se);
        }
    }

    /**
     * Appends the run value of the given input.
     *
     * @param value
     *            the value
     * @param runThis
     *            the run string
     */
    public void appendRunValue(final Appendable value, final String runThis) {
        try {
            appendValue(value, run(runThis));
        } catch (final IOException ioe) {
            appendError(value, ioe);
        } catch (final ScriptException se) {
            appendError(value, se);
        }
    }

    /**
     * Appends the json object value of the supplied comma separated keys.
     *
     * @param value
     *            the value
     * @param keys
     *            the keys
     * @param isProperty
     *            true if property
     */
    public void appendValue(final Appendable value, final String keys, final boolean isProperty) {
        appendValue(value, "target", keys, isProperty);
    }

    /**
     * Appends the json object value of the supplied comma separated keys.
     *
     * @param value
     *            the value
     * @param targetName
     *            the target name
     * @param keys
     *            the keys
     * @param isProperty
     *            true if property
     */
    public void appendValue(final Appendable value, final String targetName, final String keys,
            final boolean isProperty) {
        final String[] keyArray = keys.split(";;");
        try {
            if (keyArray.length == 1) {
                if (isProperty) {
                    value.append(BeanUtils.getProperty(targetMap.get(targetName), keyArray[0]));
                } else {
                    appendValue(value, invokeNestedMethod(targetName, keyArray[0]));
                }
                return;
            }
            value.append("{");
            for (int index = 0; index < keyArray.length; index++) {
                if (index > 0) {
                    value.append(",");
                }
                value.append('\"');
                value.append(keyArray[index].replaceAll("\"", "\\\""));
                value.append("\":");
                value.append('\"');
                if (isProperty) {
                    value.append(BeanUtils.getProperty(targetMap.get(targetName), keyArray[index]));
                } else {
                    appendValue(value, invokeNestedMethod(targetName, keyArray[index]));
                }
                value.append('\"');
            }
            value.append("}");
        } catch (final ReflectiveOperationException roe) {
            appendError(value, roe);
        } catch (final RuntimeException re) {
            appendError(value, re);
        } catch (final IOException ioe) {
            appendError(value, ioe);
        } catch (final ScriptException se) {
            appendError(value, se);
        } catch (final Throwable th) {
            appendError(value, th);
        }
    }

    /**
     * Convert an object to string.
     *
     * @param obj
     *            the object to convert
     * @return the string representation
     * @throws IOException
     *             implicit
     */
    public String convert(final Object obj) throws IOException {
        final StringBuilder value = new StringBuilder();
        appendValue(value, obj);
        return value.toString();
    }

    /**
     * Append the objects string representation to the given appendable.
     *
     * @param value
     *            the value
     * @param obj
     *            the object
     * @throws IOException
     *             implicit
     */
    public void appendValue(final Appendable value, final Object obj) throws IOException {
        if (obj == null) {
            value.append("[no return value]");
        } else {
            if (obj.getClass().isArray()) {
                final int len = Array.getLength(obj);
                for (int index = 0; index < len; index++) {
                    appendValue(value, Array.get(obj, index));
                    value.append(SystemInfo.LINE_SEPARATOR);
                }
            } else if (obj instanceof Iterator<?>) {
                final Iterator<?> iter = (Iterator<?>) obj;
                while (iter.hasNext()) {
                    appendValue(value, iter.next());
                    value.append(SystemInfo.LINE_SEPARATOR);
                }
            } else if (obj instanceof Iterable<?>) {
                final Iterable<?> iter = (Iterable<?>) obj;
                for (final Object element : iter) {
                    appendValue(value, element);
                    value.append(SystemInfo.LINE_SEPARATOR);
                }
            } else if (obj instanceof Map<?, ?>) {
                final Map<?, ?> map = (Map<?, ?>) obj;
                for (final Entry<?, ?> entry : map.entrySet()) {
                    appendValue(value, entry);
                    value.append(SystemInfo.LINE_SEPARATOR);
                }
            } else {
                value.append(convertUtil.convert(obj).replaceAll("\"", "\\\""));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.bluejeans.utils.InvokerMXBean.Invoker#runThis(java.lang.String, java.lang.String[])
     */
    @Override
    public Object runThis(final String name, final String... args) throws Exception {
        final String[] data = name.split("/", 2);
        return invokeNestedMethod(data[0], data[1]);
    }

    /**
     * Invoke a nested method/property on target object.
     *
     * @param targetName
     *            the target name
     * @param nestedMethod
     *            the nested method
     * @return the result
     * @throws ReflectiveOperationException
     *             implicit
     * @throws ScriptException
     *             rare case
     */
    public Object invokeNestedMethod(final String targetName, final String nestedMethod)
            throws ReflectiveOperationException, ScriptException {
        return invokeNestedMethod(targetMap.get(targetName), nestedMethod);
    }

    /**
     * Invoke a nested method/property on target object.
     *
     * @param targetObj
     *            the target object
     * @param nestedMethod
     *            the nested method
     * @return the result
     * @throws ReflectiveOperationException
     *             implicit
     * @throws ScriptException
     *             rare case
     */
    public Object invokeNestedMethod(final Object targetObj, final String nestedMethod)
            throws ReflectiveOperationException, ScriptException {
        final String[] methods = nestedMethod.split("\\.\\.");
        Object currentTarget = targetObj;
        for (final String method : methods) {
            if (method.startsWith("$")) {
                final String[] fieldInfo = method.split("~~", 2);
                Field field = null;
                if (fieldInfo[0].startsWith("$$")) {
                    field = MetaUtil.findFirstFieldByType(currentTarget.getClass(), fieldInfo[0].substring(2));
                } else {
                    field = MetaUtil.findFirstField(currentTarget.getClass(), fieldInfo[0].substring(1));
                }
                field.setAccessible(true);
                if (fieldInfo.length > 1) {
                    final Class<?> fieldType = Primitives.wrap(field.getType());
                    if (fieldType.equals(String.class)) {
                        field.set(currentTarget, fieldInfo[1]);
                    } else if (fieldInfo[1].startsWith("$")) {
                        final Object result = run(fieldInfo[1].substring(1));
                        if (fieldType.equals(result.getClass())) {
                            field.set(currentTarget, result);
                        } else {
                            field.set(currentTarget,
                                    fieldType.getMethod("valueOf", String.class).invoke(null, result.toString()));
                        }
                    } else {
                        field.set(currentTarget,
                                fieldType.getMethod("valueOf", String.class).invoke(null, fieldInfo[1]));
                    }
                }
                currentTarget = field.get(currentTarget);
                continue;
            } else if (method.equals("_interfaces")) {
                currentTarget = ClassUtils.getAllInterfaces(currentTarget.getClass());
                continue;
            } else if (method.equals("_extends")) {
                currentTarget = MetaUtil.allExtendedClassesOf(currentTarget.getClass());
                continue;
            } else if (method.equals("_fields")) {
                currentTarget = MetaUtil.allFieldsOf(currentTarget.getClass());
                continue;
            } else if (method.equals("_methods")) {
                currentTarget = MetaUtil.allMethodsOf(currentTarget.getClass());
                continue;
            } else if (method.equals("_length")) {
                currentTarget = Array.getLength(currentTarget);
                continue;
            }
            final String[] methodInfo = method.split("~~", 2);
            String methodName = methodInfo[0];
            if (methodName.startsWith("~")) {
                methodName = methodName.substring(1);
            }
            if (methodName.startsWith("~$")) {
                methodName = methodName.substring(2);
            }
            Object[] args = new Object[0];
            Object[] longArgs = new Object[0];
            String[] argStrs = new String[0];
            boolean nullPresent = false;
            if (methodInfo.length > 1) {
                argStrs = methodInfo[1].split("::");
                args = new Object[argStrs.length];
                longArgs = new Object[argStrs.length];
                for (int index = 0; index < args.length; index++) {
                    try {
                        try {
                            longArgs[index] = Long.valueOf(argStrs[index]);
                        } catch (final NumberFormatException nfe) {
                            // do nothing.
                        }
                        args[index] = Integer.valueOf(argStrs[index]);
                    } catch (final NumberFormatException nfe) {
                        if ("{NULL}".equals(argStrs[index])) {
                            longArgs[index] = args[index] = argStrs[index] = null;
                            nullPresent = true;
                        } else if ("{true}".equals(argStrs[index])) {
                            args[index] = Boolean.TRUE;
                        } else if ("{false}".equals(argStrs[index])) {
                            args[index] = Boolean.FALSE;
                        } else if (argStrs[index].startsWith("$")) {
                            args[index] = run(argStrs[index].substring(1));
                        } else {
                            args[index] = argStrs[index];
                        }
                    }
                }
            }
            if (nullPresent || methodInfo[0].startsWith("~")) {
                Object methodObject = null;
                if (methodInfo[0].startsWith("~$")) {
                    methodObject = run(methodName);
                }
                try {
                    currentTarget = findMethod(currentTarget.getClass(), methodName, args.length).invoke(currentTarget,
                            args);
                } catch (final IllegalArgumentException iae) {
                    try {
                        currentTarget = findMethod(currentTarget.getClass(), methodName, args.length)
                                .invoke(currentTarget, longArgs);
                    } catch (final IllegalArgumentException iae1) {
                        currentTarget = findMethod(currentTarget.getClass(), methodName, args.length)
                                .invoke(currentTarget, (Object[]) argStrs);
                    }
                } catch (final NullPointerException npe) {
                    final Object parent = currentTarget;
                    String parentType = "";
                    try {
                        currentTarget = MethodUtils.invokeMethod(currentTarget, "get",
                                Integer.parseInt(methodObject == null ? methodName : methodObject.toString()));
                        parentType = "coll";
                    } catch (final NumberFormatException | ReflectiveOperationException ex) {
                        try {
                            currentTarget = MethodUtils.invokeMethod(currentTarget, "get",
                                    methodObject == null ? methodName : methodObject);
                            parentType = "coll";
                        } catch (final ReflectiveOperationException roe) {
                            currentTarget = Array.get(currentTarget,
                                    Integer.parseInt(methodObject == null ? methodName : methodObject.toString()));
                            parentType = "array";
                        }
                    }
                    if (methodInfo.length > 1) {
                        final Class<?> valueType = Primitives.wrap(currentTarget.getClass());
                        if (valueType.equals(String.class)) {
                            currentTarget = methodInfo[1];
                        } else if (methodInfo[1].startsWith("$")) {
                            final Object result = run(methodInfo[1].substring(1));
                            if (valueType.equals(result.getClass())) {
                                currentTarget = result;
                            } else {
                                currentTarget = valueType.getMethod("valueOf", String.class).invoke(null,
                                        result.toString());
                            }
                        } else {
                            currentTarget = valueType.getMethod("valueOf", String.class).invoke(null, methodInfo[1]);
                        }
                        switch (parentType) {
                            case "array":
                                Array.set(parent,
                                        Integer.parseInt(methodObject == null ? methodName : methodObject.toString()),
                                        currentTarget);
                                break;
                            case "coll":
                                try {
                                    MethodUtils.invokeMethod(parent, "set",
                                            new Object[] { Integer.parseInt(
                                                    methodObject == null ? methodName : methodObject.toString()),
                                                    currentTarget });
                                } catch (final Exception roe) {
                                    MethodUtils.invokeMethod(parent, "put", new Object[] {
                                            methodObject == null ? methodName : methodObject, currentTarget });
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            } else {
                try {
                    currentTarget = MethodUtils.invokeMethod(currentTarget, methodName, args);
                } catch (final NoSuchMethodException nsmex) {
                    try {
                        currentTarget = MethodUtils.invokeMethod(currentTarget, methodName, longArgs);
                    } catch (final NoSuchMethodException nsmex1) {
                        try {
                            currentTarget = MethodUtils.invokeMethod(currentTarget, methodName, argStrs);
                        } catch (final NoSuchMethodException nsme) {
                            try {
                                currentTarget = MethodUtils.invokeMethod(currentTarget,
                                        "get" + methodName.substring(0, 1).toUpperCase(Locale.getDefault())
                                                + methodName.substring(1),
                                        args);
                            } catch (final NoSuchMethodException nsme1) {
                                try {
                                    currentTarget = MethodUtils.invokeMethod(currentTarget, "get",
                                            Integer.parseInt(methodName));
                                } catch (final NumberFormatException nfe) {
                                    currentTarget = MethodUtils.invokeMethod(currentTarget, "get", methodName);
                                } catch (final ReflectiveOperationException roe) {
                                    currentTarget = Array.get(currentTarget, Integer.parseInt(methodName));
                                }
                            }
                        }
                    }
                }
            }
        }
        return currentTarget;
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
     * @return the method / null if not found
     */
    public Method findMethod(final Class<?> classz, final String methodName, final int totalArguments) {
        return MetaUtil.findFirstMethod(classz, methodName, totalArguments);
    }

    /**
     * Builds error message from given exception.
     *
     * @param value
     *            the value
     * @param th
     *            the throwable
     */
    public void appendError(final Appendable value, final Throwable th) {
        try {
            value.append("{\"error\":\"");
            value.append(th.toString());
            final StringWriter exWriter = new StringWriter();
            th.printStackTrace(new PrintWriter(exWriter));
            value.append(" -->> ");
            value.append(exWriter.toString());
            value.append("\"}");
        } catch (final IOException e) {
            // do nothing.
        }
    }

    /**
     * Gives the string representation of the size of the object represented by the property over
     * the target.
     *
     * @param targetName
     *            the target name
     * @param propertyName
     *            the property name
     * @return the size string
     * @throws NoSuchMethodException
     *             implicit
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     */
    public String toSize(final String targetName, final String propertyName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return URIInvoker.readableSize(sizeofProperty(targetName, propertyName));
    }

    /**
     * Calculates the size of the object represented by the property over the target.
     *
     * @param targetName
     *            the target name
     * @param propertyName
     *            the property name
     * @return the size
     * @throws NoSuchMethodException
     *             implicit
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     */
    public long sizeofProperty(final String targetName, final String propertyName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return URIInvoker.sizeof(PropertyUtils.getProperty(targetMap.get(targetName), propertyName));
    }

    /**
     * Calculates the size of the the given object.
     *
     * @param obj
     *            the object
     * @return the size
     * @throws IllegalAccessException
     *             implicit
     */
    public static long sizeof(final Object obj) throws IllegalAccessException {
        return ObjectSizeCalculator.sizeOf(obj);
    }

    /**
     * Returns a readable size representation of the given bytes size.
     *
     * @param size
     *            the size
     * @return the readable representation of given size
     */
    public static String readableSize(final long size) {
        long newSize = size;
        final StringBuilder builder = new StringBuilder();
        long remainder = 0;
        for (final String element : URIInvoker.SIZE_STEPS) {
            remainder = newSize % 1024;
            newSize = newSize / 1024;
            builder.insert(0, '.');
            builder.insert(0, element);
            builder.insert(0, remainder);
            if (newSize == 0) {
                break;
            }
        }
        return builder.toString();
    }

}
