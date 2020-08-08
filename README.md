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

### API
- listPods =>
- startPods =>
- stopPods =>  