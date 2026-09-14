-- Create purchase_orders table
create table if not exists purchase_orders (
   po_id                   bigint primary key,
   total_cost              decimal(12,2) not null,
   number_of_items         int not null,
   estimated_delivery_date date not null,
   supplier_id             bigint not null
);

-- Create sequence for purchase_orders
create sequence purchase_orders_seq start with 1 increment by 1;

-- Create trigger for auto-incrementing po_id
create or replace trigger purchase_orders_trg before
   insert on purchase_orders
   for each row
begin
   select purchase_orders_seq.nextval
     into :new.po_id
     from dual;
end;
/

-- Create purchase_order_items table
create table if not exists purchase_order_items (
   poi_id          bigint primary key,
   raw_material_id int not null,
   required_qty    int not null,
   received_qty    int default null,
   actual_cost     decimal(12,2) default null,
   estimated_cost  decimal(12,2) not null,
   unit_of_measure varchar(50) default null,
   po_id           bigint not null,
   foreign key ( raw_material_id )
      references raw_materials ( id ),
   foreign key ( po_id )
      references purchase_orders ( po_id )
);

-- Create indexes for purchase_order_items
create index fk_purchase_order_items_raw_material on
   purchase_order_items (
      raw_material_id
   );
create index fk_purchase_order_items_po on
   purchase_order_items (
      po_id
   );

-- Create sequence for purchase_order_items
create sequence purchase_order_items_seq start with 1 increment by 1;

-- Create trigger for auto-incrementing poi_id
create or replace trigger purchase_order_items_trg before
   insert on purchase_order_items
   for each row
begin
   select purchase_order_items_seq.nextval
     into :new.poi_id
     from dual;
end;
/

-- Insert sample purchase orders
insert into purchase_orders (
   total_cost,
   number_of_items,
   estimated_delivery_date,
   supplier_id
) values ( 1250.00,
           3,
           '2025-09-15',
           1 ),( 875.50,
                 2,
                 '2025-09-20',
                 2 ),( 2100.75,
                       4,
                       '2025-09-25',
                       1 );

-- Insert sample purchase order items
-- Note: These raw_material_id values should match existing raw materials
insert into purchase_order_items (
   raw_material_id,
   required_qty,
   received_qty,
   actual_cost,
   estimated_cost,
   unit_of_measure,
   po_id
) values ( 1,
           100,
           0,
           null,
           250.00,
           'kg',
           1 ),( 2,
                 50,
                 25,
                 120.00,
                 125.00,
                 'kg',
                 1 ),( 3,
                       200,
                       0,
                       null,
                       875.00,
                       'liter',
                       1 ),( 4,
                             75,
                             0,
                             null,
                             450.00,
                             'kg',
                             2 ),( 5,
                                   30,
                                   0,
                                   null,
                                   425.50,
                                   'kg',
                                   2 ),( 1,
                                         150,
                                         0,
                                         null,
                                         375.00,
                                         'kg',
                                         3 ),( 6,
                                               80,
                                               0,
                                               null,
                                               320.75,
                                               'pieces',
                                               3 ),( 7,
                                                     25,
                                                     0,
                                                     null,
                                                     780.00,
                                                     'kg',
                                                     3 ),( 8,
                                                           60,
                                                           0,
                                                           null,
                                                           625.00,
                                                           'liter',
                                                           3 );