import com.youtrace.core.IncidentEngine;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
/** Offline adapter for the same Java core; not the Spring Boot runtime. */
public class LocalServer {
 static final IncidentEngine engine=new IncidentEngine();
 static final Map<String,String> assets=Map.of("/","demo/index.html","/app.js","frontend/src/app.js","/style.css","frontend/src/style.css","/vendor/react.js","demo/vendor/react.js","/vendor/react-dom.js","demo/vendor/react-dom.js");
 public static void main(String[] args)throws Exception{
  int port=args.length>0?Integer.parseInt(args[0]):8080;
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
  server.createContext("/",x->{try{handle(x);}catch(IllegalArgumentException e){send(x,400,"application/json",json(Map.of("error",e.getMessage())).getBytes(StandardCharsets.UTF_8));}catch(Exception e){e.printStackTrace();send(x,500,"application/json","{\"error\":\"Falha interna\"}".getBytes(StandardCharsets.UTF_8));}});
  server.start();System.out.println("YouTrace AI demo: http://localhost:"+port+" (Ctrl+C para sair)");
 }
 static void handle(HttpExchange x)throws Exception{
  String p=x.getRequestURI().getPath(),m=x.getRequestMethod();
  if(p.startsWith("/api/")){Object result;
   if(p.equals("/api/dashboard")&&m.equals("GET"))result=engine.dashboard();
   else if(p.equals("/api/reset")&&m.equals("POST"))result=engine.reset();
   else if(p.equals("/api/reports")&&m.equals("POST")){
    byte[] bytes=x.getRequestBody().readNBytes(8193);if(bytes.length>8192)throw new IllegalArgumentException("Relato muito grande.");
    var fields=new HashMap<String,String>();for(String pair:new String(bytes,StandardCharsets.UTF_8).split("&")){String[] kv=pair.split("=",2);if(kv.length==2)fields.put(URLDecoder.decode(kv[0],StandardCharsets.UTF_8),URLDecoder.decode(kv[1],StandardCharsets.UTF_8));}result=engine.add(fields.get("actor"),fields.get("text"));
   }else{send(x,404,"application/json","{\"error\":\"Rota não encontrada\"}".getBytes(StandardCharsets.UTF_8));return;}
   send(x,200,"application/json",json(result).getBytes(StandardCharsets.UTF_8));return;
  }
  if(!m.equals("GET")||!assets.containsKey(p)){send(x,404,"text/plain",new byte[0]);return;}
  String type=p.endsWith(".js")?"application/javascript":p.endsWith(".css")?"text/css":"text/html";
  send(x,200,type,Files.readAllBytes(Path.of(assets.get(p))));
 }
 static void send(HttpExchange x,int status,String type,byte[] body)throws IOException{x.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(status,body.length);try(var out=x.getResponseBody()){out.write(body);}}
 static String json(Object v){
  if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();
  if(v instanceof String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '\\'->b.append("\\\\");case '"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append("\\u%04x".formatted((int)c));else b.append(c);}}}return b.append('"').toString();}
  if(v instanceof Collection<?> c)return c.stream().map(LocalServer::json).collect(Collectors.joining(",","[","]"));
  if(v instanceof Map<?,?> m)return m.entrySet().stream().map(e->json(e.getKey())+":"+json(e.getValue())).collect(Collectors.joining(",","{","}"));
  if(v.getClass().isRecord()){var map=new LinkedHashMap<String,Object>();try{for(var c:v.getClass().getRecordComponents())map.put(c.getName(),c.getAccessor().invoke(v));}catch(ReflectiveOperationException e){throw new RuntimeException(e);}return json(map);}
  throw new IllegalArgumentException("Tipo desconhecido");
 }
}
