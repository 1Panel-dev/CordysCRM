
package io.cordys.common.uid.worker;

import io.cordys.common.uid.impl.DefaultUidGenerator;

/**
 * Represents a worker id assigner for {@link DefaultUidGenerator}
 * 

 */
public interface WorkerIdAssigner {

    /**
     * Assign worker id for {@link DefaultUidGenerator}
     * 
     * @return assigned worker id
     */
    long assignWorkerId();

}
