package org.apache.manifoldcf.agents.transformation.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.im4java.core.IM4JavaException;

import com.zaizi.sensefy.cbir.feature.extract.ColorExtractor;
import com.zaizi.sensefy.cbir.feature.extract.SIFTExtractor;

public class ImageFeatureExtractor extends BaseTransformationConnector
{

    private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
    private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
    private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

    private static final String MEDIA_TYPE_IMAGE = "image";
    private static final String MIME_TYPE_UNPROCESSED = "application/octet-stream";

    private static final String IMG_SIFT = "img:sift";
    private static final String IMG_COLOR = "img:color";
    private static final String IMG_STATUS = "img:status";

    private static final String IMG_STATUS_UNPROCESSED = "UNPROCESSED";

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
        Logging.agents.debug("Starting Low level image feature extraction");

        SpecPacker sp = new SpecPacker(pipelineDescription.getSpecification());

        // detect image types
        // if Tika extractor is used before using this connector, mimeTypes are set
        // otherwise default mimetype - application/octet-stream

        String mimeType = document.getMimeType();
        if (mimeType == null)
        {
            mimeType = "application/octet-stream";
        }
        Logging.agents.debug("Initial Mime Type: " + mimeType);

        // make sure stream supports mark and reset
        byte[] bytes = IOUtils.toByteArray(document.getBinaryStream());

        Metadata metadata = new Metadata();
        String mediaType = "";
        if (mimeType.equals(MIME_TYPE_UNPROCESSED))
        {
            Logging.agents.debug("Unprocessed MimeType");
            // use tika to check for image type
            TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
            Detector detector = tikaConfig.getDetector();
            TikaInputStream tis = TikaInputStream.get(new ByteArrayInputStream(bytes));
            MediaType media = detector.detect(tis, metadata);
            mediaType = media.getType();
        }
        else
        {
            String[] splitted = mimeType.split("/");
            mediaType = splitted[0];
        }

        Logging.agents.debug("Media Type: " + mediaType);

        // create a duplicate
        RepositoryDocument docCopy = document.duplicate();

        if (mediaType.equals(MEDIA_TYPE_IMAGE))
        {
            try
            {
                SIFTExtractor siftExtractor = new SIFTExtractor();
                String siftFeatures = siftExtractor.extractSIFTEncoded(new ByteArrayInputStream(bytes));
                metadata.add(IMG_SIFT, siftFeatures);
                String imagemagickPath = sp.getImagemagickPath();
                
                ColorExtractor colorExtractor = new ColorExtractor(imagemagickPath);
                String colorText = colorExtractor.extractColor(new ByteArrayInputStream(bytes));
                metadata.add(IMG_COLOR, colorText);
                metadata.add(IMG_STATUS, IMG_STATUS_UNPROCESSED);

                // mapping with configuration definition
                String[] metaNames = metadata.names();
                for (String metaName : metaNames)
                {
                    String value = metadata.get(metaName);
                    String target = sp.getMapping(metaName);
                    if (target != null)
                    {
                        docCopy.addField(target, value);
                    }
                    else
                    {
                        if (sp.keepAllMetadata())
                        {
                            docCopy.addField(metaName, value);
                        }
                    }
                }
                docCopy.setBinary(new ByteArrayInputStream(bytes), bytes.length);

            }
            catch (ClassNotFoundException e)
            {
                Logging.agents.error("Error in extracting sift features", e);
            }
            catch (InterruptedException e)
            {
                Logging.agents.error("Error in extracting color", e);
            }
            catch (IM4JavaException e)
            {
                Logging.agents.error("Error in extracting color with im4java", e);
            }
        }

        return activities.sendDocument(documentURI, docCopy);
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

        tabsArray.add(Messages.getString(locale, "ImageFeatureExtractor.FieldMappingTabName"));

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

        String x = variableContext.getParameter(seqPrefix + "fieldmapping_count");
        if (x != null && x.length() > 0)
        {
            // remove old node data
            int i = 0;
            while (i < os.getChildCount())
            {
                SpecificationNode node = os.getChild(i);
                if (node.getType().equals(ImageExtractorConfig.NODE_FIELDMAP)
                        || node.getType().equals(ImageExtractorConfig.NODE_KEEPMETADATA))
                    os.removeChild(i);
                else
                    i++;
            }

            // adding new form data
            int count = Integer.parseInt(x);
            i = 0;
            while (i < count)
            {
                String prefix = seqPrefix + "fieldmapping_";
                String suffix = "_" + Integer.toString(i);
                String op = variableContext.getParameter(prefix + "op" + suffix);
                if (op == null || !op.equals("Delete"))
                {
                    // Gather the fieldmap etc.
                    String source = variableContext.getParameter(prefix + "source" + suffix);
                    String target = variableContext.getParameter(prefix + "target" + suffix);
                    if (target == null)
                        target = "";
                    SpecificationNode node = new SpecificationNode(ImageExtractorConfig.NODE_FIELDMAP);
                    node.setAttribute(ImageExtractorConfig.ATTRIBUTE_SOURCE, source);
                    node.setAttribute(ImageExtractorConfig.ATTRIBUTE_TARGET, target);
                    os.addChild(os.getChildCount(), node);
                }
                i++;
            }

            String addop = variableContext.getParameter(seqPrefix + "fieldmapping_op");
            if (addop != null && addop.equals("Add"))
            {
                String source = variableContext.getParameter(seqPrefix + "fieldmapping_source");
                String target = variableContext.getParameter(seqPrefix + "fieldmapping_target");
                if (target == null)
                    target = "";
                SpecificationNode node = new SpecificationNode(ImageExtractorConfig.NODE_FIELDMAP);
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_SOURCE, source);
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_TARGET, target);
                os.addChild(os.getChildCount(), node);
            }

            // get imagemagick path
            SpecificationNode node = new SpecificationNode(ImageExtractorConfig.NODE_IMAGEMAGICKPATH);
            String imagemagickPath = variableContext.getParameter(seqPrefix + "imagemagickpath");
            if(imagemagickPath != null){
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_VALUE, imagemagickPath);
            }
            else{
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_VALUE, "");
            }
            os.addChild(os.getChildCount(), node);

            // Gather the keep all metadata parameter to be the last one
            node = new SpecificationNode(ImageExtractorConfig.NODE_KEEPMETADATA);
            String keepAll = variableContext.getParameter(seqPrefix + "keepallmetadata");
            if (keepAll != null)
            {
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_VALUE, keepAll);
            }
            else
            {
                node.setAttribute(ImageExtractorConfig.ATTRIBUTE_VALUE, "false");
            }
            // Add the new keepallmetadata config parameter
            os.addChild(os.getChildCount(), node);

        }
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
        // /field mapping filling
        List<Map<String, String>> fieldMappings = new ArrayList<Map<String, String>>();
        String keepAllMetadataValue = "true";
        String imagemagickPath = "";
        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(ImageExtractorConfig.NODE_FIELDMAP))
            {
                String source = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_SOURCE);
                String target = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_TARGET);
                String targetDisplay;
                if (target == null)
                {
                    target = "";
                    targetDisplay = "(remove)";
                }
                else
                    targetDisplay = target;

                Map<String, String> fieldMapping = new HashMap<String, String>();
                fieldMapping.put("SOURCE", source);
                fieldMapping.put("TARGET", target);
                fieldMapping.put("TARGETDISPLAY", targetDisplay);
                fieldMappings.add(fieldMapping);
            }
            else if (sn.getType().equals(ImageExtractorConfig.NODE_KEEPMETADATA))
            {
                keepAllMetadataValue = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_VALUE);
            }
            else if (sn.getType().equals(ImageExtractorConfig.NODE_IMAGEMAGICKPATH))
            {
                imagemagickPath = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_VALUE);
                if (imagemagickPath == null)
                {
                    imagemagickPath = "";
                }
            }
        }
        paramMap.put("FIELDMAPPINGS", fieldMappings);
        paramMap.put("KEEPALLMETADATA", keepAllMetadataValue);
        paramMap.put("IMAGEMAGICKPATH", imagemagickPath);
    }

    protected static class SpecPacker
    {
        private final Map<String, String> sourceTargets = new HashMap<String, String>();
        private final boolean keepAllMetadata;
        private final String imagemagickPath;
        
        public SpecPacker(Specification os)
        {
            boolean keepAllMetadata = true;
            String imagemagickPath = null;
            for (int i = 0; i < os.getChildCount(); i++)
            {
                SpecificationNode sn = os.getChild(i);

                if (sn.getType().equals(ImageExtractorConfig.NODE_KEEPMETADATA))
                {
                    String value = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_VALUE);
                    keepAllMetadata = Boolean.parseBoolean(value);
                }
                else if (sn.getType().equals(ImageExtractorConfig.NODE_FIELDMAP))
                {
                    String source = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_SOURCE);
                    String target = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_TARGET);

                    if (target == null)
                    {
                        target = "";
                    }
                    sourceTargets.put(source, target);
                }
                else if(sn.getType().equals(ImageExtractorConfig.NODE_IMAGEMAGICKPATH)){
                    imagemagickPath = sn.getAttributeValue(ImageExtractorConfig.ATTRIBUTE_VALUE);
                }
            }
            this.keepAllMetadata = keepAllMetadata;
            this.imagemagickPath = imagemagickPath;
        }

//        public SpecPacker(String packedString)
//        {
//
//            int index = 0;
//
//            // Mappings
//            final List<String> packedMappings = new ArrayList<String>();
//            index = unpackList(packedMappings, packedString, index, '+');
//            String[] fixedList = new String[2];
//            for (String packedMapping : packedMappings)
//            {
//                unpackFixedList(fixedList, packedMapping, 0, ':');
//                sourceTargets.put(fixedList[0], fixedList[1]);
//            }
//            
//            // Keep all metadata
//            if (packedString.length() > index)
//                keepAllMetadata = (packedString.charAt(index++) == '+');
//            else
//                keepAllMetadata = true;
//
//        }

        public String toPackedString()
        {
            StringBuilder sb = new StringBuilder();
            int i;

            // Mappings
            final String[] sortArray = new String[sourceTargets.size()];
            i = 0;
            for (String source : sourceTargets.keySet())
            {
                sortArray[i++] = source;
            }
            java.util.Arrays.sort(sortArray);

            List<String> packedMappings = new ArrayList<String>();
            String[] fixedList = new String[2];
            for (String source : sortArray)
            {
                String target = sourceTargets.get(source);
                StringBuilder localBuffer = new StringBuilder();
                fixedList[0] = source;
                fixedList[1] = target;
                packFixedList(localBuffer, fixedList, ':');
                packedMappings.add(localBuffer.toString());
            }
            packList(sb, packedMappings, '+');
            
            if(imagemagickPath != null){
                sb.append('+');
                sb.append(imagemagickPath);
            }
            else {
                sb.append('-');
            }

            // Keep all metadata
            if (keepAllMetadata)
                sb.append('+');
            else
                sb.append('-');

            return sb.toString();
        }

        public String getMapping(String source)
        {
            return sourceTargets.get(source);
        }
        
        public String getImagemagickPath(){
            return imagemagickPath;
        }

        public boolean keepAllMetadata()
        {
            return keepAllMetadata;
        }

    }

}
