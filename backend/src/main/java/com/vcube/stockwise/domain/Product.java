package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="products",indexes={@Index(name="idx_product_category",columnList="category"),@Index(name="idx_product_supplier",columnList="supplier_id")})
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable=false,unique=true,length=80) private String sku;
    @NotBlank @Column(nullable=false,length=160) private String name;
    @Column(length=1000) private String description;
    @NotBlank @Column(nullable=false,length=100) private String category;
    @Column(name="unit_price",nullable=false,precision=12,scale=2,columnDefinition="decimal(12,2) default 0") @DecimalMin("0.0") private BigDecimal unitPrice=BigDecimal.ZERO;
    @Column(name="cost_price",nullable=false,precision=12,scale=2,columnDefinition="decimal(12,2) default 0") @DecimalMin("0.0") private BigDecimal costPrice=BigDecimal.ZERO;
    @Column(name="current_stock",nullable=false,columnDefinition="integer default 0") @Min(0) private int currentStock;
    @Column(name="quantity",nullable=false,columnDefinition="integer default 0") private int legacyQuantity;
    @Column(name="reorder_level",nullable=false,columnDefinition="integer default 0") @Min(0) private int reorderLevel;
    @Column(name="reorder_point",nullable=false,columnDefinition="integer default 0") private int legacyReorderPoint;
    @Column(name="optimal_stock_level",nullable=false,columnDefinition="integer default 0") @Min(0) private int optimalStockLevel;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="supplier_id") private Supplier supplier;
    @Column(name="supplier_name",length=160) private String legacySupplierName;
    @Column(name="sold_last_30_days",nullable=false,columnDefinition="bigint default 0") @Min(0) private long soldLast30Days;
    @Column(name="sold_last30days",nullable=false,columnDefinition="bigint default 0") private long legacySoldLast30Days;
    @Column(nullable=false,length=30) private String unit="pcs";
    @Column(name="created_at",nullable=false,updatable=false,columnDefinition="timestamp(6) with time zone default current_timestamp") private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false,columnDefinition="timestamp(6) with time zone default current_timestamp") private Instant updatedAt=Instant.now();
    protected Product(){}
    public Product(String sku,String name,String category,int quantity,int reorderPoint,BigDecimal unitPrice,String unit,long soldLast30Days,String supplierName){this.sku=sku;this.name=name;this.category=category;this.currentStock=quantity;this.reorderLevel=reorderPoint;this.optimalStockLevel=Math.max(reorderPoint,quantity);this.unitPrice=unitPrice;this.costPrice=unitPrice.multiply(new BigDecimal("0.62")).setScale(2,java.math.RoundingMode.HALF_UP);this.unit=unit;this.soldLast30Days=soldLast30Days;this.legacySupplierName=supplierName;}
    @PrePersist void mirrorLegacyColumns(){legacyQuantity=currentStock;legacyReorderPoint=reorderLevel;legacySoldLast30Days=soldLast30Days;}
    @PreUpdate void touch(){updatedAt=Instant.now();legacyQuantity=currentStock;legacyReorderPoint=reorderLevel;legacySoldLast30Days=soldLast30Days;}
    public Long getId(){return id;} public String getSku(){return sku;} public String getName(){return name;} public String getDescription(){return description;} public String getCategory(){return category;} public BigDecimal getUnitPrice(){return unitPrice;} public BigDecimal getCostPrice(){return costPrice;} public int getCurrentStock(){return currentStock;} public int getQuantity(){return currentStock;} public int getReorderLevel(){return reorderLevel;} public int getReorderPoint(){return reorderLevel;} public int getOptimalStockLevel(){return optimalStockLevel;} public Supplier getSupplier(){return supplier;} public String getSupplierName(){return supplier!=null?supplier.getName():legacySupplierName;} public long getSoldLast30Days(){return soldLast30Days;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public String getUnit(){return unit;} public String getStockStatus(){if(currentStock<=0)return "OUT_OF_STOCK";return currentStock<=reorderLevel?"LOW_STOCK":"IN_STOCK";} public String getStatus(){return getStockStatus();}
    public void setSku(String v){sku=v;} public void setName(String v){name=v;} public void setDescription(String v){description=v;} public void setCategory(String v){category=v;} public void setUnitPrice(BigDecimal v){unitPrice=v;} public void setCostPrice(BigDecimal v){costPrice=v;} public void setCurrentStock(int v){currentStock=v;} public void setQuantity(int v){currentStock=v;} public void setReorderLevel(int v){reorderLevel=v;} public void setReorderPoint(int v){reorderLevel=v;} public void setOptimalStockLevel(int v){optimalStockLevel=v;} public void setSupplier(Supplier v){supplier=v;legacySupplierName=v==null?null:v.getName();} public void setSupplierName(String v){legacySupplierName=v;} public void setSoldLast30Days(long v){soldLast30Days=v;} public void setUnit(String v){unit=v;}
}
