package com.josephstar.k8s.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephstar.k8s.domain.K8S;
import com.josephstar.k8s.domain.K8SPod;
import com.josephstar.k8s.domain.K8SResponse;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.*;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags="k8s-pods-api-v1", description = "Kubernetes Pods API v1")
@RestController
@RequestMapping("api/v1/pods")
public class K8SPODSAPIV1 {

    private static final Logger logger = LoggerFactory.getLogger(K8SPODSAPIV1.class);

    @Value( "${k8s.master}")
    private String master;

    K8S k8S = new K8S();

    /**
     * This lists all pods.
     * @return
     */
    @GetMapping(path ="/list")
    public ResponseEntity<?> listPod() {
        K8SResponse k8SResponse = new K8SResponse();

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            PodList podList = client.pods().inAnyNamespace().list();
            k8SResponse.setData(podList);
            logger.info("Listing pods for master = " + master + " and size = " + podList.getItems().size());
            podList.getItems().forEach((obj) -> { logger.info(obj.getMetadata().getName()); });

        } catch (KubernetesClientException e) {
            k8S.setSuccess(false);
            logger.error(e.getMessage(), e);
        }
        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);

    }

    /**
     *
     * @param k8SRequest
     *      scriptName - request script name
     *      repoName - docker repository name
     *      gitRepoUrl - git repository name
     *      execId - request execution id
     * @return
     *      numberOfAgents - the number of replicas
     */
    @PostMapping(path ="/start")
    public ResponseEntity<?> startPod(@RequestBody K8S k8SRequest) {
        K8SResponse k8SResponse = new K8SResponse();
        List<String> commands = new ArrayList<>();

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            ObjectMapper mapper = new ObjectMapper();
            K8SPod k8SPod = mapper.convertValue(k8SRequest.getRequest().getData(), K8SPod.class);
            logger.info("Start Pods request with execId= " + k8SPod.getExecId() + " and numberOfAgents= " + k8SPod.getNumberOfAgents());

            commands.add(k8SPod.getScriptName());
            commands.add(k8SPod.getRepoName());
            commands.add( k8SPod.getGitRepoUrl());
            commands.add(k8SPod.getExecId());

            // make deployment
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(k8SPod.getScriptName())
                    .addToLabels("app", k8SPod.getRepoName())
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(k8SPod.getNumberOfAgents())
                    .withNewSelector()
                    .addToMatchLabels("app", k8SPod.getRepoName())
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", k8SPod.getRepoName())
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(k8SPod.getRepoName())
                    .withImage(k8SPod.getRepoName())
                    .withCommand(commands)
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            Deployment result = client.apps().deployments().inNamespace("default").createOrReplace(deployment);

            k8SResponse.setData(k8SPod);

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
