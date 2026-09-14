-- Create GRN (Goods Receipt Note) table
create table if not exists grn (
   grn_id        bigint primary key,
   received_date timestamp default null,
   po_id         bigint not null,
   supplier_id   bigint not null,
   total         decimal(12,2) default 0.00,
   grn_status    varchar(20) not null default 'PENDING',
   is_recieved   boolean not null default false,
   foreign key ( po_id )
      references purchase_orders ( po_id ),
   foreign key ( supplier_id )
      references suppliers ( supplier_id )
);

-- Create sequence for GRN
create sequence grn_seq start with 1 increment by 1;

-- Create trigger for auto-incrementing grn_id
create or replace trigger grn_trg before
   insert on grn
   for each row
begin
   select grn_seq.nextval
     into :new.grn_id
     from dual;
end;
/

-- Create GRN_ITEM table
create table if not exists grn_item (
   grn_item_id       bigint primary key,
   received_quantity decimal(12,3) not null default 0,
   uom               varchar(10) not null,
   price_per_unit    decimal(12,2) not null default 0,
   raw_material_id   bigint not null,
   grn_id            bigint not null,
   foreign key ( raw_material_id )
      references raw_materials ( id ),
   foreign key ( grn_id )
      references grn ( grn_id )
         on delete cascade,
   check ( received_quantity >= 0 ),
   check ( price_per_unit >= 0 )
);

-- Create indexes for GRN_ITEM
create index fk_grn_item_raw_material on
   grn_item (
      raw_material_id
   );
create index fk_grn_item_grn on
   grn_item (
      grn_id
   );

-- Create sequence for GRN_ITEM
create sequence grn_item_seq start with 1 increment by 1;

-- Create trigger for auto-incrementing grn_item_id
create or replace trigger grn_item_trg before
   insert on grn_item
   for each row
begin
   select grn_item_seq.nextval
     into :new.grn_item_id
     from dual;
end;
/