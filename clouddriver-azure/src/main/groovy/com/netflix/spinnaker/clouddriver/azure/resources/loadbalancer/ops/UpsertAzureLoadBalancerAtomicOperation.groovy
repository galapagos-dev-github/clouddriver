/*
 * Copyright 2015 The original authors.
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

package com.netflix.spinnaker.clouddriver.azure.resources.loadbalancer.ops

import com.microsoft.azure.management.resources.models.DeploymentExtended
import com.microsoft.azure.management.resources.models.DeploymentOperation
import com.netflix.spinnaker.clouddriver.azure.common.AzureUtilities
import com.netflix.spinnaker.clouddriver.azure.client.AzureResourceManagerClient
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.azure.templates.AzureLoadBalancerResourceTemplate
import com.netflix.spinnaker.clouddriver.azure.resources.loadbalancer.model.UpsertAzureLoadBalancerDescription

class UpsertAzureLoadBalancerAtomicOperation implements AtomicOperation<Map> {
  private static final String BASE_PHASE = "UPSERT_LOAD_BALANCER"

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  private final UpsertAzureLoadBalancerDescription description

  UpsertAzureLoadBalancerAtomicOperation(UpsertAzureLoadBalancerDescription description) {
    this.description = description
  }

  /**
   * curl -X POST -H "Content-Type: application/json" -d '[ { "upsertLoadBalancer": { "cloudProvider" : "azure", "appName" : "azure1", "loadBalancerName" : "azure1-st1-d1", "stack" : "st1", "detail" : "d1", "credentials" : "azure-cred1", "region" : "West US", "vnet" : null, "probes" : [ { "probeName" : "healthcheck1", "probeProtocol" : "HTTP", "probePort" : 7001, "probePath" : "/healthcheck", "probeInterval" : 10, "unhealthyThreshold" : 2 } ], "securityGroups" : null, "loadBalancingRules" : [ { "ruleName" : "lbRule1", "protocol" : "TCP", "externalPort" : "80", "backendPort" : "80", "probeName" : "healthcheck1", "persistence" : "None", "idleTimeout" : "4" } ], "inboundNATRules" : [ { "ruleName" : "inboundRule1", "serviceType" : "SSH", "protocol" : "TCP", "port" : "80" } ], "name" : "azure1-st1-d1", "user" : "[anonymous]" }} ]' localhost:7002/ops
   *
   * @param priorOutputs
   * @return
   */
  @Override
  Map operate(List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing upsert of load balancer $description.loadBalancerName " +
      "in $description.region..."

    Map<String, Boolean> resourceCompletedState = new HashMap<String, Boolean>()

    try {

      task.updateStatus(BASE_PHASE, "Beginning load balancer deployment")

      String resourceGroupName = AzureUtilities.getResourceGroupName(description.appName, description.region)
      DeploymentExtended deployment = description.credentials.resourceManagerClient.createResourceFromTemplate(description.credentials,
        AzureLoadBalancerResourceTemplate.getTemplate(description),
        resourceGroupName,
        description.region,
        description.loadBalancerName)

      String deploymentState = deployment.properties.provisioningState

      while (deploymentIsRunning(deploymentState)) {
        for (DeploymentOperation d : description.credentials.resourceManagerClient.getDeploymentOperations(description.credentials, resourceGroupName, deployment.name)) {
          if (!resourceCompletedState.containsKey(d.id)){
            resourceCompletedState[d.id] = false
          }
          if (d.properties.provisioningState == AzureResourceManagerClient.DeploymentState.SUCCEEDED) {
            if (!resourceCompletedState[d.id]) {
              task.updateStatus BASE_PHASE, String.format("Resource %s created", d.properties.targetResource.resourceName)
              resourceCompletedState[d.id] = true
            }
          }
          else if (d.properties.provisioningState == AzureResourceManagerClient.DeploymentState.FAILED) {
            if (!resourceCompletedState[d.id]) {
              task.updateStatus BASE_PHASE, String.format("Failed to create resource %s: %s", d.properties.targetResource.resourceName, d.properties.statusMessage)
              resourceCompletedState[d.id] = true
            }
          }
        }
        deploymentState = description.credentials.resourceManagerClient.getDeployment(description.credentials, resourceGroupName, deployment.name).properties.provisioningState
      }

      task.updateStatus BASE_PHASE, "Deployment for load balancer $description.loadBalancerName in $description.region has succeeded."
    } catch (Exception e) {
      task.updateStatus BASE_PHASE, String.format("Deployment of load balancer $description.loadBalancerName failed: %s", e.message)
      throw e
    }
    [loadBalancers: [(description.region): [name: description.loadBalancerName]]]
  }

  private static boolean deploymentIsRunning(String deploymentState) {
    deploymentState != AzureResourceManagerClient.DeploymentState.CANCELED &&
      deploymentState != AzureResourceManagerClient.DeploymentState.DELETED &&
      deploymentState != AzureResourceManagerClient.DeploymentState.FAILED &&
      deploymentState != AzureResourceManagerClient.DeploymentState.SUCCEEDED
  }
}
