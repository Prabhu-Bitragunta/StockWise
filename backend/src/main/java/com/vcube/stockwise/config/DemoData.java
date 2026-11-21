package com.vcube.stockwise.config;

import com.vcube.stockwise.domain.*;
import com.vcube.stockwise.repo.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Configuration public class DemoData {
 @Bean @Order(10) ApplicationRunner seed(AppUserRepository users,PasswordEncoder passwords,SupplierRepository suppliers,ProductRepository products,StockTransactionRepository transactions,PurchaseOrderRepository orders,PurchaseOrderItemRepository orderItems){
  return args->{
   AppUser admin=bootstrapAdmin(users,passwords);
   seedSuppliers(suppliers);
   if(products.count()==0)seedBaseProducts(products,suppliers);
   else if(products.count()<15)seedAdditionalProducts(products,suppliers);
   List<Product> inventory=products.findAll();
   if(transactions.count()==0)seedHistory(inventory,transactions,admin);
   if(orders.count()==0&&inventory.size()>=2)seedOrders(orders,suppliers,inventory,orderItems,admin);
  };
 }
 private AppUser bootstrapAdmin(AppUserRepository users,PasswordEncoder encoder){String email=System.getenv("BOOTSTRAP_ADMIN_EMAIL"),password=System.getenv("BOOTSTRAP_ADMIN_PASSWORD");if(email==null||email.isBlank()||password==null||password.length()<12)return users.findAll().stream().findFirst().orElse(null);AppUser admin=users.findByEmailIgnoreCase(email).orElseGet(()->users.save(new AppUser(email.trim().toLowerCase(Locale.ROOT),"Workspace Admin",encoder.encode(password))));admin.setRole("ADMIN");return users.save(admin);}
 private void seedSuppliers(SupplierRepository repo){Object[][] data={{"Nova Supply Co.","Maya Patel","orders@novasupply.example","+1 (555) 012-0142","24 Market Street",8,4.8},{"Paper & Pine","Elliot Reed","hello@paperpine.example","+1 (555) 013-0921","8 Stationery Lane",5,4.7},{"Hearth Goods","Priya Shah","team@hearthgoods.example","+1 (555) 018-7440","17 Homeware Road",12,4.5},{"Metro Tech Distribution","Noah Kim","sales@metrotech.example","+1 (555) 018-3310","52 Commerce Park",6,4.6},{"Evergreen Packaging","Ava Wilson","orders@evergreenpack.example","+1 (555) 019-5250","3 Industrial Way",10,4.3}};for(Object[] d:data){Supplier s=repo.findByNameIgnoreCase((String)d[0]).orElseGet(()->new Supplier((String)d[0],(String)d[1],(String)d[2],(String)d[3],(String)d[4],(Integer)d[5],(Double)d[6]));s.setContactPerson((String)d[1]);s.setEmail((String)d[2]);s.setPhone((String)d[3]);s.setAddress((String)d[4]);s.setLeadTimeDays((Integer)d[5]);s.setRating((Double)d[6]);repo.save(s);}}
 private void seedBaseProducts(ProductRepository repo,SupplierRepository suppliers){String[][] rows={{"ELE-001","Wireless Headphones","Electronics","18","20","79.99","40.00","84","Nova Supply Co."},{"ELE-002","Mechanical Keyboard","Electronics","42","15","119.00","68.00","36","Nova Supply Co."},{"OFF-014","A5 Hardcover Notebook","Stationery","9","25","8.50","3.10","112","Paper & Pine"},{"HOM-008","Ceramic Pour-over Set","Home","31","12","34.00","16.00","19","Hearth Goods"},{"OFF-021","Gel Pen Set · 12 pack","Stationery","7","18","12.00","4.00","65","Paper & Pine"},{"ELE-017","USB-C Charging Hub","Electronics","56","20","49.95","22.00","44","Nova Supply Co."},{"HOM-012","Linen Desk Organizer","Home","24","10","28.00","12.00","21","Hearth Goods"},{"ELE-023","Portable SSD · 1 TB","Electronics","11","14","99.00","61.00","58","Nova Supply Co."}};for(String[] row:rows)repo.save(product(row,suppliers));seedAdditionalProducts(repo,suppliers);}
 private void seedAdditionalProducts(ProductRepository repo,SupplierRepository suppliers){String[][] rows={{"ELE-031","1080p Webcam","Electronics","34","12","59.00","28.00","32","Metro Tech Distribution"},{"ELE-032","Bluetooth Speaker","Electronics","22","10","45.00","21.00","27","Metro Tech Distribution"},{"OFF-032","Recycled Paper Ream","Stationery","63","20","7.50","2.40","53","Paper & Pine"},{"HOM-020","Insulated Travel Mug","Home","15","12","24.00","10.00","41","Hearth Goods"},{"PKG-003","Shipping Box · Medium","Packaging","120","40","1.80","0.55","180","Evergreen Packaging"},{"PKG-007","Bubble Mailer · 10 pack","Packaging","38","18","6.50","2.00","79","Evergreen Packaging"},{"ELE-042","Ergonomic Mouse","Electronics","5","16","39.00","17.00","68","Nova Supply Co."}};for(String[] row:rows)if(!repo.existsBySkuIgnoreCase(row[0]))repo.save(product(row,suppliers));}
 private Product product(String[] row,SupplierRepository suppliers){Product p=new Product(row[0],row[1],row[2],Integer.parseInt(row[3]),Integer.parseInt(row[4]),new BigDecimal(row[5]),"pcs",Long.parseLong(row[7]),row[8]);p.setDescription(row[1]+" from the "+row[2]+" catalog.");p.setCostPrice(new BigDecimal(row[6]));p.setOptimalStockLevel(Math.max(p.getReorderLevel()*2,p.getCurrentStock()));p.setSupplier(suppliers.findByNameIgnoreCase(row[8]).orElse(null));return p;}
 private void seedHistory(List<Product> products,StockTransactionRepository repo,AppUser actor){Instant now=Instant.now();for(Product product:products){long units=product.getSoldLast30Days();if(units>0){int first=(int)(units/2),second=(int)(units-first);repo.save(new StockTransaction(product,actor,"STOCK_OUT",Math.max(1,first),"Historical sales · first half",now.minus(18,ChronoUnit.DAYS)));repo.save(new StockTransaction(product,actor,"STOCK_OUT",Math.max(1,second),"Historical sales · second half",now.minus(3,ChronoUnit.DAYS)));}repo.save(new StockTransaction(product,actor,"STOCK_IN",Math.max(1,product.getReorderLevel()),"Historical supplier replenishment",now.minus(25,ChronoUnit.DAYS)));}}
 private void seedOrders(PurchaseOrderRepository repo,SupplierRepository suppliers,List<Product> products,PurchaseOrderItemRepository items,AppUser admin){if(admin==null)return;for(int i=0;i<3;i++){Product product=products.get(i);Supplier supplier=product.getSupplier();if(supplier==null)supplier=suppliers.findAll().get(0);PurchaseOrder order=new PurchaseOrder(supplier.getName(),1);order.setSupplier(supplier);order.setCreatedBy(admin);order.setPoNumber("PO-DEMO-"+(i+1));order.setStatus(i==0?"PENDING":i==1?"APPROVED":"ORDERED");order.setExpectedDeliveryDate(java.time.LocalDate.now().plusDays(supplier.getLeadTimeDays()));BigDecimal unitCost=product.getCostPrice();order.setTotalAmount(unitCost.multiply(BigDecimal.valueOf(10+i*5)));order.addItem(new PurchaseOrderItem(product,10+i*5,unitCost));repo.save(order);}}
}
