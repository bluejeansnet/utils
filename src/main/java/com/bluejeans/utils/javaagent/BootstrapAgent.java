/**
 *
 */
package com.bluejeans.utils.javaagent;

import java.lang.instrument.Instrumentation;

import com.jcabi.manifests.Manifests;

/**
 * @author Dinesh Ilindra
 *
 */
public class BootstrapAgent {

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        try {
            Class.forName(Manifests.read("Premain-Class")).getMethod("premain", String.class, Instrumentation.class).invoke(null, agentArgs, inst);
        } catch (final ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

}
