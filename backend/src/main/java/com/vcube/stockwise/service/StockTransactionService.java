package com.vcube.stockwise.service;

import com.vcube.stockwise.api.TransactionRequest;
import com.vcube.stockwise.domain.AppUser;
import com.vcube.stockwise.domain.StockTransaction;
import com.vcube.stockwise.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service public class StockTransactionService {
 private final ProductRepository products;private final StockTransactionRepository transactions;private final AppUserRepository users;
 public StockTransactionService(ProductRepository products,StockTransactionRepository transactions,AppUserRepository users){this.products=products;this.transactions=transactions;this.users=users;}
 @Transactional public StockTransaction record(TransactionRequest request,AppUser user){
  var product=products.findById(request.productId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));String type=request.type().trim().toUpperCase(Locale.ROOT);int amount=request.quantity();
  if(type.equals("IN"))type="STOCK_IN";if(type.equals("OUT"))type="STOCK_OUT";
  if(!type.equals("STOCK_IN")&&!type.equals("STOCK_OUT")&&!type.equals("ADJUSTMENT"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Type must be STOCK_IN, STOCK_OUT, or ADJUSTMENT");
  if(amount==0||(type.equals("STOCK_IN")||type.equals("STOCK_OUT"))&&amount<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Stock in/out quantities must be positive; adjustment is a signed change");
  long next=type.equals("STOCK_IN")?(long)product.getCurrentStock()+amount:type.equals("STOCK_OUT")?(long)product.getCurrentStock()-amount:(long)product.getCurrentStock()+amount;
  if(next<0||next>Integer.MAX_VALUE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Movement would make stock negative or exceed its limit");product.setCurrentStock((int)next);
  AppUser recordedBy=user==null?null:users.findById(user.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Recording user no longer exists"));
  return transactions.save(new StockTransaction(product,recordedBy,type,amount,request.referenceNote()));
 }
}
