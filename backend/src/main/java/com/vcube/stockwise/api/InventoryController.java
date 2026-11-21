package com.vcube.stockwise.api;

import com.vcube.stockwise.domain.*;
import com.vcube.stockwise.repo.*;
import com.vcube.stockwise.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class InventoryController {
 private final InventoryService inventory;private final ProductRepository products;private final SupplierService supplierService;private final SupplierRepository suppliers;private final PurchaseOrderService orderService;private final PurchaseOrderRepository orders;private final StockTransactionRepository transactions;private final StockTransactionService transactionService;
 public InventoryController(InventoryService inventory,ProductRepository products,SupplierService supplierService,SupplierRepository suppliers,PurchaseOrderService orderService,PurchaseOrderRepository orders,StockTransactionRepository transactions,StockTransactionService transactionService){this.inventory=inventory;this.products=products;this.supplierService=supplierService;this.suppliers=suppliers;this.orderService=orderService;this.orders=orders;this.transactions=transactions;this.transactionService=transactionService;}

 @GetMapping("/products") public List<Product> products(@RequestParam(required=false)String search,@RequestParam(required=false)String category,@RequestParam(required=false)String sku,@RequestParam(required=false)String status){return inventory.all(search,category,sku,status);}
 @GetMapping("/products/{id}") public Product product(@PathVariable Long id){return products.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));}
 @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public Product create(@Valid @RequestBody Product p){return inventory.create(p);}
 @PutMapping("/products/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public Product update(@PathVariable Long id,@Valid @RequestBody Product p){return inventory.update(id,p);}
 @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void deleteProduct(@PathVariable Long id){inventory.delete(id);}
 @GetMapping("/alerts/low-stock") public List<Product> lowStock(){return inventory.lowStock();}

 @GetMapping("/suppliers") public List<Supplier> suppliers(){return supplierService.all();}
 @GetMapping("/suppliers/{id}") public Map<String,Object> supplier(@PathVariable Long id){return supplierService.detail(id);}
 @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public Supplier addSupplier(@Valid @RequestBody Supplier s){return supplierService.create(s);}
 @PutMapping("/suppliers/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public Supplier updateSupplier(@PathVariable Long id,@Valid @RequestBody Supplier s){return supplierService.update(id,s);}
 @DeleteMapping("/suppliers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void deleteSupplier(@PathVariable Long id){supplierService.delete(id);}

 @GetMapping("/purchase-orders") public List<PurchaseOrder> orders(){return orderService.all();}
 @PostMapping("/purchase-orders") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public PurchaseOrder addOrder(@Valid @RequestBody PurchaseOrderRequest body,@AuthenticationPrincipal AppUser user){return orderService.create(body,user);}
 @PatchMapping("/purchase-orders/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") public PurchaseOrder orderStatus(@PathVariable Long id,@RequestBody Map<String,String> body,@AuthenticationPrincipal AppUser user){return orderService.changeStatus(id,body.get("status"),user);}

 @GetMapping("/stock-transactions") public List<StockTransaction> transactions(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from){if(from==null)return transactions.findAll();return transactions.findByTransactionDateAfterOrderByTransactionDateAsc(from.atStartOfDay(ZoneOffset.UTC).toInstant());}
 @PostMapping("/stock-transactions") @ResponseStatus(HttpStatus.CREATED) public StockTransaction transact(@Valid @RequestBody TransactionRequest request,@AuthenticationPrincipal AppUser user){return transactionService.record(request,user);}

 @GetMapping("/reports/sales") public Map<String,Object> salesReport(){Instant since=Instant.now().minus(30,java.time.temporal.ChronoUnit.DAYS);var all=products.findAll();Map<Long,Long> soldByProduct=new HashMap<>();for(var tx:transactions.findByTypeAndTransactionDateAfter("STOCK_OUT",since))soldByProduct.merge(tx.getProductId(),(long)tx.getQuantity(),Long::sum);var rows=all.stream().map(p->{long units=soldByProduct.getOrDefault(p.getId(),0L);double salesValue=p.getUnitPrice().doubleValue()*units;return Map.<String,Object>of("productId",p.getId(),"name",p.getName(),"sku",p.getSku(),"category",p.getCategory(),"unitsSold",units,"currentStock",p.getCurrentStock(),"unitPrice",p.getUnitPrice(),"salesValue",Math.round(salesValue*100.0)/100.0);}).sorted(Comparator.comparingDouble(row->-((Number)row.get("salesValue")).doubleValue())).toList();long unitsSold=rows.stream().mapToLong(row->((Number)row.get("unitsSold")).longValue()).sum();double revenue=rows.stream().mapToDouble(row->((Number)row.get("salesValue")).doubleValue()).sum();return Map.of("periodDays",30,"unitsSold",unitsSold,"revenue",Math.round(revenue*100.0)/100.0,"products",rows);}
 @GetMapping("/reports/stock-movement") public Map<String,Object> movementReport(){Instant since=Instant.now().minus(30,java.time.temporal.ChronoUnit.DAYS);var rows=transactions.findByTransactionDateAfterOrderByTransactionDateAsc(since);Map<LocalDate,long[]> daily=new TreeMap<>();for(var tx:rows){LocalDate day=tx.getTransactionDate().atZone(ZoneOffset.UTC).toLocalDate();long[] counts=daily.computeIfAbsent(day,d->new long[2]);if(tx.getType().equals("STOCK_IN"))counts[0]+=tx.getQuantity();else if(tx.getType().equals("STOCK_OUT"))counts[1]+=tx.getQuantity();}var series=daily.entrySet().stream().map(e->Map.of("date",e.getKey().toString(),"stockIn",e.getValue()[0],"stockOut",e.getValue()[1])).toList();return Map.of("periodDays",30,"movement",series,"stockIn",rows.stream().filter(t->t.getType().equals("STOCK_IN")).mapToLong(StockTransaction::getQuantity).sum(),"stockOut",rows.stream().filter(t->t.getType().equals("STOCK_OUT")).mapToLong(StockTransaction::getQuantity).sum());}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){var all=products.findAll();var low=inventory.lowStock();double value=all.stream().mapToDouble(p->p.getCostPrice().doubleValue()*p.getCurrentStock()).sum();long pending=orders.countByStatusIn(List.of("PENDING","APPROVED","ORDERED"));Instant since=Instant.now().minus(30,java.time.temporal.ChronoUnit.DAYS);var movement=transactions.findByTransactionDateAfterOrderByTransactionDateAsc(since);long monthly=movement.stream().mapToLong(t->Math.abs((long)t.getQuantity())).sum();return Map.of("productCount",all.size(),"lowStockCount",low.size(),"inventoryValue",Math.round(value*100)/100.0,"pendingPurchaseOrders",pending,"monthlyStockMovement",monthly,"lowStock",low,"products",all);}
}
