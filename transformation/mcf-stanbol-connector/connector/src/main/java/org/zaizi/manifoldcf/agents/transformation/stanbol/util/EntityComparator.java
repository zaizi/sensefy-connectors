/**
 * (C) Copyright 2015 Zaizi Limited (http://www.zaizi.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3.0 which accompanies this distribution, and is available at 
 * http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 **/
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
