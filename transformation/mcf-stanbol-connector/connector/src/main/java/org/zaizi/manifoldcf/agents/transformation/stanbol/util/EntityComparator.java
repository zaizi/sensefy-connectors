/*******************************************************************************
 * Sensefy
 *
 * Copyright (c) Zaizi Limited, All rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *******************************************************************************/
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
