package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.Arrays;

import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PolicyRule;
import io.kubernetes.client.openapi.models.V1RoleRef;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1Subject;

public class Rbac {
	
	public static V1ServiceAccount createSA(String name, String namespace) {
		String saName = name + "-sa";
		String saNamespace = namespace;
		
		/*
		 * apiVersion: v1
		 * kind: ServiceAccount
		 * metadata:
		 *   name: %name%-sa
		 *   namespace: %namespace%
		 */
		return new V1ServiceAccount().
			     metadata(new V1ObjectMeta().
				   name(saName).
				   namespace(saNamespace));
	}
	
	public static V1Role createRole(String name, String namespace) {
		String rName = name + "-role";
		String rNamespace = namespace;
		
		/*
		 * apiVersion: rbac.authorization.k8s.io/v1beta1
		 * kind: Role
		 * metadata:
		 *   name: %name%-role
		 *   namespace: %namespace%
		 * rules:
		 *   - apiGroups: [""]
		 *     resources: ["namespaces", "configmaps", "pods"]
		 *     verbs: ["get", "list", "watch"]
		 *   - apiGroups: ["nokia.com"]
		 *     resources: ["*"]
		 *     verbs: ["*"]
		 *   - apiGroups: extensions
		 *     resourceNames: privileged
		 *     resources: podsecuritypolicies
		 *     verbs: use
		 */
		return new V1Role()
			     .metadata(new V1ObjectMeta()
			       .name(rName)
				   .namespace(rNamespace))
			     .addRulesItem(new V1PolicyRule()
			       .addApiGroupsItem("")
			       .resources(Arrays.asList("namespaces", "configmaps", "pods"))
			       .verbs(Arrays.asList("get", "list", "watch")))
			     .addRulesItem(new V1PolicyRule()
			       .addApiGroupsItem("nokia.com")
			       .addResourcesItem("*")
			       .addVerbsItem("*"))
			     .addRulesItem(new V1PolicyRule()
			       .addApiGroupsItem("extensions")
			       .addResourcesItem("podsecuritypolicies")
			       .addResourceNamesItem("privileged")
			       .addVerbsItem("use"));
			     
	}
	
	public static V1RoleBinding createRB(String name, String namespace) {
		String rbName = name + "-rb";
		String rbNamespace = namespace;
		
		/*
		 * apiVersion: rbac.authorization.k8s.io/v1
		 * kind: RoleBinding
		 * metadata:
		 *   name: %name%-rb
		 *   namespace: %namespace%
		 * roleRef:
		 *   kind: Role
		 *   apiGroup: rbac.authorization.k8s.io
		 *   name: %name%-role
		 * subjects:
		 *   - kind: ServiceAccount
		 *     name: %name%-sa
		 *     namespace: %namespace%
		 */
		return new V1RoleBinding()
			     .metadata(new V1ObjectMeta()
			       .name(rbName)
			       .namespace(rbNamespace))
			     .roleRef(new V1RoleRef()
			       .kind("Role")
			       .apiGroup("rbac.authorization.k8s.io")
			       .name(name + "-role"))
			     .addSubjectsItem(new V1Subject()
			       .kind("ServiceAccount")
			       .name(name + "-sa")
			       .namespace(namespace));
	}
}
