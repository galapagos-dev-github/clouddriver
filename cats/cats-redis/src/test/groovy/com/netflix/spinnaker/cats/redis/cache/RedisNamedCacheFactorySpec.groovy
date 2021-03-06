/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.cats.redis.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.redis.JedisPoolSource
import com.netflix.spinnaker.cats.redis.test.LocalRedisCheck
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Subject

@IgnoreIf({ LocalRedisCheck.redisUnavailable() })
class RedisNamedCacheFactorySpec extends Specification {

    @Subject
    RedisNamedCacheFactory factory

    def setup() {
        def pool = new JedisPool("localhost", 6379)
        def source = new JedisPoolSource(pool)
        Jedis jedis
        try {
            jedis = source.jedis
            jedis.flushAll()
        } finally {
            jedis?.close()
        }

        def mapper = new ObjectMapper();
        factory = new RedisNamedCacheFactory(source, mapper)
    }

    def 'caches with the same name share content'() {
        def c1 = factory.getCache('foo')
        def c2 = factory.getCache('foo')

        when:
        c1.merge('foo', new DefaultCacheData('bar', [bar: 'baz'], [:]))
        def bar = c2.get('foo', 'bar')

        then:
        bar != null
        bar.id == 'bar'
        bar.attributes.bar == 'baz'
    }

    def 'caches with different names do not share content'() {
        def c1 = factory.getCache('foo')
        def c2 = factory.getCache('foo2')

        when:
        c1.merge('foo', new DefaultCacheData('bar', [bar: 'baz'], [:]))
        def barC1 = c1.get('foo', 'bar')
        def barC2 = c2.get('foo', 'bar')

        then:
        barC1 != null
        barC1.id == 'bar'
        barC1.attributes.bar == 'baz'
        barC2 == null
    }
}
