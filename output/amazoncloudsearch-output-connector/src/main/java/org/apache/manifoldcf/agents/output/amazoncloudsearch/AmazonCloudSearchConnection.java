package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

/**
 * Amazon Cloud Search Connection class
 * 
 * @author aayala
 *
 */
public class AmazonCloudSearchConnection
{
    protected AmazonCloudSearchConfig config;

    private HttpClient client;

    @SuppressWarnings("unused")
    private String documentServiceEndpoint;

    private String resultDescription;

    private String callUrlSnippet;

    private String response;

    protected String jsonStatus = "\"status\"";
    protected String jsonException = "\"errors\"";

    public enum Result
    {
        OK, ERROR, UNKNOWN;
    }

    private Result result;

    protected AmazonCloudSearchConnection(AmazonCloudSearchConfig config, HttpClient client)
    {
        this.config = config;
        this.client = client;
        result = Result.UNKNOWN;
        response = null;
        resultDescription = "";
        callUrlSnippet = null;
        documentServiceEndpoint = config.getDocumentServerEndpoint();
    }

    protected final String urlEncode(String t) throws ManifoldCFException
    {
        try
        {
            return URLEncoder.encode(t, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ManifoldCFException(e);
        }
    }

    protected static class CallThread extends Thread
    {
        protected final HttpClient client;
        protected final HttpRequestBase method;
        protected int resultCode = -1;
        protected String response = null;
        protected Throwable exception = null;

        public CallThread(HttpClient client, HttpRequestBase method)
        {
            this.client = client;
            this.method = method;
            setDaemon(true);
        }

        @Override
        public void run()
        {
            try
            {
                try
                {
                    HttpResponse resp = client.execute(method);
                    resultCode = resp.getStatusLine().getStatusCode();
                    response = getResponseBodyAsString(resp.getEntity());
                }
                finally
                {
                    method.abort();
                }
            }
            catch (java.net.SocketTimeoutException e)
            {
                exception = e;
            }
            catch (InterruptedIOException e)
            {
                // Just exit
            }
            catch (Throwable e)
            {
                exception = e;
            }
        }

        public int getResultCode()
        {
            return resultCode;
        }

        public String getResponse()
        {
            return response;
        }

        public Throwable getException()
        {
            return exception;
        }
    }

    /**
     * Call AmazonCloudSearch.
     * 
     * @return false if there was a "rejection".
     */
    protected boolean call(HttpRequestBase method) throws ManifoldCFException, ServiceInterruption
    {
        CallThread ct = new CallThread(client, method);
        try
        {
            ct.start();
            try
            {
                ct.join();
                Throwable t = ct.getException();
                if (t != null)
                {
                    if (t instanceof HttpException)
                        throw (HttpException) t;
                    else if (t instanceof IOException)
                        throw (IOException) t;
                    else if (t instanceof RuntimeException)
                        throw (RuntimeException) t;
                    else if (t instanceof Error)
                        throw (Error) t;
                    else
                        throw new RuntimeException("Unexpected exception thrown: " + t.getMessage(), t);
                }

                response = ct.getResponse();
                return handleResultCode(ct.getResultCode(), response);
            }
            catch (InterruptedException e)
            {
                ct.interrupt();
                throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
            }
        }
        catch (HttpException e)
        {
            handleHttpException(e);
            return false;
        }
        catch (IOException e)
        {
            handleIOException(e);
            return false;
        }
    }

    private boolean handleResultCode(int code, String response) throws ManifoldCFException, ServiceInterruption
    {
        if (code == 200 || code == 201)
        {
            setResult(Result.OK, null);
            return true;
        }
        else if (code == 404)
        {
            setResult(Result.ERROR, "Page not found: " + response);
            throw new ManifoldCFException("Server/page not found");
        }
        else if (code >= 400 && code < 500)
        {
            setResult(Result.ERROR, "HTTP code = " + code + ", Response = " + response);
            return false;
        }
        else if (code >= 500 && code < 600)
        {
            setResult(Result.ERROR, "Server exception: " + response);
            long currentTime = System.currentTimeMillis();
            throw new ServiceInterruption("Server exception: " + response, new ManifoldCFException(response),
                    currentTime + 300000L, currentTime + 20L * 60000L, -1, false);
        }
        setResult(Result.UNKNOWN, "HTTP code = " + code + ", Response = " + response);
        throw new ManifoldCFException("Unexpected HTTP result code: " + code + ": " + response);
    }

    private void handleHttpException(HttpException e) throws ManifoldCFException, ServiceInterruption
    {
        setResult(Result.ERROR, e.getMessage());
        throw new ManifoldCFException(e);
    }

    private void handleIOException(IOException e) throws ManifoldCFException, ServiceInterruption
    {
        setResult(Result.ERROR, e.getMessage());
        long currentTime = System.currentTimeMillis();
        // All IO exceptions are treated as service interruptions, retried for an hour
        throw new ServiceInterruption("IO exception: " + e.getMessage(), e, currentTime + 60000L,
                currentTime + 1L * 60L * 60000L, -1, true);
    }

    private static String getResponseBodyAsString(HttpEntity entity) throws IOException, HttpException
    {
        InputStream is = entity.getContent();
        if (is != null)
        {
            try
            {
                @SuppressWarnings("deprecation")
                String charSet = EntityUtils.getContentCharSet(entity);
                if (charSet == null)
                    charSet = "utf-8";
                char[] buffer = new char[65536];
                Reader r = new InputStreamReader(is, charSet);
                Writer w = new StringWriter();
                try
                {
                    while (true)
                    {
                        int amt = r.read(buffer);
                        if (amt == -1)
                            break;
                        w.write(buffer, 0, amt);
                    }
                }
                finally
                {
                    w.flush();
                }
                return w.toString();
            }
            finally
            {
                is.close();
            }
        }
        return "";
    }

    protected String checkJson(String jsonQuery) throws ManifoldCFException
    {
        String result = null;
        if (response != null)
        {
            String[] tokens = response.replaceAll("\\{", "").replaceAll("\\}", "").split(",");
            for (String token : tokens)
                if (token.contains(jsonQuery))
                    result = token.substring(token.indexOf(":") + 1);
        }
        return result;
    }

    protected void setResult(Result res, String desc)
    {
        if (res != null)
            result = res;
        if (desc != null)
            if (desc.length() > 0)
                resultDescription = desc;
    }

    public String getResultDescription()
    {
        return resultDescription;
    }

    protected String getResponse()
    {
        return response;
    }

    public Result getResult()
    {
        return result;
    }

    public String getCallUrlSnippet()
    {
        return callUrlSnippet;
    }
}
