/*
 * arcus-java-client : Arcus Java client
 * Copyright 2010-2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.spy.memcached;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationQueueFactory;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Builder for more easily configuring a ConnectionFactory.
 */
public class ConnectionFactoryBuilder {

	private OperationQueueFactory opQueueFactory;
	private OperationQueueFactory readQueueFactory;
	private OperationQueueFactory writeQueueFactory;

	private Transcoder<Object> transcoder;
	private Transcoder<Object> collectionTranscoder;

	private FailureMode failureMode = FailureMode.Cancel;

	private Collection<ConnectionObserver> initialObservers
		= Collections.emptyList();

	private OperationFactory opFact;

	private Locator locator = Locator.ARCUSCONSISTENT;
	private long opTimeout = -1;
	private boolean isDaemon = true;
	private boolean shouldOptimize = false;
	private boolean useNagle = false;
//	private long maxReconnectDelay =
//		DefaultConnectionFactory.DEFAULT_MAX_RECONNECT_DELAY;
	private long maxReconnectDelay = 1;
	
	private int readBufSize = -1;
	private HashAlgorithm hashAlg = HashAlgorithm.KETAMA_HASH;
	private AuthDescriptor authDescriptor = null;
	private long opQueueMaxBlockTime = -1;

//	private int timeoutExceptionThreshold = DefaultConnectionFactory.DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;
	private int timeoutExceptionThreshold = 10;
	
	private int maxFrontCacheElements = DefaultConnectionFactory.DEFAULT_MAX_FRONTCACHE_ELEMENTS;
	private int frontCacheExpireTime = DefaultConnectionFactory.DEFAULT_FRONTCACHE_EXPIRETIME;
	
	private int bulkServiceThreadCount = DefaultConnectionFactory.DEFAULT_BULKSERVICE_THREAD_COUNT;
	private int bulkServiceLoopLimit = DefaultConnectionFactory.DEFAULT_BULKSERVICE_LOOP_LIMIT;
	private long bulkServiceSingleOpTimeout = DefaultConnectionFactory.DEFAULT_BULKSERVICE_SINGLE_OP_TIMEOUT;
	
	private int maxSMGetChunkSize = DefaultConnectionFactory.DEFAULT_MAX_SMGET_KEY_CHUNK_SIZE;
	
	private String frontCacheName = "ArcusFrontCache_" + this.hashCode();
	
	/**
	 * Set the operation queue factory.
	 */
	public ConnectionFactoryBuilder setOpQueueFactory(OperationQueueFactory q) {
		opQueueFactory = q;
		return this;
	}

	/**
	 * Set the read queue factory.
	 */
	public ConnectionFactoryBuilder setReadOpQueueFactory(OperationQueueFactory q) {
		readQueueFactory = q;
		return this;
	}

	/**
	 * Set the write queue factory.
	 */
	public ConnectionFactoryBuilder setWriteOpQueueFactory(OperationQueueFactory q) {
		writeQueueFactory = q;
		return this;
	}

	/**
	 * Set the maximum amount of time (in milliseconds) a client is willing to
	 * wait for space to become available in an output queue.
	 */
	public ConnectionFactoryBuilder setOpQueueMaxBlockTime(long t) {
		opQueueMaxBlockTime = t;
		return this;
	}

	/**
	 * Set the default transcoder.
	 */
	public ConnectionFactoryBuilder setTranscoder(Transcoder<Object> t) {
		transcoder = t;
		return this;
	}
	
	/**
	 * Set the default collection transcoder.
	 */
	public ConnectionFactoryBuilder setCollectionTranscoder(Transcoder<Object> t) {
		collectionTranscoder = t;
		return this;
	}

	/**
	 * Set the failure mode.
	 */
	public ConnectionFactoryBuilder setFailureMode(FailureMode fm) {
		failureMode = fm;
		return this;
	}

	/**
	 * Set the initial connection observers (will observe initial connection).
	 */
	public ConnectionFactoryBuilder setInitialObservers(
			Collection<ConnectionObserver> obs) {
		initialObservers = obs;
		return this;
	}

	/**
	 * Set the operation factory.
	 *
	 * Note that the operation factory is used to also imply the type of
	 * nodes to create.
	 *
	 * @see MemcachedNode
	 */
	public ConnectionFactoryBuilder setOpFact(OperationFactory f) {
		opFact = f;
		return this;
	}

	/**
	 * Set the default operation timeout in milliseconds.
	 */
	public ConnectionFactoryBuilder setOpTimeout(long t) {
		opTimeout = t;
		return this;
	}

	/**
	 * Set the daemon state of the IO thread (defaults to true).
	 */
	public ConnectionFactoryBuilder setDaemon(boolean d) {
		isDaemon = d;
		return this;
	}

	/**
	 * Set to false if the default operation optimization is not desirable.
	 */
	public ConnectionFactoryBuilder setShouldOptimize(boolean o) {
		shouldOptimize = o;
		return this;
	}

	/**
	 * Set the read buffer size.
	 */
	public ConnectionFactoryBuilder setReadBufferSize(int to) {
		readBufSize = to;
		return this;
	}

	/**
	 * Set the hash algorithm.
	 */
	public ConnectionFactoryBuilder setHashAlg(HashAlgorithm to) {
		hashAlg = to;
		return this;
	}

	/**
	 * Set to true if you'd like to enable the Nagle algorithm.
	 */
	public ConnectionFactoryBuilder setUseNagleAlgorithm(boolean to) {
		useNagle = to;
		return this;
	}

	/**
	 * Convenience method to specify the protocol to use.
	 */
	public ConnectionFactoryBuilder setProtocol(Protocol prot) {
		switch(prot) {
			case TEXT:
				opFact = new AsciiOperationFactory();
				break;
			case BINARY:
				opFact = new BinaryOperationFactory();
				break;
			default: assert false : "Unhandled protocol: " + prot;
		}
		return this;
	}

	/**
	 * Set the locator type.
	 */
	public ConnectionFactoryBuilder setLocatorType(Locator l) {
		locator = l;
		return this;
	}

	/**
	 * Set the maximum reconnect delay.
	 */
	public ConnectionFactoryBuilder setMaxReconnectDelay(long to) {
		assert to > 0 : "Reconnect delay must be a positive number";
		maxReconnectDelay = to;
		return this;
	}

	/**
	 * Set the auth descriptor to enable authentication on new connections.
	 */
	public ConnectionFactoryBuilder setAuthDescriptor(AuthDescriptor to) {
		authDescriptor = to;
		return this;
	}

	/**
	 * Set the maximum timeout exception threshold
	 */
	public ConnectionFactoryBuilder setTimeoutExceptionThreshold(int to) {
		assert to > 1 : "Minimum timeout exception threshold is 2";
		if (to > 1) {
			timeoutExceptionThreshold = to -2;
		}
		return this;
	}
	
	/**
	 * Set the maximum number of front cache elements.
	 */
	public ConnectionFactoryBuilder setMaxFrontCacheElements(int to) {
		assert to > 0 : "In case of front cache, the number must be a positive number";
		maxFrontCacheElements = to;
		return this;
	}
	
	/**
	 * Set front cache's expire time.
	 */
	public ConnectionFactoryBuilder setFrontCacheExpireTime(int to) {
		assert to > 0 : "Front cache's expire time must be a positive number";
		frontCacheExpireTime = to;
		return this;
	}
	
	/**
	 * Set bulk service default thread count 
	 */
	public ConnectionFactoryBuilder setBulkServiceThreadCount(int to) {
		assert to > 0 : "Bulk service's thread count must be a positive number";
		bulkServiceThreadCount = to;
		return this;
	}
	
	/**
	 * Set bulk service loop limit count 
	 */
	public ConnectionFactoryBuilder setBulkServiceLoopLimit(int to) {
		assert to > 0 : "Bulk service's loop limit must be a positive number";
		bulkServiceLoopLimit = to;
		return this;
	}
	
	/**
	 * Set bulk service each operation timeout 
	 */
	public ConnectionFactoryBuilder setBulkServiceSingleOpTimeout(long to) {
		assert to > 0 : "Bulk service's single operation timeout must be a positive number";
		bulkServiceSingleOpTimeout = to;
		return this;
	}
	
	/**
	 * Set max smget key chunk size
	 */
	public ConnectionFactoryBuilder setMaxSMGetKeyChunkSize(int size) {
		maxSMGetChunkSize = size;
		return this;
	}
	
	/**
	 * Get the ConnectionFactory set up with the provided parameters.
	 */
	public ConnectionFactory build() {
		return new DefaultConnectionFactory() {

			@Override
			public BlockingQueue<Operation> createOperationQueue() {
				return opQueueFactory == null ?
						super.createOperationQueue() : opQueueFactory.create();
			}

			@Override
			public BlockingQueue<Operation> createReadOperationQueue() {
				return readQueueFactory == null ?
						super.createReadOperationQueue()
						: readQueueFactory.create();
			}

			@Override
			public BlockingQueue<Operation> createWriteOperationQueue() {
				return writeQueueFactory == null ?
						super.createReadOperationQueue()
						: writeQueueFactory.create();
			}

			@Override
			public NodeLocator createLocator(List<MemcachedNode> nodes) {
				switch(locator) {
					case ARRAY_MOD:
						return new ArrayModNodeLocator(nodes, getHashAlg());
					case CONSISTENT:
						return new KetamaNodeLocator(nodes, getHashAlg());
					case ARCUSCONSISTENT:
						return new ArcusKetamaNodeLocator(nodes, getHashAlg());
					default: throw new IllegalStateException(
							"Unhandled locator type: " + locator);
				}
			}

			@Override
			public Transcoder<Object> getDefaultTranscoder() {
				return transcoder == null ?
						super.getDefaultTranscoder() : transcoder;
			}
			
			@Override
			public Transcoder<Object> getDefaultCollectionTranscoder() {
				return collectionTranscoder == null ?
						super.getDefaultCollectionTranscoder() : collectionTranscoder;
			}

			@Override
			public FailureMode getFailureMode() {
				return failureMode == null ?
						super.getFailureMode() : failureMode;
			}

			@Override
			public HashAlgorithm getHashAlg() {
				return hashAlg == null ? super.getHashAlg() : hashAlg;
			}

			@Override
			public Collection<ConnectionObserver> getInitialObservers() {
				return initialObservers;
			}

			@Override
			public OperationFactory getOperationFactory() {
				return opFact == null ? super.getOperationFactory() : opFact;
			}

			@Override
			public long getOperationTimeout() {
				return opTimeout == -1 ?
						super.getOperationTimeout() : opTimeout;
			}

			@Override
			public int getReadBufSize() {
				return readBufSize == -1 ?
						super.getReadBufSize() : readBufSize;
			}

			@Override
			public boolean isDaemon() {
				return isDaemon;
			}

			@Override
			public boolean shouldOptimize() {
				return shouldOptimize;
			}

			@Override
			public boolean useNagleAlgorithm() {
				return useNagle;
			}

			@Override
			public long getMaxReconnectDelay() {
				return maxReconnectDelay;
			}

			@Override
			public AuthDescriptor getAuthDescriptor() {
				return authDescriptor;
			}

			@Override
			public long getOpQueueMaxBlockTime() {
				return opQueueMaxBlockTime > -1 ? opQueueMaxBlockTime
						: super.getOpQueueMaxBlockTime();
			}

			@Override
			public int getTimeoutExceptionThreshold() {
				return timeoutExceptionThreshold;
			}
			
			@Override
			public int getMaxFrontCacheElements() {
				return maxFrontCacheElements;
			}
			
			@Override
			public int getFrontCacheExpireTime() {
				return frontCacheExpireTime;
			}

			@Override
			public int getBulkServiceThreadCount() {
				return bulkServiceThreadCount;
			}
			
			@Override
			public int getBulkServiceLoopLimit() {
				return bulkServiceLoopLimit;
			}
			
			@Override
			public long getBulkServiceSingleOpTimeout() {
				return bulkServiceSingleOpTimeout;
			}
			
			@Override
			public int getDefaultMaxSMGetKeyChunkSize() {
				return maxSMGetChunkSize;
			}
			
			@Override
			public String getFrontCacheName() {
				return frontCacheName;
			}
		};
	}

	/**
	 * Type of protocol to use for connections.
	 */
	public static enum Protocol {
		/**
		 * Use the text (ascii) protocol.
		 */
		TEXT,
		/**
		 * Use the binary protocol.
		 */
		BINARY
	}

	/**
	 * Type of node locator to use.
	 */
	public static enum Locator {
		/**
		 * Array modulus - the classic node location algorithm.
		 */
		ARRAY_MOD,
		/**
		 * Fixed Consistent hash algorithm.
		 *
		 * This uses ketema's distribution algorithm, but may be used with any
		 * hash algorithm.
		 */
		CONSISTENT,
		/**
		 * Live Consistent hash algorithm
		 * 
		 * This uses ketama's distribution algorithm, and used with 
		 * node change(add, delete)
		 */
		ARCUSCONSISTENT
	}
}
