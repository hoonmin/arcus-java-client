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

/**
 * A program to use CacheMonitor to start and
 * stop memcached node based on a znode. The program watches the
 * specified znode and saves the znode that corresponds to the
 * memcached server in the remote machine. It also changes the 
 * previous ketama node
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ArcusClientException.InitializeClientException;
import net.spy.memcached.compat.SpyThread;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

public class CacheManager extends SpyThread implements Watcher,
		CacheMonitor.CacheMonitorListener {
	public static final String CACHE_LIST_PATH = "/arcus/cache_list/";

	public static final String CLIENT_INFO_PATH = "/arcus/client_list/";

	private static final int SESSION_TIMEOUT = 15000;
	
	private static final long ZK_CONNECT_TIMEOUT = SESSION_TIMEOUT;

	private final String hostPort;

	private final String serviceCode;

	private CacheMonitor cacheMonitor;

	private ZooKeeper zk;

	private ArcusClient[] client;

	private final CountDownLatch clientInitLatch;

	private final ConnectionFactoryBuilder cfb;

	private final int waitTimeForConnect;

	private final int poolSize;

	private volatile boolean shutdownRequested = false;

	private CountDownLatch zkInitLatch;
	
	public CacheManager(String hostPort, String serviceCode,
			ConnectionFactoryBuilder cfb, CountDownLatch clientInitLatch, int poolSize,
			int waitTimeForConnect) {

		this.hostPort = hostPort;
		this.serviceCode = serviceCode;
		this.cfb = cfb;
		this.clientInitLatch = clientInitLatch;
		this.poolSize = poolSize;
		this.waitTimeForConnect = waitTimeForConnect;

		initZooKeeperClient();

		setName("Cache Manager IO for " + serviceCode + "@" + hostPort);
		setDaemon(true);
		start();

		getLogger().info(
				"CacheManager started. (" + serviceCode + "@" + hostPort + ")");
		
	}
	
	private void initZooKeeperClient() {
		try {
			getLogger().info("Trying to connect to Arcus admin(%s@%s)", serviceCode, hostPort);
			
			zkInitLatch = new CountDownLatch(1);
			zk = new ZooKeeper(hostPort, SESSION_TIMEOUT, this);

			try {
				/* In the above ZooKeeper() internals, reverse DNS lookup occurs
				 * when the getHostName() of InetSocketAddress class is called.
				 * In Windows, the reverse DNS lookup includes NetBIOS lookup
				 * that bring delay of 5 seconds (as well as dns and host file lookup).
				 * So, ZK_CONNECT_TIMEOUT is set as much like ZK session timeout.
				 */
				if (zkInitLatch.await(ZK_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS) == false) {
					getLogger().fatal("Connecting to Arcus admin(%s) timed out : %d miliseconds",
							hostPort, ZK_CONNECT_TIMEOUT);
					throw new AdminConnectTimeoutException(hostPort);
				}
				
				if (zk.exists(CacheManager.CACHE_LIST_PATH + serviceCode, false) == null) {
					getLogger().fatal(
							"Service code not found. (" + serviceCode + ")");
					throw new NotExistsServiceCodeException(serviceCode);
				}

				String path = getClientInfo();
				if (path.isEmpty()) {
					getLogger().fatal(
							"Can't create the znode of client info (" + path
									+ ")");
					throw new InitializeClientException(
							"Can't initialize Arcus client.");
				}
				
				if (zk.exists(path, false) == null) {
					zk.create(path, null, Ids.OPEN_ACL_UNSAFE,
							CreateMode.EPHEMERAL);
				}
			} catch (AdminConnectTimeoutException e) {
				shutdownZooKeeperClient();
				throw e;
			} catch (NotExistsServiceCodeException e) {
				shutdownZooKeeperClient();
				throw e;
			} catch (InterruptedException ie) {
				getLogger().fatal("Can't connect to Arcus admin(%s@%s) %s", serviceCode, hostPort, ie.getMessage());
				shutdownZooKeeperClient();
				return;
			} catch (Exception e) {
				getLogger().fatal(
						"Unexpected exception. contact to Arcus administrator");

				shutdownZooKeeperClient();
				throw new InitializeClientException(
						"Can't initialize Arcus client.", e);
			}

			cacheMonitor = new CacheMonitor(zk, serviceCode, this);
		} catch (IOException e) {
			throw new InitializeClientException(
					"Can't initialize Arcus client.", e);
		}
	}

	private String getClientInfo() {
		String path = "";
		
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			Date currentTime = new Date();
			
			// create the ephemeral znode 
			// "/arcus/client_list/{service_code}/{client hostname}_{ip address}_{pool size}_java_{client version}_{YYYYMMDDHHIISS}_{zk session id}"
			path = CLIENT_INFO_PATH + serviceCode + "/" 
					+ InetAddress.getLocalHost().getHostName() + "_"
					+ InetAddress.getLocalHost().getHostAddress() + "_"
					+ this.poolSize
					+ "_java_"
					+ ArcusClient.VERSION + "_"
					+ simpleDateFormat.format(currentTime) + "_" 
					+ zk.getSessionId();
			
		} catch (UnknownHostException e) {
			return null;
		}

		return path;
	}

	/***************************************************************************
	 * We do process only child node change event ourselves, we just need to
	 * forward them on.
	 * 
	 */
	public void process(WatchedEvent event) {
		if (event.getType() == Event.EventType.None) {
			switch (event.getState()) {
			case SyncConnected:
				getLogger().info("Connected to Arcus admin. (%s@%s)", serviceCode, hostPort);
				zkInitLatch.countDown();
			}
		}
		
		if (cacheMonitor != null) {
			cacheMonitor.process(event);
		} else {
			getLogger().debug(
					"cm is null, servicecode : %s, state:%s, type:%s",
					serviceCode, event.getState(), event.getType());
		}
	}

	public void run() {
		try {
			synchronized (this) {
				while (!shutdownRequested) {
					if (zk == null) {
						getLogger().info("Arcus admin connection is not established. (%s@%s)", serviceCode, hostPort);
						initZooKeeperClient();
					}
					
					if (!cacheMonitor.dead) {
						wait();
					} else {
						getLogger().warn("Unexpected disconnection from Arcus admin. Trying to reconnect to Arcus admin.");
						try {
							shutdownZooKeeperClient();
							initZooKeeperClient();
						} catch (AdminConnectTimeoutException e) {
							Thread.sleep(5000L);
						} catch (NotExistsServiceCodeException e) {
							Thread.sleep(5000L);
						} catch (InitializeClientException e) {
							Thread.sleep(5000L);
						}
					}
				}
			}
		} catch (InterruptedException e) {
			getLogger().warn("current arcus admin is interrupted : %s",
					e.getMessage());
		} finally {
			shutdownZooKeeperClient();
		}
	}

	public void closing() {
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Change current MemcachedNodes to new MemcachedNodes but intersection of
	 * current and new will be ruled out.
	 * 
	 * @param children
	 *            new children node list
	 */
	public void commandNodeChange(List<String> children) {
		String addrs = "";
		for (int i = 0; i < children.size(); i++) {
			String[] temp = children.get(i).split("-");
			if (i != 0) {
				addrs = addrs + "," + temp[0];
			} else {
				addrs = temp[0];
			}
		}

		if (client == null) {
			createArcusClient(addrs);
			return;
		}

		for (ArcusClient ac : client) {
			MemcachedConnection conn = ac.getMemcachedConnection();
			conn.putMemcachedQueue(addrs);
			conn.getSelector().wakeup();
		}
	}

	/**
	 * Create a ArcusClient
	 * 
	 * @param addrs
	 *            current available Memcached Addresses
	 */
	private void createArcusClient(String addrs) {

		List<InetSocketAddress> socketList = AddrUtil.getAddresses(addrs);

		final CountDownLatch latch = new CountDownLatch(socketList.size());
		final ConnectionObserver observer = new ConnectionObserver() {

			@Override
			public void connectionLost(SocketAddress sa) {

			}

			@Override
			public void connectionEstablished(SocketAddress sa,
					int reconnectCount) {
				latch.countDown();
			}
		};

		cfb.setInitialObservers(Collections.singleton(observer));

		int _awaitTime = 0;
		if (waitTimeForConnect == 0)
			_awaitTime = 50 * socketList.size();
		else
			_awaitTime = waitTimeForConnect;

		client = new ArcusClient[poolSize];
		for (int i = 0; i < poolSize; i++) {
			try {
				client[i] = ArcusClient.getInstance(cfb.build(), socketList);
				client[i].setName("Memcached IO for " + serviceCode);
				client[i].setCacheManager(this);
			} catch (IOException e) {
				getLogger()
						.fatal("Arcus Connection has critical problems. contact arcus manager.");
			}
		}
		try {
			if (latch.await(_awaitTime, TimeUnit.MILLISECONDS)) {
				getLogger().warn("All arcus connections are established.");
			} else {
				getLogger()
						.error("Some arcus connections are not established.");
			}
			// Success signal for initial connections to Zookeeper and
			// Memcached.
		} catch (InterruptedException e) {
			getLogger()
					.fatal("Arcus Connection has critical problems. contact arcus manager.");
		}
		this.clientInitLatch.countDown();

	}

	/**
	 * Returns current ArcusClient
	 * 
	 * @return current ArcusClient
	 */
	public ArcusClient[] getAC() {
		return client;
	}

	private void shutdownZooKeeperClient() {
		if (zk == null) {
			return;
		}

		try {
			getLogger().info("Close the ZooKeeper client. serviceCode=" + serviceCode + ", adminSessionId=0x" + Long.toHexString(zk.getSessionId()));
			zk.close();
			zk = null;
		} catch (InterruptedException e) {
			getLogger().warn(
					"An exception occured while closing ZooKeeper client.", e);
		}
	}

	public void shutdown() {
		if (!shutdownRequested) {
			getLogger().info("Shut down cache manager.");
			shutdownRequested = true;
			closing();
		}
	}
}
