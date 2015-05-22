<script type="text/javascript">
<!--
function checkConfig()
{
  return true;
}

function checkConfigForSave()
{
  if (editconnection.password.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.ThePasswordMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.password.focus();
    return false;
  }
  if (editconnection.server.value =="")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.ServerNameMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.server.focus();
    return false;
  }
  else if(!editconnection.server.value.indexOf('/')==-1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.ServerNameCantContainSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.server.focus();
    return false;
  }
  if (editconnection.port.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.ThePortMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.port.focus();
    return false;
  }
  else if (!isInteger(editconnection.port.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.TheServerPortMustBeAValidInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.port.focus();
    return false;
  }
  if(editconnection.path.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.PathMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('AlfrescoAuthorityConnector.Server'))");
    editconnection.path.focus();
    return false;
  }
  return true;
}
// -->
</script>