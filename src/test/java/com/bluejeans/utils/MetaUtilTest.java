/**
 *
 */
package com.bluejeans.utils;

import java.io.IOException;

import org.junit.Test;

import com.bluejeans.utils.javaagent.AgentTest;
import com.bluejeans.utils.javaagent.DurationTransformer;
import com.bluejeans.utils.javaagent.InstrumentationAgent;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        MetaUtil.createJarFromClasses(MetaUtil.class, "/tmp/test/agent1", "istrAgent",
                MetaUtil.createPropsMap("Can-Redefine-Classes=true", "Can-Retransform-Classes=true",
                        "Can-Set-Native-Method-Prefix=true", "Agent-Class=" + InstrumentationAgent.class.getName(),
                        "Premain-Class=" + InstrumentationAgent.class.getName()),
                InstrumentationAgent.class);
    }

    @Test
    public void testEsapi() throws Exception {
        System.out.println(MetaUtil.encodeForHTML(new ObjectMapper().readTree("{\"k1\":\"v1 <hi>.....\"}")));
    }

    @Test
    public void testDecompile() {
        System.out.println(MetaUtil.decompileClass("java.lang.Appendable"));
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final MetaUtilTest test = new MetaUtilTest();
        // test.testClassBytes();
        // test.testEsapi();
        // test.testEsapi();
        // test.testFtp();
        test.testDecompile();
    }

}
