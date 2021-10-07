package com.nextenso.mux.mesh;

/**
 * An OSGi service to create mux meshes.
 *
 * It can be injected. It also tracks MuxMeshListeners in the registry to automatically create mux meshes.
 */
public interface MuxMeshFactory {

    /**
     * Creates a new mux mesh.
     * @param name the mesh name.
     * @param listener the listener to notify events to.
     * @param props optional properties to customize the mesh behavior
     * @return the new mesh, that will have to be started.
     */
    public MuxMesh newMuxMesh (String name, MuxMeshListener listener, java.util.Map<String, String> props);
    
}
