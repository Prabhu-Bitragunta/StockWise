package com.vcube.stockwise.service;

import com.vcube.stockwise.domain.Supplier;
import com.vcube.stockwise.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service public class SupplierService {
 private final SupplierRepository suppliers;private final ProductRepository products;private final PurchaseOrderRepository orders;
 public SupplierService(SupplierRepository suppliers,ProductRepository products,PurchaseOrderRepository orders){this.suppliers=suppliers;this.products=products;this.orders=orders;}
 @Transactional(readOnly=true) public List<Supplier> all(){return suppliers.findAll();}
 @Transactional(readOnly=true) public Supplier get(Long id){return suppliers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Supplier not found"));}
 @Transactional public Supplier create(Supplier supplier){if(supplier.getName()==null||supplier.getName().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supplier name is required");if(suppliers.existsByNameIgnoreCase(supplier.getName().trim()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A supplier with this name already exists");supplier.setName(supplier.getName().trim());return suppliers.save(supplier);}
 @Transactional public Supplier update(Long id,Supplier changes){Supplier current=get(id);if(changes.getName()==null||changes.getName().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supplier name is required");current.setName(changes.getName().trim());current.setContactPerson(changes.getContactPerson());current.setEmail(changes.getEmail());current.setPhone(changes.getPhone());current.setAddress(changes.getAddress());current.setLeadTimeDays(changes.getLeadTimeDays());current.setRating(changes.getRating());return current;}
 @Transactional public void delete(Long id){Supplier supplier=get(id);if(products.existsBySupplier_Id(id)||!orders.findBySupplier_Id(id).isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Supplier is linked to products or purchase orders");suppliers.delete(supplier);}
 @Transactional(readOnly=true) public Map<String,Object> detail(Long id){Supplier supplier=get(id);var supplied=products.findAll().stream().filter(p->p.getSupplier()!=null&&p.getSupplier().getId().equals(id)).toList();var active=orders.findBySupplier_Id(id).stream().filter(o->Set.of("PENDING","APPROVED","ORDERED").contains(o.getStatus())).toList();return Map.of("supplier",supplier,"products",supplied,"activePurchaseOrders",active);}
}
