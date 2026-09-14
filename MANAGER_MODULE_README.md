# Bakery Management System - Manager Module

## Overview
The Manager Module is a comprehensive production planning system that allows managers to create production plans, calculate raw material requirements, and manage production orders for a bakery.

## Features

### 1. Production Planning
- Create production plans with multiple products and quantities
- Automatic calculation of raw material requirements
- Cost estimation for production plans
- Status management (DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELLED)

### 2. Raw Material Management
- Track raw material inventory
- Calculate required quantities based on recipes
- Identify stock deficits
- Cost calculation for raw materials

### 3. Recipe Management
- Store product recipes with ingredient quantities
- Calculate costs per unit for each product
- Version control for recipes

### 4. Product Management
- Manage bakery products with pricing
- Categorize products (Bread, Pastry, Cake, etc.)
- Track product status and availability

## API Endpoints

### Production Plans

#### Create Production Plan
```http
POST /api/manager/production-plan
Content-Type: application/json

{
  "planName": "Weekly Bread Production",
  "planDate": "2024-01-15T08:00:00",
  "notes": "Production plan for next week",
  "productionItems": [
    {
      "productId": 1,
      "productName": "White Bread",
      "quantity": 100
    },
    {
      "productId": 2,
      "productName": "Whole Wheat Bread",
      "quantity": 50
    }
  ]
}
```

#### Get Production Plan
```http
GET /api/manager/production-plan/{planId}
```

#### Get All Production Plans
```http
GET /api/manager/production-plans
```

#### Update Production Plan Status
```http
PUT /api/manager/production-plan/{planId}/status?status=APPROVED
```

#### Get Raw Material Requirements
```http
GET /api/manager/production-plan/{planId}/raw-materials
```

#### Get Total Raw Material Cost
```http
GET /api/manager/production-plan/{planId}/raw-material-cost
```

#### Delete Production Plan
```http
DELETE /api/manager/production-plan/{planId}
```

## Database Schema

### Core Tables

1. **products** - Bakery products with pricing and categories
2. **raw_materials** - Raw materials with stock levels and costs
3. **recipes** - Product recipes linking products to ingredients
4. **recipe_ingredients** - Individual ingredients in recipes with quantities
5. **production_plans** - Production planning documents
6. **production_plan_items** - Individual items in production plans
7. **production_orders** - Production orders based on plans
8. **production_order_items** - Items in production orders
9. **raw_material_requirements** - Calculated raw material needs

## How It Works

### 1. Production Plan Creation
1. Manager creates a production plan with products and quantities
2. System retrieves recipes for each product
3. Calculates raw material requirements based on quantities
4. Aggregates common raw materials across all products
5. Calculates total costs and stock deficits
6. Saves plan to database

### 2. Raw Material Calculation
- For each product: `required_quantity = recipe_quantity × production_quantity`
- Common materials are summed across all products
- Stock deficits are calculated: `deficit = required_quantity - available_stock`
- Total costs: `total_cost = required_quantity × unit_cost`

### 3. Recipe Management
- Each product has a recipe with multiple ingredients
- Ingredients specify quantity per unit of product
- Costs are calculated per ingredient and totaled per recipe

## Sample Data

The system comes with sample data including:
- 5 sample products (White Bread, Whole Wheat Bread, Croissant, Chocolate Cake, Donut)
- 10 raw materials (Flour, Sugar, Butter, Eggs, Milk, Yeast, Salt, Chocolate Chips, Vanilla)
- Complete recipes for all products with realistic ingredient quantities

## Getting Started

### 1. Database Setup
- Ensure MySQL is running on port 3306
- Create database `bmsdb` (or it will be created automatically)
- Update `application.yml` with your database credentials

### 2. Run the Application
```bash
mvn spring-boot:run
```

### 3. Database Migration
- Flyway will automatically run migrations on startup
- Tables will be created with sample data

### 4. Test the API
Use the provided endpoints to:
- Create production plans
- View raw material requirements
- Calculate costs
- Manage production status

## Example Usage

### Creating a Production Plan
```bash
curl -X POST http://localhost:8091/api/manager/production-plan \
  -H "Content-Type: application/json" \
  -d '{
    "planName": "Test Production",
    "planDate": "2024-01-15T08:00:00",
    "productionItems": [
      {
        "productId": 1,
        "productName": "White Bread",
        "quantity": 10
      }
    ]
  }'
```

### Viewing Raw Material Requirements
```bash
curl http://localhost:8091/api/manager/production-plan/1/raw-materials
```

## Business Logic

### Cost Calculation
- **Product Cost**: `unit_price × quantity`
- **Raw Material Cost**: Sum of all required raw materials × their unit costs
- **Total Cost**: Product Cost + Raw Material Cost

### Stock Management
- **Available Stock**: Current inventory levels
- **Required Quantity**: Calculated from recipes × production quantities
- **Stock Deficit**: `max(0, required_quantity - available_stock)`

### Recipe Scaling
- All recipes are designed for 1 unit of product
- Quantities are automatically scaled based on production requirements
- Common materials are aggregated across multiple products

## Error Handling

The system includes comprehensive error handling:
- Validation errors for invalid input
- Business logic errors (e.g., product not found, recipe missing)
- Database constraint violations
- Proper HTTP status codes and error messages

## Security

- Input validation using Bean Validation
- SQL injection protection through JPA
- Cross-Origin Resource Sharing (CORS) enabled
- Comprehensive logging for audit trails

## Performance Considerations

- Database indexes on frequently queried fields
- Lazy loading for related entities
- Efficient aggregation queries for calculations
- Transactional operations for data consistency

## Future Enhancements

- Production scheduling and timeline management
- Inventory alerts for low stock
- Supplier management and ordering
- Production cost analysis and reporting
- Integration with accounting systems
- Mobile application support
