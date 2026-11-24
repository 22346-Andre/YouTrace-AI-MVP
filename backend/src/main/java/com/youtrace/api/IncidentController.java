package com.youtrace.api;
import com.youtrace.core.IncidentEngine;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class IncidentController {
 private final IncidentEngine engine=new IncidentEngine();
 @GetMapping("/dashboard") public IncidentEngine.Dashboard dashboard(){return engine.dashboard();}
 @PostMapping(value="/reports",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public IncidentEngine.Dashboard add(@RequestParam("actor") String actor,@RequestParam("text") String text){return engine.add(actor,text);}
 @PostMapping("/reset") public IncidentEngine.Dashboard reset(){return engine.reset();}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
}
