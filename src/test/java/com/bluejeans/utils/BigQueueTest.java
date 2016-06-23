/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bluejeans.utils.BigQueue;

/**
 * Big Queue test
 *
 * @author Dinesh Ilindra
 */
public class BigQueueTest {

    private final BigQueue<Long> queue = new BigQueue<>("/tmp/bigqueue", "test2", Long.class);

    // @Test
    public void testBigQueue() throws Exception {
        pushToBigQueue();
        // peekFromBigQueue();
        // pullFromBigQueue();
        compareBQ();
    }

    public void pushToBigQueue() throws Exception {
        for (int i = 0; i < 1000; i++) {
            queue.push(new Date().getTime());
            // Thread.sleep(1000);
        }
        queue.close();
    }

    public void compareBQ() {
        final List<Long> peeked = new ArrayList<>();
        final List<Long> drained = new ArrayList<>();
        queue.peekTo(peeked, 1000);
        queue.drainTo(drained, 1000);
        System.out.println(peeked.equals(drained));
    }

    public void pullFromBigQueue() throws Exception {
        final List<Long> drained = new ArrayList<>();
        queue.drainTo(drained, 1000);
        queue.close();
    }

    public void peekFromBigQueue() throws Exception {
        final List<Long> peeked = new ArrayList<>();
        queue.peekTo(peeked, 1000);
        System.out.println(peeked.size());
        System.out.println(peeked.get(peeked.size() - 1));
        queue.close();
    }

    public static void main(final String args[]) throws Exception {
        new BigQueueTest().testBigQueue();
    }
}
