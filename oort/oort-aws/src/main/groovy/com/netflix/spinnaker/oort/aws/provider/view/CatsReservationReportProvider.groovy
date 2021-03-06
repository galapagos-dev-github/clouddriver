/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.spinnaker.oort.aws.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.clouddriver.model.ReservationReport
import com.netflix.spinnaker.clouddriver.model.ReservationReportProvider
import com.netflix.spinnaker.oort.aws.model.AmazonReservationReport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.oort.aws.data.Keys.Namespace.RESERVATION_REPORTS

@Component
class CatsReservationReportProvider implements ReservationReportProvider {
  @Autowired
  Cache cacheView

  @Autowired
  ObjectMapper objectMapper

  @Override
  ReservationReport getReservationReport() {
    def cacheData = cacheView.get(RESERVATION_REPORTS.ns, "latest")
    if (!cacheData) {
      return null
    }

    return objectMapper.readValue(objectMapper.writeValueAsString(cacheData.attributes.report), AmazonReservationReport)
  }
}

