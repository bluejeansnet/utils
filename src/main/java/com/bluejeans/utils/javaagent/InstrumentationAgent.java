package com.bluejeans.utils.javaagent;

import java.lang.instrument.Instrumentation;

/**
 * Simple instrumentation agent which exposes a static reference to Instrumentation object
 *
 * @author Dinesh Ilindra
 */
public class InstrumentationAgent {

    private static Instrumentation instrumentation;

    /**
     * @return the instrumentation
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * agent main
     *
     * @param agentArgs
     * @param inst
     */
    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * agent premain
     *
     * @param agentArgs
     * @param inst
     */
    public static void premain(final String agentArgs, final Instrumentation inst) {
        instrumentation = inst;
    }

}
