/*
 * Copyright 2015 Pivotal, Inc.
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

package com.netflix.spinnaker.clouddriver.cf

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.clouddriver.cf.security.CloudFoundryAccountCredentials
import groovy.transform.CompileStatic

/**
 * Support code for Cloud Foundry testing
 *
 *
 */
@CompileStatic
class TestCredential {

  public static CloudFoundryAccountCredentials named(String name, Map params = [:]) {
    def credJson = [
        name: name,
        password: 'test password',
        org: 'spinnaker',
        space: 'test'
    ] + params
    new ObjectMapper().convertValue(credJson, CloudFoundryAccountCredentials)
  }

}
