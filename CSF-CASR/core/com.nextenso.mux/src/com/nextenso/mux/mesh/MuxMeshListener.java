package com.nextenso.mux.mesh;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;

/**
 * The listener notified when mux events occur.
 */
public interface MuxMeshListener {

    /**
     * The property name for the mesh name.
     */
    public static final String MESH_NAME = "mesh.name";

    /**
     * Called when the mux connection is opened.
     * @param mesh the mesh to which the mux connection is associated
     * @param connection the mux connection opened
     */
    public void muxOpened (MuxMesh mesh, MuxConnection connection);
    /**
     * Called when the mux connection is closed.
     * @param mesh the mesh to which the mux connection is associated
     * @param connection the mux connection closed
     */
    public void muxClosed (MuxMesh mesh, MuxConnection connection);
    /**
     * Called when data arrive on the mux connection.
     * @param mesh the mesh to which the mux connection is associated
     * @param connection the mux connection opened
     * @param header the mux header
     * @param buffer the data
     */
    public void muxData (MuxMesh mesh,
			 MuxConnection connection,
			 MuxHeader header,
			 java.nio.ByteBuffer buffer);
    
}
