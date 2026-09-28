# resteaz-web-backend

## Customer ordering API

The customer flow uses the scanned table QR token as the authoritative table context:

1. `GET /api/tables/{qrToken}` validates the QR and returns the table and its restaurant.
2. `GET /api/categories` and `GET /api/menu` provide category and item data, including category/item availability, prices, and image URLs.
3. The client keeps the QR token for the checkout session and submits it with the order. It must generate one stable `idempotencyKey` per checkout and reuse that key for retries.
4. `POST /api/orders` creates the order and its items. The server resolves restaurant and table from the QR token and calculates prices from the current menu; required `restaurantId` and `tableId` values are checked against that QR context.
5. The response contains the order ID, table, item quantities/prices, total, and initial `PENDING` status. `GET /api/orders/{id}` returns the current confirmation/status.

Example order body:

```json
{
  "qrToken": "scanned-table-token",
  "idempotencyKey": "client-generated-checkout-uuid",
  "restaurantId": 7,
  "tableId": 12,
  "items": [
    { "menuItemId": 21, "quantity": 2 }
  ]
}
```

Invalid or inactive QR/table/restaurant context, empty carts, invalid quantities, and unavailable items are rejected. `restaurantId` and `tableId` must match the scanned QR context. Repeating an identical request with the same idempotency key returns the original order; reusing the key for a different table or cart is rejected.

## Order management APIs

Orders start as `PENDING` (the placed state) with payment status `UNPAID`. Allowed status changes are `PENDING → CONFIRMED → PREPARING → READY → COMPLETED`; cancellation is allowed only from `PENDING` or `CONFIRMED`. `COMPLETED` represents served orders.

* `GET /api/orders/restaurant/{restaurantId}` returns a restaurant's orders for the kitchen display.
* `GET /api/orders/table/{tableId}` returns the order history for a table.
* `PATCH /api/orders/{id}/status` accepts `{"status":"CONFIRMED"}` and enforces forward lifecycle transitions.
* `POST /api/orders/{id}/cancel` cancels an order when cancellation is still allowed.

Order responses include creation/update timestamps, payment status, optional customer name/phone, table details, item quantity and server-priced unit/subtotal snapshots, and the order total. The customer tracking page polls the order API, while the manager KDS at `/admin/orders` refreshes the restaurant queue automatically.