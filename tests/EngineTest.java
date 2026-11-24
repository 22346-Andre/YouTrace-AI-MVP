import com.youtrace.core.IncidentEngine;
public class EngineTest {
 static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);System.out.println("PASS: "+name);}
 public static void main(String[] args){
  var e=new IncidentEngine();var d=e.dashboard();
  check(d.reportCount()==7&&d.incidents().size()==3&&d.unresolved()==1,"dados iniciais");
  var pay=d.incidents().stream().filter(i->i.id().equals("PAY")).findFirst().orElseThrow();
  check(pay.count()==3&&pay.logs().size()==2&&pay.severity().equals("Alta"),"checkout e severidade");
  check(pay.logs().stream().noneMatch(l->l.id().equals("L-05")),"exclusao de log antigo");
  d=e.add("Comprador","Erro de timeout no pagamento, não consigo finalizar a compra no checkout.");
  check(d.reportCount()==8&&d.incidents().getFirst().count()==4,"novo relato checkout");
  d=e.add("Creator","A venda da campanha não aparece no rastreamento do link de afiliado.");
  check(d.incidents().stream().anyMatch(i->i.id().equals("LINK")&&i.count()==3),"novo relato tracking");
  d=e.add("Comprador","Gostaria de mudar a cor do aplicativo para azul.");check(d.unresolved()==2,"fora do escopo");
  boolean invalid=false;try{e.add("Comprador","oi");}catch(IllegalArgumentException ex){invalid=true;}check(invalid,"validacao de entrada");
  check(e.reset().reportCount()==7,"restauracao");
 }
}
