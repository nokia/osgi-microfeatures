package com.nokia.as.k8s.controller.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.k8s.controller.impl.Watcher.WatchHandleImpl;
import com.nokia.as.k8s.controller.serialization.SerializationUtils;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AdmissionregistrationV1beta1Api;
import io.kubernetes.client.openapi.apis.ApiextensionsV1beta1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressList;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ClusterRoleBindingList;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1RoleBindingList;
import io.kubernetes.client.openapi.models.V1RoleList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1ServiceAccountList;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1beta1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1beta1CustomResourceDefinitionList;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1beta1MutatingWebhookConfiguration;
import io.kubernetes.client.openapi.models.V1beta1MutatingWebhookConfigurationList;
import io.kubernetes.client.openapi.models.V1beta1ValidatingWebhookConfiguration;
import io.kubernetes.client.openapi.models.V1beta1ValidatingWebhookConfigurationList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

@Component
public class ResourceServiceImpl implements ResourceService {

	@ServiceDependency
	private LogServiceFactory logFactory;

	@ServiceDependency
	PlatformExecutors pfes;

	@Inject
	private BundleContext bc;

	private ApiClient kubernetesClient;
	private CoreV1Api corev1Api;
	private CustomObjectsApi customObjectsApi;
	private ExtensionsV1beta1Api extensionsv1beta1Api;
	private RbacAuthorizationV1Api rbacApi;
	private BatchV1Api batchApi;
	private ApiextensionsV1beta1Api apiExtensionsApi;
	private AdmissionregistrationV1beta1Api admissionRegistrationApi;
	private AppsV1Api appsApi;

	private LogService logger;
	private Watcher watch;

	private String currentNamespace;
	private List<String> namespaces;

	Map<Class<?>, ExceptionalTriConsumer> creators;
	Map<Class<?>, ExceptionalTriConsumer> updaters;
	Map<Class<?>, ExceptionalTriConsumer> deleters;
	Map<Class<?>, ExceptionalTriConsumer> getters;
	Map<Class<?>, ExceptionalTriConsumer> allGetters;
	Map<Class<?>, ExceptionalBiFunction> watchers;
	Map<Class<?>, BiFunction<Object, V1OwnerReference, Object>> ownerReference;
	Map<Class<?>, Function<Object, V1OwnerReference>> ownerReferenceBlock;
	
	Map<Class<?>, Function<Object, List<?>>> toResList;
	Map<Class<?>, Function<Object, String>> toName;
	
	private static boolean inCluster = System.getProperty("k8s.namespace") != null && !System.getProperty("k8s.namespace").isEmpty();
	
	@SuppressWarnings("unchecked")
	@Start
	public void start() throws Exception {
		
		logger = logFactory.getLogger(getClass());
		if(!inCluster) {
			logger.warn("Not running in cluster...");
			return;
		}

		kubernetesClient = Config.defaultClient();
		//kubernetesClient.setDebugging(true);
		kubernetesClient.setHttpClient(kubernetesClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build());
		corev1Api = new CoreV1Api(kubernetesClient);
		customObjectsApi = new CustomObjectsApi(kubernetesClient);
		extensionsv1beta1Api = new ExtensionsV1beta1Api(kubernetesClient);
		rbacApi = new RbacAuthorizationV1Api(kubernetesClient);
		batchApi = new BatchV1Api(kubernetesClient);
		apiExtensionsApi = new ApiextensionsV1beta1Api(kubernetesClient);
		admissionRegistrationApi = new AdmissionregistrationV1beta1Api(kubernetesClient);
		appsApi = new AppsV1Api(kubernetesClient);
		
		currentNamespace = currentNamespace().get(10, TimeUnit.SECONDS);
		logger.info("current namespace " + currentNamespace);
		
		try {
			namespaces = listNamespaces().get(10, TimeUnit.SECONDS);
			logger.info("known namespaces " + namespaces);
			watch = new Watcher(namespaces, bc);
		} catch(Exception e) {
			logger.warn("Unable to get all namespaces... maybe not enough permissions?");
			logger.debug("Exception is: ", e);
			watch = new Watcher(Arrays.asList(currentNamespace), bc);
		}
		
		creators = new HashMap<>();
		creators.put(CustomResource.class, (n, o, f) -> {
			CustomResource cr = (CustomResource) o;
			customObjectsApi.createNamespacedCustomObjectAsync(cr.definition().group(), cr.definition().version(), n, 
															   cr.definition().names().plural(), cr.attributes(), null, null, null, f);
		});
		creators.put(V1Pod.class, (n, o, f) -> corev1Api.createNamespacedPodAsync(n, (V1Pod) o, null, null, null, f));
		creators.put(V1ConfigMap.class, (n, o, f) -> corev1Api.createNamespacedConfigMapAsync(n, (V1ConfigMap) o, null, null, null, f));
		creators.put(V1Secret.class, (n, o, f) -> corev1Api.createNamespacedSecretAsync(n, (V1Secret) o, null, null, null, f));
		creators.put(V1Service.class, (n, o, f) -> corev1Api.createNamespacedServiceAsync(n, (V1Service) o, null, null, null, f));
		creators.put(ExtensionsV1beta1Ingress.class, (n, o, f) -> extensionsv1beta1Api.createNamespacedIngressAsync(n, (ExtensionsV1beta1Ingress) o, null, null, null, f));
		creators.put(V1Deployment.class, (n, o, f) -> appsApi.createNamespacedDeploymentAsync(n, (V1Deployment) o, null, null, null, f));
		creators.put(V1DaemonSet.class, (n, o, f) -> appsApi.createNamespacedDaemonSetAsync(n, (V1DaemonSet) o, null, null, null, f));
		creators.put(V1ServiceAccount.class, (n, o, f) -> corev1Api.createNamespacedServiceAccountAsync(n, (V1ServiceAccount) o, null, null, null, f));
		creators.put(V1ClusterRole.class, (n, o, f) -> rbacApi.createClusterRoleAsync((V1ClusterRole) o, null, null, null, f));
		creators.put(V1ClusterRoleBinding.class, (n, o, f) -> rbacApi.createClusterRoleBindingAsync((V1ClusterRoleBinding) o, null, null, null, f));
		creators.put(V1Role.class, (n, o, f) -> rbacApi.createNamespacedRoleAsync(n, (V1Role) o, null, null, null, f));
		creators.put(V1RoleBinding.class, (n, o, f) -> rbacApi.createNamespacedRoleBindingAsync(n, (V1RoleBinding) o, null, null, null, f));
		creators.put(V1Job.class, (n, o, f) -> batchApi.createNamespacedJobAsync(n, (V1Job) o, null, null, null, f));
		creators.put(V1beta1CustomResourceDefinition.class, (n, o, f) -> apiExtensionsApi.createCustomResourceDefinitionAsync((V1beta1CustomResourceDefinition) o, null, null, null, f));
		creators.put(V1beta1ValidatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.createValidatingWebhookConfigurationAsync((V1beta1ValidatingWebhookConfiguration) o, null, null, null, f));
		creators.put(V1beta1MutatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.createMutatingWebhookConfigurationAsync((V1beta1MutatingWebhookConfiguration) o, null, null, null, f));
		
		updaters = new HashMap<>();
		updaters.put(CustomResource.class, (n, o, f) -> {
			CustomResource cr = (CustomResource) ((NameRes) o).res;
			customObjectsApi.patchNamespacedCustomObjectAsync(cr.definition().group(), cr.definition().version(), n, 
															   cr.definition().names().plural(), ((NameRes) o).name, cr.attributes(), null, null, null, f);
		});
		updaters.put(V1Pod.class, (n, o, f) -> corev1Api.replaceNamespacedPodAsync(((NameRes) o).name, n, (V1Pod) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1ConfigMap.class, (n, o, f) -> corev1Api.replaceNamespacedConfigMapAsync(((NameRes) o).name, n, (V1ConfigMap) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1Secret.class, (n, o, f) -> corev1Api.replaceNamespacedSecretAsync(((NameRes) o).name, n, (V1Secret) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1Service.class, (n, o, f) -> corev1Api.replaceNamespacedServiceAsync(((NameRes) o).name, n, (V1Service) ((NameRes) o).res, null, null, null, f));
		updaters.put(ExtensionsV1beta1Ingress.class, (n, o, f) -> extensionsv1beta1Api.replaceNamespacedIngressAsync(((NameRes) o).name, n, (ExtensionsV1beta1Ingress) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1Deployment.class, (n, o, f) -> appsApi.replaceNamespacedDeploymentAsync(((NameRes) o).name, n, (V1Deployment) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1DaemonSet.class, (n, o, f) -> appsApi.replaceNamespacedDaemonSetAsync(((NameRes) o).name, n, (V1DaemonSet) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1ServiceAccount.class, (n, o, f) -> corev1Api.replaceNamespacedServiceAccountAsync(((NameRes) o).name, n, (V1ServiceAccount) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1ClusterRole.class, (n, o, f) -> rbacApi.replaceClusterRoleAsync(((NameRes) o).name, (V1ClusterRole) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1ClusterRoleBinding.class, (n, o, f) -> rbacApi.replaceClusterRoleBindingAsync(((NameRes) o).name, (V1ClusterRoleBinding) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1Role.class, (n, o, f) -> rbacApi.replaceNamespacedRoleAsync(((NameRes) o).name, n, (V1Role) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1RoleBinding.class, (n, o, f) -> rbacApi.replaceNamespacedRoleBindingAsync(((NameRes) o).name, n, (V1RoleBinding) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1Job.class, (n, o, f) -> batchApi.replaceNamespacedJobAsync(((NameRes) o).name, n, (V1Job) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1beta1CustomResourceDefinition.class, (n, o, f) -> apiExtensionsApi.replaceCustomResourceDefinitionAsync(((NameRes) o).name, (V1beta1CustomResourceDefinition) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1beta1ValidatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.replaceValidatingWebhookConfigurationAsync(((NameRes) o).name, (V1beta1ValidatingWebhookConfiguration) ((NameRes) o).res, null, null, null, f));
		updaters.put(V1beta1MutatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.replaceMutatingWebhookConfigurationAsync(((NameRes) o).name, (V1beta1MutatingWebhookConfiguration) ((NameRes) o).res, null, null, null, f));
		
		final String policy = "Foreground";
		V1DeleteOptions delOpts = new V1DeleteOptions().gracePeriodSeconds(5l).propagationPolicy(policy);
		deleters = new HashMap<>();
		deleters.put(CustomResource.class, (n, o, f) -> {
			CustomResource cr = (CustomResource) o;
			customObjectsApi.deleteNamespacedCustomObjectAsync(cr.definition().group(), cr.definition().version(), n, 
															   cr.definition().names().plural(), cr.name(), 5, null, policy, null, delOpts, f);	
		});
		deleters.put(V1Pod.class, (n, o, f) -> corev1Api.deleteNamespacedPodAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1ConfigMap.class, (n, o, f) -> corev1Api.deleteNamespacedConfigMapAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1Secret.class, (n, o, f) -> corev1Api.deleteNamespacedSecretAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1Service.class, (n, o, f) -> corev1Api.deleteNamespacedServiceAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(ExtensionsV1beta1Ingress.class, (n, o, f) -> extensionsv1beta1Api.deleteNamespacedIngressAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1Deployment.class, (n, o, f) -> appsApi.deleteNamespacedDeploymentAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1DaemonSet.class, (n, o, f) -> appsApi.deleteNamespacedDaemonSetAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1ServiceAccount.class, (n, o, f) -> corev1Api.deleteNamespacedServiceAccountAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1ClusterRole.class, (n, o, f) -> rbacApi.deleteClusterRoleAsync((String) o, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1ClusterRoleBinding.class, (n, o, f) -> rbacApi.deleteClusterRoleBindingAsync((String) o, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1Role.class, (n, o, f) -> rbacApi.deleteNamespacedRoleAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1RoleBinding.class, (n, o, f) -> rbacApi.deleteNamespacedRoleBindingAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1Job.class, (n, o, f) -> batchApi.deleteNamespacedJobAsync((String) o, n, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1beta1CustomResourceDefinition.class, (n, o, f) -> apiExtensionsApi.deleteCustomResourceDefinitionAsync((String) o, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1beta1ValidatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.deleteValidatingWebhookConfigurationAsync((String) o, null, null, 5, null, policy, delOpts, f));
		deleters.put(V1beta1MutatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.deleteMutatingWebhookConfigurationAsync((String) o, null, null, 5, null, policy, delOpts, f));
		
		getters = new HashMap<>();
		getters.put(CustomResource.class, (n, o, f) -> {
			CustomResource cr = (CustomResource) o;
			customObjectsApi.getNamespacedCustomObjectAsync(cr.definition().group(), cr.definition().version(), n, 
															cr.definition().names().plural(), cr.name(), f);
		});
		getters.put(V1Pod.class, (n, o, f) -> corev1Api.readNamespacedPodAsync((String) o, n, null, null, null, f));
		getters.put(V1ConfigMap.class, (n, o, f) -> corev1Api.readNamespacedConfigMapAsync((String) o, n, null, null, null, f));
		getters.put(V1Secret.class, (n, o, f) -> corev1Api.readNamespacedSecretAsync((String) o, n, null, null, null, f));
		getters.put(V1Service.class, (n, o, f) -> corev1Api.readNamespacedServiceAsync((String) o, n, null, null, null, f));
		getters.put(ExtensionsV1beta1Ingress.class, (n, o, f) -> extensionsv1beta1Api.readNamespacedIngressAsync((String) o, n, null, null, null, f));
		getters.put(V1Deployment.class, (n, o, f) -> appsApi.readNamespacedDeploymentAsync((String) o, n, null, null, null, f));
		getters.put(V1DaemonSet.class, (n, o, f) -> appsApi.readNamespacedDaemonSetAsync((String) o, n, null, null, null, f));
		getters.put(V1ServiceAccount.class, (n, o, f) -> corev1Api.readNamespacedServiceAccountAsync((String) o, n, null, null, null, f));
		getters.put(V1ClusterRole.class, (n, o, f) -> rbacApi.readClusterRoleAsync((String) o, null, f));
		getters.put(V1ClusterRoleBinding.class, (n, o, f) -> rbacApi.readClusterRoleBindingAsync((String) o, null, f));
		getters.put(V1Role.class, (n, o, f) -> rbacApi.readNamespacedRoleAsync((String) o, n, null, f));
		getters.put(V1RoleBinding.class, (n, o, f) -> rbacApi.readNamespacedRoleBindingAsync((String) o, n, null, f));
		getters.put(V1Job.class, (n, o, f) -> batchApi.readNamespacedJobAsync((String) o, n, null, null, null, f));
		getters.put(V1beta1CustomResourceDefinition.class, (n, o, f) -> apiExtensionsApi.readCustomResourceDefinitionAsync((String) o, null, null, null, f));
		getters.put(V1beta1ValidatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.readValidatingWebhookConfigurationAsync((String) o, null, null, null, f));
		getters.put(V1beta1MutatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.readMutatingWebhookConfigurationAsync((String) o, null, null, null, f));
		
		allGetters = new HashMap<>();
		allGetters.put(CustomResource.class, (n, o, f) -> {
			CustomResourceDefinition crd = (CustomResourceDefinition) o;
			customObjectsApi.listNamespacedCustomObjectAsync(crd.group(), crd.version(), n, crd.names().plural(), 
															 null, null, null, null, null, null, null, null, f);
		});
		allGetters.put(V1Pod.class, (n, o, f) -> corev1Api.listNamespacedPodAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1ConfigMap.class, (n, o, f) -> corev1Api.listNamespacedConfigMapAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1Secret.class, (n, o, f) -> corev1Api.listNamespacedSecretAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1Service.class, (n, o, f) -> corev1Api.listNamespacedServiceAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(ExtensionsV1beta1Ingress.class, (n, o, f) -> extensionsv1beta1Api.listNamespacedIngressAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1Deployment.class, (n, o, f) -> appsApi.listNamespacedDeploymentAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1DaemonSet.class, (n, o, f) -> appsApi.listNamespacedDaemonSetAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1ServiceAccount.class, (n, o, f) -> corev1Api.listNamespacedServiceAccountAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1ClusterRole.class, (n, o, f) -> rbacApi.listClusterRoleAsync(null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1ClusterRoleBinding.class, (n, o, f) -> rbacApi.listClusterRoleBindingAsync(null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1Role.class, (n, o, f) -> rbacApi.listNamespacedRoleAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1RoleBinding.class, (n, o, f) -> rbacApi.listNamespacedRoleBindingAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1Job.class, (n, o, f) -> batchApi.listNamespacedJobAsync(n, null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1beta1CustomResourceDefinition.class, (n, o, f) -> apiExtensionsApi.listCustomResourceDefinitionAsync(null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1beta1ValidatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.listValidatingWebhookConfigurationAsync(null, null, null, null, null, null, null, null, null, null, f));
		allGetters.put(V1beta1MutatingWebhookConfiguration.class, (n, o, f) -> admissionRegistrationApi.listMutatingWebhookConfigurationAsync(null, null, null, null, null, null, null, null, null, null, f));
		
		toResList = new HashMap<>();
		toResList.put(V1Pod.class, o -> ((V1PodList) o).getItems());
		toResList.put(V1ConfigMap.class, o -> ((V1ConfigMapList) o).getItems());
		toResList.put(V1Secret.class, o -> ((V1SecretList) o).getItems());
		toResList.put(V1Service.class, o -> ((V1ServiceList) o).getItems());
		toResList.put(ExtensionsV1beta1Ingress.class, o -> ((ExtensionsV1beta1IngressList) o).getItems());
		toResList.put(V1Deployment.class, o -> ((V1DeploymentList) o).getItems());
		toResList.put(V1DaemonSet.class, o -> ((V1DaemonSetList) o).getItems());
		toResList.put(V1ServiceAccount.class, o -> ((V1ServiceAccountList) o).getItems());
		toResList.put(V1ClusterRole.class, o -> ((V1ClusterRoleList) o).getItems());
		toResList.put(V1ClusterRoleBinding.class, o -> ((V1ClusterRoleBindingList) o).getItems());
		toResList.put(V1Role.class, o -> ((V1RoleList) o).getItems());
		toResList.put(V1RoleBinding.class, o -> ((V1RoleBindingList) o).getItems());
		toResList.put(V1Job.class, o -> ((V1JobList) o).getItems());
		toResList.put(V1beta1CustomResourceDefinition.class, o -> ((V1beta1CustomResourceDefinitionList) o).getItems());
		toResList.put(V1beta1ValidatingWebhookConfiguration.class, o -> ((V1beta1ValidatingWebhookConfigurationList) o).getItems());
		toResList.put(V1beta1MutatingWebhookConfiguration.class, o -> ((V1beta1MutatingWebhookConfigurationList) o).getItems());
		
		toName = new HashMap<>();
		toName.put(CustomResource.class, o -> ((CustomResource) o).name());
		toName.put(V1Pod.class, o -> ((V1Pod) o).getMetadata().getName());
		toName.put(V1ConfigMap.class, o -> ((V1ConfigMap) o).getMetadata().getName());
		toName.put(V1Secret.class, o -> ((V1Secret) o).getMetadata().getName());
		toName.put(V1Service.class, o -> ((V1Service) o).getMetadata().getName());
		toName.put(ExtensionsV1beta1Ingress.class, o -> ((ExtensionsV1beta1Ingress) o).getMetadata().getName());
		toName.put(V1Deployment.class, o -> ((V1Deployment) o).getMetadata().getName());
		toName.put(V1DaemonSet.class, o -> ((V1DaemonSet) o).getMetadata().getName());
		toName.put(V1ServiceAccount.class, o -> ((V1ServiceAccount) o).getMetadata().getName());
		toName.put(V1ClusterRole.class, o -> ((V1ClusterRole) o).getMetadata().getName());
		toName.put(V1ClusterRoleBinding.class, o -> ((V1ClusterRoleBinding) o).getMetadata().getName());
		toName.put(V1Role.class, o -> ((V1Role) o).getMetadata().getName());
		toName.put(V1RoleBinding.class, o -> ((V1RoleBinding) o).getMetadata().getName());
		toName.put(V1Job.class, o -> ((V1Job) o).getMetadata().getName());
		toName.put(V1beta1CustomResourceDefinition.class, o -> ((V1beta1CustomResourceDefinition) o).getMetadata().getName());
		toName.put(V1beta1ValidatingWebhookConfiguration.class, o -> ((V1beta1ValidatingWebhookConfiguration) o).getMetadata().getName());
		toName.put(V1beta1MutatingWebhookConfiguration.class, o -> ((V1beta1MutatingWebhookConfiguration) o).getMetadata().getName());
		
		ownerReferenceBlock = new HashMap<>();
		ownerReferenceBlock.put(CustomResource.class, p -> createOwnerReferenceBlock(((CustomResource) p).kind(), ((CustomResource) p).name(), String.valueOf(((CustomResource) p).metadata().get("uid"))));
		ownerReferenceBlock.put(V1Pod.class, p -> createOwnerReferenceBlock(((V1Pod) p).getKind(), ((V1Pod) p).getMetadata().getName(), ((V1Pod) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1ConfigMap.class, p -> createOwnerReferenceBlock(((V1ConfigMap) p).getKind(), ((V1ConfigMap) p).getMetadata().getName(), ((V1ConfigMap) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1Secret.class, p -> createOwnerReferenceBlock(((V1Secret) p).getKind(), ((V1Secret) p).getMetadata().getName(), ((V1Secret) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1Service.class, p -> createOwnerReferenceBlock(((V1Service) p).getKind(), ((V1Service) p).getMetadata().getName(), ((V1Service) p).getMetadata().getUid()));
		ownerReferenceBlock.put(ExtensionsV1beta1Ingress.class, p -> createOwnerReferenceBlock(((ExtensionsV1beta1Ingress) p).getKind(), ((ExtensionsV1beta1Ingress) p).getMetadata().getName(), ((ExtensionsV1beta1Ingress) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1Deployment.class, p -> createOwnerReferenceBlock(((V1Deployment) p).getKind(), ((V1Deployment) p).getMetadata().getName(), ((V1Deployment) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1DaemonSet.class, p -> createOwnerReferenceBlock(((V1DaemonSet) p).getKind(), ((V1DaemonSet) p).getMetadata().getName(), ((V1DaemonSet) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1ServiceAccount.class, p -> createOwnerReferenceBlock(((V1ServiceAccount) p).getKind(), ((V1ServiceAccount) p).getMetadata().getName(), ((V1ServiceAccount) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1ClusterRole.class, p -> createOwnerReferenceBlock(((V1ClusterRole) p).getKind(), ((V1ClusterRole) p).getMetadata().getName(), ((V1ClusterRole) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1ClusterRoleBinding.class, p -> createOwnerReferenceBlock(((V1ClusterRoleBinding) p).getKind(), ((V1ClusterRoleBinding) p).getMetadata().getName(), ((V1ClusterRoleBinding) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1Role.class, p -> createOwnerReferenceBlock(((V1Role) p).getKind(), ((V1Role) p).getMetadata().getName(), ((V1Role) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1RoleBinding.class, p -> createOwnerReferenceBlock(((V1RoleBinding) p).getKind(), ((V1RoleBinding) p).getMetadata().getName(), ((V1RoleBinding) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1Job.class, p -> createOwnerReferenceBlock(((V1Job) p).getKind(), ((V1Job) p).getMetadata().getName(), ((V1Job) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1beta1CustomResourceDefinition.class, p -> createOwnerReferenceBlock(((V1beta1CustomResourceDefinition) p).getKind(), ((V1beta1CustomResourceDefinition) p).getMetadata().getName(), ((V1beta1CustomResourceDefinition) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1beta1ValidatingWebhookConfiguration.class, p -> createOwnerReferenceBlock(((V1beta1ValidatingWebhookConfiguration) p).getKind(), ((V1beta1ValidatingWebhookConfiguration) p).getMetadata().getName(), ((V1beta1ValidatingWebhookConfiguration) p).getMetadata().getUid()));
		ownerReferenceBlock.put(V1beta1MutatingWebhookConfiguration.class, p -> createOwnerReferenceBlock(((V1beta1MutatingWebhookConfiguration) p).getKind(), ((V1beta1MutatingWebhookConfiguration) p).getMetadata().getName(), ((V1beta1MutatingWebhookConfiguration) p).getMetadata().getUid()));
		
		ownerReference = new HashMap<>();
		ownerReference.put(CustomResource.class, (c, o) -> {
			CustomResource res = (CustomResource) c;
			res.metadata("ownerReferences", Arrays.asList(o));
			return res;
		});
		ownerReference.put(V1Pod.class, (c, o) -> ((V1Pod) c).metadata(((V1Pod) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1ConfigMap.class, (c, o) -> ((V1ConfigMap) c).metadata(((V1ConfigMap) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1Secret.class, (c, o) -> ((V1Secret) c).metadata(((V1Secret) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1Service.class, (c, o) -> ((V1Service) c).metadata(((V1Service) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(ExtensionsV1beta1Ingress.class, (c, o) -> ((ExtensionsV1beta1Ingress) c).metadata(((ExtensionsV1beta1Ingress) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1Deployment.class, (c, o) -> ((V1Deployment) c).metadata(((V1Deployment) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1DaemonSet.class, (c, o) -> ((V1DaemonSet) c).metadata(((V1DaemonSet) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1ServiceAccount.class, (c, o) -> ((V1ServiceAccount) c).metadata(((V1ServiceAccount) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1ClusterRole.class, (c, o) -> ((V1ClusterRole) c).metadata(((V1ClusterRole) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1ClusterRoleBinding.class, (c, o) -> ((V1ClusterRoleBinding) c).metadata(((V1ClusterRoleBinding) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1Role.class, (c, o) -> ((V1Role) c).metadata(((V1Role) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1RoleBinding.class, (c, o) -> ((V1RoleBinding) c).metadata(((V1RoleBinding) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1Job.class, (c, o) -> ((V1Job) c).metadata(((V1Job) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1beta1CustomResourceDefinition.class, (c, o) -> ((V1beta1CustomResourceDefinition) c).metadata(((V1beta1CustomResourceDefinition) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1beta1ValidatingWebhookConfiguration.class, (c, o) -> ((V1beta1ValidatingWebhookConfiguration) c).metadata(((V1beta1ValidatingWebhookConfiguration) c).getMetadata().addOwnerReferencesItem(o)));
		ownerReference.put(V1beta1MutatingWebhookConfiguration.class, (c, o) -> ((V1beta1MutatingWebhookConfiguration) c).metadata(((V1beta1MutatingWebhookConfiguration) c).getMetadata().addOwnerReferencesItem(o)));
		
		watchers = new HashMap<>();
		watchers.put(CustomResource.class, (n, o) -> {
			CustomResourceDefinition crd = (CustomResourceDefinition) o;
			return Watch.createWatch(kubernetesClient, 
									 customObjectsApi.listNamespacedCustomObjectCall(crd.group(), crd.version(), n, crd.names().plural(), null, null, null, null, null, getResourceVersion(n, crd), null, true, null), 
									 new TypeToken<Watch.Response<Object>>(){}.getType());
		});
		watchers.put(V1Pod.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  corev1Api.listNamespacedPodCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Pod.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Pod>>(){}.getType()));
		watchers.put(V1ConfigMap.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  corev1Api.listNamespacedConfigMapCall(n, null, null, null, null, null, null, getResourceVersion(n, V1ConfigMap.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1ConfigMap>>(){}.getType()));
		watchers.put(V1Secret.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  corev1Api.listNamespacedSecretCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Secret.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Secret>>(){}.getType()));
		watchers.put(V1Service.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  corev1Api.listNamespacedServiceCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Service.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Service>>(){}.getType()));
		watchers.put(ExtensionsV1beta1Ingress.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  extensionsv1beta1Api.listNamespacedIngressCall(n, null, null, null, null, null, null, getResourceVersion(n, ExtensionsV1beta1Ingress.class), null, null, true, null), 
				  new TypeToken<Watch.Response<ExtensionsV1beta1Ingress>>(){}.getType()));
		watchers.put(V1Deployment.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  appsApi.listNamespacedDeploymentCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Deployment.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Deployment>>(){}.getType()));
		watchers.put(V1DaemonSet.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  appsApi.listNamespacedDaemonSetCall(n, null, null, null, null, null, null, getResourceVersion(n, V1DaemonSet.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1DaemonSet>>(){}.getType()));
		watchers.put(V1ServiceAccount.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  corev1Api.listNamespacedServiceAccountCall(n, null, null, null, null, null, null, getResourceVersion(n, V1ServiceAccount.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1ServiceAccount>>(){}.getType()));
		watchers.put(V1ClusterRole.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  rbacApi.listClusterRoleCall(null, null, null, null, null, null, getResourceVersion(n, V1ClusterRole.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1ClusterRole>>(){}.getType()));
		watchers.put(V1ClusterRoleBinding.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  rbacApi.listClusterRoleBindingCall(null, null, null, null, null, null, getResourceVersion(n, V1ClusterRoleBinding.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1ClusterRoleBinding>>(){}.getType()));
		watchers.put(V1Role.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  rbacApi.listNamespacedRoleCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Role.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Role>>(){}.getType()));
		watchers.put(V1RoleBinding.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  rbacApi.listNamespacedRoleBindingCall(n, null, null, null, null, null, null, getResourceVersion(n, V1RoleBinding.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1RoleBinding>>(){}.getType()));
		watchers.put(V1Job.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  batchApi.listNamespacedJobCall(n, null, null, null, null, null, null, getResourceVersion(n, V1Job.class), null, null, true, null), 
				  new TypeToken<Watch.Response<V1Job>>(){}.getType()));
		watchers.put(V1beta1CustomResourceDefinition.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  apiExtensionsApi.listCustomResourceDefinitionCall(null, null, null, null, null, null, getResourceVersion(n, V1beta1CustomResourceDefinition.class), null, null, true, null),
				  new TypeToken<Watch.Response<V1beta1CustomResourceDefinition>>(){}.getType()));
		watchers.put(V1beta1ValidatingWebhookConfiguration.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  admissionRegistrationApi.listValidatingWebhookConfigurationCall(null, null, null, null, null, null, getResourceVersion(n, V1beta1ValidatingWebhookConfiguration.class), null, null, true, null),
				  new TypeToken<Watch.Response<V1beta1ValidatingWebhookConfiguration>>(){}.getType()));
		watchers.put(V1beta1MutatingWebhookConfiguration.class, (n, o) -> Watch.createWatch(kubernetesClient, 
				  admissionRegistrationApi.listMutatingWebhookConfigurationCall(null, null, null, null, null, null, getResourceVersion(n, V1beta1MutatingWebhookConfiguration.class), null, null, true, null),
				  new TypeToken<Watch.Response<V1beta1MutatingWebhookConfiguration>>(){}.getType()));
	}
	
	private <T> String getResourceVersion(String namespace, Class<T> clazz) {
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			allGetters.get(clazz).apply(namespace, null, future);
		} catch (ApiException e) {
			logger.warn("GetAll failed", e);
			future.completeExceptionally(e);
		}
		
		Map<Class<?>, Function<Object, String>> toResVer = new HashMap<>();
		toResVer.put(V1Pod.class, o -> ((V1PodList) o).getMetadata().getResourceVersion());
		toResVer.put(V1ConfigMap.class, o -> ((V1ConfigMapList) o).getMetadata().getResourceVersion());
		toResVer.put(V1Secret.class, o -> ((V1SecretList) o).getMetadata().getResourceVersion());
		toResVer.put(V1Service.class, o -> ((V1ServiceList) o).getMetadata().getResourceVersion());
		toResVer.put(ExtensionsV1beta1Ingress.class, o -> ((ExtensionsV1beta1Ingress) o).getMetadata().getResourceVersion());
		toResVer.put(V1Deployment.class, o -> ((V1DeploymentList) o).getMetadata().getResourceVersion());
		toResVer.put(V1DaemonSet.class, o -> ((V1DaemonSetList) o).getMetadata().getResourceVersion());
		toResVer.put(V1ServiceAccount.class, o -> ((V1ServiceAccountList) o).getMetadata().getResourceVersion());
		toResVer.put(V1ClusterRole.class, o -> ((V1ClusterRoleList) o).getMetadata().getResourceVersion());
		toResVer.put(V1ClusterRoleBinding.class, o -> ((V1ClusterRoleBindingList) o).getMetadata().getResourceVersion());
		toResVer.put(V1Role.class, o -> ((V1RoleList) o).getMetadata().getResourceVersion());
		toResVer.put(V1RoleBinding.class, o -> ((V1RoleBindingList) o).getMetadata().getResourceVersion());
		toResVer.put(V1Job.class, o -> ((V1JobList) o).getMetadata().getResourceVersion());
		toResVer.put(V1beta1CustomResourceDefinition.class, o -> ((V1beta1CustomResourceDefinitionList) o).getMetadata().getResourceVersion());
		toResVer.put(V1beta1ValidatingWebhookConfiguration.class, o -> ((V1beta1ValidatingWebhookConfigurationList) o).getMetadata().getResourceVersion());
		toResVer.put(V1beta1MutatingWebhookConfiguration.class, o -> ((V1beta1MutatingWebhookConfigurationList) o).getMetadata().getResourceVersion());
		
		try {
			String version = future.thenCompose(o -> {
				if(o == null) return null;
				return CompletableFuture.completedFuture(toResVer.get(clazz).apply(o));
			}).get(5, TimeUnit.SECONDS);
			logger.debug("Resource version for %s is %s", clazz, version);
			return version;
		} catch (Exception e) {
			logger.debug("Could not fetch resource version for %s", clazz);
			return null;
		}
	}
	
	private String getResourceVersion(String namespace, CustomResourceDefinition def) {
		
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			allGetters.get(CustomResource.class).apply(namespace, def, future);
		} catch (ApiException e) {
			logger.warn("GetAll failed", e);
			future.completeExceptionally(e);
		}
		
		try {
			String version = future.thenCompose(o -> {
				Map<String,Object> result = (Map<String, Object>) o;
				Map<String,Object> metadata = (Map<String, Object>) result.get("metadata");
				return CompletableFuture.completedFuture(String.valueOf(metadata.get("resourceVersion")));
			}).get(5, TimeUnit.SECONDS);
			logger.debug("Resource version for %s is %s", def, version);
			return version;
		} catch (Exception e) {
			logger.debug("Could not fetch resource version for %s", def);
			return null;
		}
	}

	public <T> WatchHandle<T> watch(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		return watch(currentNamespace, clazz, added, modified, deleted);
	}
	
	public <T> WatchHandle<T> watch(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		WatchHandleImpl<T> handle = watch.createHandle(namespace, clazz, added, modified, deleted);
				
		recoverWatch(namespace, clazz, added).whenComplete((nothing, ex) -> {
			if(ex != null) {
				logger.warn("Exception raised when trying to recover existing resources for watch - " + ex);
			}
			logger.debug("Preparing watch task");
			watch.prepareWatchTask(namespace, clazz, added, modified, deleted, watchers.get(clazz));
			handle.valid(true);
		}).exceptionally((e -> {
				logger.warn("An exception occured %s ", e.toString());
				return null;
		}));
		
		return handle;
	}
	
	public WatchHandle<CustomResource> watch(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		return watch(currentNamespace, def, added, modified, deleted);
	}
	
	public WatchHandle<CustomResource> watch(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		WatchHandleImpl<CustomResource> handle = watch.createHandle(namespace, def, added, modified, deleted);
		
		recoverWatch(namespace, def, added).whenComplete((nothing, ex) -> {
			if(ex != null) {
				logger.warn("Exception raised when trying to recover existing resources for watch - " + ex);
			}
			logger.debug("Preparing watch task");
			watch.prepareWatchTask(namespace, def, added, modified, deleted, watchers.get(CustomResource.class));
			handle.valid(true);
			}).exceptionally((e -> {
				logger.warn("An exception occured %s ", e.toString());
				return null;
		}));
		
		return handle;
	}
	
	private CompletableFuture<Void> recoverWatch(String namespace, CustomResourceDefinition crd, Consumer<CustomResource> added) {
		return getAll(namespace, crd).thenCompose(res -> {
			for(CustomResource r : res) {
				added.accept(r);
			}
			return CompletableFuture.completedFuture(null);
		});
	}
	
	private <T> CompletableFuture<Void> recoverWatch(String namespace, Class<T> clazz, Consumer<T> added) {
		return getAll(namespace, clazz).thenCompose(res -> {
			for(T r : res) {
				added.accept(r);
			}
			return CompletableFuture.completedFuture(null);
		});
	}
	
	@Override
	public <T> CompletableFuture<Boolean> create(Class<T> clazz, T obj) {
		return create(currentNamespace, clazz, obj);
	}
	
	@Override
	public <T> CompletableFuture<Boolean> create(String namespace, Class<T> clazz, T obj) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			creators.get(clazz).apply(namespace, obj, future);
		} catch (ApiException e) {
			logger.warn("Create failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureBoolean();
	}
	
	@Override
	public CompletableFuture<Boolean> create(CustomResourceDefinition crd, CustomResource cr) {
		return create(currentNamespace, crd, cr);
	}
	
	@Override
	public CompletableFuture<Boolean> create(String namespace, CustomResourceDefinition crd, CustomResource cr) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			creators.get(CustomResource.class).apply(namespace, cr, future);
		} catch (ApiException e) {
			logger.warn("Create failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureBoolean();
	}
	
	@Override
	public <T> CompletableFuture<Boolean> update(Class<T> clazz, String resourceName, T obj) {
		return update(currentNamespace, clazz, resourceName, obj);
	}
	
	@Override
	public <T> CompletableFuture<Boolean> update(String namespace, Class<T> clazz, String resourceName, T obj) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			updaters.get(clazz).apply(namespace, new NameRes(resourceName, obj), future);
		} catch (ApiException e) {
			logger.warn("Create failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureBoolean();
	}
	
	@Override
	public CompletableFuture<Boolean> update(CustomResourceDefinition crd, String resourceName, CustomResource cr) {
		return update(currentNamespace, crd, resourceName, cr);
	}
	
	@Override
	public CompletableFuture<Boolean> update(String namespace, CustomResourceDefinition crd, String resourceName, CustomResource cr) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			updaters.get(CustomResource.class).apply(namespace, new NameRes(resourceName, cr), future);
		} catch (ApiException e) {
			logger.warn("Create failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureBoolean();
	}
	
	@Override
	public <T> CompletableFuture<Boolean> delete(Class<T> clazz, String resourceName) {
		return delete(currentNamespace, clazz, resourceName);
	}

	@Override
	public <T> CompletableFuture<Boolean> delete(String namespace, Class<T> clazz, String resourceName) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			deleters.get(clazz).apply(namespace, resourceName, future);
		} catch (ApiException e) {
			logger.warn("Delete failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureBoolean();
	}
	
	@Override
	public CompletableFuture<Boolean> delete(CustomResourceDefinition def, String resourceName) {
		return delete(currentNamespace, def, resourceName);
	}
	
	@Override
	public CompletableFuture<Boolean> delete(String namespace, CustomResourceDefinition def, String resourceName) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		CustomResource tmp = new CustomResource(def);
		tmp.name(resourceName);
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			deleters.get(CustomResource.class).apply(namespace, tmp, future);
		} catch (ApiException e) {
			logger.warn("Delete failed", e);
			future.completeExceptionally(e);
		} catch(JsonSyntaxException e) {
		    if (e.getCause() instanceof IllegalStateException) {
			  IllegalStateException ise = (IllegalStateException) e.getCause();
			  if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT"))
				  logger.debug("Catching exception because of issue https://github.com/kubernetes-client/java/issues/86", e);
			  else throw e;
		    }
			else throw e;
		}
		return future.toFutureBoolean();
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(Class<T> clazz, String resourceName) {
		return get(currentNamespace, clazz, resourceName);
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(String namespace, Class<T> clazz, String resourceName) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<T> future = new ApiCallbackCompletableFuture<>();
		try {
			getters.get(clazz).apply(namespace, resourceName, future);
		} catch (ApiException e) {
			logger.warn("Get failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureOptionalResource();
	}
	
	@Override
	public CompletableFuture<Optional<CustomResource>> get(CustomResourceDefinition def, String resourceName) {
		return get(currentNamespace, def, resourceName);
	}
	
	@Override
	public CompletableFuture<Optional<CustomResource>> get(String namespace, CustomResourceDefinition def, String resourceName) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		CustomResource tmp = new CustomResource(def);
		tmp.name(resourceName);
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			getters.get(CustomResource.class).apply(namespace, tmp, future);
		} catch (ApiException e) {
			logger.warn("Get failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureOptionalResource(def);
	}

	@Override
	public <T> CompletableFuture<List<T>> getAll(Class<T> clazz) {
		return getAll(currentNamespace, clazz);
	}

	@Override
	public <T> CompletableFuture<List<T>> getAll(String namespace, Class<T> clazz) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			allGetters.get(clazz).apply(namespace, null, future);
		} catch (ApiException e) {
			logger.warn("GetAll failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureResourceList(toResList.get(clazz));
	}
	
	@Override
	public CompletableFuture<List<CustomResource>> getAll(CustomResourceDefinition def) {
		return getAll(currentNamespace, def);
	}

	@Override
	public CompletableFuture<List<CustomResource>> getAll(String namespace, CustomResourceDefinition def) {
		if(!inCluster) throw new IllegalStateException("No running in cluster");
		ApiCallbackCompletableFuture<?> future = new ApiCallbackCompletableFuture<>();
		try {
			allGetters.get(CustomResource.class).apply(namespace, def, future);
		} catch (ApiException e) {
			logger.warn("GetAll failed", e);
			future.completeExceptionally(e);
		}
		return future.toFutureResourceList(def);
	}
	
	private V1OwnerReference createOwnerReferenceBlock(String kind, String name, String uid) {
		return new V1OwnerReference()
					.apiVersion("apps/v1")
					.blockOwnerDeletion(true)
					.controller(true)
					.kind(kind)
					.name(name)
					.uid(uid);
	}
	
	@Override
	public <T, U> CompletableFuture<Boolean> addDependent(Class<T> pClazz, T parent, Class<U> cClazz, U child) {
		return addDependent(currentNamespace, pClazz, parent, cClazz, child);
	}

	@Override
	public <T, U> CompletableFuture<Boolean> addDependent(String namespace, Class<T> pClazz, T parent, Class<U> cClazz, U child) {
		V1OwnerReference ownerRef = ownerReferenceBlock.get(pClazz).apply(parent);
		logger.debug("ownerReference block %s", ownerRef);
		
		return
		this.get(cClazz, toName.get(cClazz).apply(child))
			.thenCompose(o -> {
				U r = o.get();
				U updated = (U) ownerReference.get(cClazz).apply(r, ownerRef);
				logger.debug("updated %s", updated);
				return update(cClazz, toName.get(cClazz).apply(r), updated); 
			});
	}

    public CompletableFuture<List<String>> listNamespaces() {
    	if(!inCluster) throw new IllegalStateException("No running in cluster");
    	ApiCallbackCompletableFuture<V1NamespaceList> future = new ApiCallbackCompletableFuture<>();
    	try {
    		corev1Api.listNamespaceAsync(null, null, null, null, null, null, null, null, null, null, future);
    	} catch(ApiException e) { 
    		future.completeExceptionally(e); 
    	}
		return future.thenCompose(l -> {
			List<String> namespaces = 
			l.getItems().stream()
						.map(ns -> ns.getMetadata().getName())
						.collect(Collectors.toList());
			return CompletableFuture.completedFuture(namespaces);
		});
	}

	public CompletableFuture<String> currentNamespace() {
		CompletableFuture<String> future = new CompletableFuture<>();
		String namespace = System.getProperty("k8s.namespace");
		
		if(namespace == null || namespace.isEmpty()) future.completeExceptionally(new Exception("Not running in cluster"));
		return CompletableFuture.completedFuture(namespace);
	}
	
	@Override
	public <T> CharSequence toJSON(T r) throws Exception {
		return SerializationUtils.toJSON(r);
	}

	@Override
	public <T> CharSequence toYAML(T r) throws Exception {
		return SerializationUtils.toYAML(r);
	}

	@Override
	public CustomResource fromJSON(CharSequence cs, CustomResourceDefinition crd) throws Exception {
		return SerializationUtils.fromJSON(cs.toString(), crd);
	}

	@Override
	public CustomResource fromYAML(CharSequence cs, CustomResourceDefinition crd) throws Exception {
		return SerializationUtils.fromYAML(cs.toString(), crd);
	}
	
	@Override
	public <T> T fromJSON(CharSequence cs, Class<T> clazz) throws Exception {
		return SerializationUtils.fromJSON(cs.toString(), clazz);
	}

	@Override
	public <T> T fromYAML(CharSequence cs, Class<T> clazz) throws Exception {
		return SerializationUtils.fromYAML(cs.toString(), clazz);
	}
	
	@FunctionalInterface
	interface ExceptionalTriConsumer {
		public void apply(String namespace, Object payload, ApiCallbackCompletableFuture future) throws ApiException;
	}
	
	@FunctionalInterface
	interface ExceptionalBiFunction {
		public Iterable<?> apply(String namespace, Object payload) throws ApiException;
	}
	
	private class NameRes {
		final String name;
		final Object res;
		NameRes(String name, Object res) {
			this.name = name;
			this.res = res;
		}
	}
}
