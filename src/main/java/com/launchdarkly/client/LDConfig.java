package com.launchdarkly.client;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This class exposes advanced configuration options for the {@link LDClient}. Instances of this class must be constructed with a {@link com.launchdarkly.client.LDConfig.Builder}.
 *
 */
public final class LDConfig implements ILoggerFactory{
  private static final URI DEFAULT_BASE_URI = URI.create("https://app.launchdarkly.com");
  private static final URI DEFAULT_EVENTS_URI = URI.create("https://events.launchdarkly.com");
  private static final URI DEFAULT_STREAM_URI = URI.create("https://stream.launchdarkly.com");
  private static final int DEFAULT_CAPACITY = 10000;
  private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
  private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
  private static final int DEFAULT_FLUSH_INTERVAL = 5;
  private static final long DEFAULT_POLLING_INTERVAL_MILLIS = 1000L;
  private static final long DEFAULT_START_WAIT_MILLIS = 5000L;
  private static final int DEFAULT_SAMPLING_INTERVAL = 0;
  private static final long DEFAULT_RECONNECT_TIME_MILLIS = 1000;
  private static ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
  private final Logger logger;

  protected static final LDConfig DEFAULT = new Builder().build();

  final URI baseURI;
  final URI eventsURI;
  final URI streamURI;
  final int capacity;
  final int connectTimeout;
  final int socketTimeout;
  final int flushInterval;
  final HttpHost proxyHost;
  final boolean stream;
  final FeatureStore featureStore;
  final boolean useLdd;
  final boolean offline;
  final long pollingIntervalMillis;
  final long startWaitMillis;
  final int samplingInterval;
  final long reconnectTimeMs;
  final String clientVersion;

  static Logger getLogger(Class<?> klass) {
    return loggerFactory.getLogger(klass.getName());
  }

  protected LDConfig(Builder builder) {
    loggerFactory = builder.loggerFactory;
    this.logger = getLogger(LDConfig.class);
    this.clientVersion = getClientVersion();

    this.baseURI = builder.baseURI;
    this.eventsURI = builder.eventsURI;
    this.capacity = builder.capacity;
    this.connectTimeout = builder.connectTimeout;
    this.socketTimeout = builder.socketTimeout;
    this.flushInterval = builder.flushInterval;
    this.proxyHost = builder.proxyHost();
    this.streamURI = builder.streamURI;
    this.stream = builder.stream;
    this.featureStore = builder.featureStore;
    this.useLdd = builder.useLdd;
    this.offline = builder.offline;
    if (builder.pollingIntervalMillis < DEFAULT_POLLING_INTERVAL_MILLIS) {
      this.pollingIntervalMillis = DEFAULT_POLLING_INTERVAL_MILLIS;
    } else {
      this.pollingIntervalMillis = builder.pollingIntervalMillis;
    }
    this.startWaitMillis = builder.startWaitMillis;
    this.samplingInterval = builder.samplingInterval;
    this.reconnectTimeMs = builder.reconnectTimeMs;
  }

  private String getClientVersion() {
    Class clazz = LDConfig.class;
    String className = clazz.getSimpleName() + ".class";
    String classPath = clazz.getResource(className).toString();
    if (!classPath.startsWith("jar")) {
      // Class not from JAR
      return "Unknown";
    }
    String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
        "/META-INF/MANIFEST.MF";
    Manifest manifest = null;
    try {
      manifest = new Manifest(new URL(manifestPath).openStream());
      Attributes attr = manifest.getMainAttributes();
      String value = attr.getValue("Implementation-Version");
      return value;
    } catch (IOException e) {
      logger.warn("Unable to determine LaunchDarkly client library version", e);
      return "Unknown";
    }
  }

  @Override
  public Logger getLogger(String s) {
    return null;
  }

  /**
   * A <a href="http://en.wikipedia.org/wiki/Builder_pattern">builder</a> that helps construct {@link com.launchdarkly.client.LDConfig} objects. Builder
   * calls can be chained, enabling the following pattern:
   *
   * <pre>
   * LDConfig config = new LDConfig.Builder()
   *      .connectTimeout(3)
   *      .socketTimeout(3)
   *      .build()
   * </pre>
   */
  public static class Builder {
    private URI baseURI = DEFAULT_BASE_URI;
    private URI eventsURI = DEFAULT_EVENTS_URI;
    private URI streamURI = DEFAULT_STREAM_URI;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int capacity = DEFAULT_CAPACITY;
    private int flushInterval = DEFAULT_FLUSH_INTERVAL;
    private String proxyHost;
    private int proxyPort = -1;
    private String proxyScheme;
    private boolean stream = true;
    private boolean useLdd = false;
    private boolean offline = false;
    private long pollingIntervalMillis = DEFAULT_POLLING_INTERVAL_MILLIS;
    private FeatureStore featureStore = new InMemoryFeatureStore();
    private long startWaitMillis = DEFAULT_START_WAIT_MILLIS;
    private int samplingInterval = DEFAULT_SAMPLING_INTERVAL;
    private long reconnectTimeMs = DEFAULT_RECONNECT_TIME_MILLIS;
    private ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

    /**
     * Creates a builder with all configuration parameters set to the default
     */
    public Builder() {
    }

    /**
     * Set the base URL of the LaunchDarkly server for this configuration
     * @param baseURI the base URL of the LaunchDarkly server for this configuration
     * @return the builder
     */
    public Builder baseURI(URI baseURI) {
      this.baseURI = baseURI;
      return this;
    }

    /**
     * Set the events URL of the LaunchDarkly server for this configuration
     * @param eventsURI the events URL of the LaunchDarkly server for this configuration
     * @return the builder
     */
    public Builder eventsURI(URI eventsURI) {
      this.eventsURI = eventsURI;
      return this;
    }

    /**
     * Set the base URL of the LaunchDarkly streaming server for this configuration
     * @param streamURI the base URL of the LaunchDarkly streaming server
     * @return the builder
     */
    public Builder streamURI(URI streamURI) {
      this.streamURI = streamURI;
      return this;
    }

    public Builder featureStore(FeatureStore store) {
      this.featureStore = store;
      return this;
    }

    /**
     * Set whether streaming mode should be enabled. By default, streaming is enabled.
     * @param stream whether streaming mode should be enabled
     * @return the builder
     */
    public Builder stream(boolean stream) {
      this.stream = stream;
      return this;
    }

    /**
     * Set the connection timeout in seconds for the configuration. This is the time allowed for the underlying HTTP client to connect
     * to the LaunchDarkly server. The default is 2 seconds.
     *
     * <p>Both this method and {@link #connectTimeoutMillis(int) connectTimeoutMillis} affect the same property internally.</p>
     *
     * @param connectTimeout the connection timeout in seconds
     * @return the builder
     */
    public Builder connectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout * 1000;
      return this;
    }

    /**
     * Set the socket timeout in seconds for the configuration. This is the number of seconds between successive packets that the
     * client will tolerate before flagging an error. The default is 10 seconds.
     *
     * <p>Both this method and {@link #socketTimeoutMillis(int) socketTimeoutMillis} affect the same property internally.</p>
     *
     * @param socketTimeout the socket timeout in seconds
     * @return the builder
     */
    public Builder socketTimeout(int socketTimeout) {
      this.socketTimeout = socketTimeout * 1000;
      return this;
    }

    /**
     * Set the connection timeout in milliseconds for the configuration. This is the time allowed for the underlying HTTP client to connect
     * to the LaunchDarkly server. The default is 2000 ms.
     *
     * <p>Both this method and {@link #connectTimeout(int) connectTimeout} affect the same property internally.</p>
     *
     * @param connectTimeoutMillis the connection timeout in milliseconds
     * @return the builder
     */
    public Builder connectTimeoutMillis(int connectTimeoutMillis) {
      this.connectTimeout = connectTimeoutMillis;
      return this;
    }

    /**
     * Set the socket timeout in milliseconds for the configuration. This is the number of milliseconds between successive packets that the
     * client will tolerate before flagging an error. The default is 10,000 milliseconds.
     *
     * <p>Both this method and {@link #socketTimeout(int) socketTimeout} affect the same property internally.</p>
     *
     * @param socketTimeoutMillis the socket timeout in milliseconds
     * @return the builder
     */
    public Builder socketTimeoutMillis(int socketTimeoutMillis) {
      this.socketTimeout = socketTimeoutMillis;
      return this;
    }

    /**
     * Set the number of seconds between flushes of the event buffer. Decreasing the flush interval means
     * that the event buffer is less likely to reach capacity.
     *
     * @param flushInterval the flush interval in seconds
     * @return the builder
     */
    public Builder flushInterval(int flushInterval) {
      this.flushInterval = flushInterval;
      return this;
    }

    /**
     * Set the capacity of the events buffer. The client buffers up to this many events in memory before flushing. If the capacity is exceeded before the buffer is flushed, events will be discarded.
     * Increasing the capacity means that events are less likely to be discarded, at the cost of consuming more memory.
     *
     * @param capacity the capacity of the event buffer
     * @return the builder
     */
    public Builder capacity(int capacity) {
      this.capacity = capacity;
      return this;
    }

    /**
     * Set the host to use as an HTTP proxy for making connections to LaunchDarkly. If this is not set, but
     * {@link #proxyPort(int)} or  {@link #proxyScheme(String)} are specified, this will default to <code>localhost</code>.
     * <p>
     * If none of {@link #proxyHost(String)}, {@link #proxyPort(int)} or {@link #proxyScheme(String)} are specified,
     * a proxy will not be used, and {@link LDClient} will connect to LaunchDarkly directly.
     * </p>
     *
     * @param host
     * @return the builder
     */
    public Builder proxyHost(String host) {
      this.proxyHost = host;
      return this;
    }

    /**
     * Set the port to use for an HTTP proxy for making connections to LaunchDarkly.  If not set (but {@link #proxyHost(String)}
     * or {@link #proxyScheme(String)} are specified, the default port for the scheme will be used.
     * <p>
     * <p>
     * If none of {@link #proxyHost(String)}, {@link #proxyPort(int)} or {@link #proxyScheme(String)} are specified,
     * a proxy will not be used, and {@link LDClient} will connect to LaunchDarkly directly.
     * </p>
     *
     * @param port
     * @return the builder
     */
    public Builder proxyPort(int port) {
      this.proxyPort = port;
      return this;
    }

    /**
     * Set the scheme to use for an HTTP proxy for making connections to LaunchDarkly.  If not set (but {@link #proxyHost(String)}
     * or {@link #proxyPort(int)} are specified, the default <code>https</code> scheme will be used.
     * <p>
     * <p>
     * If none of {@link #proxyHost(String)}, {@link #proxyPort(int)} or {@link #proxyScheme(String)} are specified,
     * a proxy will not be used, and {@link LDClient} will connect to LaunchDarkly directly.
     * </p>
     *
     * @param scheme
     * @return the builder
     */
    public Builder proxyScheme(String scheme) {
      this.proxyScheme = scheme;
      return this;
    }

    /**
     * Set whether this client should subscribe to the streaming API, or whether the LaunchDarkly daemon is in use
     * instead
     *
     * @param useLdd
     * @return the builder
     */
    public Builder useLdd(boolean useLdd) {
      this.useLdd = useLdd;
      return this;
    }

    /**
     * Set whether this client is offline.
     *
     * @param offline when set to true no calls to LaunchDarkly will be made.
     * @return the builder
     */
    public Builder offline(boolean offline) {
      this.offline = offline;
      return this;
    }

    /**
     * Set the polling interval (when streaming is disabled). Values less than the default of 1000
     * will be set to 1000.
     *
     * @param pollingIntervalMillis rule update polling interval in milliseconds.
     * @return the builder
     */
    public Builder pollingIntervalMillis(long pollingIntervalMillis) {
      this.pollingIntervalMillis = pollingIntervalMillis;
      return this;
    }

    /**
     * Set how long the constructor will block awaiting a successful connection to LaunchDarkly.
     * Setting this to 0 will not block and cause the constructor to return immediately.
     * Default value: 5000
     *
     *
     * @param startWaitMillis milliseconds to wait
     * @return the builder
     */
    public Builder startWaitMillis(long startWaitMillis) {
      this.startWaitMillis = startWaitMillis;
      return this;
    }

    /**
     * Enable event sampling. When set to the default of zero, sampling is disabled and all events
     * are sent back to LaunchDarkly. When set to greater than zero, there is a 1 in
     * <code>samplingInterval</code> chance events will be will be sent.
     *
     * <p>Example: if you want 5% sampling rate, set <code>samplingInterval</code> to 20.
     *
     * @param samplingInterval the sampling interval.
     * @return the builder
     */
    public Builder samplingInterval(int samplingInterval) {
      this.samplingInterval = samplingInterval;
      return this;
    }

    /**
     * The reconnect base time in milliseconds for the streaming connection. The streaming connection
     * uses an exponential backoff algorithm (with jitter) for reconnects, but will start the backoff
     * with a value near the value specified here.
     *
     * @param reconnectTimeMs the reconnect time base value in milliseconds
     * @return the builder
     */
    public Builder reconnectTimeMs(long reconnectTimeMs) {
      this.reconnectTimeMs = reconnectTimeMs;
      return this;
    }


    HttpHost proxyHost() {
      if (this.proxyHost == null && this.proxyPort == -1 && this.proxyScheme == null) {
        return null;
      } else {
        String hostname = this.proxyHost == null ? "localhost" : this.proxyHost;
        String scheme = this.proxyScheme == null ? "https" : this.proxyScheme;
        return new HttpHost(hostname, this.proxyPort, scheme);
      }
    }

    /**
     * Build the configured {@link com.launchdarkly.client.LDConfig} object
     *
     * @return the {@link com.launchdarkly.client.LDConfig} configured by this builder
     */
    public LDConfig build() {
      return new LDConfig(this);
    }

  }

  private URIBuilder getBuilder() {
    return new URIBuilder()
        .setScheme(baseURI.getScheme())
        .setHost(baseURI.getHost())
        .setPort(baseURI.getPort());
  }

  private URIBuilder getEventsBuilder() {
    return new URIBuilder()
        .setScheme(eventsURI.getScheme())
        .setHost(eventsURI.getHost())
        .setPort(eventsURI.getPort());
  }

  HttpGet getRequest(String sdkKey, String path) {
    URIBuilder builder = this.getBuilder().setPath(path);

    try {
      HttpGet request = new HttpGet(builder.build());
      request.addHeader("Authorization", sdkKey);
      request.addHeader("User-Agent", "JavaClient/" + clientVersion);

      return request;
    } catch (Exception e) {
      logger.error("Unhandled exception in LaunchDarkly client", e);
      return null;
    }
  }

  HttpPost postEventsRequest(String sdkKey, String path) {
    URIBuilder builder = this.getEventsBuilder().setPath(eventsURI.getPath() + path);

    try {
      HttpPost request = new HttpPost(builder.build());
      request.addHeader("Authorization", sdkKey);
      request.addHeader("User-Agent", "JavaClient/" + clientVersion);

      return request;
    } catch (Exception e) {
      logger.error("Unhandled exception in LaunchDarkly client", e);
      return null;
    }
  }
}