/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;

/**
 * Invoker MBean
 *
 * @author Dinesh Ilindra
 */
public interface InvokerMXBean {

    /**
     * Print the value by running any on this.
     *
     * @param name
     *            the name
     * @param args
     *            the aeguments
     * @return the print result
     */
    String printThis(final String name, final String... args);

    /**
     * The invoker basic implementation.
     *
     * @author Dinesh Ilindra
     */
    public static class Invoker implements InvokerMXBean {

        /**
         * Registers this as MBean.
         */
        public Invoker() {
            try {
                MetaUtil.registerAsMBean(this);
            } catch (final Exception jme) {
                //jme.printStackTrace();
            }
        }

        /**
         * Run any on this.
         *
         * @param name
         *            the name
         * @param args
         *            the arguments
         * @return the result
         * @throws Exception
         *             any problem
         */
        public Object runThis(final String name, final String... args) throws Exception {
            int len = 0;
            if (args != null) {
                len = args.length;
            }
            final Object[] invokeArgs = new Object[len];
            final Method method = MetaUtil.findFirstMethod(this.getClass(), name, len);
            for (int index = 0; index < len; index++) {
                Class<?> paramType = method.getParameterTypes()[index];
                try {
                    if (paramType.isPrimitive()) {
                        paramType = ClassUtils.primitiveToWrapper(paramType);
                    }
                    invokeArgs[index] = paramType.getMethod("valueOf", String.class).invoke(null, args[index]);
                } catch (final NoSuchMethodException nsme) {
                    invokeArgs[index] = paramType.getConstructor(String.class).newInstance(args[index]);
                }
            }
            return method.invoke(this, invokeArgs);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.bjn.utils.InvokerMBean#printThis(java.lang.String, java.lang.String[])
         */
        @Override
        public String printThis(final String name, final String... args) {
            final StringBuilder val = new StringBuilder();
            try {
                final Object result = runThis(name, args);
                if (result.getClass().isArray()) {
                    for (int index = 0; index < Array.getLength(result); index++) {
                        val.append(Array.get(result, index));
                        val.append("\n");
                    }
                } else {
                    val.append(runThis(name, args));
                }
            } catch (final Exception ex) {
                final StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                val.append(sw);
            }
            return val.toString();
        }

    }
}
