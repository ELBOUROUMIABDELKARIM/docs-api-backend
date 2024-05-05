package fr.norsys.docsapi.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DocumentLocalServiceTest extends MyContainer{

    @Test
    public void test_upload_success(){

    }

    @Test
    public void test_upload_IOError(){

    }

    @Test
    public void test_upload_duplicate_document(){

    }

    @Test
    public void testDownload_Success(){

    }

    @Test
    public void testDownload_Unauthorized(){

    }

    @Test
    public void testDownload_DocumentNotFound(){

    }

    @Test
    public void testGetList_Success(){

    }

    @Test
    public void testGetList_NoDocumentsFound(){

    }

    @Test
    public void testGetListPagination_Success(){

    }

    @Test
    public void testGetListPagination_NoDocumentsFound(){

    }

    @Test
    public void testShare_Success(){

    }

    @Test
    public void testShare_InvalidPermissions(){

    }

    @Test
    public void testShare_UserNotFound(){

    }

    @Test
    public void testSharedWithMe_Success(){

    }

    @Test
    public void testSharedWithMe_NoSharedDocumentsFound(){

    }

    @Test
    public void testSearch_Success(){

    }

    @Test
    public void testSearch_NoResults(){

    }

    @Test
    public void testSearchSharedWithMe_Success(){

    }

    @Test
    public void testSearchSharedWithMe_NoResults(){

    }

    @Test
    public void testGet_Success(){

    }

    @Test
    public void testGet_DocumentNotFound(){

    }

    @Test
    public void testGet_UnauthorizedAccess(){

    }

    @Test
    public void testDelete_Success(){

    }

    @Test
    public void testDelete_Unauthorized(){

    }

    @Test
    public void testDelete_DocumentNotFound(){

    }

    @Test
    public void testValidatePermissions(){

    }

    @Test
    public void testResolveEffectivePermissions(){

    }

    @Test
    public void testParsePermissions(){

    }

    @Test
    public void testGetAuthenticatedUser(){

    }

    @Test
    public void testCreateDocument(){

    }

    @Test
    public void testCreateDirectories() {

    }

    @Test
    public void testValidateDocument(){

    }

    @Test
    public void testConvertToDto(){

    }

    @Test
    public void testHasPermission(){

    }


}
