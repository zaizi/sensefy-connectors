/**
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
 **/
package org.zaizi.manifoldcf.agents.output.solrwrapper.activity;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @Author: Alessandro Benedetti
 * Date: 02/07/2014
 */
public class EmptyOutputAddActivity implements IOutputAddActivity
{
    @Override
    public String qualifyAccessToken( String authorityNameString, String accessToken )
        throws ManifoldCFException
    {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void recordActivity( Long startTime, String activityType, Long dataSize, String entityURI, String resultCode,
                                String resultDescription )
        throws ManifoldCFException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int sendDocument( String s, RepositoryDocument repositoryDocument )
        throws ManifoldCFException, ServiceInterruption, IOException
    {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void noDocument()
        throws ManifoldCFException, ServiceInterruption
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkMimeTypeIndexable( String s )
        throws ManifoldCFException, ServiceInterruption
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkDocumentIndexable( File file )
        throws ManifoldCFException, ServiceInterruption
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkLengthIndexable( long l )
        throws ManifoldCFException, ServiceInterruption
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkURLIndexable( String s )
        throws ManifoldCFException, ServiceInterruption
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@Override
	public boolean checkDateIndexable(Date date) throws ManifoldCFException, ServiceInterruption {
		// TODO Auto-generated method stub
		return false;
	}

}
