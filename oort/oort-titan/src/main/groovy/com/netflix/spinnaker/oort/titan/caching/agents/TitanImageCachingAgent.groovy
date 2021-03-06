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

package com.netflix.spinnaker.oort.titan.caching.agents
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.titan.TitanClientProvider
import com.netflix.spinnaker.clouddriver.titan.credentials.NetflixTitanCredentials
import com.netflix.spinnaker.oort.titan.caching.TitanCachingProvider
import com.netflix.titanclient.TitanClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.oort.titan.caching.Keys.Namespace.IMAGES

/**
 * TODO: This is a WIP. There needs to be docker registry APIs to support this agent
 */
class TitanImageCachingAgent implements CachingAgent {

  private static final Logger log = LoggerFactory.getLogger(TitanImageCachingAgent)

  final Set<AgentDataType> types = Collections.unmodifiableSet([
    AUTHORITATIVE.forType(IMAGES.ns)
  ] as Set)

  private final TitanClient titanClient
  private final NetflixTitanCredentials account
  private final String region
  private final ObjectMapper objectMapper

  TitanImageCachingAgent(TitanClientProvider titanClientProvider,
                         NetflixTitanCredentials account,
                         String region,
                         ObjectMapper objectMapper) {
    this.account = account
    this.region = region
    this.objectMapper = objectMapper
    this.titanClient = titanClientProvider.getTitanClient(account, region)
  }

  @Override
  String getProviderName() {
    TitanCachingProvider.PROVIDER_NAME
  }

  @Override
  String getAgentType() {
    "${account.name}/${region}/${TitanImageCachingAgent.simpleName}"
  }

  @Override
  Collection<AgentDataType> getProvidedDataTypes() {
    return types
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List images = [] // TODO: Fetch a list of docker images - add specific constraints to this agent (via constructor) like account/region
    Collection<CacheData> imageCacheData = new ArrayList<>(images.size())
    /*
    Collection<CacheData> namedImageCacheData = new ArrayList<>(images.size())
    for (each image) {
      Map<String, Object> attributes = objectMapper.convertValue(image, new TypeReference<Map<String, Object>>() {})
      def imageId = Keys.getImageKey(_, _)
      def namedImageId = Keys.getNamedImageKey(_)
      imageCacheData.add(new DefaultCacheData(imageId, attributes, [(NAMED_IMAGES.ns):[namedImageId]]))
      namedImageCacheData.add(new DefaultCacheData(namedImageId, [name: image.name], [(IMAGES.ns):[imageId]]))
    }
    */
    new DefaultCacheResult([(IMAGES.ns): imageCacheData])
  }

}
