package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Amazon Cloud Search Index
 * 
 * @author aayala
 * 
 */
public class AmazonCloudSearchIndex extends AmazonCloudSearchConnection
{
    private static final String SUCCESS_MSG = " \"success\"";
    /** The allow attribute name */
    protected final static String allowAttributeName = "allow_token_";
    /** The deny attribute name */
    protected final static String denyAttributeName = "deny_token_";
    /** The no-security token */
    protected final static String noSecurityToken = "__nosecurity__";

    // Default path for documents indexing batch
    private final static String INDEX_BATCH_PATH = "/2011-02-01/documents/batch";

    /**
     * Custom Entity for indexing requests
     * 
     * @author aayala
     * 
     */
    private class IndexRequestEntity implements HttpEntity
    {
        private String documentURI;
        private RepositoryDocument document;
        @SuppressWarnings("unused")
        private String outputDescription;
        private int length;
        private JSONArray json;

        public IndexRequestEntity(String documentURI, String outputDescription, RepositoryDocument document)
                throws ManifoldCFException
        {
            this.documentURI = documentURI;
            this.document = document;
            this.outputDescription = outputDescription;
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
         * Sanitize a string to be compatible with amazon regular expression for fields
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
         * Build JSON needed for indexing request
         * 
         * @throws JSONException
         * @throws IOException
         */
        private void buildJSON() throws JSONException, IOException
        {
            JSONObject body = new JSONObject();
            body.put("type", "add");

            body.put("id", sanitize(documentURI));

            // TODO version is current time / 1000
            body.put("version", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            body.put("lang", "en");

            JSONObject fields = new JSONObject();
            Iterator<String> i = document.getFields();

            while (i.hasNext())
            {
                String fieldName = i.next();

                String[] fieldValues = document.getFieldAsStrings(fieldName);
                writeField(fields, sanitize(fieldName), fieldValues);
            }

            // acl fields
            writeACLs(fields, "document", document.getACL(), document.getDenyACL());
            writeACLs(fields, "share", document.getShareACL(), document.getShareDenyACL());

            if (document.getFileName() != null)
            {
                fields.put("name", document.getFileName());
            }
            if (document.getMimeType().equals("text/plain") && document.getBinaryStream() != null)
            {
                fields.put("content", IOUtils.toString(document.getBinaryStream(), "UTF-8"));
            }
            if (StringUtils.isEmpty(fields.optString("mimetype")))
            {
                fields.put("mimetype", document.getMimeType());
            }

            body.put("fields", fields);

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
            // return new BasicHeader("Content-encoding", "UTF-8");
            return null;
        }

    }

    /**
     * Write a field into a JSON
     * 
     * @param fields
     * @param fieldName
     * @param fieldValues
     * @throws IOException
     * @throws JSONException
     */
    protected static void writeField(JSONObject fields, String fieldName, String[] fieldValues) throws IOException,
            JSONException
    {
        /*
         * Multivalue field. Generating the array in the JSON -> attr: [val1,val2,...] instead of attr: val1 , attr:
         * val2, ...
         */
        if (fieldValues == null)
        {
            return;
        }
        boolean isMultivalued = fieldValues.length > 1 ? true : false;

        if (isMultivalued)
        {
            JSONArray values = new JSONArray(fieldValues);
            fields.put(fieldName, values);
        }
        else
        {
            String fieldValue = fieldValues[0];
            fields.put(fieldName, fieldValue);
        }

    }

    /**
     * Output an acl level into a JSON
     * 
     * @throws JSONException
     */
    protected static void writeACLs(JSONObject fields, String aclType, String[] acl, String[] denyAcl)
            throws IOException, JSONException
    {
        String metadataACLName = allowAttributeName + aclType;
        if (acl != null && acl.length > 0)
            writeField(fields, metadataACLName, acl);
        // else if (!useNullValue)
        // writeField(fields, metadataACLName, new String[] { noSecurityToken });
        String metadataDenyACLName = denyAttributeName + aclType;
        if (denyAcl != null && denyAcl.length > 0)
            writeField(fields, metadataDenyACLName, denyAcl);
        // else if (!useNullValue)
        // writeField(fields, metadataDenyACLName, new String[] { noSecurityToken });
    }

    /**
     * Escpe a string for json compatibility
     * 
     * @param value
     * @return
     */
    protected static String jsonStringEscape(String value)
    {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++)
        {
            char x = value.charAt(i);
            if (x == '\"' || x == '\\' || x == '/')
                sb.append('\\');
            sb.append(x);
        }
        sb.append("\"");
        return sb.toString();
    }

    public AmazonCloudSearchIndex(HttpClient client, AmazonCloudSearchConfig config)
    {
        super(config, client);
    }

    /**
     * Do the indexing.
     * 
     * @return false to indicate that the document was rejected.
     */
    public boolean execute(String documentURI, String outputDescription, RepositoryDocument document,
            InputStream inputStream) throws ManifoldCFException, ServiceInterruption
    {
        StringBuffer url = new StringBuffer(config.getDocumentServerEndpoint());
        HttpPost post = new HttpPost(url.toString() + INDEX_BATCH_PATH);
        post.setEntity(new IndexRequestEntity(documentURI, outputDescription, document));
        if (call(post) == false)
            return false;
        if (SUCCESS_MSG.equals(checkJson(jsonStatus)))
            return true;
        String error = checkJson(jsonException);
        setResult(Result.ERROR, error);
        Logging.connectors.warn("AmazonCloudSearch: Index failed: " + getResponse());
        return true;
    }

}
