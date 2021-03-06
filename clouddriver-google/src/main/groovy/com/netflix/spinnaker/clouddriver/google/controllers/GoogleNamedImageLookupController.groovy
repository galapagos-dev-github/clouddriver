/*
 * Copyright 2015 Google, Inc.
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

package com.netflix.spinnaker.clouddriver.google.controllers

import com.netflix.spinnaker.cats.mem.InMemoryCache
import com.netflix.spinnaker.clouddriver.google.model.GoogleResourceRetriever
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/gce/images")
class GoogleNamedImageLookupController {

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  GoogleResourceRetriever googleResourceRetriever

  @RequestMapping(value = '/find', method = RequestMethod.GET)
  List<Map> find(LookupOptions lookupOptions) {
    def imageMap = googleResourceRetriever.imageMap
    def results = []

    if (lookupOptions.account) {
      def imageList = imageMap?.get(lookupOptions.account) ?: []

      results = imageList.collect {
        [ imageName: it.name ]
      }
    } else {
      imageMap?.entrySet().each { Map.Entry<String, List<String>> accountNameToImageNamesEntry ->
        accountNameToImageNamesEntry.value.each {
          results << [ account: accountNameToImageNamesEntry.key, imageName: it.name ]
        }
      }
    }

    def glob = lookupOptions.q?.trim() ?: "*"

    // Wrap in '*' if there are no glob-style characters in the query string.
    if (!glob.contains('*') && !glob.contains('?') && !glob.contains('[') && !glob.contains('\\')) {
      glob = "*${glob}*"
    }

    def pattern = new InMemoryCache.Glob(glob).toPattern()

    return results.findAll { pattern.matcher(it.imageName).matches() }
  }

  private static class LookupOptions {
    String q
    String account
    String region
  }
}
