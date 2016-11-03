/**
 *
 */
package com.bluejeans.utils;

import java.io.IOException;

import org.junit.Test;

import com.bluejeans.utils.javaagent.BootstrapAgent;

/**
 * @author Dinesh Ilindra
 *
 */
public class MetaUtilTest {

    @Test
    public void testClassBytes() throws IOException {
        System.out.println(MetaUtil.createJarFromClasses("/tmp/test/classes", "testJar",
                MetaUtil.createPropsMap("Can-Redefine-Classes=true", "Can-Retransform-Classes=true",
                        "Premain-Class=" + BootstrapAgent.class.getName(),
                        "Agent-Class=" + BootstrapAgent.class.getName()),
                MetaUtil.class, BootstrapAgent.class));

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
