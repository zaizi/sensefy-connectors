package org.apache.manifoldcf.crawler.connectors.box;

import com.box.sdk.BoxCollaboration;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxUser;
import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alessandro Benedetti
 *         14/04/2015
 *         mcf-box-connector
 */
public class BoxRepositoryConnectorTest extends TestCase{

    private BoxRepositoryConnector toTest;


    public void setUp(){
        toTest=new BoxRepositoryConnector();
    }

    public void testVersionGeneration() throws ParseException {
        BoxFile fileMock=mock(BoxFile.class);
        BoxFile.Info mockFileInfo = mock(BoxFile.Info.class);;
        BoxFolder.Info mockParentInfo = mock(BoxFolder.Info.class);
        BoxFolder mockParentFolder= mock(BoxFolder.class);
        BoxUser.Info mockOwnedBy = mock(BoxUser.Info.class);
        BoxCollaboration.Info mockCollaborationInfo1 = mock(BoxCollaboration.Info.class);
        BoxCollaboration.Info mockCollaborationInfo2 = mock(BoxCollaboration.Info.class);
        BoxUser.Info mockCollaborator1=mock(BoxUser.Info.class);
        BoxUser.Info mockCollaborator2=mock(BoxUser.Info.class);
        List<BoxCollaboration.Info> collaborationList=new ArrayList<BoxCollaboration.Info>();
        collaborationList.add(mockCollaborationInfo1);
        collaborationList.add(mockCollaborationInfo2);
        List<BoxFolder> mockCollectionPath=mock(List.class);
        String description="originalDescription";
        String name="originalName";
        List<String> tags=new LinkedList<String>();
        tags.add("tag1");
        tags.add("tag2");


        Date modifiedAt = new SimpleDateFormat("yyyy-MM-dd").parse("2015-01-01");
        Date contentModifiedAt = new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-01");
        //Mock File getters
        when(mockFileInfo.getModifiedAt()).thenReturn(modifiedAt);
        when(mockFileInfo.getContentModifiedAt()).thenReturn(contentModifiedAt);
        when(mockFileInfo.getParent()).thenReturn(mockParentInfo);
        when(mockFileInfo.getSharedLink()).thenReturn(null);
        when(mockFileInfo.getPathCollection()).thenReturn(mockCollectionPath);
        when(mockFileInfo.getDescription()).thenReturn(description);
        when(mockFileInfo.getName()).thenReturn(name);
        when(mockFileInfo.getTags()).thenReturn(tags);

        //Mock parent Info getters
        when(mockParentInfo.getResource()).thenReturn(mockParentFolder);
        when(mockParentInfo.getOwnedBy()).thenReturn(mockOwnedBy);
        when(mockOwnedBy.getID()).thenReturn("ownerId");
        when(mockParentFolder.getInfo(anyString())).thenReturn(mockParentInfo);
        //Mock Collaborations
        when(mockCollaborationInfo1.getRole()).thenReturn(BoxCollaboration.Role.VIEWER);
        when(mockCollaborationInfo2.getRole()).thenReturn(BoxCollaboration.Role.EDITOR);
        when(mockCollaborationInfo1.getAccessibleBy()).thenReturn(mockCollaborator1);
        when(mockCollaborationInfo2.getAccessibleBy()).thenReturn(mockCollaborator2);
        when(mockCollaborator1.getID()).thenReturn("c1");
        when(mockCollaborator2.getID()).thenReturn("c2");
        when(mockParentFolder.getCollaborations()).thenReturn(collaborationList);
        //Mock File info
        when(fileMock.getInfo("modified_at",
                "content_modified_at", "parent","path_collection","description","name","tags")).thenReturn(mockFileInfo);
        when(fileMock.getInfo("shared_link")).thenReturn(mockFileInfo);

        String version=toTest.generateVersion(fileMock);

        //Twin file
        BoxFile fileMock2=mock(BoxFile.class);
        BoxFile.Info mockFileInfo2 = mock(BoxFile.Info.class);;
        BoxFolder.Info mockParentInfo2 = mock(BoxFolder.Info.class);
        BoxFolder mockParentFolder2= mock(BoxFolder.class);
        BoxUser.Info mockOwnedBy2 = mock(BoxUser.Info.class);
        BoxCollaboration.Info mockCollaborationInfo12 = mock(BoxCollaboration.Info.class);
        BoxCollaboration.Info mockCollaborationInfo22 = mock(BoxCollaboration.Info.class);
        BoxUser.Info mockCollaborator12=mock(BoxUser.Info.class);
        BoxUser.Info mockCollaborator22=mock(BoxUser.Info.class);
        List<BoxCollaboration.Info> collaborationList2=new ArrayList<BoxCollaboration.Info>();
        collaborationList2.add(mockCollaborationInfo12);
        collaborationList2.add(mockCollaborationInfo22);


        Date modifiedAt2 = new SimpleDateFormat("yyyy-MM-dd").parse("2015-01-01");
        Date contentModifiedAt2 = new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-01");
        //Mock File getters
        when(mockFileInfo2.getModifiedAt()).thenReturn(modifiedAt2);
        when(mockFileInfo2.getContentModifiedAt()).thenReturn(contentModifiedAt2);
        when(mockFileInfo2.getParent()).thenReturn(mockParentInfo2);
        when(mockFileInfo2.getSharedLink()).thenReturn(null);
        when(mockFileInfo2.getPathCollection()).thenReturn(mockCollectionPath);
        when(mockFileInfo2.getDescription()).thenReturn(description);
        when(mockFileInfo2.getName()).thenReturn(name);
        when(mockFileInfo2.getTags()).thenReturn(tags);
        //Mock parent Info getters
        when(mockParentInfo2.getResource()).thenReturn(mockParentFolder2);
        when(mockParentInfo2.getOwnedBy()).thenReturn(mockOwnedBy2);
        when(mockOwnedBy2.getID()).thenReturn("ownerId");
        when(mockParentFolder2.getInfo(anyString())).thenReturn(mockParentInfo2);
        //Mock Collaborations
        when(mockCollaborationInfo12.getRole()).thenReturn(BoxCollaboration.Role.VIEWER);
        when(mockCollaborationInfo22.getRole()).thenReturn(BoxCollaboration.Role.EDITOR);
        when(mockCollaborationInfo12.getAccessibleBy()).thenReturn(mockCollaborator12);
        when(mockCollaborationInfo22.getAccessibleBy()).thenReturn(mockCollaborator22);
        when(mockCollaborator12.getID()).thenReturn("c1");
        when(mockCollaborator22.getID()).thenReturn("c2");
        when(mockParentFolder2.getCollaborations()).thenReturn(collaborationList2);
        //Mock File info
        when(fileMock2.getInfo("modified_at",
                "content_modified_at", "parent","path_collection","description","name","tags")).thenReturn(mockFileInfo2);
        when(fileMock2.getInfo("shared_link")).thenReturn(mockFileInfo2);
        assertEquals(version,toTest.generateVersion(fileMock2));

        //we add one permission

        BoxCollaboration.Info mockCollaborationInfo3 = mock(BoxCollaboration.Info.class);
        BoxUser.Info mockCollaborator3=mock(BoxUser.Info.class);
        collaborationList.add(mockCollaborationInfo3);
        when(mockCollaborationInfo3.getRole()).thenReturn(BoxCollaboration.Role.EDITOR);
        when(mockCollaborationInfo3.getAccessibleBy()).thenReturn(mockCollaborator3);
        when(mockCollaborator3.getID()).thenReturn("c3");

        String version2=toTest.generateVersion(fileMock);

        assertFalse(version.equals(version2));
        assertEquals(version2,toTest.generateVersion(fileMock));

        //we change a permission role
        when(mockCollaborationInfo1.getRole()).thenReturn(BoxCollaboration.Role.PREVIEWER);
        String version3=toTest.generateVersion(fileMock);
        assertFalse(version3.equals(version2));

        //we remove one permission
        collaborationList.remove(mockCollaborationInfo1);
        String version4=toTest.generateVersion(fileMock);
        assertEquals(version4, version3);

        //we remove another permission
        collaborationList.remove(mockCollaborationInfo2);
        String version5=toTest.generateVersion(fileMock);
        assertFalse(version5.equals(version4));
        //we move the file
        List<BoxFolder> mockCollectionPath2=mock(List.class);
        when(mockFileInfo.getPathCollection()).thenReturn(mockCollectionPath2);
        String version6=toTest.generateVersion(fileMock);
        assertFalse(version6.equals(version5));
        //change the description
        String description2="changed description";
        when(mockFileInfo.getDescription()).thenReturn(description2);
        String version7=toTest.generateVersion(fileMock);
        assertFalse(version7.equals(version6));
        //remove the description
        when(mockFileInfo.getDescription()).thenReturn(null);
        String version8=toTest.generateVersion(fileMock);
        assertFalse(version8.equals(version7));
        //change the name
        String name2="changed name";
        when(mockFileInfo.getName()).thenReturn(name2);
        String version9=toTest.generateVersion(fileMock);
        assertFalse(version9.equals(version8));
        //add one tag
        tags.add("tag4");
        String version10=toTest.generateVersion(fileMock);
        assertFalse(version10.equals(version9));
        //remove one tag
        tags.remove("tag4");
        String version11=toTest.generateVersion(fileMock);
        assertFalse(version11.equals(version10));
        //remove all tags
        tags=null;
        when(mockFileInfo.getTags()).thenReturn(tags);
        String version12=toTest.generateVersion(fileMock);
        assertFalse(version12.equals(version11));



    }

    public void testUrlGeneration(){
        BoxFile.Info mockFileInfo = mock(BoxFile.Info.class);;
        BoxFolder.Info mockParentInfo = mock(BoxFolder.Info.class);
        when(mockFileInfo.getID()).thenReturn("fileId");
        when(mockParentInfo.getID()).thenReturn("parentId");
        String urlGenerated = toTest.generateUrl(mockFileInfo, mockParentInfo);
        assertEquals("https://app.box.com/files/0/f/parentId/1/f_fileId",urlGenerated);
    }
}
