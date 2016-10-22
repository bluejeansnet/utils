/**
 *
 */
package com.bluejeans.utils.javaagent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

/**
 * @author Dinesh Ilindra
 *
 */
public class BootstrapAgent {

    /**
     * Read manifest property values
     *
     * @throws IOException
     */
    public static List<String> fetchManifestValues(final String key) throws IOException {
        return fetchManifestValues(key, BootstrapAgent.class);
    }

    /**
     * Read manifest property values
     *
     * @throws IOException
     */
    public static List<String> fetchManifestValues(final String key, final Class<?> classz) throws IOException {
        final List<String> values = new ArrayList<>();
        final Enumeration<URL> resources = classz.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            final Manifest manifest = new Manifest(resources.nextElement().openStream());
            final String value = manifest.getMainAttributes().getValue(key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        try {
            Class.forName(fetchManifestValues("Premain-Class").get(0))
                    .getMethod("premain", String.class, Instrumentation.class).invoke(null, agentArgs, inst);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

}
