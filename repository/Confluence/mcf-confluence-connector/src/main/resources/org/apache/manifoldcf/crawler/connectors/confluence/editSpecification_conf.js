

<script type="text/javascript">
<!--
function checkSpecificationForSave()
{

	
  
  return true;
}
 
function SpecOp(n, opValue, anchorvalue)
{
  eval("editjob."+n+".value = \""+opValue+"\"");
  postFormSetAnchor(anchorvalue);
}

function SpecDeleteToken(i)
{
  SpecOp("accessop_"+i,"Delete","token_"+i);
}

function SpecAddToken(i)
{
  if (editjob.spectoken.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfRepositoryConnector.TypeInAnAccessToken'))");
    editjob.spectoken.focus();
    return;
  }
  SpecOp("accessop","Add","token_"+i);
}

//-->
</script>
