package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="stock_transactions",indexes={@Index(name="idx_stock_product_date",columnList="product_id,transaction_date"),@Index(name="idx_stock_type_date",columnList="type,transaction_date")})
public class StockTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id") private AppUser user;
    @Column(nullable=false,length=20) private String type;
    @Column(nullable=false) private int quantity;
    @Column(name="reference_note",length=500) private String referenceNote;
    @Column(name="transaction_date",nullable=false,columnDefinition="timestamp(6) with time zone default current_timestamp") private Instant transactionDate=Instant.now();
    @Column(name="productName",length=160) private String legacyProductName;
    @Column(name="note",length=500) private String legacyNote;
    @Column(name="created_at",nullable=false) private Instant legacyCreatedAt=Instant.now();

    protected StockTransaction(){}
    public StockTransaction(Product product,AppUser user,String type,int quantity,String referenceNote){this(product,user,type,quantity,referenceNote,Instant.now());}
    public StockTransaction(Product product,AppUser user,String type,int quantity,String referenceNote,Instant transactionDate){this.product=product;this.user=user;this.type=type;this.quantity=quantity;this.referenceNote=referenceNote;this.transactionDate=transactionDate;this.legacyCreatedAt=transactionDate;this.legacyProductName=product.getName();this.legacyNote=referenceNote;}
    public Long getId(){return id;} public Product getProduct(){return product;} public Long getProductId(){return product==null?null:product.getId();} public String getProductName(){return product==null?legacyProductName:product.getName();} public AppUser getUser(){return user;} public String getType(){return type;} public int getQuantity(){return quantity;} public String getReferenceNote(){return referenceNote==null?legacyNote:referenceNote;} public String getNote(){return getReferenceNote();} public Instant getTransactionDate(){return transactionDate;} public Instant getCreatedAt(){return transactionDate;}
}
