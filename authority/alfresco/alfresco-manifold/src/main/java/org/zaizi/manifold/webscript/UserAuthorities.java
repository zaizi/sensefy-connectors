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
package org.zaizi.manifold.webscript;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * UserAuthorities Webscript for Getting Authorities for an user
 * 
 * @author aayala
 * 
 */
public class UserAuthorities extends DeclarativeWebScript
{
    // Logger
    private static final Log logger = LogFactory.getLog(UserAuthorities.class);

    private static final String USERNAME = "userName";
    private static final String AUTHORITIES = "authorities";
    private static final String RESULTS = "results";

    private static final String DEFAULT_AUTHORITIES = "guest";

    private AuthorityService authorityService;
    private PersonService personService;

    @Override
    public Map<String, Object> executeImpl(WebScriptRequest req, Status status)
    {
        Map<String, Object> model = new HashMap<String, Object>(1);

        if (logger.isDebugEnabled())
        {
            logger.debug("Retrieving and checking the parameters...");
        }

        try
        {
            String userName = req.getParameter(USERNAME);

            JSONObject json = new JSONObject();
            JSONArray jsonAuths = new JSONArray();

            if (userName != null && userName.length() > 0)
            {
                if (personService.personExists(userName))
                {
                    Set<String> setAuths = authorityService.getAuthoritiesForUser(userName);
                    setAuths.add(userName);
                    jsonAuths = new JSONArray(setAuths);
                }
                else
                {
                    jsonAuths.put(DEFAULT_AUTHORITIES);
                }
            }
            else
            {
                jsonAuths.put(DEFAULT_AUTHORITIES);
            }

            json.put(AUTHORITIES, jsonAuths);

            model.put(RESULTS, json);
        }
        catch (Exception e)
        {
            logger.error("Error when getting authorities for user: " + e.getMessage(), e);
            status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR);
        }
        return model;

    }

    /**
     * Set alfresco services
     * 
     * @param serviceRegistry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.authorityService = serviceRegistry.getAuthorityService();
        this.personService = serviceRegistry.getPersonService();
    }

}
