package com.hongik.graduate.BE.importdata;
import com.hongik.graduate.BE.common.exception.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
@Service @Transactional
public class WebImportService {
 private static final int MAX_BYTES=1048576;
 private final ImportedSourceRepository repository;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build();
 public record Request(String url){}
 public WebImportService(ImportedSourceRepository repository){this.repository=repository;}
 public ImportedSource collect(UUID user,Request request){
  if(request==null)throw bad();
  URI uri=validateUrl(request.url());
  String html=fetch(uri);
  Document document=Jsoup.parse(html,uri.toString());
  document.select("script,style,noscript,svg,nav,footer,header").remove();
  String title=document.title();
  if(title.isBlank())title=uri.getHost();
  if(title.length()>255)title=title.substring(0,255);
  String body=document.body()==null?"":document.body().text();
  if(body.isBlank())throw bad();
  if(body.length()>100000)body=body.substring(0,100000);
  ImportedSource saved=new ImportedSource(user,"URL",title);
  saved.sourceUrl=uri.toString();saved.collectedText=body;saved.sizeBytes=body.getBytes(StandardCharsets.UTF_8).length;
  return repository.save(saved);
 }
 public static URI validateUrl(String raw){
  if(raw==null||raw.isBlank()||raw.length()>2048)throw bad();
  try{
   URI uri=new URI(raw);
   String scheme=uri.getScheme();
   if(scheme==null||!Set.of("http","https").contains(scheme.toLowerCase(Locale.ROOT))
      ||uri.getRawUserInfo()!=null||uri.getHost()==null||uri.getRawFragment()!=null)throw bad();
   int port=uri.getPort();
   if(port!=-1&&port!=80&&port!=443)throw bad();
   String host=uri.getHost().toLowerCase(Locale.ROOT);
   if(host.equals("localhost")||host.endsWith(".localhost")||host.endsWith(".local")||
      host.endsWith(".internal")||host.endsWith(".onion")||host.indexOf('.')<0)throw bad();
   InetAddress[] addresses=InetAddress.getAllByName(host);
   if(addresses.length==0)throw bad();
   for(InetAddress ip:addresses){
    if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||
       ip.isSiteLocalAddress()||ip.isMulticastAddress())throw bad();
    byte[] b=ip.getAddress();
    if(b.length==4){
     int a=b[0]&255,c=b[1]&255;
     if(a==0||a==10||a==127||a>=224||a==169&&c==254||
        a==172&&c>=16&&c<=31||a==192&&c==168||
        a==100&&c>=64&&c<=127||a==192&&c==0||
        a==198&&(c==18||c==19)||a==203&&c==0||a==198&&c==51)throw bad();
    }else if((b[0]&254)==252||(b[0]&255)==254)throw bad();
   }
   return uri;
  }catch(URISyntaxException|UnknownHostException e){throw bad();}
 }
 private String fetch(URI uri){
  try{
   HttpRequest req=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
    .header("Accept","text/html").header("User-Agent","PofitImporter/1.0")
    .GET().build();
   HttpResponse<java.io.InputStream> response=client.send(req,HttpResponse.BodyHandlers.ofInputStream());
   try(var stream=response.body()){
    if(response.statusCode()!=200)throw bad();
    String mime=response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
    if(!mime.startsWith("text/html"))throw bad();
    byte[] bytes=stream.readNBytes(MAX_BYTES+1);
    if(bytes.length>MAX_BYTES)throw bad();
    return new String(bytes,StandardCharsets.UTF_8);
   }
  }catch(BusinessException e){throw e;}
   catch(Exception e){throw bad();}
 }
 private static BusinessException bad(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
}
