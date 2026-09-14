# Material Issuance Feature Implementation

## Overview

This implementation provides a complete solution for material issuance in the Bakery Management System following OOP principles and clean code practices.

## Feature Details

### Endpoint

```
POST /STK/v1/approved-plans/issue-plan
```

### Request Body

```json
{
  "productionPlanId": 123
}
```

### Response

```json
{
  "message": "Materials successfully issued for production plan",
  "productionPlanId": 123,
  "productionPlanName": "Daily Bread Production Plan",
  "issuanceDateTime": "2025-09-09T14:30:00",
  "issuedMaterials": [
    {
      "materialId": 1,
      "materialName": "Wheat Flour",
      "materialCode": "WF001",
      "issuedQuantity": 50.0,
      "unitOfMeasure": "kg",
      "previousStock": 150.0,
      "updatedStock": 100.0,
      "unitCost": 2.5,
      "totalCost": 125.0
    }
  ]
}
```

## Implementation Architecture

### 1. DTOs (Data Transfer Objects)

- `IssueMaterialsRequestDto`: Input validation with production plan ID
- `IssueMaterialsResponseDto`: Comprehensive response with all required details

### 2. Exception Handling

Custom exceptions for different error scenarios:

- `ProductionPlanNotFoundException`: Plan doesn't exist
- `ProductionPlanNotApprovedException`: Plan not in APPROVED status
- `InsufficientStockException`: Not enough stock for materials
- `MaterialsAlreadyIssuedException`: Materials already issued for the plan

### 3. Service Layer Architecture

Following Single Responsibility Principle (SRP):

- `MaterialIssuanceService`: Dedicated service for material issuance logic
- `StoreKeeperProductionPlanService`: Delegates to MaterialIssuanceService

### 4. Repository Layer

- `RawMaterialRequirementRepository`: Handle raw_material_requirements table
- `ProductionOrderRepository`: Handle production_orders table
- Reused existing repositories for other entities

### 5. Business Logic Flow

#### Validation Phase:

1. **Production Plan Validation**

   - Check if production plan exists
   - Verify plan status is APPROVED
   - Ensure materials haven't been issued already

2. **Stock Validation**
   - Check sufficient stock for ALL materials
   - Fail fast if any material has insufficient stock
   - Provide detailed error with material names and quantities

#### Issuance Phase (Atomic Transaction):

1. **Stock Updates**

   - Reduce `current_stock` in `raw_materials` table
   - Update for all materials in the plan

2. **Record Creation**

   - Create/get production order for the plan
   - Insert records in `raw_material_requirements` table
   - Track issued quantities, costs, and stock levels

3. **Status Update**
   - Update production plan status to COMPLETED

## Database Impact

### Tables Modified:

1. **raw_materials**: `current_stock` reduced by issued quantities
2. **raw_material_requirements**: New records created for tracking
3. **production_plans**: Status updated to COMPLETED
4. **production_orders**: Created if doesn't exist

### Foreign Key Relationships:

- Uses existing production plan ID
- Links to existing raw material IDs
- Creates production order with proper relationships

## Error Handling Examples

### 1. Production Plan Not Found (404)

```json
{
  "timestamp": "2025-09-09T14:30:00",
  "status": 404,
  "error": "Production Plan Not Found",
  "message": "Production plan not found with ID: 999",
  "path": "/STK/v1/approved-plans/issue-plan"
}
```

### 2. Insufficient Stock (400)

```json
{
  "timestamp": "2025-09-09T14:30:00",
  "status": 400,
  "error": "Insufficient Stock",
  "message": "Insufficient stock for materials: Wheat Flour (Required: 100.00, Available: 50.00), Sugar (Required: 25.00, Available: 10.00)",
  "path": "/STK/v1/approved-plans/issue-plan",
  "details": {
    "insufficientMaterials": [
      "Wheat Flour (Required: 100.00, Available: 50.00)",
      "Sugar (Required: 25.00, Available: 10.00)"
    ]
  }
}
```

### 3. Plan Not Approved (400)

```json
{
  "timestamp": "2025-09-09T14:30:00",
  "status": 400,
  "error": "Production Plan Not Approved",
  "message": "Production plan with ID 123 is not approved. Current status: DRAFT",
  "path": "/STK/v1/approved-plans/issue-plan"
}
```

## Clean Code Principles Applied

### 1. Single Responsibility Principle (SRP)

- Each class has one reason to change
- MaterialIssuanceService only handles material issuance
- Exception classes handle specific error types

### 2. Open/Closed Principle (OCP)

- Service interfaces allow for easy extension
- New validation rules can be added without modifying existing code

### 3. Dependency Inversion Principle (DIP)

- Services depend on interfaces, not concrete implementations
- Easy to mock for testing

### 4. Clean Architecture

- Clear separation between controllers, services, repositories
- DTOs for data transfer between layers
- Custom exceptions for domain-specific errors

## Transaction Management

- `@Transactional` ensures atomicity
- If any step fails, all changes are rolled back
- Prevents partial material issuance

## Logging and Monitoring

- Comprehensive logging at DEBUG and INFO levels
- Error logging for troubleshooting
- Performance tracking for material issuance operations

## Files Created/Modified

### New Files:

1. `IssueMaterialsRequestDto.java`
2. `IssueMaterialsResponseDto.java`
3. `MaterialIssuanceService.java`
4. `MaterialIssuanceServiceImpl.java`
5. `RawMaterialRequirementRepository.java`
6. `ProductionOrderRepository.java`
7. Exception classes (4 files)
8. `StoreKeeperExceptionHandler.java`
9. `ErrorResponse.java`

### Modified Files:

1. `StoreKeeperController.java` - Updated endpoint
2. `StoreKeeperProductionPlanService.java` - Added new method
3. `StoreKeeperProductionPlanServiceImpl.java` - Delegate to MaterialIssuanceService

## Testing Recommendations

### Unit Tests:

- Test each validation scenario
- Test successful material issuance
- Test rollback on failures
- Mock repository dependencies

### Integration Tests:

- Test complete flow with database
- Test transaction rollback
- Test concurrent access scenarios

### API Tests:

- Test all error response formats
- Test successful issuance response
- Test input validation

This implementation ensures robust, maintainable, and scalable material issuance functionality.
