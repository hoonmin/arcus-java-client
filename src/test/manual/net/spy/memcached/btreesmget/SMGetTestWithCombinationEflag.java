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
package net.spy.memcached.btreesmget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;
import net.spy.memcached.collection.BaseIntegrationTest;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.collection.SMGetElement;
import net.spy.memcached.internal.SMGetFuture;

public class SMGetTestWithCombinationEflag extends BaseIntegrationTest {

	private final String KEY = this.getClass().getSimpleName();
	List<String> keyList = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		try {
			mc.delete(KEY).get();
		} catch (Exception e) {

		}
		Assert.assertNull(mc.asyncGetAttr(KEY).get());
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			mc.delete(KEY).get();
		} catch (Exception e) {

		}
		super.tearDown();
	}

	public void testSMGetMissAll() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 1, 2,
						ElementFlagFilter.DO_NOT_FILTER, 0, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertTrue(map.isEmpty());
			Assert.assertEquals(future.getMissedKeyList().toString(), 10,
					future.getMissedKeyList().size());
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitAll() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 50; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < 50; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 10,
						ElementFlagFilter.DO_NOT_FILTER, 0, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertEquals(10, map.size());
			Assert.assertTrue(future.getMissedKeyList().isEmpty());

			for (int i = 0; i < map.size(); i++) {
				Assert.assertEquals(KEY + i, map.get(i).getKey());
				Assert.assertEquals(i, map.get(i).getBkey());
				Assert.assertEquals("VALUE" + i, map.get(i).getValue());
			}
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitAllWithOffsetMoreCount() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 50; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < 50; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 10,
						ElementFlagFilter.DO_NOT_FILTER, 1, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertEquals(10, map.size());
			Assert.assertTrue(future.getMissedKeyList().isEmpty());

			for (int i = 0; i < map.size(); i++) {
				Assert.assertEquals(KEY + (i + 1), map.get(i).getKey());
				Assert.assertEquals(i + 1, map.get(i).getBkey());
				Assert.assertEquals("VALUE" + (i + 1), map.get(i).getValue());
			}
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitAllWithOffsetExactCount() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < 10; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 10,
						ElementFlagFilter.DO_NOT_FILTER, 1, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertEquals(9, map.size());
			Assert.assertTrue(future.getMissedKeyList().isEmpty());

			for (int i = 0; i < map.size(); i++) {
				Assert.assertEquals(KEY + (i + 1), map.get(i).getKey());
				Assert.assertEquals(i + 1, map.get(i).getBkey());
				Assert.assertEquals("VALUE" + (i + 1), map.get(i).getValue());
			}
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitAllWithOffsetLessThanCount() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 9; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < 9; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 10,
						ElementFlagFilter.DO_NOT_FILTER, 1, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertEquals(8, map.size());
			Assert.assertTrue(future.getMissedKeyList().isEmpty());

			for (int i = 0; i < map.size(); i++) {
				Assert.assertEquals(KEY + (i + 1), map.get(i).getKey());
				Assert.assertEquals(i + 1, map.get(i).getBkey());
				Assert.assertEquals("VALUE" + (i + 1), map.get(i).getValue());
			}
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitAllDesc() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < 10; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 10, 0,
						ElementFlagFilter.DO_NOT_FILTER, 0, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			Assert.assertEquals(10, map.size());
			Assert.assertTrue(future.getMissedKeyList().isEmpty());
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitHalf() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}

			for (int i = 0; i < 5; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 10,
						ElementFlagFilter.DO_NOT_FILTER, 0, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			assertEquals(5, map.size());

			assertEquals(future.getMissedKeyList().toString(), 5, future
					.getMissedKeyList().size());
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testSMGetHitHalfDesc() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}

			for (int i = 0; i < 5; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get();
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 10, 0,
						ElementFlagFilter.DO_NOT_FILTER, 0, 10);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			assertEquals(5, map.size());

			assertEquals(future.getMissedKeyList().toString(), 5, future
					.getMissedKeyList().size());
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testTimeout() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 1000; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}

			for (int i = 0; i < 500; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get(1000L,
							TimeUnit.MILLISECONDS);
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get(1000L,
							TimeUnit.MILLISECONDS);
			}
		} catch (TimeoutException e) {

		} catch (Exception e) {
			fail(e.getMessage());
		}

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 1000,
						ElementFlagFilter.DO_NOT_FILTER, 0, 500);
		try {
			List<SMGetElement<Object>> map = future.get(1L,
					TimeUnit.MILLISECONDS);

			fail("Timeout is not tested.");
		} catch (TimeoutException e) {
			future.cancel(true);
			return;
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			fail(e.getMessage());
		}
		fail("There's no timeout.");
	}

	public void testPerformanceGet1000KeysWithoutOffset() {
		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < 1000; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}

			for (int i = 0; i < 1000; i++) {
				if (i % 2 == 0)
					mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(),
							"VALUE" + i, new CollectionAttributes()).get(1000L,
							TimeUnit.MILLISECONDS);
				else
					mc.asyncBopInsert(KEY + i, i, null, "VALUE" + i,
							new CollectionAttributes()).get(1000L,
							TimeUnit.MILLISECONDS);
			}
		} catch (TimeoutException e) {

		} catch (Exception e) {
			fail(e.getMessage());
		}

		long start = System.currentTimeMillis();

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, 1000,
						ElementFlagFilter.DO_NOT_FILTER, 0, 500);
		try {
			List<SMGetElement<Object>> map = future.get(1000L,
					TimeUnit.MILLISECONDS);

			// System.out.println((System.currentTimeMillis() - start) + "ms");
		} catch (TimeoutException e) {
			future.cancel(true);
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSMGetWithMassiveKeys() {
		int testSize = 100;

		try {
			keyList = new ArrayList<String>();
			for (int i = 0; i < testSize; i++) {
				mc.delete(KEY + i).get();
				keyList.add(KEY + i);
			}
			for (int i = 0; i < testSize; i++) {
				if (i % 2 == 0) {
					continue;
				}
				mc.asyncBopInsert(KEY + i, i, "EFLAG".getBytes(), "VALUE" + i,
						new CollectionAttributes()).get();
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}

		long start = System.currentTimeMillis();

		SMGetFuture<List<SMGetElement<Object>>> future = mc
				.asyncBopSortMergeGet(keyList, 0, testSize,
						ElementFlagFilter.DO_NOT_FILTER, 0, 500);
		try {
			List<SMGetElement<Object>> map = future
					.get(1000L, TimeUnit.SECONDS);

			// System.out.println(System.currentTimeMillis() - start + "ms");

			Assert.assertEquals(50, map.size());

			List<String> missed = future.getMissedKeyList();
			Assert.assertEquals(testSize / 2, missed.size());
		} catch (Exception e) {
			future.cancel(true);
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

}
