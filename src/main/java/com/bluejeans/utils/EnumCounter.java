/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Maintains all the counts related to given enum.
 *
 * @author Dinesh Ilindra
 */
public class EnumCounter<E extends Enum<E>> {

    /**
     * Map for maintaining enum counts.
     *
     * @author Dinesh Ilindra
     */
    public class EventCountMap extends EnumMap<E, AtomicLong> {

        private static final long serialVersionUID = -6906468090357760718L;

        /**
         * The <tt>Class</tt> object for the enum type of all the keys of this
         * map.
         *
         * @serial
         */
        private final Class<E> keyType;

        public EventCountMap(final Class<E> keyType) {
            super(keyType);
            this.keyType = keyType;
        }

        private boolean autoCreateValue = false;

        /**
         * @return the autoCreateValue
         */
        public boolean isAutoCreateValue() {
            return autoCreateValue;
        }

        /**
         * @param autoCreateValue
         *            the autoCreateValue to set
         */
        public void setAutoCreateValue(final boolean autoCreateValue) {
            this.autoCreateValue = autoCreateValue;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.EnumMap#get(java.lang.Object)
         */
        @Override
        public AtomicLong get(final Object key) {
            AtomicLong value = super.get(key);
            if (value == null && autoCreateValue) {
                value = getS(key);
            }
            return value;
        }

        /**
         * Get synchronized.
         *
         * @param key
         *            the key
         * @return the value
         */
        public synchronized AtomicLong getS(final Object key) {
            return getN(key);
        }

        /**
         * Get the value, if null reset it.
         *
         * @param key
         *            the key
         * @return the value
         */
        public AtomicLong getN(final Object key) {
            AtomicLong value = super.get(key);
            if (value == null && autoCreateValue) {
                value = new AtomicLong();
                put(getEnum(key.toString()), value);
            }
            return value;
        }

        /**
         * Get the value, the old way.
         *
         * @param key
         *            the key
         * @return the value
         */
        public AtomicLong getO(final Object key) {
            return super.get(key);
        }

        /**
         * get the enum for the given string
         *
         * @param enumString
         *            the enum as string
         * @return the enum
         */
        public E getEnum(final String enumString) {
            return Enum.valueOf(keyType, enumString);
        }

        /**
         * get the count of given enum stirng
         *
         * @param event
         *            the event
         * @return the value
         */
        public long getValue(final String event) {
            return getO(getEnum(event)).get();
        }

    }

    private final EventCountMap eventCounts, secEventCounts;

    /**
     * initializes all the counts.
     *
     * @param keyType
     *            the key type
     */
    @SuppressWarnings("unchecked")
    public EnumCounter(final Class<E> keyType) {
        eventCounts = new EventCountMap(keyType);
        secEventCounts = new EventCountMap(keyType);
        try {
            for (final E type : (E[]) keyType.getMethod("values").invoke(null)) {
                eventCounts.put(type, new AtomicLong());
                secEventCounts.put(type, new AtomicLong());
            }
        } catch (final ReflectiveOperationException roe) {
            // do nothing
        }
    }

    /**
     * Resets all the counts to zeros.
     */
    public void reset() {
        for (final Object key : eventCounts.keySet()) {
            eventCounts.get(key).set(0);
        }
        for (final Object key : secEventCounts.keySet()) {
            secEventCounts.get(key).set(0);
        }
    }

    /**
     * Resets the secondary event counts.
     */
    public void resetSecEventCounts() {
        for (final Object key : secEventCounts.keySet()) {
            secEventCounts.get(key).set(0);
        }
    }

    /**
     * @return the eventCounts
     */
    public EventCountMap getEventCounts() {
        return eventCounts;
    }

    /**
     * @return the secEventCounts
     */
    public EventCountMap getSecEventCounts() {
        return secEventCounts;
    }

    /**
     * get the event count
     *
     * @param event
     *            the event
     * @return the value
     */
    public long getValue(final String event) {
        return eventCounts.getValue(event);
    }

    /**
     * Increments an event count.
     *
     * @param event
     *            the event
     */
    public void incrementEventCount(final E event) {
        incrementEventCount(event, 1);
    }

    /**
     * Increments an event count by given value.
     *
     * @param event
     *            the event
     * @param count
     *            the count
     */
    public void incrementEventCount(final E event, final int count) {
        eventCounts.get(event).addAndGet(count);
        secEventCounts.get(event).addAndGet(count);
    }

}
