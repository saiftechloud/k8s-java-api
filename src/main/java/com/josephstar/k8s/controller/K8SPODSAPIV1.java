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
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Api(tags="k8s-pods-api-v1", description = "Kubernetes Pods API v1")
@RestController
@RequestMapping("api/v1/pods")
public class K8SPODSAPIV1 {

    private static final Logger logger = LoggerFactory.getLogger(K8SPODSAPIV1.class);

    @Value( "${k8s.master}")
    private String masterK8S;

    @Value( "${os.master}")
    private String masterOS;

    K8S k8S = new K8S();

    /**
     * This lists all pods.
     */
    @PostMapping(path ="/list")
    public ResponseEntity<?> listPod(@RequestBody K8S k8SRequest) {
        K8SResponse k8SResponse = new K8SResponse();

        try{
            ObjectMapper mapper = new ObjectMapper();
            K8SPod k8SPod = mapper.convertValue(k8SRequest.getRequest().getData(), K8SPod.class);
            logger.info("START_PODS_API_K8S_REQUEST_PARAMS = " + k8SPod.toString());

            if(k8SPod.getAction().equals("K8S")){
                logger.info("K8S");
                Config config = new ConfigBuilder().withMasterUrl(masterK8S).build();
                final KubernetesClient client = new DefaultKubernetesClient(config);
                PodList podList = client.pods().inAnyNamespace().list();
                k8SResponse.setData(podList);
                logger.info("Listing pods for master = " + masterK8S + " and size = " + podList.getItems().size());
                podList.getItems().forEach((obj) -> logger.info(obj.getMetadata().getName()));
            }else if(k8SPod.getAction().equals("OS")){
                logger.info("OS");
                Config config = new ConfigBuilder().withMasterUrl(masterOS).build();
                final OpenShiftClient client = new DefaultOpenShiftClient(config);
                PodList podList = client.pods().inAnyNamespace().list();
                k8SResponse.setData(podList);
                logger.info("Listing pods for master = " + masterOS + " and size = " + podList.getItems().size());
                podList.getItems().forEach((obj) -> logger.info(obj.getMetadata().getName()));
            }else{
                logger.info("UNKNOWN_ACTION_TYPE");
                throw new Exception("Unknow Action Type");
            }


        }catch (Exception ex){
            k8S.setSuccess(false);
            logger.error(ex.getMessage(), ex);
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
        List<String> args = new ArrayList<>();
        Config config;
        KubernetesClient k8sClient = null;
        OpenShiftClient openShiftClient = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            K8SPod k8SPod = mapper.convertValue(k8SRequest.getRequest().getData(), K8SPod.class);
            logger.info("START_PODS_API_K8S_REQUEST_PARAMS = " + k8SPod.toString());

            if(k8SPod.getAction().equals("K8S")){
                logger.info("K8S");
                config = new ConfigBuilder().withMasterUrl(masterK8S).build();
                k8sClient = new DefaultKubernetesClient(config);
            }else if(k8SPod.getAction().equals("OS")){
                logger.info("OS");
                config = new ConfigBuilder().withMasterUrl(masterOS).build();
                openShiftClient = new DefaultOpenShiftClient(config);
            }else{
                logger.info("UNKNOWN_ACTION_TYPE");
                throw new Exception("Unknow Action Type");
            }

            // Pass docker arguments
            for(Map.Entry<String, String> entry : k8SPod.getArgs().entrySet()) {
                args.add(entry.getValue());
            }
            args.add(k8SPod.getUuid());

            // make deployment
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(k8SPod.getUuid())
                    .addToLabels("execId", k8SPod.getUuid())
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(k8SPod.getNumberOfAgents())
                    .withNewSelector()
                    .addToMatchLabels("execId", k8SPod.getUuid())
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("execId", k8SPod.getUuid())
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("test")
                    .withImage("lkd483/test:v1")
                    .withArgs(args)
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            // Create an ObjectMapper mapper for YAML
            ObjectMapper mapperYAML = new ObjectMapper(new YAMLFactory());
            String deploymentYAML = mapperYAML.writeValueAsString(deployment);
            logger.info("START_PODS_API_K8S_DEPLOYMENT_TEMPLATE = \n" + deploymentYAML);

            if(k8SPod.getAction().equals("K8S")){
                k8sClient.apps().deployments().inNamespace("default").createOrReplace(deployment);
            }else if(k8SPod.getAction().equals("OS")){
                openShiftClient.apps().deployments().inNamespace("default").createOrReplace(deployment);
            }else{
                logger.info("UNKNOWN_ACTION_TYPE");
                throw new Exception("Unknow Action Type");
            }

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
        Config config;
        KubernetesClient k8sClient = null;
        OpenShiftClient openShiftClient = null;
        DeploymentList deploymentList = null;

        try {

            ObjectMapper mapper = new ObjectMapper();
            K8SPod k8SPod = mapper.convertValue(k8SRequest.getRequest().getData(), K8SPod.class);
            logger.info("STOP_PODS_API_K8S_REQUEST_PARAMS = " + k8SPod.toString());

            if(k8SPod.getAction().equals("K8S")){
                logger.info("K8S");
                config = new ConfigBuilder().withMasterUrl(masterK8S).build();
                k8sClient = new DefaultKubernetesClient(config);
            }else if(k8SPod.getAction().equals("OS")){
                logger.info("OS");
                config = new ConfigBuilder().withMasterUrl(masterOS).build();
                openShiftClient = new DefaultOpenShiftClient(config);
            }else{
                logger.info("UNKNOWN_ACTION_TYPE");
                throw new Exception("Unknow Action Type");
            }

            // Check deployment existence by label
            if(k8SPod.getAction().equals("K8S")){
                deploymentList = k8sClient.apps().deployments().inAnyNamespace().list();
            }else if(k8SPod.getAction().equals("OS")){
                deploymentList = openShiftClient.apps().deployments().inAnyNamespace().list();
            }else{
                logger.info("UNKNOWN_ACTION_TYPE");
                throw new Exception("Unknow Action Type");
            }

            for (Deployment deployment : deploymentList.getItems()){
                logger.info("uuid = " + k8SPod.getUuid());
                if (deployment.getMetadata().getLabels().containsValue(k8SPod.getUuid())){
                    ObjectMapper mapperYAML = new ObjectMapper(new YAMLFactory());
                    String deploymentYAML = mapperYAML.writeValueAsString(deployment);
                    logger.info("STOP_PODS_API_K8S_DEPLOYMENT_TEMPLATE = \n" + deploymentYAML);
                    deploymentExist = true;
                    k8SPod.setNumberOfAgents(deployment.getSpec().getReplicas());
                    break;
                }
            }

            // Delete Deployment if exists
            if(deploymentExist){
                if(k8SPod.getAction().equals("K8S")){
                    k8sClient.apps().deployments()
                            .inNamespace("default")
                            .withLabel("execId", k8SPod.getUuid())
                            .delete();
                }else if(k8SPod.getAction().equals("OS")){
                    k8sClient.apps().deployments()
                            .inNamespace("default")
                            .withLabel("execId", k8SPod.getUuid())
                            .delete();
                }else{
                    logger.info("UNKNOWN_ACTION_TYPE");
                    throw new Exception("Unknow Action Type");
                }

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
