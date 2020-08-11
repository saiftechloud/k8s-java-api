package com.josephstar.k8s.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class K8SPod {

    private String scriptName;
    private String repoName;
    private String gitRepoUrl;
    private String execId;
    private String uuid;
    private Integer numberOfAgents;

}
