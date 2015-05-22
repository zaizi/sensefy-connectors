
<script type="text/javascript">
<!--
function checkConfig()
{
  if (editconnection.confport.value != "" && !isInteger(editconnection.confport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfPortMustBeAnInteger'))");
    editconnection.confport.focus();
    return false;
  }

  if (editconnection.confhost.value != "" && editconnection.confhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfHostMustNotIncludeSlash'))");
    editconnection.confhost.focus();
    return false;
  }

  if (editconnection.confpath.value != "" && !(editconnection.confpath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfPathMustBeginWithASlash'))");
    editconnection.confpath.focus();
    return false;
  }

  if (editconnection.confproxyport != null && editconnection.confproxyport.value != "" && !isInteger(editconnection.confproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfProxyPortMustBeAnInteger'))");
    editconnection.confproxyport.focus();
    return false;
  }

  if (editconnection.confproxyhost != null && editconnection.confproxyhost.value != "" && editconnection.confproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfProxyHostMustNotIncludeSlash'))");
    editconnection.confproxyhost.focus();
    return false;
  }

  return true;
}
 
function checkConfigForSave()
{
    
  if (editconnection.confhost.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfHostMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Server'))");
    editconnection.confhost.focus();
    return false;
  }
  
  if (editconnection.confhost.value != "" && editconnection.confhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Server'))");
    editconnection.confhost.focus();
    return false;
  }

  if (editconnection.confport.value != "" && !isInteger(editconnection.confport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Server'))");
    editconnection.confport.focus();
    return false;
  }

  if (editconnection.confpath.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfPathMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Server'))");
    editconnection.confpath.focus();
    return false;
  }
  
  if (editconnection.confpath.value != "" && !(editconnection.confpath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfPathMustBeginWithASlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Server'))");
    editconnection.confpath.focus();
    return false;
  }
  
  if (editconnection.confproxyhost !=null &&  editconnection.confproxyhost.value != "" && editconnection.confproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfProxyHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Proxy'))");
    editconnection.confhost.focus();
    return false;
  }

  if (editconnection.confproxyport != null &&  editconnection.confproxyport.value != "" && !isInteger(editconnection.confproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.ConfProxyPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.Proxy'))");
    editconnection.confport.focus();
    return false;
  }

  return true;
}
//-->
</script>
