package com.nokia.as.features.admin.k8s;

public interface DeployStatus {
	
	public String statusStr();
	public Object attachment();
	
	/** RUNTIME STATUS **/
	public static DeployStatus UNDEPLOYED = 
		new DeployStatus() {
			
			public String statusStr() {
				return "UNDEPLOYED";
			}
			
			public Object attachment() {
				return null;
			}
		};
	
	public static DeployStatus DEPLOYED(int replicas) {
		return new DeployStatus() {
			
			public String statusStr() {
				return "DEPLOYED (" + replicas + "/" + replicas + ")";
			}
			
			public Object attachment() {
				return replicas;
			}
		};
	}
	
	public static DeployStatus PENDING(int deployed, int max) {
		return new DeployStatus() {
			
			public String statusStr() {
				return "PENDING (" + deployed + "/" + max + ")";
			}
			
			public Object attachment() {
				return deployed;
			}
		};
	}
	
	public static DeployStatus ERROR(Exception e) {
		return new DeployStatus() {
			
			public String statusStr() {
				return "ERROR: " + e.getMessage();
			}
			
			public Object attachment() {
				return e;
			}
		};
	}
	
	public static DeployStatus ERRORK8S(Exception e) {
		return new DeployStatus() {
			
			public String statusStr() {
				return "ERROR (KUBERNETES): " + e.getMessage();
			}
			
			public Object attachment() {
				return e;
			}
		};
	}
	
	/** POD STATUS **/
	public static DeployStatus READY =
		new DeployStatus() {
			
			public String statusStr() {
				return "READY";
			}
			
			public Object attachment() {
				return null;
			}
		};
	
	public static DeployStatus UNREADY =
		new DeployStatus() {
			
			public String statusStr() {
				return "UNREADY";
			}
			
			public Object attachment() {
				return null;
			}
		};
		
	/** FUNCTION AND ROUTE STATUS **/
	public static DeployStatus CREATED =
		new DeployStatus() {
				
			public String statusStr() {
				return "CREATED";
			}
					
			public Object attachment() {
				return null;
			}
		};
			
	public static DeployStatus DELETED =
		new DeployStatus() {
				
			public String statusStr() {
				return "DELETED";
			}
					
			public Object attachment() {
				return null;
			}
		};
}
