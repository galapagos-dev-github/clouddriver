/*
 * Copyright 2016 Google, Inc.
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

package com.netflix.spinnaker.clouddriver.kubernetes.deploy.validators

import com.netflix.spinnaker.clouddriver.kubernetes.deploy.description.CloneKubernetesAtomicOperationDescription
import com.netflix.spinnaker.clouddriver.kubernetes.deploy.description.KubernetesContainerDescription
import com.netflix.spinnaker.clouddriver.kubernetes.deploy.description.KubernetesResourceDescription
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesCredentials
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.DefaultAccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.security.MapBackedAccountCredentialsRepository
import org.springframework.validation.Errors
import spock.lang.Shared
import spock.lang.Specification

class CloneKubernetesAtomicOperationValidatorSpec extends Specification {
  private static final DESCRIPTION = "cloneKubernetesAtomicOperationDescription"

  private static final VALID_APPLICATION = "app"
  private static final VALID_STACK = "stack"
  private static final VALID_DETAILS = "the-details"
  private static final VALID_TARGET_SIZE = 3
  private static final VALID_IMAGE = "container-image"
  private static final VALID_NAME = "a-name"
  private static final VALID_MEMORY1 = "200"
  private static final VALID_MEMORY2 = "200Mi"
  private static final VALID_CPU1 = "200"
  private static final VALID_CPU2 = "200m"
  private static final VALID_CREDENTIALS = "auto"
  private static final VALID_LOAD_BALANCERS = ["x", "y"]
  private static final VALID_SECURITY_GROUPS = ["a-1", "b-2"]
  private static final VALID_SOURCE_SERVER_GROUP_NAME = "myapp-test-v000"

  private static final INVALID_APPLICATION = "-app-"
  private static final INVALID_STACK = " stack"
  private static final INVALID_DETAILS = "the details"
  private static final INVALID_TARGET_SIZE = -7
  private static final INVALID_IMAGE = ""
  private static final INVALID_NAME = "a?name"
  private static final INVALID_MEMORY = "200asdf"
  private static final INVALID_CPU = "-1_"
  private static final INVALID_CREDENTIALS = "valid"
  private static final INVALID_LOAD_BALANCERS = [" ", "--"]
  private static final INVALID_SECURITY_GROUPS = [" ", "--"]

  @Shared
  CloneKubernetesAtomicOperationValidator validator

  void setupSpec() {
    validator = new CloneKubernetesAtomicOperationValidator()
    def credentialsRepo = new MapBackedAccountCredentialsRepository()
    def credentialsProvider = new DefaultAccountCredentialsProvider(credentialsRepo)
    def namedCredentialsMock = Mock(KubernetesNamedAccountCredentials)
    namedCredentialsMock.getName() >> VALID_CREDENTIALS
    namedCredentialsMock.getCredentials() >> new KubernetesCredentials(null, null)
    credentialsRepo.save(VALID_CREDENTIALS, namedCredentialsMock)
    validator.accountCredentialsProvider = credentialsProvider
  }

  KubernetesContainerDescription fullValidContainerDescription1
  KubernetesContainerDescription fullValidContainerDescription2
  KubernetesContainerDescription partialValidContainerDescription
  KubernetesResourceDescription fullValidResourceDescription1
  KubernetesResourceDescription fullValidResourceDescription2
  KubernetesResourceDescription partialValidResourceDescription

  KubernetesContainerDescription fullInvalidContainerDescription
  KubernetesContainerDescription partialInvalidContainerDescription
  KubernetesResourceDescription fullInvalidResourceDescription

  void setup() {
    fullValidResourceDescription1 = new KubernetesResourceDescription(memory: VALID_MEMORY1, cpu: VALID_CPU1)
    fullValidResourceDescription2 = new KubernetesResourceDescription(memory: VALID_MEMORY2, cpu: VALID_CPU2)
    partialValidResourceDescription = new KubernetesResourceDescription(memory: VALID_MEMORY1)
    fullValidContainerDescription1 = new KubernetesContainerDescription(name: VALID_NAME, image: VALID_IMAGE, limits: fullValidResourceDescription1, requests: fullValidResourceDescription1)
    fullValidContainerDescription2 = new KubernetesContainerDescription(name: VALID_NAME, image: VALID_IMAGE, limits: fullValidResourceDescription2, requests: fullValidResourceDescription2)
    partialValidContainerDescription = new KubernetesContainerDescription(name: VALID_NAME, image: VALID_IMAGE, limits: partialValidResourceDescription)

    fullInvalidResourceDescription = new KubernetesResourceDescription(memory: INVALID_MEMORY, cpu: INVALID_CPU)
    fullInvalidContainerDescription = new KubernetesContainerDescription(name: INVALID_NAME, image: INVALID_IMAGE, limits: fullInvalidResourceDescription, requests: fullInvalidResourceDescription)
    partialInvalidContainerDescription = new KubernetesContainerDescription(name: INVALID_NAME, image: INVALID_IMAGE)
  }

  void "validation accept (all fields filled)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        freeFormDetails: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          fullValidContainerDescription1,
          fullValidContainerDescription2
        ],
        loadBalancers: VALID_LOAD_BALANCERS,
        securityGroups: VALID_SECURITY_GROUPS,
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      0 * errorsMock._
  }

  void "validation accept (minimal fields filled)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ],
        credentials: VALID_CREDENTIALS)
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      0 * errorsMock._
  }

  void "validation reject (missing credentials)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.credentials", "${DESCRIPTION}.credentials.empty")
      0 * errorsMock._
  }

  void "validation reject (invalid stack)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: INVALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.stack", "${DESCRIPTION}.stack.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._
  }

  void "validation reject (invalid application)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: INVALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])

      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.application", "${DESCRIPTION}.application.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._
  }

  void "validation reject (invalid target size)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: INVALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.targetSize", "${DESCRIPTION}.targetSize.negative")
      0 * errorsMock._
  }

  void "validation reject (invalid partial container)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialInvalidContainerDescription
        ],
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].name", "${DESCRIPTION}.container[0].name.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].image", "${DESCRIPTION}.container[0].image.empty")
      0 * errorsMock._
  }

  void "validation reject (invalid full container)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          fullInvalidContainerDescription
        ],
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].name", "${DESCRIPTION}.container[0].name.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].image", "${DESCRIPTION}.container[0].image.empty")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].requests.memory", "${DESCRIPTION}.container[0].requests.memory.invalid (Must match ${StandardKubernetesAttributeValidator.quantityPattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].limits.memory", "${DESCRIPTION}.container[0].limits.memory.invalid (Must match ${StandardKubernetesAttributeValidator.quantityPattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].requests.cpu", "${DESCRIPTION}.container[0].requests.cpu.invalid (Must match ${StandardKubernetesAttributeValidator.quantityPattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.container[0].limits.cpu", "${DESCRIPTION}.container[0].limits.cpu.invalid (Must match ${StandardKubernetesAttributeValidator.quantityPattern})")
      0 * errorsMock._
  }

  void "validation reject (invalid load balancers)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        loadBalancers: INVALID_LOAD_BALANCERS,
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.loadBalancers[0]", "${DESCRIPTION}.loadBalancers[0].invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.loadBalancers[1]", "${DESCRIPTION}.loadBalancers[1].invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._
  }

  void "validation reject (invalid security groups)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          partialValidContainerDescription
        ],
        securityGroups: INVALID_SECURITY_GROUPS,
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: VALID_SOURCE_SERVER_GROUP_NAME
        ])
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.securityGroups[0]", "${DESCRIPTION}.securityGroups[0].invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      1 * errorsMock.rejectValue("${DESCRIPTION}.securityGroups[1]", "${DESCRIPTION}.securityGroups[1].invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._
  }

  void "validation reject (empty source)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        freeFormDetails: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          fullValidContainerDescription1,
          fullValidContainerDescription2
        ],
        loadBalancers: VALID_LOAD_BALANCERS,
        securityGroups: VALID_SECURITY_GROUPS,
        credentials: VALID_CREDENTIALS
      )
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.source", "${DESCRIPTION}.source.empty")
      0 * errorsMock._
  }

  void "validation reject (empty source server group name)"() {
    setup:
      def description = new CloneKubernetesAtomicOperationDescription(application: VALID_APPLICATION,
        stack: VALID_STACK,
        freeFormDetails: VALID_STACK,
        targetSize: VALID_TARGET_SIZE,
        containers: [
          fullValidContainerDescription1,
          fullValidContainerDescription2
        ],
        loadBalancers: VALID_LOAD_BALANCERS,
        securityGroups: VALID_SECURITY_GROUPS,
        credentials: VALID_CREDENTIALS,
        source: [
          serverGroupName: ""
        ]
      )
      def errorsMock = Mock(Errors)
    when:
      validator.validate([], description, errorsMock)
    then:
      1 * errorsMock.rejectValue("${DESCRIPTION}.source", "${DESCRIPTION}.source.empty")
      0 * errorsMock._
  }

}
