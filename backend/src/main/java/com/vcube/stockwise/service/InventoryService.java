package com.vcube.stockwise.service;

import com.vcube.stockwise.domain.Product;
import com.vcube.stockwise.domain.Supplier;
import com.vcube.stockwise.repo.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service @Transactional
public class InventoryService {
 private final ProductRepository products; private final SupplierRepository suppliers; private final StockTransactionRepository transactions; private final PurchaseOrderItemRepository orderItems;
 public InventoryService(ProductRepository products,SupplierRepository suppliers,StockTransactionRepository transactions,PurchaseOrderItemRepository orderItems){this.products=products;this.suppliers=suppliers;this.transactions=transactions;this.orderItems=orderItems;}
 public List<Product> all(String search){return all(search,null,null,null);}
 public List<Product> all(String search,String category,String sku,String status){return products.findAll().stream().filter(p->search==null||search.isBlank()||(p.getName()+" "+p.getSku()+" "+p.getCategory()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).filter(p->category==null||category.isBlank()||p.getCategory().equalsIgnoreCase(category)).filter(p->sku==null||sku.isBlank()||p.getSku().toLowerCase(Locale.ROOT).contains(sku.toLowerCase(Locale.ROOT))).filter(p->status==null||status.isBlank()||p.getStockStatus().equalsIgnoreCase(status)).toList();}
 public Product create(Product p){p.setSku(p.getSku().trim().toUpperCase(Locale.ROOT));if(products.existsBySkuIgnoreCase(p.getSku()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A product with this SKU already exists");applySupplier(p);return products.save(p);}
 public Product update(Long id,Product p){var current=products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));if(p.getSku()!=null&&!p.getSku().equalsIgnoreCase(current.getSku())&&products.existsBySkuIgnoreCase(p.getSku()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A product with this SKU already exists");current.setSku(p.getSku().trim().toUpperCase(Locale.ROOT));current.setName(p.getName());current.setDescription(p.getDescription());current.setCategory(p.getCategory());current.setQuantity(p.getQuantity());current.setReorderPoint(p.getReorderPoint());current.setOptimalStockLevel(p.getOptimalStockLevel());current.setUnitPrice(p.getUnitPrice());current.setCostPrice(p.getCostPrice());current.setSoldLast30Days(p.getSoldLast30Days());current.setUnit(p.getUnit());current.setSupplier(null);current.setSupplierName(p.getSupplierName());applySupplier(current);return current;}
 public void delete(Long id){Product p=products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));if(transactions.existsByProduct_Id(id)||orderItems.existsByProduct_Id(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Product has stock or purchase history and cannot be deleted");products.delete(p);}
 public List<Product> lowStock(){return products.findAll().stream().filter(p->p.getCurrentStock()<=p.getReorderLevel()).toList();}
 private void applySupplier(Product product){Supplier supplied=product.getSupplier();String name=product.getSupplierName();if(supplied!=null&&supplied.getId()!=null){product.setSupplier(suppliers.findById(supplied.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supplier not found")));return;}if(name!=null&&!name.isBlank())product.setSupplier(suppliers.findByNameIgnoreCase(name).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select an existing supplier")));}
}
