package com.bluejeans.utils.javaagent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class DurationTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        if (className.contains("/bluejeans/")) {
            try {
                final ClassPool classPool = ClassPool.getDefault();
                final CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                final CtMethod[] methods = ctClass.getDeclaredMethods();
                for (final CtMethod method : methods) {
                    try {
                        method.addLocalVariable("startTime", CtClass.longType);
                        method.insertBefore("startTime = System.nanoTime();");
                        method.insertAfter("System.out.println(\"" + method.getLongName() + " : execution Duration "
                                + "(nano sec): \"+ (System.nanoTime() - startTime) );");
                    } catch (final Throwable ex) {
                        ex.printStackTrace();
                    }
                }
                byteCode = ctClass.toBytecode();
                ctClass.detach();
            } catch (final Throwable ex) {
                ex.printStackTrace();
            }
        }
        return byteCode;
    }

}
