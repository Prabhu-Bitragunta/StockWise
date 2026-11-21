package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name="purchase_orders",indexes=@Index(name="idx_purchase_order_status",columnList="status"))
public class PurchaseOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="po_number",nullable=false,unique=true,length=40,columnDefinition="varchar(40) default ''") private String poNumber;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="supplier_id") private Supplier supplier;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="created_by") private AppUser createdBy;
    @Column(nullable=false,length=20) private String status="PENDING";
    @Column(name="total_amount",nullable=false,precision=14,scale=2,columnDefinition="decimal(14,2) default 0") private BigDecimal totalAmount=BigDecimal.ZERO;
    @Column(name="expected_delivery_date") private LocalDate expectedDeliveryDate;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="supplier_name",length=160) private String legacySupplierName;
    @Column(name="line_count",nullable=false,columnDefinition="integer default 0") private int legacyLineCount;
    @OneToMany(mappedBy="purchaseOrder",cascade=CascadeType.ALL,orphanRemoval=true) private List<PurchaseOrderItem> items=new ArrayList<>();

    protected PurchaseOrder(){}
    public PurchaseOrder(String supplierName,int lineCount){this.legacySupplierName=supplierName;this.legacyLineCount=lineCount;}
    @PrePersist void beforeInsert(){if(poNumber==null||poNumber.isBlank())poNumber="PO-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();if(createdAt==null)createdAt=Instant.now();}
    public void addItem(PurchaseOrderItem item){items.add(item);item.setPurchaseOrder(this);}
    public Long getId(){return id;} public String getPoNumber(){return poNumber;} public Supplier getSupplier(){return supplier;} public String getSupplierName(){return supplier!=null?supplier.getName():legacySupplierName;} public AppUser getCreatedBy(){return createdBy;} public String getStatus(){return status;} public BigDecimal getTotalAmount(){return totalAmount;} public LocalDate getExpectedDeliveryDate(){return expectedDeliveryDate;} public Instant getCreatedAt(){return createdAt;} public int getLineCount(){return items.isEmpty()?legacyLineCount:items.size();} public List<PurchaseOrderItem> getItems(){return items;}
    public void setPoNumber(String v){poNumber=v;} public void setSupplier(Supplier v){supplier=v;legacySupplierName=v==null?null:v.getName();} public void setCreatedBy(AppUser v){createdBy=v;} public void setStatus(String v){status=v;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public void setExpectedDeliveryDate(LocalDate v){expectedDeliveryDate=v;} public void setLegacySupplierName(String v){legacySupplierName=v;}
}
