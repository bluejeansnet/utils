/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;

/**
 * The tester class for testing all the Get and Set methods in the classes in
 * the given package / for the list of specified objects.
 *
 * @author Dinesh Ilindra
 */
public class SuperTester {

    /**
     * The list of the objects to test.
     */
    private final transient Object[] objects;

    /**
     * Constructs the Tester with supplied list of objects.
     *
     * @param objects
     *            The list of objects
     * @throws IOException
     *             implicit
     * @throws InstantiationException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     */
    public SuperTester(final Object[] objects) throws IllegalAccessException, InstantiationException, IOException {
        this.objects = Arrays.copyOf(objects, objects.length);
    }

    /**
     * Constructs the Tester with supplied package and class names.
     *
     * @param packageName
     *            The package name
     * @param classNames
     *            The list of class names
     * @throws InstantiationException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     * @throws IOException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws InvocationTargetException
     *             implicit
     */
    public SuperTester(final String packageName, final String[] classNames) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException, IOException, InvocationTargetException,
            NoSuchMethodException, NoSuchFieldException {
        objects = new Object[classNames.length];
        for (int index = 0; index < classNames.length; index++) {
            objects[index] = createInstance(Class.forName(packageName + "." + classNames[index]));
        }
    }

    /**
     * Constructs the Tester with supplied package and class names.
     *
     * @param classes
     *            The list of class
     * @throws InstantiationException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     * @throws IOException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws InvocationTargetException
     *             implicit
     */
    public SuperTester(final Class<?>... classes) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        objects = new Object[classes.length];
        for (int index = 0; index < classes.length; index++) {
            objects[index] = createInstance(classes[index]);
        }
    }

    /**
     * This method invokes all the getter methods.
     *
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws InstantiationException
     *             implicit
     */
    public void testGetters() throws IllegalAccessException, InvocationTargetException, InstantiationException,
            NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        for (final Object object : objects) {
            final Method[] methods = object.getClass().getMethods();
            for (final Method method : methods) {
                if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                    invokeMethod(method, object);
                }
            }
        }
    }

    /**
     * This method invokes all the setter methods.
     *
     * @throws InstantiationException
     *             implicit
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     */
    public void testSetters() throws IllegalAccessException, InvocationTargetException, InstantiationException,
            NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        for (final Object object : objects) {
            final Method[] methods = object.getClass().getMethods();
            for (final Method method : methods) {
                if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                    invokeMethod(method, object);
                }
            }
        }
    }

    /**
     * This method invokes all the available public methods.
     *
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws InstantiationException
     *             implicit
     */
    public void testPublics() throws IllegalAccessException, InvocationTargetException, InstantiationException,
            NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        for (final Object object : objects) {
            final Method[] methods = object.getClass().getMethods();
            for (final Method method : methods) {
                invokeMethod(method, object);
            }
        }
    }

    /**
     * Invokes the specified method on given object by constructing param
     * instances as necessary.
     *
     * @param method
     *            the method
     * @param object
     *            the object
     */
    private void invokeMethod(final Method method, final Object object) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (belogToClass(method, object.getClass()) && !isTestMethod(method)) {
                        final Class<?>[] paramTypes = method.getParameterTypes();
                        final Object[] params = new Object[paramTypes.length];
                        for (int index = 0; index < params.length; index++) {
                            params[index] = createInstance(paramTypes[index]);
                        }
                        method.invoke(object, params);
                    }
                } catch (final Exception ex) {
                    // do nothing.
                }
            }
        }).start();
    }

    /**
     * Checks whether the given method is present in specified class definition.
     *
     * @param method
     *            the method
     * @param classs
     *            the class
     * @return true if belongs
     */
    private boolean belogToClass(final Method method, final Class<?> classs) {
        boolean value = false;
        final Method[] declaredMethods = classs.getDeclaredMethods();
        for (final Method declared : declaredMethods) {
            if (declared.equals(method)) {
                value = true;
                break;
            }
        }
        return value;
    }

    /**
     * To test if the method has any annotations containing "test" within their
     * class names.
     *
     * @param method
     *            the method
     * @return true if test one
     */
    private boolean isTestMethod(final Method method) {
        boolean value = false;
        /*
         * Annotation[] annotations = method.getAnnotations(); for (Annotation
         * annotation : annotations) { if
         * (annotation.getClass().getSimpleName().
         * toLowerCase(Locale.getDefault()).indexOf("test") >= 0) { value =
         * true; break; } }
         */
        value = method.getName().toLowerCase(Locale.getDefault()).indexOf("test") >= 0;
        return value;
    }

    /**
     * Creates a new instance of the supplied class.
     *
     * @param classs
     *            the class
     * @throws InvocationTargetException
     *             implicit
     * @throws IllegalAccessException
     *             implicit
     * @throws InstantiationException
     *             implicit
     * @throws NoSuchFieldException
     *             implicit
     * @throws NoSuchMethodException
     *             implicit
     * @throws ClassNotFoundException
     *             implicit
     */
    private Object createInstance(final Class<?> classs) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        Object obj = null;
        if (classs.isArray()) {
            final Class<?> component = classs.getComponentType();
            // obj = Array.newInstance(component, 0);
            obj = Array.newInstance(component, 1);
            Array.set(obj, 0, createInstance(component));
        } else if (classs.isPrimitive()) {
            final Class<?> boxClass = ClassUtils.wrapperToPrimitive(classs);
            final Class<?>[] primitiveTypes = new Class<?>[1];
            primitiveTypes[0] = classs;
            final Object[] primitiveValues = new Object[1];
            primitiveValues[0] = 0;
            obj = boxClass.getConstructor(primitiveTypes).newInstance(primitiveValues);
        } else if (classs.isAssignableFrom(List.class)) {
            obj = new ArrayList<Object>();
        } else if (classs.isAssignableFrom(Set.class)) {
            obj = new HashSet<Object>();
        } else if (classs.isAssignableFrom(Map.class)) {
            obj = new HashMap<Object, Object>();
        } else {
            Constructor<?> construct = null;
            try {
                construct = classs.getConstructor();
            } catch (final NoSuchMethodException nsme) {
                // do nothing.
            }
            final Constructor<?>[] constructs = classs.getConstructors();
            if (constructs.length > 0) {
                if (construct == null) {
                    construct = constructs[0];
                }
                final Class<?>[] paramTypes = construct.getParameterTypes();
                final Object[] params = new Object[paramTypes.length];
                for (int index = 0; index < params.length; index++) {
                    params[index] = createInstance(paramTypes[index]);
                }
                obj = construct.newInstance(params);
            }
        }
        return obj;
    }

    /**
     * Test all.
     */
    public void testAll() {
        try {
            testGetters();
        } catch (final Exception ex) {
            // do nothing.
        }
        try {
            testSetters();
        } catch (final Exception ex) {
            // do nothing.
        }
        try {
            testPublics();
        } catch (final Exception ex) {
            // do nothing.
        }
    }

    /**
     * Blindly tests all the objects created from the classes from the given
     * package.
     *
     * @param packageName
     *            the package name
     * @throws Exception
     *             implicit
     */
    public static void testAll(final String packageName) throws Exception {
        final SuperTester tester = new SuperTester(getClasses(packageName));
        tester.testAll();
    }

    /**
     * Blindly tests all the objects.
     *
     * @param objects
     *            the objects
     * @throws Exception
     *             implicit
     */
    public static void testObjects(final Object... objects) throws Exception {
        final SuperTester tester = new SuperTester(objects);
        tester.testAll();
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and sub packages.
     *
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     *             implicit
     * @throws IOException
     *             implicit
     */
    private static Class<?>[] getClasses(final String packageName) throws ClassNotFoundException, IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        final String path = packageName.replace('.', '/');
        final Enumeration<URL> resources = classLoader.getResources(path);
        final List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        for (final File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and sub
     * dirs.
     *
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     *             implicit
     */
    private static List<Class<?>> findClasses(final File directory, final String packageName)
            throws ClassNotFoundException {
        final List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        final File[] files = directory.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

}
