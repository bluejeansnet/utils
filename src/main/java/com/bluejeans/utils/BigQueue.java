/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;

/**
 * Big queue extension
 *
 * @author Dinesh Ilindra
 * @param <E>
 *            the entity type
 */
public class BigQueue<E> extends com.bluejeans.bigqueue.BigQueue {

    private Class<E> entityType;

    public BigQueue(final String queueDir, final String queueName, final int pageSize) {
        super(queueDir, queueName, pageSize);
    }

    public BigQueue(final String queueDir, final String queueName) {
        super(queueDir, queueName);
    }

    public BigQueue(final String queueDir, final String queueName, final int pageSize, final Class<E> entityType) {
        super(queueDir, queueName, pageSize);
        this.entityType = entityType;
    }

    public BigQueue(final String queueDir, final String queueName, final Class<E> entityType) {
        super(queueDir, queueName);
        this.entityType = entityType;
    }

    @SuppressWarnings("unchecked")
    public void push(final E element) {
        if (entityType == null) {
            synchronized (this) {
                if (entityType == null) {
                    entityType = (Class<E>) element.getClass();
                }
            }
        }
        if (entityType.equals(byte[].class)) {
            enqueue((byte[]) element);
        } else if (entityType.equals(String.class)) {
            enqueue(((String) element).getBytes());
        } else {
            enqueue(SerializationUtils.serialize((Serializable) element));
        }
    }

    public E pop() {
        return element(dequeue());
    }

    @SuppressWarnings("unchecked")
    public E element(final byte[] data) {
        if (data != null) {
            if (entityType == null) {
                synchronized (this) {
                    if (entityType == null) {
                        try {
                            entityType = (Class<E>) SerializationUtils.deserialize(data).getClass();
                        } catch (final Exception ex) {
                            try {
                                entityType = (Class<E>) String.class;
                            } catch (final Exception ex1) {
                                entityType = (Class<E>) byte[].class;
                            }
                        }
                    }
                }
            }
            if (entityType.equals(byte[].class)) {
                return (E) data;
            } else if (entityType.equals(String.class)) {
                return (E) new String(data);
            } else {
                try {
                    return (E) SerializationUtils.deserialize(data);
                } catch (final Exception ex) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public int drainTo(final Collection<? super E> c, final int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        final List<byte[]> elements = dequeueMulti(maxElements);
        for (final byte[] data : elements) {
            c.add(element(data));
        }
        return elements.size();
    }

    public int peekTo(final Collection<? super E> c, final int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        final List<byte[]> elements = peekMulti(maxElements);
        for (final byte[] data : elements) {
            c.add(element(data));
        }
        return elements.size();
    }

}
