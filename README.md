# backerymanagmentsystem-Backend

## Store Keeper API Updates

- `GET /STK/v1/approved-plans/material-summary` now includes a `productSummaries` array for each approved production plan. Each entry reports the associated production order items (product id, name, planned/completed quantities, unit cost, total cost, and raw material cost). Plans without a production order continue to return an empty `productSummaries` list.
- `GET /STK/v1/mini-stores/outlets/{outletId}/daily-inventory` retrieves the current day's mini-store snapshot for the given outlet, separating raw-material and product inventory rows (including names, codes, quantities, variances, and timestamps).
