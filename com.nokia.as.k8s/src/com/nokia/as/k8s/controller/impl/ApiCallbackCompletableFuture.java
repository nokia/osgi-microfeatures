package com.nokia.as.k8s.controller.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;

import io.kubernetes.client.openapi.ApiException;

public class ApiCallbackCompletableFuture<T> extends CompletableFuture<T> implements io.kubernetes.client.openapi.ApiCallback<T> {
	
	private static Logger LOG = Logger.getLogger(ApiCallbackCompletableFuture.class);

	@Override
	public void onUploadProgress(long arg0, long arg1, boolean arg2) {
		LOG.debug("onUploadProgress " + arg0 + " " + arg1 + " " + arg2);
	}
	
	@Override
	public void onSuccess(T arg0, int arg1, Map<String, List<String>> arg2) {
		LOG.debug("onSuccess " + arg0 + " " + arg1 + " " + arg2 + " this is " + toString());
		complete(arg0);
	}
	
	@Override
	public void onFailure(ApiException arg0, int arg1, Map<String, List<String>> arg2) {
		LOG.debug("OnFailure " + arg0 + " " + arg1 + " " + arg2);
		completeExceptionally(arg0);
	}
	
	@Override
	public void onDownloadProgress(long arg0, long arg1, boolean arg2) {
		LOG.debug("onDownloadProgress " + " " + arg0 + " " + arg1 + " " + arg2);
	}
	
	public CompletableFuture<Boolean> toFutureBoolean() {
		return thenCompose(o ->  CompletableFuture.completedFuture(o != null));
	}
	
	public <U> CompletableFuture<List<U>> toFutureResourceList(Function<Object, List<?>> mapper) {
		return thenCompose(o -> {
			if(o == null) return CompletableFuture.completedFuture(Collections.emptyList());
			List<U> result = mapper.apply(o).stream().map(l -> (U) l).collect(Collectors.toList());
			return CompletableFuture.completedFuture(result);
		});
	}
	
	public CompletableFuture<List<CustomResource>> toFutureResourceList(CustomResourceDefinition crd) {
		return thenCompose(o -> {
			Map<String,Object> result = (Map<String, Object>) o;
			if(result == null) return CompletableFuture.completedFuture(Collections.emptyList());
			
			List<Map<String,Object>> struct = (List<Map<String, Object>>) result.get("items");
			if(struct == null) return CompletableFuture.completedFuture(Collections.emptyList());
			
			List<CustomResource> resList = struct.stream()
				.map(cr -> new CustomResource(cr, crd))
				.collect(Collectors.toList());
			LOG.debug(resList);
			return CompletableFuture.completedFuture(resList);
		});
	}
	
	public CompletableFuture<Optional<T>> toFutureOptionalResource() {
		return thenCompose(o -> {
			LOG.debug(o);
			return CompletableFuture.completedFuture(Optional.ofNullable((T) o));
		});
	}

	public CompletableFuture<Optional<CustomResource>> toFutureOptionalResource(CustomResourceDefinition crd) {
		return thenCompose(o -> {
			Map<String,Object> result = (Map<String, Object>) o;
			LOG.debug(result);
			
			if(result == null || result.isEmpty()) {
				return CompletableFuture.completedFuture(Optional.empty());
			} else {
				return CompletableFuture.completedFuture(Optional.of(new CustomResource(result, crd)));
			}
		});
	}
	
	
}
