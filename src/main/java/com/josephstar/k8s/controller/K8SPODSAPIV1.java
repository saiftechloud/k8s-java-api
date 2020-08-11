package com.josephstar.k8s.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.josephstar.k8s.domain.K8S;
import com.josephstar.k8s.domain.K8SPod;
import com.josephstar.k8s.domain.K8SResponse;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.*;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
            logger.info("START_PODS_API_K8S_REQUEST_PARAMS = " + k8SPod.toString());

            commands.add(k8SPod.getScriptName());
            commands.add(k8SPod.getRepoName());
            commands.add( k8SPod.getGitRepoUrl());
            commands.add(k8SPod.getExecId());
            commands.add(k8SPod.getUuid());

            // make deployment
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(k8SPod.getUuid())
                    .addToLabels("app", k8SPod.getUuid())
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(k8SPod.getNumberOfAgents())
                    .withNewSelector()
                    .addToMatchLabels("app", k8SPod.getUuid())
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", k8SPod.getUuid())
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

            // Create an ObjectMapper mapper for YAML
            ObjectMapper mapperYAML = new ObjectMapper(new YAMLFactory());
            String deploymentYAML = mapperYAML.writeValueAsString(deployment);
            logger.info("START_PODS_API_K8S_DEPLOYMENT_TEMPLATE = \n" + deploymentYAML);

            client.apps().deployments().inNamespace("default").createOrReplace(deployment);

            k8SResponse.setData(k8SPod);

        } catch (Exception ex) {
            k8S.setSuccess(false);
            logger.error(ex.getMessage(), ex);
        }

        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);
    }

    @PostMapping(path ="/stop")
    public ResponseEntity<?> stopPod(@RequestBody K8S k8SRequest) {
        K8SResponse k8SResponse = new K8SResponse();
        boolean deploymentExist = false;

        Config config = new ConfigBuilder().withMasterUrl(master).build();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            ObjectMapper mapper = new ObjectMapper();
            K8SPod k8SPod = mapper.convertValue(k8SRequest.getRequest().getData(), K8SPod.class);
            logger.info("STOP_PODS_API_K8S_REQUEST_PARAMS = " + k8SPod.toString());

            // Check deployment existence by label
            DeploymentList deploymentList = client.apps().deployments().inAnyNamespace().list();

            for (Deployment deployment : deploymentList.getItems()){
                logger.info("app-label = " + deployment.getMetadata().getLabels().containsValue(k8SPod.getUuid()));
                logger.info("uuid = " + k8SPod.getUuid());
                if (deployment.getMetadata().getLabels().containsValue(k8SPod.getUuid())){
                    ObjectMapper mapperYAML = new ObjectMapper(new YAMLFactory());
                    String deploymentYAML = mapperYAML.writeValueAsString(deployment);
                    logger.info("STOP_PODS_API_K8S_DEPLOYMENT_TEMPLATE = \n" + deploymentYAML);
                    deploymentExist = true;
                    break;
                }
            }

            // Delete Deployment if exists
            if(deploymentExist){
                client.apps().deployments()
                        .inNamespace("default")
                        .withLabel("app", k8SPod.getUuid())
                        .delete();
            }else {
                logger.info("STOP_PODS_API_K8S_EXISTENCE not found for UUID = " + k8SPod.getUuid());
                k8S.setSuccess(false);
                k8S.setError("Error when stopping deployment: NOT EXIST");
            }

            k8SResponse.setData(k8SPod);

        } catch (Exception ex) {
            k8S.setSuccess(false);
            k8S.setMessage(ex.getMessage());
            logger.error(ex.getMessage(), ex);
        }
        k8S.setResponse(k8SResponse);
        return ResponseEntity.ok(k8S);
    }

}
