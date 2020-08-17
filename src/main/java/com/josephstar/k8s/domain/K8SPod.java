package com.josephstar.k8s.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class K8SPod {

    private HashMap<String, String> args = new HashMap<>();
    private String uuid;
    private Integer numberOfAgents;

}
