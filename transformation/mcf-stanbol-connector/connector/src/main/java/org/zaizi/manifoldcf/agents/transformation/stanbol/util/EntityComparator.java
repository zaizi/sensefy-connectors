package org.zaizi.manifoldcf.agents.transformation.stanbol.util;

import java.util.Comparator;

import org.apache.stanbol.client.enhancer.model.EntityAnnotation;

/**
 * @author Dileepa Jayakody <djayakody@zaizi.com>
 */
public class EntityComparator implements Comparator<EntityAnnotation>
{
    @Override
    public int compare( EntityAnnotation o1, EntityAnnotation o2 )
    {
        return o1.getEntityReference().compareTo( o2.getEntityReference());
    }

}
