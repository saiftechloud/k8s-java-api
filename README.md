# k8s-java-api
- Kubernetes Java API implementation project.
- Version: 0.1.1

### How to Run
- Git clone and change into directory
- Edit configuration k8s.master variable in properties
- To run spring boot application, run the following command in a terminal window
```console
./gradlew bootRun
```
- If you have a jar file
```console
java -jar k8s-java-api.jar
```
- Application will start on localhost:8080

### Swagger
- http://localhost:8080/swagger-ui.html

### Minikube
- https://kubernetes.io/docs/tasks/tools/install-minikube/

### Kubernetes
- This project uses Kubernetes & OpenShift Java Client of fabric8
- https://github.com/fabric8io/kubernetes-client

## API Examples
### listPods
- URL = http://localhost:8080/api/v1/pods/list
- Type = GET
### startPods
- URL = http://localhost:8080/api/v1/pods/start
- Type = POST
- Request
```json
{
    "request" : {
        "data" : {
            "args" : {
                "scriptName": "lkd483_test",
                "repoName" : "yusufyildiz",
                "gitRepoUrl" : "https://github.com/nginxinc/docker-nginx"
            },
            "uuid" : "5a4b8c20-5313-432e-9eb3-bf8d9025e439",
            "numberOfAgents" : 3
        }
    }
}
```
- Response
```json
{
    "success": true,
    "time": "2020-08-17T16:43:05.693+00:00",
    "message": null,
    "error": null,
    "pagination": null,
    "request": null,
    "response": {
        "data": {
            "args": {
                "repoName": "yusufyildiz",
                "gitRepoUrl": "https://github.com/nginxinc/docker-nginx",
                "scriptName": "lkd483_test"
            },
            "uuid": "5a4b8c20-5313-432e-9eb3-bf8d9025e439",
            "numberOfAgents": 3
        }
    },
    "client": null
}
```
### stopPods
- URL = http://localhost:8080/api/v1/pods/stop
- TYPE = POST
- Request
```json
{
    "request" : {
        "data" : {
            "uuid": "5a4b8c20-5313-432e-9eb3-bf8d9025e439"
        }
    }
}
```
- Response
```json
{
    "success": true,
    "time": "2020-08-17T17:07:00.478+00:00",
    "message": null,
    "error": null,
    "pagination": null,
    "request": null,
    "response": {
        "data": {
            "args": {},
            "uuid": "5a4b8c20-5313-432e-9eb3-bf8d9025e439",
            "numberOfAgents": 3
        }
    },
    "client": null
}
```