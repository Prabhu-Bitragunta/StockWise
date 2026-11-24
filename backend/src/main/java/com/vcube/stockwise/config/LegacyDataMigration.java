package com.vcube.stockwise.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class LegacyDataMigration {
    @Bean @Order(0) ApplicationRunner migrateLegacyColumns(JdbcTemplate jdbc,DataSource dataSource){
        return args->{
            ensureColumn(dataSource,jdbc,"users","email_verified","BOOLEAN NOT NULL DEFAULT TRUE");
            ensureColumn(dataSource,jdbc,"suppliers","created_at","TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(dataSource,jdbc,"products","created_at","TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(dataSource,jdbc,"products","updated_at","TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(dataSource,jdbc,"stock_transactions","transaction_date","TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(dataSource,jdbc,"stock_transactions","reference_note","VARCHAR(500) NULL");
            if(hasColumn(dataSource,"products","quantity"))jdbc.update("UPDATE products SET current_stock=quantity WHERE current_stock=0 AND quantity>0");
            if(hasColumn(dataSource,"products","reorder_point"))jdbc.update("UPDATE products SET reorder_level=reorder_point WHERE reorder_level=0 AND reorder_point>0");
            if(hasColumn(dataSource,"products","sold_last30days"))jdbc.update("UPDATE products SET sold_last_30_days=sold_last30days WHERE sold_last_30_days=0 AND sold_last30days>0");
            if(hasColumn(dataSource,"products","cost_price")&&hasColumn(dataSource,"products","unit_price"))jdbc.update("UPDATE products SET cost_price=ROUND(unit_price*0.62,2) WHERE cost_price=0 AND unit_price>0");
            if(hasColumn(dataSource,"products","optimal_stock_level"))jdbc.update("UPDATE products SET optimal_stock_level=GREATEST(optimal_stock_level,reorder_level,current_stock)");
            if(hasColumn(dataSource,"products","supplier_name")&&hasColumn(dataSource,"products","supplier_id"))jdbc.update("UPDATE products p SET supplier_id=s.id FROM suppliers s WHERE s.name=p.supplier_name AND p.supplier_id IS NULL");
            if(hasColumn(dataSource,"purchase_orders","po_number"))jdbc.update("UPDATE purchase_orders SET po_number=CONCAT('PO-',LPAD(CAST(id AS VARCHAR),6,'0')) WHERE po_number IS NULL OR po_number=''");
            if(hasColumn(dataSource,"purchase_orders","supplier_name")&&hasColumn(dataSource,"purchase_orders","supplier_id"))jdbc.update("UPDATE purchase_orders p SET supplier_id=s.id FROM suppliers s WHERE s.name=p.supplier_name AND p.supplier_id IS NULL");
            if(hasColumn(dataSource,"purchase_orders","status"))jdbc.update("UPDATE purchase_orders SET status=CASE LOWER(status) WHEN 'draft' THEN 'PENDING' WHEN 'ordered' THEN 'ORDERED' WHEN 'received' THEN 'RECEIVED' WHEN 'cancelled' THEN 'CANCELLED' WHEN 'approved' THEN 'APPROVED' ELSE status END");
            if(hasColumn(dataSource,"stock_transactions","type"))jdbc.update("UPDATE stock_transactions SET type=CASE UPPER(type) WHEN 'IN' THEN 'STOCK_IN' WHEN 'OUT' THEN 'STOCK_OUT' ELSE UPPER(type) END");
            if(hasColumn(dataSource,"stock_transactions","created_at"))jdbc.update("UPDATE stock_transactions SET transaction_date=created_at");
            if(hasColumn(dataSource,"stock_transactions","note"))jdbc.update("UPDATE stock_transactions SET reference_note=note WHERE reference_note IS NULL");
        };
    }
    private void ensureColumn(DataSource source,JdbcTemplate jdbc,String table,String column,String definition)throws SQLException{if(!hasColumn(source,table,column))jdbc.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    private boolean hasColumn(DataSource source,String table,String column)throws SQLException{
        try(var connection=source.getConnection();var columns=connection.getMetaData().getColumns(connection.getCatalog(),connection.getSchema(),table,column)){return columns.next();}
    }
}
