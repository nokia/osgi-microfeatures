// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface defines the API that allows to create, update, delete, gat and watch Kubernetes resources
 * The following resources are supported (the classes are in the io.kubernetes.client.models package):
 *   - Pod (V1Pod.class)
 *   - ConfigMap (V1ConfigMap.class)
 *   - Secret (V1Secret.class)
 *   - Service (V1Service.class)
 *   - Ingress (V1beta1Ingress.class)
 *   - Deployment (ExtensionsV1beta1Deployment.class)
 *   - DaemonSet (V1beta1DaemonSet.class)
 *   - ServiceAccount (V1ServiceAccount.class)
 *   - ClusterRole (V1ClusterRole.class)
 *   - ClusterRoleBinding (V1ClusterRoleBinding.class)
 *   - Role (V1Role.class)
 *   - RoleBinding (V1RoleBinding.class)
 *   - Job (V1Job.class)
 *   - CustomResourceDefinition (V1beta1CustomResourceDefinition.class)
 *   - Any user-defined custom resource (com.nokia.as.k8s.controller.CustomResource.class)
 */
@ProviderType
public interface ResourceService {

	public static final String ALL_NAMESPACES = "*";

	public <T> WatchHandle<T> watch(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted);
	public <T> WatchHandle<T> watch(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted);
	public WatchHandle<CustomResource> watch(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted);
	public WatchHandle<CustomResource> watch(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted);
    
	public <T> CompletableFuture<Boolean> create(Class<T> clazz, T obj);
	public <T> CompletableFuture<Boolean> create(String namespace, Class<T> clazz, T obj);
	public CompletableFuture<Boolean> create(CustomResourceDefinition crd, CustomResource res);
	public CompletableFuture<Boolean> create(String namespace, CustomResourceDefinition crd, CustomResource res);
	
	public <T> CompletableFuture<Boolean> update(Class<T> clazz, String resourceName, T obj);
	public <T> CompletableFuture<Boolean> update(String namespace, Class<T> clazz, String resourceName, T obj);
	public CompletableFuture<Boolean> update(CustomResourceDefinition crd, String resourceName, CustomResource res);
	public CompletableFuture<Boolean> update(String namespace, CustomResourceDefinition crd, String resourceName, CustomResource res);

	public <T> CompletableFuture<Boolean> delete(Class<T> clazz, String resourceName);
	public <T> CompletableFuture<Boolean> delete(String namespace, Class<T> clazz, String resourceName);
	public CompletableFuture<Boolean> delete(CustomResourceDefinition def, String resourceName);
	public CompletableFuture<Boolean> delete(String namespace, CustomResourceDefinition def, String resourceName);

	public <T> CompletableFuture<Optional<T>> get(Class<T> clazz, String resourceName);
	public <T> CompletableFuture<Optional<T>> get(String namespace, Class<T> clazz, String resourceName);
	public CompletableFuture<Optional<CustomResource>> get(CustomResourceDefinition def, String resourceName);
	public CompletableFuture<Optional<CustomResource>> get(String namespace, CustomResourceDefinition def, String resourceName);
	
	public <T> CompletableFuture<List<T>> getAll(Class<T> clazz);
	public <T> CompletableFuture<List<T>> getAll(String namespace, Class<T> clazz);
	public CompletableFuture<List<CustomResource>> getAll(CustomResourceDefinition def);
	public CompletableFuture<List<CustomResource>> getAll(String namespace, CustomResourceDefinition def);
	
	public <T, U> CompletableFuture<Boolean> addDependent(Class<T> pClazz, T parent, Class<U> cClazz, U child);
	public <T, U> CompletableFuture<Boolean> addDependent(String namespace, Class<T> pClazz, T parent, Class<U> cClazz, U child);
	
	public CompletableFuture<String> currentNamespace();
	public CompletableFuture<List<String>> listNamespaces();
	
	public <T> CharSequence toJSON(T obj) throws Exception;
	public <T> CharSequence toYAML(T obj) throws Exception;
	public CustomResource fromJSON(CharSequence cs, CustomResourceDefinition crd) throws Exception;
	public CustomResource fromYAML(CharSequence cs, CustomResourceDefinition crd) throws Exception;
	public <T> T fromJSON(CharSequence cs, Class<T> clazz) throws Exception;
	public <T> T fromYAML(CharSequence cs, Class<T> clazz) throws Exception;

}
