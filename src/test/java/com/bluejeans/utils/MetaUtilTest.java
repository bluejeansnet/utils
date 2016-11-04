/**
 *
 */
package com.bluejeans.utils;

import java.io.IOException;

import org.junit.Test;

import com.bluejeans.utils.javaagent.AgentTest;
import com.bluejeans.utils.javaagent.DurationTransformer;

/**
 * @author Dinesh Ilindra
 *
 */
public class MetaUtilTest {

    @Test
    public void testClassBytes() throws IOException {
        System.out.println(MetaUtil.createJarFromClasses(MetaUtil.class,
                "/tmp/test/agent", "agenttest", MetaUtil.createPropsMap("Can-Redefine-Classes=true",
                        "Can-Retransform-Classes=true", "Agent-Class=" + AgentTest.class.getName()),
                AgentTest.class, DurationTransformer.class));
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final MetaUtilTest test = new MetaUtilTest();
        test.testClassBytes();
    }

}
