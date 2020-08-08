package com.josephstar.k8s.domain;

import io.fabric8.kubernetes.api.model.PodList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class K8SResponse {

    private Object data;

}
