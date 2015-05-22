package org.apache.manifoldcf.crawler.connectors.confluence;

import org.apache.manifoldcf.agents.interfaces.IOutputConnection;
import org.apache.manifoldcf.agents.interfaces.IOutputConnectionManager;
import org.apache.manifoldcf.agents.interfaces.OutputConnectionManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.ThreadContextFactory;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IJobDescription;
import org.apache.manifoldcf.crawler.interfaces.IJobManager;
import org.apache.manifoldcf.crawler.interfaces.IRepositoryConnection;
import org.apache.manifoldcf.crawler.interfaces.IRepositoryConnectionManager;
import org.apache.manifoldcf.crawler.interfaces.JobManagerFactory;
import org.apache.manifoldcf.crawler.interfaces.RepositoryConnectionManagerFactory;
import org.apache.manifoldcf.crawler.tests.ManifoldCFInstance;
import org.junit.Before;
import org.junit.Test;

public class ConfConnectorTest {

	protected org.apache.manifoldcf.crawler.tests.ManifoldCFInstance instance;

	@Before
	public  void init() {
		this.instance = new ManifoldCFInstance("9011",true);
	}

	@Test
	public void test() throws ManifoldCFException, InterruptedException {
		try {
			IThreadContext tc = ThreadContextFactory.make();
			IRepositoryConnectionManager mgr = RepositoryConnectionManagerFactory
					.make(tc);
			IRepositoryConnection conn = mgr.create();
			conn.setName("Confluence Connection");
			conn.setDescription("Confluence Connection");
			conn.setClassName("org.apache.manifoldcf.crawler.connectors.confluence.ConfluenceRepositoryConnector");
			conn.setMaxConnections(100);
			ConfigParams cp = conn.getConfigParams();
			cp.setParameter(ConfluenceConfig.CONF_PROTOCOL_PARAM, "https");
			cp.setParameter(ConfluenceConfig.CONF_HOST_PARAM, "dev.zaizi.org");
			cp.setParameter(ConfluenceConfig.CONF_PORT_PARAM, "443");
			cp.setParameter(ConfluenceConfig.CONF_PATH_PARAM,
					"/confluence/rest/api/content/");
			cp.setParameter(ConfluenceConfig.CLIENT_ID_PARAM, "kgunaratnam");
			cp.setParameter(ConfluenceConfig.CLIENT_SECRET_PARAM, "foCus2812");
			mgr.save(conn);// saves the connection
			// output connection
			IOutputConnectionManager outputMgr = OutputConnectionManagerFactory
					.make(tc);
			IOutputConnection outputConn = outputMgr.create();
			outputConn.setName("Null Connection");
			outputConn.setDescription("Null Connection");
			outputConn
					.setClassName("org.apache.manifoldcf.agents.tests.TestingOutputConnector");
			outputConn.setMaxConnections(100);
			// Now, save
			outputMgr.save(outputConn);
			IJobManager jobManager = JobManagerFactory.make(tc);
			IJobDescription job = jobManager.createJob();
			job.setDescription("Test Job");
			job.setConnectionName("Confluence Connection");
			job.addPipelineStage(-1, true, "Null Connection", "");
			job.setType(job.TYPE_SPECIFIED);
			job.setStartMethod(job.START_WINDOWBEGIN);
			// documents
			DocumentSpecification ds = job.getSpecification();
			jobManager.save(job);
			long startTime = System.currentTimeMillis();
			jobManager.manualStart(job.getID());
			instance.waitJobInactiveNative(jobManager, job.getID(), 220000000L);
			
		} 
		catch(InterruptedException ie){
		  
		}
		catch(ManifoldCFException me){
			
		}		
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
