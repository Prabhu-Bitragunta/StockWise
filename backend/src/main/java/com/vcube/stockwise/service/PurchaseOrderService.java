package com.vcube.stockwise.service;

import com.vcube.stockwise.api.PurchaseOrderRequest;
import com.vcube.stockwise.domain.*;
import com.vcube.stockwise.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@Service public class PurchaseOrderService {
 private final PurchaseOrderRepository orders;private final SupplierRepository suppliers;private final ProductRepository products;private final StockTransactionRepository transactions;private final AppUserRepository users;
 public PurchaseOrderService(PurchaseOrderRepository orders,SupplierRepository suppliers,ProductRepository products,StockTransactionRepository transactions,AppUserRepository users){this.orders=orders;this.suppliers=suppliers;this.products=products;this.transactions=transactions;this.users=users;}
 @Transactional public PurchaseOrder create(PurchaseOrderRequest request,AppUser creator){
  Supplier supplier=suppliers.findById(request.supplierId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Supplier not found"));PurchaseOrder order=new PurchaseOrder(supplier.getName(),request.items().size());order.setSupplier(supplier);order.setCreatedBy(managedActor(creator));order.setExpectedDeliveryDate(request.expectedDeliveryDate());order.setStatus("PENDING");BigDecimal total=BigDecimal.ZERO;
  for(var line:request.items()){Product product=products.findById(line.productId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found: "+line.productId()));BigDecimal cost=line.unitCost()==null?product.getCostPrice():line.unitCost();order.addItem(new PurchaseOrderItem(product,line.quantity(),cost));total=total.add(cost.multiply(BigDecimal.valueOf(line.quantity())));}
  order.setTotalAmount(total);return orders.save(order);
 }
 @Transactional public PurchaseOrder changeStatus(Long id,String requested,AppUser actor){
  PurchaseOrder order=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Purchase order not found"));String next=requested==null?"":requested.trim().toUpperCase(Locale.ROOT);String current=order.getStatus().toUpperCase(Locale.ROOT);
  boolean allowed=switch(current){case "PENDING"->Set.of("APPROVED","CANCELLED").contains(next);case "APPROVED"->Set.of("ORDERED","RECEIVED","CANCELLED").contains(next);case "ORDERED"->Set.of("RECEIVED","CANCELLED").contains(next);default->false;};
  if(!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid purchase order transition from "+current+" to "+next);
  if(next.equals("RECEIVED")){AppUser receivedBy=managedActor(actor);for(PurchaseOrderItem line:order.getItems()){
   Product product=products.findById(line.getProduct().getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product in order no longer exists"));long updated=(long)product.getCurrentStock()+line.getQuantity();if(updated>Integer.MAX_VALUE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Receiving this order exceeds the stock limit");product.setCurrentStock((int)updated);transactions.save(new StockTransaction(product,receivedBy,"STOCK_IN",line.getQuantity(),"Received "+order.getPoNumber()));
  }}
  order.setStatus(next);return orders.save(order);
 }
 @Transactional(readOnly=true) public List<PurchaseOrder> all(){return orders.findAll();}
 private AppUser managedActor(AppUser actor){return actor==null?null:users.findById(actor.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Acting user no longer exists"));}
}
