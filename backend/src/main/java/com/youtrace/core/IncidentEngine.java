package com.youtrace.core;

import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Local NLP baseline: TF-IDF + cosine, then service/time evidence correlation. */
public final class IncidentEngine {
 public record Pattern(String id,String title,String service,String text,String hypothesis,String test) {}
 public record Report(String id,String actor,String text,String time) {}
 public record Log(String id,String service,String message,String time) {}
 public record Match(String reportId,String patternId,double similarity,double margin) {}
 public record Incident(String id,String title,String service,String severity,int count,double similarity,String hypothesis,String test,List<Report> reports,List<Log> logs) {}
 public record Dashboard(int reportCount,int logCount,int unresolved,List<Incident> incidents,List<Report> unmatched,List<Report> reports) {}
 private static final Set<String> STOP=Set.of("a","o","as","os","um","uma","de","do","da","dos","das","e","em","no","na","nos","nas","para","por","com","que","ao","me","eu","meu","minha","esta","estou","foi","ser","se","nao","quando","mas","mais","ja","muito","apenas","tentei");
 private static final double THRESHOLD=.20, MIN_MARGIN=.035;
 private final List<Pattern> patterns=List.of(
  new Pattern("PAY","Falha no checkout","checkout","pagamento checkout compra pagar pix cartao pedido finalizar erro timeout demora travado recusa","Possível timeout na integração de pagamento; confirmar latência e resposta do provedor.","Em homologação, simular resposta lenta do provedor. Verificar mensagem de erro, ausência de cobrança duplicada e possibilidade de nova tentativa."),
  new Pattern("LINK","Rastreamento de links","tracking","creator link afiliado rastreamento clique cliques conversao venda atribuicao cupom campanha comissao contabilizada","Possível falha de atribuição de vendas ao link; verificar parâmetros e eventos de tracking.","Abrir link de creator em sessão limpa, concluir compra fictícia e conferir atribuição e comissão uma única vez."),
  new Pattern("ACCESS","Acesso a produto digital","access","acesso curso produto digital login senha entrar aula liberacao bloqueado comprado conteudo email","Possível atraso ou falha na liberação de acesso após confirmação da compra.","Confirmar pedido fictício, verificar evento de liberação, login e acesso ao conteúdo; repetir evento e verificar idempotência."));
 private final List<Report> reports=new ArrayList<>();
 private final List<Log> logs=new ArrayList<>();
 private int sequence=0;
 public IncidentEngine(){reset();}
 public synchronized Dashboard reset(){
  reports.clear();logs.clear();sequence=0;
  var now=Instant.now();
  seed("Comprador","Não consigo finalizar a compra, o checkout trava no pagamento",now.minusSeconds(240));
  seed("Comprador","Pix demora e aparece erro ao pagar o pedido",now.minusSeconds(180));
  seed("Comprador","Erro de timeout no pagamento com cartão",now.minusSeconds(120));
  seed("Creator","Meu link de afiliado recebe cliques mas a venda não foi atribuída",now.minusSeconds(160));
  seed("Creator","Conversão da campanha não aparece no rastreamento do link",now.minusSeconds(100));
  seed("Comprador","Comprei o curso digital e meu acesso ao conteúdo está bloqueado",now.minusSeconds(80));
  seed("Comprador","Gostaria de mudar a cor do aplicativo",now.minusSeconds(60));
  logs.add(new Log("L-01","checkout","PAYMENT_PROVIDER_TIMEOUT (simulado)",now.minusSeconds(220).toString()));
  logs.add(new Log("L-02","checkout","UPSTREAM_READ_TIMEOUT (simulado)",now.minusSeconds(130).toString()));
  logs.add(new Log("L-03","tracking","ATTRIBUTION_EVENT_MISSING (simulado)",now.minusSeconds(145).toString()));
  logs.add(new Log("L-04","access","ENTITLEMENT_EVENT_DELAYED (simulado)",now.minusSeconds(90).toString()));
  logs.add(new Log("L-05","checkout","TIMEOUT_ANTIGO (fora da janela de correlação)",now.minusSeconds(7200).toString()));
  return dashboard();
 }
 private void seed(String actor,String text,Instant time){reports.add(new Report("R-%03d".formatted(++sequence),actor,text,time.toString()));}
 public synchronized Dashboard add(String actor,String text){
  if(!Set.of("Comprador","Creator","Produtor","Equipe").contains(actor==null?"":actor))throw new IllegalArgumentException("Perfil inválido.");
  if(text==null||text.strip().length()<10||text.length()>500)throw new IllegalArgumentException("Informe um relato de 10 a 500 caracteres.");
  if(reports.size()>=200)throw new IllegalArgumentException("Limite da demonstração atingido. Reinicie os dados.");
  seed(actor,text.strip(),Instant.now());return dashboard();
 }
 private static List<String> tokens(String text){return Arrays.stream(Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
  .filter(w->w.length()>1&&!STOP.contains(w)).map(w->w.length()>4&&w.endsWith("s")?w.substring(0,w.length()-1):w).toList();}
 private static Map<String,Double> vector(List<String> words,Map<String,Double> idf){var v=new HashMap<String,Double>();for(String w:words)v.merge(w,idf.getOrDefault(w,0d),Double::sum);return v;}
 private static double cosine(Map<String,Double>a,Map<String,Double>b){double dot=0,aa=0,bb=0;for(var e:a.entrySet()){dot+=e.getValue()*b.getOrDefault(e.getKey(),0d);aa+=e.getValue()*e.getValue();}for(double x:b.values())bb+=x*x;return aa==0||bb==0?0:dot/Math.sqrt(aa*bb);}
 public synchronized Match classify(Report report){
  var docs=new ArrayList<List<String>>();patterns.forEach(p->docs.add(tokens(p.text())));docs.add(tokens(report.text()));
  var df=new HashMap<String,Integer>();for(var doc:docs)for(String w:new HashSet<>(doc))df.merge(w,1,Integer::sum);
  var idf=new HashMap<String,Double>();df.forEach((w,n)->idf.put(w,1+Math.log((docs.size()+1d)/(n+1d))));
  var q=vector(docs.getLast(),idf);double best=-1,second=-1;String id="";
  for(int i=0;i<patterns.size();i++){double score=cosine(q,vector(docs.get(i),idf));if(score>best){second=best;best=score;id=patterns.get(i).id();}else second=Math.max(second,score);}
  return new Match(report.id(),best>=THRESHOLD&&best-second>=MIN_MARGIN?id:"UNKNOWN",best,best-second);
 }
 public synchronized Dashboard dashboard(){
  var classified=new LinkedHashMap<String,List<Report>>();var scores=new HashMap<String,Double>();var unknown=new ArrayList<Report>();
  for(var r:reports){var m=classify(r);if(m.patternId().equals("UNKNOWN")){unknown.add(r);continue;}classified.computeIfAbsent(m.patternId(),x->new ArrayList<>()).add(r);scores.merge(m.patternId(),m.similarity(),Double::sum);}
  var incidents=new ArrayList<Incident>();
  for(var p:patterns){var rs=classified.get(p.id());if(rs==null)continue;
   var related=logs.stream().filter(l->l.service().equals(p.service())&&rs.stream().anyMatch(r->Math.abs(Duration.between(Instant.parse(l.time()),Instant.parse(r.time())).toSeconds())<=900)).toList();
   String severity=rs.size()>=3&&!related.isEmpty()?"Alta":rs.size()>=2?"Média":"Baixa";
   String hypothesis=related.isEmpty()?"Sem evidência técnica na janela de ±15 minutos. Investigar antes de sugerir causa.":p.hypothesis();
   incidents.add(new Incident(p.id(),p.title(),p.service(),severity,rs.size(),Math.round(1000*scores.get(p.id())/rs.size())/1000d,hypothesis,p.test(),List.copyOf(rs),related));
  }
  incidents.sort(Comparator.comparingInt(Incident::count).reversed());
  return new Dashboard(reports.size(),logs.size(),unknown.size(),List.copyOf(incidents),List.copyOf(unknown),List.copyOf(reports));
 }
}
