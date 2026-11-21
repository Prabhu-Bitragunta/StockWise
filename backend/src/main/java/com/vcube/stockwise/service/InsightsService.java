package com.vcube.stockwise.service;

import com.vcube.stockwise.domain.Product;
import com.vcube.stockwise.repo.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InsightsService {
 private static final Logger log=LoggerFactory.getLogger(InsightsService.class);
 private final ProductRepository products;
 private final ChatClient chatClient;
 private final boolean aiConfigured;
 public InsightsService(ProductRepository products,ObjectProvider<ChatModel> chatModelProvider,@Value("${spring.ai.openai.api-key:}") String apiKey){this.products=products;ChatModel chatModel=chatModelProvider.getIfAvailable();this.chatClient=chatModel==null?null:ChatClient.create(chatModel);this.aiConfigured=apiKey!=null&&!apiKey.isBlank()&&this.chatClient!=null;}
 public Map<String,Object> forecast(){var items=products.findAll().stream().map(p->{double daily=p.getSoldLast30Days()/30.0; int days=daily==0?999:(int)Math.floor(p.getQuantity()/daily); int target=(int)Math.ceil(daily*30); return Map.<String,Object>of("productId",p.getId(),"name",p.getName(),"onHand",p.getQuantity(),"dailyDemand",Math.round(daily*10)/10.0,"daysOfCover",days,"suggestedOrder",Math.max(0,target-p.getQuantity()),"risk",days<=7?"high":days<=14?"medium":"low");}).sorted(Comparator.comparingInt(a->(int)a.get("daysOfCover"))).toList();
  return Map.of("method","30-day moving average","confidence","Baseline estimate from recorded sales","items",items);}
 public Map<String,Object> assistant(String question){
  String q=question==null?"":question.toLowerCase(Locale.ROOT);
  var inventory=products.findAll();
  var low=inventory.stream().filter(p->p.getQuantity()<=p.getReorderPoint()).toList();
  String answer;
  if(q.contains("order")||q.contains("buy")||q.contains("purchase")) answer=low.isEmpty()?"Stock levels are healthy. No immediate purchase orders are recommended.":"Prioritize replenishment for "+low.stream().limit(3).map(Product::getName).reduce((a,b)->a+", "+b).orElse("")+". Suggested quantities are based on 30-day sales velocity.";
  else if(q.contains("risk")||q.contains("low")||q.contains("stock")) answer=low.size()+" products are at or below their reorder point: "+low.stream().map(Product::getName).limit(5).reduce((a,b)->a+", "+b).orElse("none")+".";
  else answer="Based on current stock and 30-day sales, "+low.size()+" items need attention. Try asking which products to reorder or what is at risk.";
  String source="Rule-based live-data fallback";
  if(aiConfigured)try{
   answer=chatClient.prompt().system(systemPrompt()).user("Inventory data:\n"+inventoryContext(inventory)+"\n\nQuestion: "+question).call().content();
   if(answer==null||answer.isBlank())throw new IllegalStateException("Spring AI returned an empty answer");
   source="Spring AI grounded in live inventory data";
  }catch(Exception failure){log.warn("Spring AI request failed ({}); using live-data fallback",failure.getClass().getSimpleName());}
  return Map.of("answer",answer,"source",source,"generatedAt",java.time.Instant.now().toString());
 }
 private String inventoryContext(List<Product> inventory){return inventory.stream().map(p->String.format(Locale.ROOT,"%s | sku %s | category %s | on hand %d | reorder point %d | sold last 30 days %d | unit cost %s | supplier %s",p.getName(),p.getSku(),p.getCategory(),p.getQuantity(),p.getReorderPoint(),p.getSoldLast30Days(),p.getUnitPrice(),p.getSupplierName())).reduce((a,b)->a+"\n"+b).orElse("No products recorded.");}
 private String systemPrompt(){return "You are Stockwise, an inventory analyst. Answer only from the supplied inventory rows. If the data cannot answer, say what is missing. For reorder advice, use the 30-day sales velocity and reorder point. Keep answers concise and identify suggested quantities as estimates.";}
}
