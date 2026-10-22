package com.hongik.graduate.experience;
import com.hongik.graduate.BE.importdata.*;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.BE.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ImportIntegrationTest{
 @Autowired MemberRepository members;
 @Autowired ImportedSourceRepository imports;
 @Autowired NotionImportService notion;
 @TempDir Path tempDir;
 @Test void pdfValidationOwnershipAndDownload() throws Exception{
  var owner=members.save(Member.google("import-owner@example.com","import-owner"));
  var other=members.save(Member.google("import-other@example.com","import-other"));
  ImportFileService service=new ImportFileService(imports,tempDir.toString());
  var pdf=new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.4\nhello".getBytes());
  var saved=service.upload(owner.getId(),pdf);
  assertEquals("PDF",saved.sourceType);
  assertArrayEquals(pdf.getBytes(),service.download(owner.getId(),saved.id));
  assertThrows(RuntimeException.class,()->service.get(other.getId(),saved.id));
  assertThrows(RuntimeException.class,()->service.download(other.getId(),saved.id));
  assertThrows(RuntimeException.class,()->service.upload(owner.getId(),new MockMultipartFile(
   "file","fake.pdf","application/pdf","not a pdf".getBytes())));
  assertThrows(RuntimeException.class,()->service.upload(owner.getId(),new MockMultipartFile(
   "file","fake.txt","text/plain","%PDF-1.4".getBytes())));
 }
 @Test void notionRequiresConfiguredIntegrationToken(){
  var owner=members.save(Member.google("notion-import@example.com","notion-import"));
  NotionImportService withoutToken=new NotionImportService(imports,"");
  assertThrows(RuntimeException.class,()->withoutToken.collect(owner.getId(),new NotionImportService.ImportRequest("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
 }
}