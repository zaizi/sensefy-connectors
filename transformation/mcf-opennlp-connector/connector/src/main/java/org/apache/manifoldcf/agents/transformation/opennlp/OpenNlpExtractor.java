package org.apache.manifoldcf.agents.transformation.opennlp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.agents.transformation.BaseTransformationConnector;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;

public class OpenNlpExtractor extends BaseTransformationConnector
{
    private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
    private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
    private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";
    
    private static final String TEXT_FEATURES_FIELD = "text_features";
    private static final String HAS_CONTENT = "has_content";

    protected static final String ACTIVITY_EXTRACT = "extract";

    protected static final String[] activitiesList = new String[] { ACTIVITY_EXTRACT };

    /**
     * Return a list of activities that this connector generates. The connector does NOT need to be connected before
     * this method is called.
     * 
     * @return the set of activities.
     */
    @Override
    public String[] getActivitiesList()
    {
        return activitiesList;
    }

    /**
     * Get a pipeline version string, given a pipeline specification object. The version string is used to uniquely
     * describe the pertinent details of the specification and the configuration, to allow the Connector Framework to
     * determine whether a document will need to be processed again. Note that the contents of any document cannot be
     * considered by this method; only configuration and specification information can be considered.
     * 
     * This method presumes that the underlying connector object has been configured.
     * 
     * @param spec is the current pipeline specification object for this connection for the job that is doing the
     *            crawling.
     * @return a string, of unlimited length, which uniquely describes configuration and specification in such a way
     *         that if two such strings are equal, nothing that affects how or whether the document is indexed will be
     *         different.
     */
    @Override
    public VersionContext getPipelineDescription(Specification os) throws ManifoldCFException, ServiceInterruption
    {
        SpecPacker sp = new SpecPacker(os);
        return new VersionContext(sp.toPackedString(), params, os);
    }

    /**
     * Add (or replace) a document in the output data store using the connector. This method presumes that the connector
     * object has been configured, and it is thus able to communicate with the output data store should that be
     * necessary. The OutputSpecification is *not* provided to this method, because the goal is consistency, and if
     * output is done it must be consistent with the output description, since that was what was partly used to
     * determine if output should be taking place. So it may be necessary for this method to decode an output
     * description string in order to determine what should be done.
     * 
     * @param documentURI is the URI of the document. The URI is presumed to be the unique identifier which the output
     *            data store will use to process and serve the document. This URI is constructed by the repository
     *            connector which fetches the document, and is thus universal across all output connectors.
     * @param outputDescription is the description string that was constructed for this document by the
     *            getOutputDescription() method.
     * @param document is the document data to be processed (handed to the output data store).
     * @param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in
     *            with the repository document. May be null.
     * @param activities is the handle to an object that the implementer of a pipeline connector may use to perform
     *            operations, such as logging processing activity, or sending a modified document to the next stage in
     *            the pipeline.
     * @return the document status (accepted or permanently rejected).
     * @throws IOException only if there's a stream error reading the document data.
     */
    @Override
    public int addOrReplaceDocumentWithException(String documentURI, VersionContext pipelineDescription,
            RepositoryDocument document, String authorityNameString, IOutputAddActivity activities)
            throws ManifoldCFException, ServiceInterruption, IOException
    {
        // assumes use of Tika extractor before using this connector
        Logging.agents.debug("Starting OpenNlp extraction");
        
        SpecPacker sp = new SpecPacker(pipelineDescription.getSpecification());
        
        byte[] bytes = IOUtils.toByteArray(document.getBinaryStream());
        
        SentenceDetector sentenceDetector = OpenNlpExtractorConfig.sentenceDetector();
        Tokenizer tokenizer = OpenNlpExtractorConfig.tokenizer();
        POSTagger posTagger = OpenNlpExtractorConfig.posTagger();
        
        // create a duplicate
        RepositoryDocument docCopy = document.duplicate();
        
        if(document.getBinaryLength() > 0){
            String textContent = new String(bytes);
            StringBuffer buffer = new StringBuffer();
            String[] sentences = sentenceDetector.sentDetect(textContent);
            for (String sentence : sentences)
            {
                String[] tokens = tokenizer.tokenize(sentence);
                String[] posTags = posTagger.tag(tokens);
                
                for (int i = 0; i < tokens.length; i++)
                {
                    if(tokens[i].equals(posTags[i])){
                        continue;
                    }
                    if(sp.extractNouns()){
                        if(isMatchingNouns(posTags[i])){
                            buffer.append(tokens[i].toLowerCase());
                            buffer.append(",");
                        }
                    }
                    
                }
            }
            buffer.deleteCharAt(buffer.length()-1);
            Logging.agents.debug(buffer.toString());
            docCopy.addField(TEXT_FEATURES_FIELD, buffer.toString());
            docCopy.addField(HAS_CONTENT, "true");
        }
        // reset original stream
        docCopy.setBinary(new ByteArrayInputStream(bytes), bytes.length);

        return activities.sendDocument(documentURI, docCopy);
    }

    private boolean isMatchingNouns(String postag)
    {
        if (postag.equals("NN") || postag.equals("NNS")
                || postag.equals("NNP")
                || postag.equals("NNPS")) {
            return true;
        } else
            return false;
    }

    // ////////////////////////
    // UI Methods
    // ////////////////////////

    /**
     * Obtain the name of the form check javascript method to call.
     * 
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return the name of the form check javascript method.
     */
    @Override
    public String getFormCheckJavascriptMethodName(int connectionSequenceNumber)
    {
        return "s" + connectionSequenceNumber + "_checkSpecification";
    }

    /**
     * Obtain the name of the form presave check javascript method to call.
     * 
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return the name of the form presave check javascript method.
     */
    @Override
    public String getFormPresaveCheckJavascriptMethodName(int connectionSequenceNumber)
    {
        return "s" + connectionSequenceNumber + "_checkSpecificationForSave";
    }

    /**
     * Output the specification header section. This method is called in the head section of a job page which has
     * selected an output connection of the current type. Its purpose is to add the required tabs to the list, and to
     * output any javascript methods that might be needed by the job editing HTML.
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param os is the current output specification for this job.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param tabsArray is an array of tab names. Add to this array any tab names that are specific to the connector.
     */
    @Override
    public void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification os,
            int connectionSequenceNumber, List<String> tabsArray) throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

        tabsArray.add(Messages.getString(locale, "OpenNlpExtractor.FieldMappingTabName"));

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JS, paramMap);
    }

    /**
     * Output the specification body section. This method is called in the body section of a job page which has selected
     * an output connection of the current type. Its purpose is to present the required form elements for editing. The
     * coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>,
     * and <form> tags. The name of the form is "editjob".
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param os is the current output specification for this job.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param actualSequenceNumber is the connection within the job that has currently been selected.
     * @param tabName is the current tab name.
     */
    @Override
    public void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification os, int connectionSequenceNumber,
            int actualSequenceNumber, String tabName) throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        paramMap.put("TABNAME", tabName);
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));
        paramMap.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

        fillInFieldMappingSpecificationMap(paramMap, os);

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_FIELDMAPPING_HTML, paramMap);
    }

    /**
     * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a
     * possibility that form data for a connection has been posted. Its purpose is to gather form information and modify
     * the output specification accordingly. The name of the posted form is "editjob".
     * 
     * @param variableContext contains the post data, including binary file-upload information.
     * @param locale is the preferred local of the output.
     * @param os is the current output specification for this job.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return null if all is well, or a string error message if there is an error that should prevent saving of the job
     *         (and cause a redirection to an error page).
     */
    @Override
    public String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification os,
            int connectionSequenceNumber) throws ManifoldCFException
    {
        String seqPrefix = "s" + connectionSequenceNumber + "_";

        // remove old node data
        int i = 0;
        while (i < os.getChildCount())
        {
            SpecificationNode node = os.getChild(i);
            if (node.getType().equals(OpenNlpExtractorConfig.NODE_EXTRACT_NOUNS))
                os.removeChild(i);
            else
                i++;
        }

        SpecificationNode node = new SpecificationNode(OpenNlpExtractorConfig.NODE_EXTRACT_NOUNS);
        String extractNouns = variableContext.getParameter(seqPrefix + "extractnouns");
        if (extractNouns != null)
        {
            node.setAttribute(OpenNlpExtractorConfig.ATTRIBUTE_VALUE, extractNouns);
        }
        else
        {
            node.setAttribute(OpenNlpExtractorConfig.ATTRIBUTE_VALUE, "false");
        }
        // Add the new extractnouns config parameter
        os.addChild(os.getChildCount(), node);

        return null;
    }
    
    /**
     * View specification. This method is called in the body section of a job's view page. Its purpose is to present the
     * output specification information to the user. The coder can presume that the HTML that is output from this
     * configuration will be within appropriate <html> and <body> tags.
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param os is the current output specification for this job.
     */
    @Override
    public void viewSpecification(IHTTPOutput out, Locale locale, Specification os, int connectionSequenceNumber)
            throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

        fillInFieldMappingSpecificationMap(paramMap, os);
        Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);
    }

    protected static void fillInFieldMappingSpecificationMap(Map<String, Object> paramMap, Specification os)
    {
        String extractNouns = "true";
        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(OpenNlpExtractorConfig.NODE_EXTRACT_NOUNS))
            {
                extractNouns = sn.getAttributeValue(OpenNlpExtractorConfig.ATTRIBUTE_VALUE);
            }
        }
        paramMap.put("EXTRACTNOUNS", extractNouns);
    }

    protected static class SpecPacker
    {

        private final boolean extractNouns;

        public SpecPacker(Specification os)
        {
            boolean extractNouns = true;
            for (int i = 0; i < os.getChildCount(); i++)
            {
                SpecificationNode sn = os.getChild(i);

                if (sn.getType().equals(OpenNlpExtractorConfig.NODE_EXTRACT_NOUNS))
                {
                    String value = sn.getAttributeValue(OpenNlpExtractorConfig.ATTRIBUTE_VALUE);
                    extractNouns = Boolean.parseBoolean(value);
                }

            }
            this.extractNouns = extractNouns;
        }

        public String toPackedString()
        {
            StringBuilder sb = new StringBuilder();

            // extract nouns
            if (extractNouns)
                sb.append('+');
            else
                sb.append('-');

            return sb.toString();
        }

        public Boolean extractNouns()
        {
            return extractNouns;
        }
    }

}
