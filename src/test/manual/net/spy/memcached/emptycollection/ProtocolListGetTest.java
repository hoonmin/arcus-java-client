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
package net.spy.memcached.emptycollection;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.spy.memcached.collection.ListGet;

public class ProtocolListGetTest extends TestCase {

	private static final int index = 10;

	public void testStringfy() {
		// default setting : dropIfEmpty = true
		Assert.assertEquals("10 drop",
				(new ListGet<Object>(index, true)).stringify());
		Assert.assertEquals("10",
				(new ListGet<Object>(index, false)).stringify());

		Assert.assertEquals("10 drop",
				(new ListGet<Object>(index, true, true)).stringify());
		Assert.assertEquals("10 delete", (new ListGet<Object>(index, true,
				false)).stringify());
		Assert.assertEquals("10",
				(new ListGet<Object>(index, false, true)).stringify());
		Assert.assertEquals("10",
				(new ListGet<Object>(index, false, false)).stringify());

		Assert.assertEquals("10..20 drop",
				(new ListGet<Object>(10, 20, true)).stringify());
		Assert.assertEquals("10..20",
				(new ListGet<Object>(10, 20, false)).stringify());

		Assert.assertEquals("10..20 delete", (new ListGet<Object>(10, 20, true,
				false)).stringify());
		Assert.assertEquals("10..20 drop", (new ListGet<Object>(10, 20, true,
				true)).stringify());
		Assert.assertEquals("10..20",
				(new ListGet<Object>(10, 20, false, true)).stringify());
		Assert.assertEquals("10..20",
				(new ListGet<Object>(10, 20, false, false)).stringify());
	}
}
