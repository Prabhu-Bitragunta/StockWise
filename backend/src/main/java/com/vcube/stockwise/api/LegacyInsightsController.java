package com.vcube.stockwise.api;
import com.vcube.stockwise.service.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/insights") public class LegacyInsightsController {
 private final AiService ai;public LegacyInsightsController(AiService ai){this.ai=ai;}
 @PostMapping("/assistant") public Map<String,Object> assistant(@RequestBody Map<String,String> body){return ai.chat(body.get("question"));}
 @GetMapping("/forecast") public Map<String,Object> forecast(){return ai.optimize();}
}
