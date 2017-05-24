/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils.theon;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluejeans.utils.BulkOperationUtil;
import com.bluejeans.utils.BulkOperationUtil.BulkOperation;
import com.bluejeans.utils.EnumCounter;

/**
 * Theon client to post using bulk operations
 *
 * @author Dinesh Ilindra
 * @param <E>
 *            the entity type to post
 */
public class TheonClient<E extends Serializable> {

    private static final Logger logger = LoggerFactory.getLogger(TheonClient.class);

    /**
     * Theon event enum
     */
    public static enum TheonStatus {

        /**
         * message added here
         */
        MESSAGE_ADDED,

        /**
         * http post success
         */
        HTTP_POST_SUCCESS,

        /**
         * http post failure
         */
        HTTP_POST_FAILURE,

        /**
         * message send success
         */
        MESSAGE_SEND_SUCCESS,

        /**
         * message send failure
         */
        MESSAGE_SEND_FAILURE,

        /**
         * Post size limit reached
         */
        POST_SIZE_LIMIT_REACHED,

    }

    /**
     * Encapsulates a theon message with topic and key
     *
     * @author Dinesh Ilindra
     * @param <E>
     *            the message type
     */
    public static class TheonMessage<E extends Serializable> implements Serializable {

        private static final long serialVersionUID = 339681800724722659L;

        private final String topic, key;
        private final E message;

        /**
         * Construct with topic and message, key will be empty string
         *
         * @param topic
         *            the topic
         * @param message
         *            the message
         */
        public TheonMessage(final String topic, final E message) {
            this(topic, "", message);
        }

        /**
         * Construct with topic, key and message
         *
         * @param topic
         *            the topic
         * @param key
         *            the key
         * @param message
         *            the message
         */
        public TheonMessage(final String topic, final String key, final E message) {
            this.topic = topic;
            this.key = key;
            this.message = message;
        }

        /**
         * @return the topic
         */
        public String getTopic() {
            return topic;
        }

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the message
         */
        public E getMessage() {
            return message;
        }

    }

    private String theonUrl;

    private URI theonUri;

    private String username;

    private String password;

    private final BulkOperation<TheonMessage<E>> bulkOperation = new BulkOperation<TheonMessage<E>>() {
        /*
         * (non-Javadoc)
         *
         * @see com.bluejeans.common.utils.BulkOperationUtil. BulkOperation
         * #doBulk (java.util.Collection)
         */
        @Override
        public void doBulk(final Collection<TheonMessage<E>> coll) {
            final Map<String, Map<String, List<E>>> messageMap = new HashMap<String, Map<String, List<E>>>();
            boolean status = true;
            for (final TheonMessage<E> tm : coll) {
                if(tm.topic!=null) {
                    if (!messageMap.containsKey(tm.topic)) {
                        messageMap.put(tm.topic, new HashMap<String, List<E>>());
                    }
                    if (!messageMap.get(tm.topic).containsKey(tm.key)) {
                        messageMap.get(tm.topic).put(tm.key, new ArrayList<E>());
                    }
                    messageMap.get(tm.topic).get(tm.key).add(tm.message);
                }
            }
            if (postPerKey) {
                for (final String topic : messageMap.keySet()) {
                    for (final String key : messageMap.get(topic).keySet()) {
                        status &= postMessagesNow(topic, key, messageMap.get(topic).get(key));
                    }
                }
            } else {
                for (final String topic : messageMap.keySet()) {
                    status = postMessagesNow(topic, defaultKey, messageMap.get(topic));
                }
            }
            if (!status) {
                throw new RuntimeException("Error in posting messages");
            }
        }
    };

    private BulkOperationUtil<TheonMessage<E>> bulkOperationUtil;

    private BulkOperationUtil<TheonMessage<E>> parallelBulkOperationUtil;

    private boolean fileBasedQueue = false;

    private boolean peekEnabled = false;

    private final boolean waitEnabled = false;

    private String queueDir;

    private String queueName;

    private long bigQueueTimerInterval = 30000;

    private boolean parallelEnabled = false;

    private int queueCapacity = 5000;

    private int bulkPollIntervalSecs = 5;

    private int maxPostEntitySize = 1024 * 1024 * 10;

    private CloseableHttpClient httpClient;

    private RequestConfig requestConfig;

    private boolean initialized = false;

    private EnumCounter<TheonStatus> theonCounter;

    private int bulkMessageSize = 200;

    private int httpConnPoolSize = 10;

    private boolean postPerKey = false;

    private String defaultKey = "";

    private boolean gzipEnabled = false;

    private boolean certValidationDisabled = false;

    private boolean stringType = false;

    /**
     * The default one
     */
    public TheonClient() {
        super();
    }

    /**
     * With url and credentials
     *
     * @param theonUrl
     *            the theon url not ending with slash
     * @param username
     *            the username
     * @param password
     *            the password
     */
    public TheonClient(final String theonUrl, final String username, final String password) {
        this.theonUrl = theonUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Initialized the bulk util and the http client
     *
     * @throws URISyntaxException
     *             if invalid theon url
     */
    @PostConstruct
    public void init() throws URISyntaxException {
        if (StringUtils.isEmpty(theonUrl) || StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return;
        }
        String usedUrl = theonUrl;
        if (usedUrl.charAt(usedUrl.length() - 1) == '/') {
            usedUrl = usedUrl.substring(0, usedUrl.length() - 1);
        }
        theonUri = new URI(usedUrl);
        final String host = theonUri.getHost();
        final int port = theonUri.getPort();
        PoolingHttpClientConnectionManager cm;
        final SSLContextBuilder sslBuilder = SSLContexts.custom();
        if (certValidationDisabled) {
            try {
                sslBuilder.loadTrustMaterial(null, new TrustStrategy() {
                    @Override
                    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                        return true;
                    }});
                final SSLContext sslContext = sslBuilder.build();
                final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                        NoopHostnameVerifier.INSTANCE);
                final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory> create().register("https", sslsf)
                        .register("http", new PlainConnectionSocketFactory()).build();
                cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                logger.warn("Problem in disabling SSL certificate checks", ex);
                cm = new PoolingHttpClientConnectionManager();
            }
        } else {
            cm = new PoolingHttpClientConnectionManager();
        }
        cm.setMaxTotal(httpConnPoolSize);
        cm.setDefaultMaxPerRoute(httpConnPoolSize);
        final BasicCredentialsProvider credentialProvider = new BasicCredentialsProvider();
        credentialProvider.setCredentials(new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password));
        final HttpClientBuilder builder = HttpClientBuilder.create().setConnectionManager(cm)
                .setRetryHandler(DefaultHttpRequestRetryHandler.INSTANCE)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(60000).setSoKeepAlive(true).build())
                .setDefaultCredentialsProvider(credentialProvider);
        httpClient = builder.build();
        final RequestConfig.Builder config = RequestConfig.copy(RequestConfig.DEFAULT);
        config.setConnectionRequestTimeout(60000);
        config.setSocketTimeout(60000);
        requestConfig = config.build();
        bulkOperationUtil = BulkOperationUtil.create(bulkPollIntervalSecs, queueCapacity, queueDir, queueName,
                bigQueueTimerInterval, bulkOperation, bulkMessageSize, 1, 1, false);
        parallelBulkOperationUtil = BulkOperationUtil.create(bulkPollIntervalSecs, queueCapacity, queueDir, queueName,
                bigQueueTimerInterval, bulkOperation, bulkMessageSize, httpConnPoolSize, httpConnPoolSize, false);
        bulkOperationUtil.setFileBased(fileBasedQueue);
        parallelBulkOperationUtil.setFileBased(fileBasedQueue);
        bulkOperationUtil.setPeekEnabled(peekEnabled);
        parallelBulkOperationUtil.setPeekEnabled(peekEnabled);
        bulkOperationUtil.setWaitEnabled(waitEnabled);
        parallelBulkOperationUtil.setWaitEnabled(waitEnabled);
        parallelBulkOperationUtil.setParallel(true);
        theonCounter = new EnumCounter<TheonStatus>(TheonStatus.class);
        final TheonMessage<E> tm = new TheonMessage<E>(null, null);
        bulkOperationUtil.dummyElementIs(tm);
        parallelBulkOperationUtil.dummyElementIs(tm);
        if(stringType) {
            final TheonMessage<String> stm = new TheonMessage<String>("", "");
            bulkOperationUtil.entityTypeIs((Class<TheonMessage<E>>) stm.getClass());
            parallelBulkOperationUtil.entityTypeIs((Class<TheonMessage<E>>) stm.getClass());
        } else {
            bulkOperationUtil.entityTypeIs((Class<TheonMessage<E>>) tm.getClass());
            parallelBulkOperationUtil.entityTypeIs((Class<TheonMessage<E>>) tm.getClass());
        }
        bulkOperationUtil.start();
        parallelBulkOperationUtil.start();
        initialized = true;
    }

    /**
     * Stop the bulk util and close the http client
     */
    @PreDestroy
    public void destroy() {
        if (!initialized) {
            return;
        }
        bulkOperationUtil.stop();
        parallelBulkOperationUtil.stop();
        try {
            httpClient.close();
        } catch (final IOException ioe) {
            logger.warn("problem closing the HTTP client", ioe);
        }
    }

    /**
     * Post the given list of messages now
     *
     * @param topic
     *            the topic
     * @param defaultKey
     *            the defaultKey
     * @param messagesMap
     *            the key messages map
     */
    public boolean postMessagesNow(final String topic, final String defaultKey, final Map<String, List<E>> messagesMap) {
        if (!initialized) {
            return false;
        }
        boolean status = true;
        StringBuilder builder = new StringBuilder();
        int count = 0;
        int total = 0;
        int msgSize = 0;
        for (final List<E> msgs : messagesMap.values()) {
            msgSize += msgs.size();
        }
        for (final String key : messagesMap.keySet()) {
            final List<E> messages = messagesMap.get(key);
            String postKey = defaultKey;
            if(key == null) {
                logger.warn("Key is 'null' for below messages - \n\t" + messages);
            } else {
                postKey = key.indexOf(':') == -1?key:key.replaceAll(":", "_");
            }
            for (final E message : messages) {
                builder.append(postKey);
                builder.append(':');
                builder.append(message.toString());
                builder.append("\r\n");
                count++;
                total++;
                if (builder.length() > maxPostEntitySize || total == msgSize) {
                    if (builder.length() > maxPostEntitySize) {
                        theonCounter.incrementEventCount(TheonStatus.POST_SIZE_LIMIT_REACHED);
                    }
                    String url = theonUri + "/" + topic;
                    if (StringUtils.isNotBlank(defaultKey)) {
                        url += "/" + defaultKey;
                    }
                    final HttpPost post = new HttpPost(url);
                    post.setConfig(requestConfig);
                    CloseableHttpResponse response = null;
                    try {
                        if (gzipEnabled) {
                            post.setEntity(new GzipCompressingEntity(new StringEntity(builder.toString())));
                        } else {
                            post.setEntity(new StringEntity(builder.toString()));
                        }
                        response = httpClient.execute(post);
                        theonCounter.incrementEventCount(TheonStatus.HTTP_POST_SUCCESS);
                        theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_SUCCESS, count);
                    } catch (final Exception ex) {
                        theonCounter.incrementEventCount(TheonStatus.HTTP_POST_FAILURE);
                        theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_FAILURE, count);
                        logger.error(
                                "Could not bulk post with length " + builder.length() + " to - "
                                        + post.getRequestLine(), ex);
                        status = false;
                    } finally {
                        try {
                            response.close();
                        } catch (final Exception ex) {
                            // do nothing
                        }
                    }
                    builder = new StringBuilder();
                    count = 0;
                }
            }
        }
        return status;
    }

    /**
     * Post all the given messages now for the given key
     *
     * @param topic
     *            the topic
     * @param key
     *            the key
     * @param messages
     *            the messages
     */
    @SuppressWarnings("unchecked")
    public void postMessagesNow(final String topic, final String key, final E... messages) {
        postMessagesNow(topic, key, Arrays.asList(messages));
    }

    /**
     * Post the given list of messages now for specific key
     *
     * @param topic
     *            the topic
     * @param key
     *            the key
     * @param messages
     *            the message list
     */
    public boolean postMessagesNow(final String topic, final String key, final List<E> messages) {
        if (!initialized) {
            return false;
        }
        boolean status = true;
        StringBuilder builder = new StringBuilder();
        int count = 0;
        int total = 0;
        for (final E message : messages) {
            builder.append(':');
            builder.append(message.toString());
            count++;
            total++;
            if (builder.length() > maxPostEntitySize || total == messages.size()) {
                if (builder.length() > maxPostEntitySize) {
                    theonCounter.incrementEventCount(TheonStatus.POST_SIZE_LIMIT_REACHED);
                }
                String url = theonUri + "/" + topic;
                if (StringUtils.isNotBlank(key)) {
                    url += "/" + key;
                }
                final HttpPost post = new HttpPost(url);
                post.setConfig(requestConfig);
                CloseableHttpResponse response = null;
                try {
                    post.setEntity(new StringEntity(builder.toString()));
                    response = httpClient.execute(post);
                    theonCounter.incrementEventCount(TheonStatus.HTTP_POST_SUCCESS);
                    theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_SUCCESS, count);
                } catch (final Exception ex) {
                    theonCounter.incrementEventCount(TheonStatus.HTTP_POST_FAILURE);
                    theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_FAILURE, count);
                    logger.error(
                            "Could not bulk post with length " + builder.length() + " to - " + post.getRequestLine(),
                            ex);
                    status = false;
                } finally {
                    try {
                        response.close();
                    } catch (final Exception ex) {
                        // do nothing
                    }
                }
                builder = new StringBuilder();
                count = 0;
            } else {
                builder.append("\r\n");
            }
        }
        return status;
    }

    /**
     * Post the given message now
     *
     * @param topic
     *            the topic
     * @param key
     *            the key
     * @param message
     *            the message
     */
    public void postMessageNow(final String topic, final String key, final E message) {
        if (!initialized) {
            return;
        }
        final HttpPost post = new HttpPost(theonUri + "/" + topic + "/" + key);
        post.setConfig(requestConfig);
        CloseableHttpResponse response = null;
        try {
            post.setEntity(new StringEntity(':' + message.toString()));
            response = httpClient.execute(post);
            theonCounter.incrementEventCount(TheonStatus.HTTP_POST_SUCCESS);
            theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_SUCCESS);
        } catch (final IOException ioe) {
            theonCounter.incrementEventCount(TheonStatus.HTTP_POST_FAILURE);
            theonCounter.incrementEventCount(TheonStatus.MESSAGE_SEND_FAILURE);
            logger.error("Could not post to - " + post.getRequestLine(), ioe);
        } finally {
            try {
                response.close();
            } catch (final Exception ex) {
                // do nothing
            }
        }
    }

    /**
     * Add the message to the bulk queue
     *
     * @param topic
     *            the topic
     * @param key
     *            the key
     * @param message
     *            the message
     */
    public void postMessage(final String topic, final String key, final E message) {
        postMessage(topic, key, message, parallelEnabled);
    }

    /**
     * Add the message to the bulk queue
     *
     * @param topic
     *            the topic
     * @param key
     *            the key
     * @param message
     *            the message
     * @param parallel
     *            is message posted to parallel bulk op / not
     */
    public void postMessage(final String topic, final String key, final E message, final boolean parallel) {
        if (!initialized) {
            return;
        }
        if (parallel) {
            parallelBulkOperationUtil.add(new TheonMessage<E>(topic, key, message));
        } else {
            bulkOperationUtil.add(new TheonMessage<E>(topic, key, message));
        }
        theonCounter.incrementEventCount(TheonStatus.MESSAGE_ADDED);
    }

    /**
     * @return the theonUrl
     */
    public String getTheonUrl() {
        return theonUrl;
    }

    /**
     * @param theonUrl
     *            the theonUrl to set
     */
    public void setTheonUrl(final String theonUrl) {
        this.theonUrl = theonUrl;
    }

    /**
     * @return the theonUri
     */
    public URI getTheonUri() {
        return theonUri;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     *            the username to set
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * @return the bulkPollIntervalSecs
     */
    public int getBulkPollIntervalSecs() {
        return bulkPollIntervalSecs;
    }

    /**
     * @param bulkPollIntervalSecs
     *            the bulkPollIntervalSecs to set
     */
    public void setBulkPollIntervalSecs(final int bulkPollIntervalSecs) {
        this.bulkPollIntervalSecs = bulkPollIntervalSecs;
    }

    /**
     * @return the queueCapacity
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * @param queueCapacity
     *            the queueCapacity to set
     */
    public void setQueueCapacity(final int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * @return the maxPostEntitySize
     */
    public int getMaxPostEntitySize() {
        return maxPostEntitySize;
    }

    /**
     * @param maxPostEntitySize
     *            the maxPostEntitySize to set
     */
    public void setMaxPostEntitySize(final int maxPostEntitySize) {
        this.maxPostEntitySize = maxPostEntitySize;
    }

    /**
     * @return the bulkOperationUtil
     */
    public BulkOperationUtil<TheonMessage<E>> getBulkOperationUtil() {
        return bulkOperationUtil;
    }

    /**
     * @return the bulkOperation
     */
    public BulkOperation<TheonMessage<E>> getBulkOperation() {
        return bulkOperation;
    }

    /**
     * @return the parallelBulkOperationUtil
     */
    public BulkOperationUtil<TheonMessage<E>> getParallelBulkOperationUtil() {
        return parallelBulkOperationUtil;
    }

    /**
     * @return the fileBasedQueue
     */
    public boolean isFileBasedQueue() {
        return fileBasedQueue;
    }

    /**
     * @param fileBasedQueue
     *            the fileBasedQueue to set
     */
    public void setFileBasedQueue(final boolean fileBasedQueue) {
        this.fileBasedQueue = fileBasedQueue;
    }

    /**
     * @return the httpClient
     */
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the theonCounter
     */
    public EnumCounter<TheonStatus> getTheonCounter() {
        return theonCounter;
    }

    /**
     * @return the httpConnPoolSize
     */
    public int getHttpConnPoolSize() {
        return httpConnPoolSize;
    }

    /**
     * @param httpConnPoolSize
     *            the httpConnPoolSize to set
     */
    public void setHttpConnPoolSize(final int httpConnPoolSize) {
        this.httpConnPoolSize = httpConnPoolSize;
    }

    /**
     * @return the bulkMessageSize
     */
    public int getBulkMessageSize() {
        return bulkMessageSize;
    }

    /**
     * @param bulkMessageSize
     *            the bulkMessageSize to set
     */
    public void setBulkMessageSize(final int bulkMessageSize) {
        this.bulkMessageSize = bulkMessageSize;
    }

    /**
     * @return the postPerKey
     */
    public boolean isPostPerKey() {
        return postPerKey;
    }

    /**
     * @param postPerKey
     *            the postPerKey to set
     */
    public void setPostPerKey(final boolean postPerKey) {
        this.postPerKey = postPerKey;
    }

    /**
     * @return the defaultKey
     */
    public String getDefaultKey() {
        return defaultKey;
    }

    /**
     * @param defaultKey
     *            the defaultKey to set
     */
    public void setDefaultKey(final String defaultKey) {
        this.defaultKey = defaultKey;
    }

    /**
     * @return the gzipEnabled
     */
    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    /**
     * @param gzipEnabled
     *            the gzipEnabled to set
     */
    public void setGzipEnabled(final boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

    /**
     * @return the parallelEnabled
     */
    public boolean isParallelEnabled() {
        return parallelEnabled;
    }

    /**
     * @param parallelEnabled
     *            the parallelEnabled to set
     */
    public void setParallelEnabled(final boolean parallelEnabled) {
        this.parallelEnabled = parallelEnabled;
    }

    /**
     * @return the queueDir
     */
    public String getQueueDir() {
        return queueDir;
    }

    /**
     * @param queueDir
     *            the queueDir to set
     */
    public void setQueueDir(final String queueDir) {
        this.queueDir = queueDir;
    }

    /**
     * @return the queueName
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @param queueName
     *            the queueName to set
     */
    public void setQueueName(final String queueName) {
        this.queueName = queueName;
    }

    /**
     * @return the bigQueueTimerInterval
     */
    public long getBigQueueTimerInterval() {
        return bigQueueTimerInterval;
    }

    /**
     * @param bigQueueTimerInterval
     *            the bigQueueTimerInterval to set
     */
    public void setBigQueueTimerInterval(final long bigQueueTimerInterval) {
        this.bigQueueTimerInterval = bigQueueTimerInterval;
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
     * @return the certValidationDisabled
     */
    public boolean isCertValidationDisabled() {
        return certValidationDisabled;
    }

    /**
     * @param certValidationDisabled the certValidationDisabled to set
     */
    public void setCertValidationDisabled(final boolean certValidationDisabled) {
        this.certValidationDisabled = certValidationDisabled;
    }

    /**
     * @return the stringType
     */
    public boolean isStringType() {
        return stringType;
    }

    /**
     * @param stringType the stringType to set
     */
    public void setStringType(final boolean stringType) {
        this.stringType = stringType;
    }

}
