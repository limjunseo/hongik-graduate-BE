package com.hongik.graduate.BE.importdata;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
@Service @Transactional
public class ImportFileService{
 private final ImportedSourceRepository repository;
 private final Path baseDir;
 public ImportFileService(ImportedSourceRepository repository,
 @Value("${app.imports.directory:./uploads/imports}") String directory){
 this.repository=repository;this.baseDir=Path.of(directory).toAbsolutePath().normalize();}
 public ImportedSource upload(UUID member,MultipartFile file){
 if(file==null||file.isEmpty()||file.getSize()>10*1024*1024)throw bad();
 String original=Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf");
 if(!original.toLowerCase(Locale.ROOT).endsWith(".pdf"))throw bad();
 String title=Path.of(original.replace('\\','/')).getFileName().toString();
 if(title.length()>255)throw bad();
 String key=UUID.randomUUID()+".pdf";Path target=baseDir.resolve(key);
 try{
  Files.createDirectories(baseDir);
  try(InputStream stream=file.getInputStream()){
   byte[] magic=stream.readNBytes(5);
   if(!Arrays.equals(magic,"%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))throw bad();
   try(OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){
    out.write(magic);
    stream.transferTo(out);
   }
  }
  if(Files.size(target)>10*1024*1024){Files.deleteIfExists(target);throw bad();}
  ImportedSource item=new ImportedSource(member,"PDF",title);
  item.storageKey=key;item.sizeBytes=Files.size(target);
  try{return repository.save(item);}
  catch(RuntimeException e){Files.deleteIfExists(target);throw e;}
 }catch(IOException e){try{Files.deleteIfExists(target);}catch(IOException ignored){}throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);}
 }
 @Transactional(readOnly=true) public List<ImportedSource> list(UUID member){return repository.findByMemberIdOrderByCreatedAtDesc(member);}
 @Transactional(readOnly=true) public ImportedSource get(UUID member,UUID id){
 return repository.findByIdAndMemberId(id,member).orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
 }
 @Transactional(readOnly=true) public byte[] download(UUID member,UUID id){
 ImportedSource source=get(member,id);
 if(!"PDF".equals(source.sourceType)||source.storageKey==null)throw bad();
 try{return Files.readAllBytes(baseDir.resolve(source.storageKey));}
 catch(IOException e){throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);}
 }
 private BusinessException bad(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
}