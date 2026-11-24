package com.youtrace.api;
import com.youtrace.core.IncidentEngine;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class IncidentController {
 private final IncidentEngine engine=new IncidentEngine();
 private final AiDiagnosisService ai;
 public IncidentController(AiDiagnosisService ai){this.ai=ai;}
 @PostMapping("/incidents/{id}/diagnosis") public AiDiagnosisService.Diagnosis diagnosis(@PathVariable("id") String id){
  var incident=engine.dashboard().incidents().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Incidente não encontrado."));
  return ai.analyze(incident);
 }
 @GetMapping("/dashboard") public IncidentEngine.Dashboard dashboard(){return engine.dashboard();}
 @PostMapping(value="/reports",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public IncidentEngine.Dashboard add(@RequestParam("actor") String actor,@RequestParam("text") String text){return engine.add(actor,text);}
 @PostMapping("/reset") public IncidentEngine.Dashboard reset(){return engine.reset();}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
}
