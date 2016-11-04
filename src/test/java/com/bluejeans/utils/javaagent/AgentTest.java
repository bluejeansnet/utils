/**
 *
 */
package com.bluejeans.utils.javaagent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import com.bluejeans.utils.MetaUtil;

/**
 * @author Dinesh Ilindra
 *
 */
public class AgentTest {

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
        try {
            apply();
        } catch (final UnmodifiableClassException e) {
            e.printStackTrace();
        }
    }

    public static void apply() throws UnmodifiableClassException {
        if (instrumentation != null) {
            System.out.println(instrumentation.isModifiableClass(String.class));
            System.out.println(instrumentation.isNativeMethodPrefixSupported());
            System.out.println(instrumentation.isRedefineClassesSupported());
            System.out.println(instrumentation.isRetransformClassesSupported());
            instrumentation.addTransformer(new DurationTransformer(), true);
            instrumentation.retransformClasses(MetaUtil.class);
        }
    }

    public static void main(final String[] args) {
    }

}
