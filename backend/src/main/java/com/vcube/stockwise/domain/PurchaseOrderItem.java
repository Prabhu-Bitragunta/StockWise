package com.vcube.stockwise.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Entity @Table(name="purchase_order_items",indexes=@Index(name="idx_po_item_product",columnList="product_id"))
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="purchase_order_id",nullable=false) private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
    @Min(1) @Column(nullable=false) private int quantity;
    @Column(name="unit_cost",nullable=false,precision=12,scale=2) private BigDecimal unitCost;
    protected PurchaseOrderItem(){}
    public PurchaseOrderItem(Product product,int quantity,BigDecimal unitCost){this.product=product;this.quantity=quantity;this.unitCost=unitCost;}
    public Long getId(){return id;} public PurchaseOrder getPurchaseOrder(){return purchaseOrder;} public Product getProduct(){return product;} public int getQuantity(){return quantity;} public BigDecimal getUnitCost(){return unitCost;}
    public void setPurchaseOrder(PurchaseOrder v){purchaseOrder=v;}
}
