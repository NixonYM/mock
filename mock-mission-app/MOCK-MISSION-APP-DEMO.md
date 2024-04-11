## Demo Objectives

1. Prove IaC scripts created an operational EKS cluster using a mock mission app deployment
2. Show Grafana providing diagnostic info about the "misbehaving" mock mission app
3. Show Grafana providing information about policy engine violations in the cluster
4. Show Keycloak as a possible SSO solution for Ops tool logons and as an additional layer of protection

## Setup for the demo

1. Stand up a cluster in the CIE using the terraform and instructions from https://git.c3ms.org/jasc-projects/jcc2-dsop

2. Deploy big bang using the default core modules installs, and adding in keycloak (this is w/o using zarf for now). These steps steps will always use the "c3ms.org" domain name and server certs.

```
ssh -F ssh_config 10.52.8.176  (login to mgmt server)
copy env from ./get-role-vars.sh cie-ato-admin
aws eks update-kubeconfig --name jeff

cluster/cluster$ ./deploy-ebs-csi.sh jeff

export REGISTRY1_USERNAME=<REPLACE_ME>
export REGISTRY1_PASSWORD=<REPLACE_ME>

$HOME/git clone https://repo1.dso.mil/big-bang/bigbang.git
git checkout 2.2.0

$HOME/bigbang/scripts/install_flux.sh -u $REGISTRY1_USERNAME -p $REGISTRY1_PASSWORD
kubectl get po -n=flux-system

copy/update file $HOME/mock-mission-app-demo/ib_creds.yaml using [SSO QUICKSTART](https://docs-bigbang.dso.mil/latest/docs/guides/deployment-scenarios/sso-quickstart/#Step-6-Install-Big-Bang-on-Workload-Cluster)

update $HOME/mock-mission-app-demo/keycloak-ingress-certs.yaml with the current c3ms.org private key and cert chain.

edit $HOME/bigbang/chart/values.yaml to change "domain:" value to "c3ms.org"
edit $HOME/bigbang/docs/assets/configs/example/keycloak-dev-values.yaml to change:
  delete first 3 lines, which is the "comments:" attribute
  change "domain:" value to "c3ms.org"
  change the "KC_HOST" value to "c3ms.org"

helm upgrade --install bigbang $HOME/bigbang/chart \
  --values $HOME/mock-mission-app-demo/keycloak-ingress-certs.yaml \
  --values $HOME/mock-mission-app-demo/ib_creds.yaml \
  --values $HOME/bigbang/docs/assets/configs/example/keycloak-dev-values.yaml \
  --namespace=bigbang --create-namespace
```

3. Deploy mock-mission-app that will be protected (next step) by authservice and keycloak SSO
```
kubectl create ns sso-mock-mission-app
kubectl label ns sso-mock-mission-app istio-injection=enabled
kubectl get secret private-registry -n=istio-system -o yaml | sed 's/namespace: .*/namespace: sso-mock-mission-app/' | kubectl apply -f -
kubectl apply -k github.com/stefanprodan/podinfo/kustomize -n sso-mock-mission-app  (intentional fail)
kubectl edit clusterpolicy -n kyverno
  > image: ghcr.io* | registry1.dso.mil* | registry.dso.mil*  (x3 lines)
kubectl apply -k github.com/stefanprodan/podinfo/kustomize -n sso-mock-mission-app

kubectl apply -f $HOME/mock-mission-app-demo/sso-mock-mission-app-vs.yaml -n sso-mock-mission-app
```

4. Deploy Authservice and enable the SSO login option on Grafana UI. SSO/Keycloak autoredirect will be enabled for the sso-mock-mission-app, but not enabled for Grafana.
```
Login to Keycloak as an admin user.

Create the Keycloak client for the sso-mock-mission-app [](https://docs-bigbang.dso.mil/latest/docs/guides/deployment-scenarios/sso-quickstart/#Step-14-Create-an-Application-Identity--Service-Account--Non-Person-Entity-in-Keycloak-for-the-authdemo-webpage)

Copy the "client secret" for both 1) grafana and 2) authdemo and update the following yaml files 

copy/update file $HOME/mock-mission-app-demo/auth_service_demo_values.yaml using [SSO QUICKSTART](https://docs-bigbang.dso.mil/latest/docs/guides/deployment-scenarios/sso-quickstart/#Step-15-Deploy-auth-service-to-the-workload-cluster-and-use-it-to-protect-the-mock-mission-app)

copy/update file to $HOME/mock-mission-app-demo/grafana_auth_service_enabled.yaml from this repo

helm upgrade --install bigbang $HOME/bigbang/chart \
  --values $HOME/mock-mission-app-demo/keycloak-ingress-certs.yaml \
  --values $HOME/mock-mission-app-demo/ib_creds.yaml \
  --values $HOME/bigbang/docs/assets/configs/example/keycloak-dev-values.yaml \
  --values $HOME/mock-mission-app-demo/grafana_auth_service_enabled.yaml \
  --values $HOME/mock-mission-app-demo/auth_service_demo_values.yaml \
  --namespace=bigbang
```

5. Enforce Keycloak redirect when trying to access the sso-mock-mission-app 
```
kubectl patch deployment podinfo --type merge --patch "$(cat $HOME/mock-mission-app-demo/pods-in-deployment-label-patch.yaml)" -n sso-mock-mission-app
```

6. Deploy the mock mission application that will not be protected by SSO. This will trigger two policy violations that we can show in Grafana. Violations will be 1) pull images from unauthorized registry and 2) namespace not labeled with "istio-injection".
```
ssh -F ssh_config 10.52.8.176  (login to mgmt server)
copy env from ./get-role-vars.sh cie-ato-admin
aws eks update-kubeconfig --name jeff

kubectl create ns mock-mission-app
kubectl apply -f mock-mission-app-manifests.yaml -n mock-mission-app  (intentional fail)
kubectl edit clusterpolicy -n kyverno
  > image: gcr.io* | docker.io* | ghcr.io* | registry1.dso.mil* | registry.dso.mil*  (x3 lines)
  > validationFailureAction: audit  (~line 2480)
kubectl apply -f mock-mission-app-manifests.yaml -n mock-mission-app

```

## Demo steps
1. Scale the mock app load generator. Do this ahead of time, not live.
```
kubectl scale --replicas=15 deployment/loadgenerator -n mock-mission-app
wait for 2 minutes
kubectl scale --replicas=1 deployment/loadgenerator -n mock-mission-app
```

2. Update /etc/hosts file in the case of the LB IPs changing.
```
kubectl get services -n istio-system      (x2 IPs to verify)
kubectl get services -n mock-mission-app  (x1 IP to verify)

```
3. Briefly show mock-mission-app UI as proof point that our Terraform worked.
```
http://frontend.default.svc.cluster.local/
```

4. Show Grafana as a Ops tool that can be used to find unusual cluster behavior.
```
Login to Grafana
Prep steps:
  Remove any home page UI panels that show blogs, etc. 
  Have two dashboards already starred so they are easily launched from the home page
  1. Dashboards > Kubernetes > Compute Resources / Cluster
  2. Dashboards > PolicyReports
```

```
Demo steps:
Dashboards > Kubernetes > Compute Resources / Cluster
Set time range to "Last 30 minutes" and refresh to "5s"
 1. "CPU > click entry for namespace mock-mission-app and show uptick from loadgenerator pods
 2. Memory > click entry for namespace mock-mission-app and show uptick from loadgenerator pods
 
Dashboards > PolicyReports >
 1. "Failing policies by namespace" > point out count of mock-mission-app namespace
 2. "Failing policies graph" > click on each policy name to show violations over time:
   require-istio-on-namespaces
   restrict-image-registries
 3. "Failing ClusterPolicyRules" > show mock-mission-app can be identified for:
   require-istio-on-namespaces
   restrict-image-registries

Optional steps:
 1. kubectl label namespace mock-mission-application istio-injection=enabled
 2. kubectl -n kiali create token kiali-service-account (for Kiali UI access) 
```

5. Talking point of how Keycloak can be used as an SSO entry point for EKS Ops tools and possibly JCC2 mission applications.
```
Sign out of Grafana and show login page has "Sign in with SSO" option.
  -- Automatic redirect can be enabled to enforce this as the only login option and serve as a "visibility cloak" for users not assigned group policy.
Click "Sign in with SSO" > Keycloak UI
  -- Talking points: SSO, UI branding, enforcing MFA, least priviledge for Ops team, OIDC standards, self-registration
URL to https://keycloak.bigbang.dev/auth/admin
  -- Manage > Group > Members

Show automatic login redirect for the SSO protect mock mission app: https://authdemo.bigbang.dev
  -- Requires deployment of app from [SSO QUICKSTART](https://docs-bigbang.dso.mil/latest/docs/guides/deployment-scenarios/sso-quickstart/#Step-11-Deploy-a-mock-mission-application-to-the-workload-cluster)
```

## Teardown
```
kubectl delete ns mock-mission-app
kubectl delete ns sso-mock-mission-app (if second sample app was used)
helm uninstall bigbang -n bigbang
kubectl delete namespace flux-system

terraform destroy, as applicable
```


## Scratchpad for future work

1. Use zarf deployment of BB
2. Mock-mission-app(s) is zarf packaged
3. CAC integration
4. Show xfer process of Dev teams getting artifacts & manifests to cluster "staging" -> ECR
5.  Load balancer stability
