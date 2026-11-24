package com.vcube.stockwise.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="suppliers")
public class Supplier {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable=false) private String name;
    @Column(name="contact_person") private String contactPerson;
    @Email private String email;
    private String phone;
    @Column(length=500) private String address;
    @Column(name="lead_time_days",nullable=false) @Min(0) private int leadTimeDays=7;
    @Column(nullable=false) @Min(0) @Max(5) private double rating=4.0;
    @Column(nullable=false,length=30) private String status="ACTIVE";
    @Column(name="created_at",nullable=false,updatable=false,columnDefinition="timestamp(6) with time zone default current_timestamp") private Instant createdAt=Instant.now();
    @JsonIgnore @OneToMany(mappedBy="supplier") private List<Product> products=new ArrayList<>();
    @JsonIgnore @OneToMany(mappedBy="supplier") private List<PurchaseOrder> purchaseOrders=new ArrayList<>();
    protected Supplier(){}
    public Supplier(String name,String email,String phone){this.name=name;this.email=email;this.phone=phone;}
    public Supplier(String name,String contactPerson,String email,String phone,String address,int leadTimeDays,double rating){this.name=name;this.contactPerson=contactPerson;this.email=email;this.phone=phone;this.address=address;this.leadTimeDays=leadTimeDays;this.rating=rating;}
    public Long getId(){return id;} public String getName(){return name;} public String getContactPerson(){return contactPerson;} public String getEmail(){return email;} public String getPhone(){return phone;} public String getAddress(){return address;} public int getLeadTimeDays(){return leadTimeDays;} public double getRating(){return rating;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public List<Product> getProducts(){return products;} public List<PurchaseOrder> getPurchaseOrders(){return purchaseOrders;}
    public void setName(String v){name=v;} public void setContactPerson(String v){contactPerson=v;} public void setEmail(String v){email=v;} public void setPhone(String v){phone=v;} public void setAddress(String v){address=v;} public void setLeadTimeDays(int v){leadTimeDays=v;} public void setRating(double v){rating=v;}
}
