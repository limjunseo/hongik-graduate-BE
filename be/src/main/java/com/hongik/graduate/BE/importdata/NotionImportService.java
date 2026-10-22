package com.hongik.graduate.BE.importdata;
import com.fasterxml.jackson.databind.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.*;import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service @Transactional
public class NotionImportService {
 private final ImportedSourceRepository repository;
 private final String token;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper mapper=new ObjectMapper();
 public record ImportRequest(String pageId){}
 public NotionImportService(ImportedSourceRepository repository,
 @Value("${app.imports.notion-token:}") String token){this.repository=repository;this.token=token;}
 public ImportedSource collect(UUID member,ImportRequest request){
  if(request==null||request.pageId()==null||token.isBlank())throw new BusinessException(ErrorCode.INVALID_REQUEST);
  UUID page;
  try{
   String input=request.pageId();
   String value=input.startsWith("https://")?URI.create(input).getPath():input;
   java.util.regex.Matcher match=java.util.regex.Pattern.compile("([0-9a-fA-F]{32}|[0-9a-fA-F-]{36})/?$").matcher(value);
   if(!match.find())throw new IllegalArgumentException();
   String raw=match.group(1).replace("-","");
   page=UUID.fromString(raw.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})","$1-$2-$3-$4-$5"));
  }catch(Exception e){throw new BusinessException(ErrorCode.INVALID_REQUEST);}
  StringBuilder text=new StringBuilder();
  ArrayDeque<String> queue=new ArrayDeque<>();
  queue.add(page.toString());
  int requests=0;
  while(!queue.isEmpty()&&requests<20&&text.length()<100000){
   String block=queue.removeFirst(),cursor=null;
   do {
    String endpoint="https://api.notion.com/v1/blocks/"+block+"/children?page_size=100";
    if(cursor!=null)endpoint+="&start_cursor="+URLEncoder.encode(cursor,java.nio.charset.StandardCharsets.UTF_8);
    JsonNode root=fetch(endpoint);requests++;
    for(JsonNode node:root.path("results")){
     if(node.path("has_children").asBoolean()&&queue.size()<100)queue.add(node.path("id").asText());
     String type=node.path("type").asText();
     JsonNode rich=node.path(type).path("rich_text");
     for(JsonNode part:rich){
      String value=part.path("plain_text").asText("");
      if(text.length()+value.length()>100000)break;
      text.append(value);
     }
     if(rich.isArray()&&!rich.isEmpty())text.append('\n');
    }
    cursor=root.path("has_more").asBoolean()?root.path("next_cursor").asText(null):null;
   }while(cursor!=null&&requests<20&&text.length()<100000);
  }
  ImportedSource item=new ImportedSource(member,"NOTION","Notion page "+page);
  item.sourceUrl="https://www.notion.so/"+page.toString().replace("-","");
  item.collectedText=text.toString();item.sizeBytes=text.length();
  return repository.save(item);
 }
 private JsonNode fetch(String url){
  try{
   HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
    .header("Authorization","Bearer "+token).header("Notion-Version","2022-06-28")
    .header("Accept","application/json").GET().build();
   HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw new BusinessException(ErrorCode.INVALID_REQUEST);
   return mapper.readTree(response.body());
  }catch(BusinessException e){throw e;}
  catch(Exception e){throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);}
 }
}