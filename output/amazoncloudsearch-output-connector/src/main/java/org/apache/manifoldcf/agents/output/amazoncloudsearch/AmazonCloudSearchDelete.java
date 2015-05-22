package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Amazon Cloud Search Delete
 * 
 * @author aayala
 * 
 */
public class AmazonCloudSearchDelete extends AmazonCloudSearchConnection
{
    private static final String SUCCESS_MSG = " \"success\"";
    private final static String INDEX_BATCH_PATH = "/2011-02-01/documents/batch";

    private class DeleteRequestEntity implements HttpEntity
    {
        private String documentURI;
        private int length;
        private JSONArray json;

        /**
         * Custom Entity for delete requests
         * 
         * @param documentURI
         * @throws ManifoldCFException
         */
        public DeleteRequestEntity(String documentURI) throws ManifoldCFException
        {
            this.documentURI = documentURI;
            try
            {
                buildJSON();
            }
            catch (Exception e)
            {
                throw new ManifoldCFException(e);
            }
        }

        @Override
        public boolean isChunked()
        {
            return false;
        }

        @Override
        public void consumeContent() throws IOException
        {
            EntityUtils.consume(this);
        }

        @Override
        public boolean isRepeatable()
        {
            return false;
        }

        @Override
        public boolean isStreaming()
        {
            return false;
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException
        {
            return null;
        }

        /**
         * Sanitize strings for amazon cloud search fields compatibility
         * 
         * @param input
         * @return
         */
        private String sanitize(String input)
        {
            // regular expression in amazon cloud search for fields: [a-z0-9][a-z0-9_]*$
            return input.toLowerCase().replaceAll("[^a-z0-9+]", "_");
        }

        /**
         * Builds JSON for delete requests
         * 
         * @throws JSONException
         * @throws IOException
         */
        private void buildJSON() throws JSONException, IOException
        {
            JSONObject body = new JSONObject();
            body.put("type", "delete");

            body.put("id", sanitize(documentURI));
            body.put("version", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            json = new JSONArray();
            json.put(body);

            length = json.toString().getBytes("UTF-8").length;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException
        {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "utf-8"));
            try
            {
                pw.print(json.toString());
            }
            finally
            {
                pw.flush();
                IOUtils.closeQuietly(pw);
            }
        }

        @Override
        public long getContentLength()
        {
            return length;
        }

        @Override
        public Header getContentType()
        {
            return new BasicHeader("Content-type", "application/json; charset=UTF-8");
        }

        @Override
        public Header getContentEncoding()
        {
            return null;
        }

    }

    public AmazonCloudSearchDelete(HttpClient client, AmazonCloudSearchConfig config)
    {
        super(config, client);
    }

    /**
     * Do the delete.
     * 
     * @return false to indicate that the document was rejected.
     */
    public boolean execute(String documentURI) throws ManifoldCFException, ServiceInterruption
    {
        StringBuffer url = new StringBuffer(config.getDocumentServerEndpoint());
        HttpPost post = new HttpPost(url.toString() + INDEX_BATCH_PATH);
        post.setEntity(new DeleteRequestEntity(documentURI));
        if (call(post) == false)
            return false;
        if (SUCCESS_MSG.equals(checkJson(jsonStatus)))
            return true;
        String error = checkJson(jsonException);
        setResult(Result.ERROR, error);
        Logging.connectors.warn("AmazonCloudSearch: Delete failed: " + getResponse());
        return true;
    }

}
