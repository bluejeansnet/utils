/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Object Size calculator
 *
 * @author Dinesh Ilindra
 */
public class ObjectSizeCalculator {

    private static final int REFERENCE_SIZE;

    private static final int HEADER_SIZE;

    private static final int LONG_SIZE = 8;

    private static final int INT_SIZE = 4;

    private static final int BYTE_SIZE = 1;

    private static final int BOOLEAN_SIZE = 1;

    private static final int CHAR_SIZE = 2;

    private static final int SHORT_SIZE = 2;

    private static final int FLOAT_SIZE = 4;

    private static final int DOUBLE_SIZE = 8;

    private static final int ALIGNMENT = 8;

    static {
        try {
            if (System.getProperties().get("java.vm.name").toString().contains("64")) {
                // java.vm.name is something like
                // "Java HotSpot(TM) 64-Bit Server VM"
                REFERENCE_SIZE = 8;
                HEADER_SIZE = 16;
            } else {
                REFERENCE_SIZE = 4;
                HEADER_SIZE = 8;
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new AssertionError(ex);
        }
    }

    /**
     * calculates the size of given object
     *
     * @param o
     *            the object
     * @return the size
     * @throws IllegalAccessException implicit
     */
    public static long sizeOf(final Object o) throws IllegalAccessException {
        return sizeOf(o, new HashSet<ObjectWrapper>());
    }

    private static long sizeOf(final Object o, final Set<ObjectWrapper> visited) throws IllegalAccessException {
        if (o == null) {
            return 0;
        }
        final ObjectWrapper objectWrapper = new ObjectWrapper(o);
        if (visited.contains(objectWrapper)) {
            // We have reference graph with cycles.
            return 0;
        }
        visited.add(objectWrapper);
        long size = HEADER_SIZE;
        final Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            if (clazz == long[].class) {
                final long[] objs = (long[]) o;
                size += objs.length * LONG_SIZE;
            } else if (clazz == int[].class) {
                final int[] objs = (int[]) o;
                size += objs.length * INT_SIZE;
            } else if (clazz == byte[].class) {
                final byte[] objs = (byte[]) o;
                size += objs.length * BYTE_SIZE;
            } else if (clazz == boolean[].class) {
                final boolean[] objs = (boolean[]) o;
                size += objs.length * BOOLEAN_SIZE;
            } else if (clazz == char[].class) {
                final char[] objs = (char[]) o;
                size += objs.length * CHAR_SIZE;
            } else if (clazz == short[].class) {
                final short[] objs = (short[]) o;
                size += objs.length * SHORT_SIZE;
            } else if (clazz == float[].class) {
                final float[] objs = (float[]) o;
                size += objs.length * FLOAT_SIZE;
            } else if (clazz == double[].class) {
                final double[] objs = (double[]) o;
                size += objs.length * DOUBLE_SIZE;
            } else {
                final Object[] objs = (Object[]) o;
                for (final Object obj : objs) {
                    size += sizeOf(obj, visited) + REFERENCE_SIZE;
                }
            }
            size += INT_SIZE;
        } else {
            final Field[] fields = o.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (Modifier.isStatic(fields[i].getModifiers())) {
                    continue;
                }
                if (!fields[i].isAccessible()) {
                    fields[i].setAccessible(true);
                }
                final String fieldType = fields[i].getGenericType().toString();
                if (fieldType.equals("long")) {
                    size += LONG_SIZE;
                } else if (fieldType.equals("int")) {
                    size += INT_SIZE;
                } else if (fieldType.equals("byte")) {
                    size += BYTE_SIZE;
                } else if (fieldType.equals("boolean")) {
                    size += BOOLEAN_SIZE;
                } else if (fieldType.equals("char")) {
                    size += CHAR_SIZE;
                } else if (fieldType.equals("short")) {
                    size += SHORT_SIZE;
                } else if (fieldType.equals("float")) {
                    size += FLOAT_SIZE;
                } else if (fieldType.equals("double")) {
                    size += DOUBLE_SIZE;
                } else {
                    size += sizeOf(fields[i].get(o), visited) + REFERENCE_SIZE;
                }
            }
        }
        if (size % ALIGNMENT != 0) {
            size = ALIGNMENT * (size / ALIGNMENT + 1);
        }
        return size;
    }

    /**
     * The object wrapper
     *
     * @author Dinesh Ilindra
     */
    private static final class ObjectWrapper {

        private final Object object;

        /**
         * The default one
         */
        public ObjectWrapper(final Object object) {
            this.object = object;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != ObjectWrapper.class) {
                return false;
            }
            return object == ((ObjectWrapper) obj).object;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + System.identityHashCode(object);
            return hash;
        }
    }
}
