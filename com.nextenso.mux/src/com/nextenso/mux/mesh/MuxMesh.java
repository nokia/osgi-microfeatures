package com.nextenso.mux.mesh;

import com.alcatel.as.service.metering2.Monitorable;

/**
 * A Mux Mesh.
 */
public interface MuxMesh {

    /**
     * Returns the name.
     * @return the name
     */
    public String getName ();
    /**
     * Attaches an object.
     * @param o the attachment
     */
    public void attach (Object o);
    /**
     * Returns the attachment.
     * @return the attachment
     */
    public <T> T attachment ();
    /**
     * Returns the properties.
     * @return the properties
     */
    public java.util.Map<String, String> getProperties ();
    /**
     * Returns the listener.
     * @return the listener
     */
    public MuxMeshListener getListener ();
    /**
     * Returns the associated Monitorable.
     * The Monitorable is started when the mesh is started.
     * @return the monitorable.
     */
    public Monitorable getMonitorable ();
    /**
     * Starts the mesh.
     * @return this
     */
    public MuxMesh start ();

}
