package com.josephstar.k8s.domain;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class K8S {

    private Boolean success = true;
    private Timestamp time = new Timestamp(System.currentTimeMillis());
    private K8SMessage message;
    private K8SError error;
    private K8SPagination pagination;
    private K8SRequest request;
    private K8SResponse response;
    private K8SClient client;

}
