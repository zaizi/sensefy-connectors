package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import java.util.HashMap;
import java.util.Map;

import org.apache.manifoldcf.agents.output.amazoncloudsearch.AmazonCloudSearchParam.ParameterEnum;

/**
 * Parameters data for the amazoncloudsearch output connector.
 */
public class AmazonCloudSearchParam extends HashMap<ParameterEnum, String>
{
    /** Parameters constants */
    public enum ParameterEnum
    {
        DOCUMENTSERVICEENDPOINT("http://abcde.eu-west-1.cloudsearch.amazonaws.com"),

        MAXFILESIZE("16777216"),

        MIMETYPES("application/msword\n" + "application/vnd.ms-excel\n"
                + "application/vnd.openxmlformats-officedocument.wordprocessingml.document\n"
                + "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\n" + "text/html\n"
                + "application/pdf\n" + "application/vnd.ms-powerpoint\n"
                + "application/vnd.openxmlformats-officedocument.presentationml.presentation\n"
                + "application/vnd.oasis.opendocument.text\n" + "application/vnd.oasis.opendocument.spreadsheet\n"
                + "application/vnd.oasis.opendocument.formula\n" + "application/rtf\n" + "text/plain\n"
                + "audio/mpeg\n" + "audio/x-wav\n" + "audio/ogg\n" + "audio/flac\n" + "application/x-bittorrent"),

        EXTENSIONS("doc\n" + "docx\n" + "xls\n" + "xlsx\n" + "ppt\n" + "pptx\n" + "html\n" + "pdf\n" + "odt\n"
                + "ods\n" + "rtf\n" + "txt\n" + "mp3\n" + "mp4\n" + "wav\n" + "ogg\n" + "flac\n" + "torrent");

        final protected String defaultValue;

        private ParameterEnum(String defaultValue)
        {
            this.defaultValue = defaultValue;
        }
    }

    private static final long serialVersionUID = -1593234685772720029L;

    protected AmazonCloudSearchParam(ParameterEnum[] params)
    {
        super(params.length);
    }

    final public Map<String, String> buildMap()
    {
        Map<String, String> rval = new HashMap<String, String>();
        for (Map.Entry<ParameterEnum, String> entry : this.entrySet())
            rval.put(entry.getKey().name(), entry.getValue());
        return rval;
    }

}
