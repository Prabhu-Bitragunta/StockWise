package com.vcube.stockwise.api;

import com.vcube.stockwise.service.AiService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/ai") @Validated
public class AiController {
 private final AiService ai;
 public AiController(AiService ai){this.ai=ai;}
 @GetMapping("/forecast/{productId}") public Map<String,Object> forecast(@PathVariable Long productId){return ai.forecast(productId);}
 @PostMapping("/optimize") public Map<String,Object> optimize(){return ai.optimize();}
 @PostMapping("/purchase-recommendations") public Map<String,Object> purchaseRecommendations(){return ai.purchaseRecommendations();}
 @PostMapping("/chat") public Map<String,Object> chat(@RequestBody Map<String,String> body){String question=body.get("question");if(question==null||question.isBlank())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Question is required");return ai.chat(question);}
}
