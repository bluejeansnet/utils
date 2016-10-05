package com.bluejeans.utils;

import static com.google.common.collect.Lists.newLinkedList;

import java.io.IOException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluejeans.bigqueue.BigArray;

/**
 * Bulk operation utils for queuing elements and performing bulk operation on them at once, interval
 * based
 *
 * @author Dinesh Ilindra
 * @param <E>
 *            the entity type
 */
public class BulkOperationUtil<E> {

    public static enum BulkStatus {

        /**
         * do bulk error
         */
        DO_BULK_ERROR,

        /**
         * Internal error
         */
        INTERNAL_ERROR,

        /**
         * item added to queue
         */
        QUEUE_ADD,
    }

    private static Logger logger = LoggerFactory.getLogger(BulkOperationUtil.class);

    private final BlockingQueue<E> queue;

    private BigQueue<E> bigQueue;

    private boolean fileBased;

    private boolean peekEnabled;

    private boolean waitEnabled;

    private final String queueDir;

    private final String queueName;

    private final long bigQueueTimerInterval;

    private final Timer bigQueueTimer = new Timer();

    private final long bulkPollInterval;

    private final BulkOperation<E> bulkOperation;

    private final Doer doer;

    private int batchSize;

    private int minBatchSize = 100;

    private final int bulkExecutorSize;

    private final int bulkExecutorQueueCapacity;

    private final ThreadPoolExecutor bulkExecutor;

    private final Runnable bulkRunnable = new Runnable() {
        @Override
        public void run() {
            doBulk();
        }
    };

    private final AtomicLong queueAddFailCount;

    private final EnumCounter<BulkStatus> bulkStatusCounter = new EnumCounter<BulkStatus>(BulkStatus.class);

    private boolean stopped = false;

    private boolean parallel = false;

    private boolean bulkRetryEnabled = true;

    private int bulkRetryCount = Integer.MAX_VALUE - 1;

    private BulkOperationUtil(final int bulkPollIntervalSecs, final int capacity, final String queueDir,
            final String queueName, final long bigQueueTimerInterval, final BulkOperation<E> bulkOperation,
            final int batchSize, final int bulkExecutorSize, final int bulkExecutorQueueCapacity) {
        this.bulkPollInterval = bulkPollIntervalSecs * 1000;
        this.queue = new LinkedBlockingQueue<E>(capacity);
        this.bulkOperation = bulkOperation;
        this.batchSize = batchSize;
        this.bulkExecutorSize = bulkExecutorSize;
        this.bulkExecutorQueueCapacity = bulkExecutorQueueCapacity;
        this.doer = new Doer();
        queueAddFailCount = new AtomicLong();
        bulkExecutor = new ThreadPoolExecutor(bulkExecutorSize, bulkExecutorSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(bulkExecutorQueueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.queueDir = queueDir;
        this.queueName = queueName;
        this.bigQueueTimerInterval = bigQueueTimerInterval;
        if (queueDir != null) {
            try {
                bigQueue = new BigQueue<E>(this.queueDir, this.queueName, BigArray.MINIMUM_DATA_PAGE_SIZE);
                bigQueueTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        bigQueue.gc();
                    }
                }, 0, bigQueueTimerInterval);
            } catch (final RuntimeException rex) {
                logger.warn("Problem creating big queue", rex);
            }
        }
    }

    /**
     * Create based on parameters
     *
     * @param bulkPollInterval
     *            the poll interval
     * @param queueDir
     *            the queue directory
     * @param queueName
     *            the queue name
     * @param bulkOperation
     *            the bulk operation itself
     * @param batchSize
     *            the drain batch size
     * @param bulkExecutorSize
     *            the bulk executor size
     * @param <E>
     *            the entity type
     * @return the created utility
     */
    public static <E> BulkOperationUtil<E> create(final int bulkPollInterval, final int capacity, final String queueDir,
            final String queueName, final long bigQueueTimerInterval, final BulkOperation<E> bulkOperation,
            final int batchSize, final int bulkExecutorSize, final int bulkExecutorQueueCapacity) {
        final BulkOperationUtil<E> bulkOperationUtil = new BulkOperationUtil<E>(bulkPollInterval, capacity, queueDir,
                queueName, bigQueueTimerInterval, bulkOperation, batchSize, bulkExecutorSize,
                bulkExecutorQueueCapacity);
        bulkOperationUtil.doer.start();
        return bulkOperationUtil;
    }

    /**
     * Create based on parameters
     *
     * @param bulkPollInterval
     *            the poll interval
     * @param capacity
     *            the queue capacity
     * @param bulkOperation
     *            the bulk operation itself
     * @param <E>
     *            the entity type
     * @return the created utility
     */
    public static <E> BulkOperationUtil<E> create(final int bulkPollInterval, final int capacity,
            final BulkOperation<E> bulkOperation, final int batchSize, final int bulkExecutorSize,
            final int bulkExecutorQueueCapacity) {
        return create(bulkPollInterval, capacity, null, null, 300000, bulkOperation, batchSize, bulkExecutorSize,
                bulkExecutorQueueCapacity);
    }

    /**
     * Create based on parameters
     *
     * @param bulkPollInterval
     *            the poll interval
     * @param capacity
     *            the queue capacity
     * @param bulkOperation
     *            the bulk operation itself
     * @param <E>
     *            the entity type
     * @return the created utility
     */
    public static <E> BulkOperationUtil<E> create(final int bulkPollInterval, final int capacity,
            final BulkOperation<E> bulkOperation) {
        return create(bulkPollInterval, capacity, null, null, 300000, bulkOperation, 1000, 1, 1);
    }

    /**
     * The interface for the bulk operation
     *
     * @author Dinesh Ilindra
     * @param <E>
     *            the entity type
     */
    public interface BulkOperation<E> {

        /**
         * Specify what to do with the bulk collection
         *
         * @param c
         *            the bulk vollection
         */
        void doBulk(Collection<E> c);
    }

    /**
     * The facilitator for the bulkiness
     *
     * @author Dinesh Ilindra
     */
    private class Doer extends Thread {

        /**
         * Construct and set the thread name
         */
        public Doer() {
            setName("bulk-operation-worker");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            while (true) {
                // do bulk
                if (stopped) {
                    if (getQueueSize() == 0) {
                        break;
                    } else {
                        logger.warn("DO NOT KILL, WILL STOP AFTER PROCESSING " + queue.size() + " MESSAGES");
                    }
                }
                if (parallel) {
                    bulkExecutor.execute(bulkRunnable);
                } else {
                    doBulk();
                }
                try {
                    if (getQueueSize() < minBatchSize && !stopped) {
                        sleep(bulkPollInterval);
                    }
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.warn("STOPPED");
        }
    };

    private boolean bulkInternal(final Collection<E> coll) {
        boolean success = false;
        if (bulkRetryEnabled) {
            int retryCount = 0;
            do {
                try {
                    bulkOperation.doBulk(coll);
                    success = true;
                } catch (final NullPointerException npe) {
                    success = true;
                    bulkStatusCounter.incrementEventCount(BulkStatus.INTERNAL_ERROR);
                    logger.error("Internal Error", npe);
                } catch (final RuntimeException ex) {
                    bulkStatusCounter.incrementEventCount(BulkStatus.DO_BULK_ERROR);
                    logger.error("Error in bulk operation {}", ex);
                }
                retryCount++;
            } while (!success && retryCount <= bulkRetryCount);
        } else {
            try {
                bulkOperation.doBulk(coll);
                success = true;
            } catch (final RuntimeException ex) {
                bulkStatusCounter.incrementEventCount(BulkStatus.DO_BULK_ERROR);
                logger.error("Error in bulk operation {}", ex);
            }
        }
        return success;
    }

    /**
     * Do the bulk operation.
     */
    public void doBulk() {
        final Collection<E> coll = newLinkedList();
        if (bigQueue == null) {
            queue.drainTo(coll, batchSize);
            if (!coll.isEmpty()) {
                bulkInternal(coll);
            }
        } else if (peekEnabled) {
            int peeked = 0;
            if (fileBased) {
                if (queue.size() > 0) {
                    queue.drainTo(coll, batchSize);
                }
                if (bigQueue.size() > 0) {
                    peeked = bigQueue.peekTo(coll, batchSize - coll.size());
                }
            } else {
                if (bigQueue.size() > 0) {
                    peeked = bigQueue.peekTo(coll, batchSize);
                }
                if (queue.size() > 0) {
                    queue.drainTo(coll, batchSize - coll.size());
                }
            }
            if (!coll.isEmpty()) {
                if (bulkInternal(coll)) {
                    bigQueue.dequeueMulti(peeked);
                }
            }
        } else {
            if (fileBased) {
                if (queue.size() > 0) {
                    queue.drainTo(coll, batchSize);
                }
                if (bigQueue.size() > 0) {
                    bigQueue.drainTo(coll, batchSize - coll.size());
                }
            } else {
                if (bigQueue.size() > 0) {
                    bigQueue.drainTo(coll, batchSize);
                }
                if (queue.size() > 0) {
                    queue.drainTo(coll, batchSize - coll.size());
                }
            }
            if (!coll.isEmpty()) {
                bulkInternal(coll);
            }
        }
    }

    /**
     * Get the queue size
     *
     * @return the queue size
     */
    public long getQueueSize() {
        if (fileBased && bigQueue != null) {
            return bigQueue.size();
        } else {
            return queue.size();
        }
    }

    /**
     * Add the given element to queue
     *
     * @param e
     *            the element to add
     */
    public void add(final E e) {
        if (fileBased && bigQueue != null) {
            bigQueue.push(e);
            bulkStatusCounter.incrementEventCount(BulkStatus.QUEUE_ADD);
        } else {
            boolean inserted = false;
            if (waitEnabled) {
                try {
                    queue.put(e);
                    inserted = true;
                } catch (final InterruptedException e1) {
                    inserted = false;
                }
            } else {
                inserted = queue.offer(e);
            }
            if (inserted) {
                bulkStatusCounter.incrementEventCount(BulkStatus.QUEUE_ADD);
            } else {
                queueAddFailCount.incrementAndGet();
                logger.error("Failed to insert into queue", e);
            }
        }
    }

    /**
     * Stop the utility and operate on any remaining elements in queue.
     */
    public void stop() {
        stopped = true;
        if (bigQueue != null) {
            bigQueueTimer.cancel();
            bigQueue.gc();
            try {
                bigQueue.close();
            } catch (final IOException ioe) {
                logger.warn("Problem closing big queue", ioe);
            }
        }
    }

    /**
     * @return the queue
     */
    public BlockingQueue<E> getQueue() {
        return queue;
    }

    /**
     * @return the bigQueue
     */
    public BigQueue<E> getBigQueue() {
        return bigQueue;
    }

    /**
     * @return the fileBased
     */
    public boolean isFileBased() {
        return fileBased;
    }

    /**
     * @param fileBased
     *            the fileBased to set
     */
    public void setFileBased(final boolean fileBased) {
        this.fileBased = fileBased;
    }

    /**
     * @return the peekEnabled
     */
    public boolean isPeekEnabled() {
        return peekEnabled;
    }

    /**
     * @param peekEnabled
     *            the peekEnabled to set
     */
    public void setPeekEnabled(final boolean peekEnabled) {
        this.peekEnabled = peekEnabled;
    }

    /**
     * @return the waitEnabled
     */
    public boolean isWaitEnabled() {
        return waitEnabled;
    }

    /**
     * @param waitEnabled
     *            the waitEnabled to set
     */
    public void setWaitEnabled(final boolean waitEnabled) {
        this.waitEnabled = waitEnabled;
    }

    /**
     * @return the queueDir
     */
    public String getQueueDir() {
        return queueDir;
    }

    /**
     * @return the queueName
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return the bigQueueTimerInterval
     */
    public long getBigQueueTimerInterval() {
        return bigQueueTimerInterval;
    }

    /**
     * @return the bigQueueTimer
     */
    public Timer getBigQueueTimer() {
        return bigQueueTimer;
    }

    /**
     * @return the bulkPollInterval
     */
    public long getBulkPollInterval() {
        return bulkPollInterval;
    }

    /**
     * @return the bulkOperation
     */
    public BulkOperation<E> getBulkOperation() {
        return bulkOperation;
    }

    /**
     * @return the doer
     */
    public Doer getDoer() {
        return doer;
    }

    /**
     * @return the queueAddFailCount
     */
    public AtomicLong getQueueAddFailCount() {
        return queueAddFailCount;
    }

    /**
     * @return the stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * @return the batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @param batchSize
     *            the batchSize to set
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @return the bulkExecutorSize
     */
    public int getBulkExecutorSize() {
        return bulkExecutorSize;
    }

    /**
     * @return the bulkExecutorQueueCapacity
     */
    public int getBulkExecutorQueueCapacity() {
        return bulkExecutorQueueCapacity;
    }

    /**
     * @return the bulkExecutor
     */
    public ThreadPoolExecutor getBulkExecutor() {
        return bulkExecutor;
    }

    /**
     * @return the bulkRunnable
     */
    public Runnable getBulkRunnable() {
        return bulkRunnable;
    }

    /**
     * @return the parallel
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * @param parallel
     *            the parallel to set
     */
    public void setParallel(final boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * @return the minBatchSize
     */
    public int getMinBatchSize() {
        return minBatchSize;
    }

    /**
     * @param minBatchSize
     *            the minBatchSize to set
     */
    public void setMinBatchSize(final int minBatchSize) {
        this.minBatchSize = minBatchSize;
    }

    /**
     * @return the bulkRetryEnabled
     */
    public boolean isBulkRetryEnabled() {
        return bulkRetryEnabled;
    }

    /**
     * @param bulkRetryEnabled
     *            the bulkRetryEnabled to set
     */
    public void setBulkRetryEnabled(final boolean bulkRetryEnabled) {
        this.bulkRetryEnabled = bulkRetryEnabled;
    }

    /**
     * @return the bulkRetryCount
     */
    public int getBulkRetryCount() {
        return bulkRetryCount;
    }

    /**
     * @param bulkRetryCount
     *            the bulkRetryCount to set
     */
    public void setBulkRetryCount(final int bulkRetryCount) {
        this.bulkRetryCount = bulkRetryCount;
    }

    /**
     * @return the bulkStatusCounter
     */
    public EnumCounter<BulkStatus> getBulkStatusCounter() {
        return bulkStatusCounter;
    }

}
