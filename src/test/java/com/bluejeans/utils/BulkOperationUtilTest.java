/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.BeforeClass;

import com.bluejeans.utils.BulkOperationUtil;
import com.bluejeans.utils.BulkOperationUtil.BulkOperation;

/**
 * Test for Bulk operation util test
 *
 * @author Dinesh Ilindra
 */
public class BulkOperationUtilTest {

    public static class Node implements Serializable {
        private static final long serialVersionUID = 5741290093411296714L;
        private final Date date = new Date();
        private final long now = System.currentTimeMillis();
        @Override
        public String toString() {
            return now + " ++ " + date;
        }
    }

    // @Test
    public void testBulkOperationUtils() throws Exception {
        final Map<Long, Integer> times1 = new ConcurrentHashMap<>();
        final Map<Long, Integer> times2 = new ConcurrentHashMap<>();
        final long instant = System.nanoTime();
        final BulkOperationUtil<String> normal = BulkOperationUtil.create(1, 1000000, new BulkOperation<String>() {
            @Override
            public void doBulk(final Collection<String> c) {
                times2.put(System.nanoTime() - instant, c.size());
            }
        });
        final BulkOperationUtil<String> fileBased = BulkOperationUtil.create(1, 1000000, "/tmp/filequeue", "test",
                2000, new BulkOperation<String>() {
                    @Override
                    public void doBulk(final Collection<String> c) {
                        times1.put(System.nanoTime() - instant, c.size());
                    }
                }, 1000, 1, 1);
        fileBased.setFileBased(true);
        fileBased.setPeekEnabled(true);
        // fileBased.getBigQueue().gc();
        final long t1 = System.currentTimeMillis();
        new Thread() {
            /*
             * (non-Javadoc)
             *
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                for (int i = 0; i < 500000; i++) {
                    final Node n = new Node();
                    fileBased.add(n.toString());
                }
                System.out.println(System.currentTimeMillis() - t1);
            }
        }.start();
        for (int i = 0; i < 1000000; i++) {
            final Node n = new Node();
            normal.add(n.toString());
        }
        System.out.println(System.currentTimeMillis() - t1);
        fileBased.getBigQueue().gc();
        final long t2 = System.currentTimeMillis();
        for (int i = 0; i < 500000; i++) {
            final Node n = new Node();
            fileBased.add(n.toString());
        }
        System.out.println(System.currentTimeMillis() - t2);
        Thread.sleep(4000);
        System.out.println(times1.size() + " -> " + times1);
        System.out.println(times2.size() + " -> " + times2);
        fileBased.getBigQueue().gc();
        synchronized (this) {
            wait();
        }
    }

    public void testRetry() throws Exception {
        final BulkOperationUtil<String> fileBased = BulkOperationUtil.create(1, 1000000, "/tmp/filequeue", "test",
                2000, new BulkOperation<String>() {
                    private int count = 0;
                    @Override
                    public void doBulk(final Collection<String> c) {
                        System.out.println(count + ") I am called - doBulk with " + c.size());
                        count++;
                        if (count < 100) {
                            throw new RuntimeException("Failed");
                        }
                    }
                }, 1000, 1, 1);
        fileBased.setBulkRetryEnabled(true);
        fileBased.setFileBased(true);
        fileBased.setPeekEnabled(true);
        for (int i = 0; i < 1000; i++) {
            final Node n = new Node();
            fileBased.add(n.toString());
        }
        synchronized (this) {
            wait();
        }
    }

    public static void main(final String[] args) throws Exception {
        // new BulkOperationUtilTest().testBulkOperationUtils();
        new BulkOperationUtilTest().testRetry();
    }

}
