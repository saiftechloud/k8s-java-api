package com.josephstar.k8s.controller;

import com.josephstar.k8s.domain.K8S;
import com.josephstar.k8s.domain.K8SResponse;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.*;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags="k8s-pods-api-v1", description = "Kubernetes Pods API v1")
@RestController
@RequestMapping("api/v1/pods")
public class K8SPODSAPIV1 {

    private static final Logger logger = LoggerFactory.getLogger(K8SPODSAPIV1.class);

    @Value( "${k8s.master}")
    private String master;


    K8S k8S = new K8S();

    @GetMapping(path ="/list")
    public ResponseEntity<?> listPod() {
        K8SResponse k8SResponse = new K8SResponse();

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            PodList podList = client.pods().inAnyNamespace().list();
            k8SResponse.setPodList(podList);
            logger.info("Listing pods for master = " + master + " and size = " + podList.getItems().size());
            podList.getItems().forEach((obj) -> { logger.info(obj.getMetadata().getName()); });

        } catch (KubernetesClientException e) {
            k8S.setSuccess(false);
            logger.error(e.getMessage(), e);
        }
        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);

    }

    @GetMapping(path ="/start")
    public ResponseEntity<?> startPod() {
        K8SResponse k8SResponse = new K8SResponse();

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName("nginx-deployment")
                    .addToLabels("app", "nginx")
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                    .addToMatchLabels("app", "nginx")
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", "nginx")
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("nginx")
                    .withImage("nginx:1.7.9")
                    .addNewPort().withContainerPort(80).endPort()
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            client.apps().deployments().inNamespace("default").createOrReplace(deployment);

        } catch (KubernetesClientException e) {
            k8S.setSuccess(false);
            logger.error(e.getMessage(), e);
        }
        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);
    }

    @GetMapping(path ="/stop")
    public ResponseEntity<?> stopPod() {
        K8SResponse k8SResponse = new K8SResponse();

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            // Deletion
            Boolean isDeleted = client.apps().deployments()
                    .inNamespace("default")
                    .withName("nginx-deployment")
                    .delete();

        } catch (KubernetesClientException e) {
            k8S.setSuccess(false);
            logger.error(e.getMessage(), e);
        }
        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);
    }

}
