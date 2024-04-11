## Setup for the demo

1. Stand up a cluster in the CIE using the terraform and instructions from https://git.c3ms.org/jasc-projects/jcc2-dsop
2. Deploy big bang using the instructions in the [README](https://git.c3ms.org/jasc-projects/cie/k8s-eval/bigbang/-/blob/main/zarf/README.md) in this repo

3. Download the sample app package to the management server (if you need to modify this package, see [create microservice zarf package](README.md#create-microservice-zarf-package))
``` shell
aws s3 cp s3://jcc2-dsop/deployment-packages/zarf-package-microservices-demo-amd64-0.1.tar.zst .
```
4. Deploy the zarf package for microservices (`zarf package deploy`)
5. Update the boutique-with-istio namespace with the istio label
``` shell
kubectl label namespace boutique-with-istio istio-injection=enabled
```
8. Check the deployment
``` shell
kubectl get all -n boutique-with-istio
```
9. Get the IP address for the loadbalancer for the frontend
``` shell
kubectl get services -n boutique-with-istio
```
The output should be similar to
```shell
NAME                            TYPE           CLUSTER-IP       EXTERNAL-IP  
service/frontend-external       LoadBalancer   172.20.66.77     a7ec05c3042674e7ab52f5f9b32e4b88-1283451201.us-gov-west-1.elb.amazonaws.com
```
ping the load balancer to get the IP
```shell 
ping a7ec05c3042674e7ab52f5f9b32e4b88-1283451201.us-gov-west-1.elb.amazonaws.com
```
10. update your local /etc/hosts
	1. disconnect from the management server
	2. edit etc/hosts
	3. Add an entry in /etc/hosts with an entry for the External IP from #9 with the URL frontend.default.svc.cluster.local
11. open a browser and go to `frontend.default.svc.cluster.local` - the sample app page should open

## Demo steps

1. From your local environment, export the credentials needed to assume the role used in the CIE (I used cie-admin but in the future it will probably be ato-admin). - This simulates the process used on site to assume a role to run any AWS CLI commands
	1. run `get-role-vars.sh` from jcc2-dsop/dev-env/iam - you should have this from setting up the cluster
	``` shell
	./get-role-vars.sh cie-ato-admin
	```
	2. Copy the output of the script
2. ssh to the management server
3. paste in the output from the `get-role-vars.sh` - this sets the credentials so that now all AWS CLI commands will use the
crednetials for the role assumed in #1
4. Setup kubeconfig for the cluster (this probably is already set, but is a good talking point for the demo)
```shell 
aws eks update-kubeconfig --name <cluster-name>
```
5. Show the running pods - this demonstrates that the cluster is setup correctly (pods can deploy) and that we can connect to it
``` shell
kubectl get pods -A
```
6. Show grafana - this demos that the big bang tools have been deployed
	1. open a web browser
	2. go to https://grafana.c3ms.org
	3. login with the credentials provided in the big bang documentation - https://docs-bigbang.dso.mil/latest/docs/guides/using-bigbang/default-credentials/
	4. Open the Kubernetes/Compute Resources/Cluster Dashboard - this is provides cluster administrators with a way to monitor the 
	cluster and provide feedback to the mission app teams
		1. Dashboards on the left panel
		2. Scroll down to and find Kubernetes/Compute Resources/Cluster Dashboard
		3. When the dashboard loads talk about the different namespaces - maybe highlight the boutique-with-istio namespace as the
		sample mission app
7. Show the sample mission app - go to `frontend.default.svc.cluster.local` - this is a sample microservices application 
with multiple microservices providing the shopping page, shopping cart, etc.

---

## Create microservices zarf package

This repo contains files copied from `https://github.com/GoogleCloudPlatform/microservices-demo` necessary to deploy the microservice
demo to a cluster. These files have been used to create a zarf package that works to deploy the sample app, however there are still 
limitations with istio. If a new package is needed, use the following steps after making the necessary changes to the deployment yaml files

1. Update `zarf.yaml`
This is the file that will create the zarf package. If the are additional manifests to add, or images that have been added/updated
this file will need to be updated.

NOTE: The manifests are applied in bottom up order and may overwrite each other

2. Create a new package 
```shell 
zarf package create
```

3. Upload the package to S3 or the management server
