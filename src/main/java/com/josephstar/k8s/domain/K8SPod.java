package com.josephstar.k8s.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class K8SPod {

    private String scriptName;
    private String repoName;
    private String gitRepoUrl;
    private String execId;
    private Integer numberOfAgents;

}
