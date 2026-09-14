-- Create Sales Tables for POS Module
-- This migration creates the sales, sale_items, and discounts tables

-- Create discounts table (optional for future use)
create table discounts (
   discount_id         number
      generated always as identity
   primary key,
   name                varchar(100) not null,
   description         varchar(255),
   discount_percentage decimal(5,2),
   discount_amount     decimal(10,2),
   is_active           boolean not null default true
);

-- Create sales table
create table sales (
   sale_id      number
      generated always as identity
   primary key,
   sale_date    date not null,
   sale_time    time not null,
   cashier_id   raw(16) not null,
   total_amount decimal(10,2) not null,
   constraint fk_sales_cashier foreign key ( cashier_id )
      references bmsauth ( id )
);

-- Create sale_items table
create table sale_items (
   sale_item_id           number
      generated always as identity
   primary key,
   day_production_item_id int not null,
   qty                    int not null,
   price                  decimal(10,2) not null,
   free_meal_reason       varchar(255) null,
   bank_transfer_code     varchar(100) null,
   discount_id            int null,
   payment_method_id      int not null,
   sale_id                int not null,
   cashier_id             raw(16) not null,
   constraint fk_si_day_production_item foreign key ( day_production_item_id )
      references day_production_items ( day_production_item_id ),
   constraint fk_si_discount foreign key ( discount_id )
      references discounts ( discount_id ),
   constraint fk_si_payment_method foreign key ( payment_method_id )
      references payment_methods ( payment_method_id ),
   constraint fk_si_sale foreign key ( sale_id )
      references sales ( sale_id ),
   constraint fk_si_cashier foreign key ( cashier_id )
      references bmsauth ( id )
);

-- Insert some sample discounts
insert into discounts (
   name,
   description,
   discount_percentage,
   is_active
) values ( 'Staff Discount',
           '10% discount for staff members',
           10.00,
           true ),( 'Student Discount',
                    '5% discount for students',
                    5.00,
                    true ),( 'Senior Citizen',
                             '15% discount for senior citizens',
                             15.00,
                             true ),( 'Bulk Purchase',
                                      '20% discount for bulk purchases',
                                      20.00,
                                      true ),( 'Special Promotion',
                                               'Limited time special promotion',
                                               25.00,
                                               false );