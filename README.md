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

## Menu add-ons

`GET /api/add-ons` lists reusable extras. `POST /api/add-ons` creates one with `name`, `price`, optional `description`, and optional `available`; `PUT /api/add-ons/{id}` updates it. Menu item create/update requests accept `addOns: [{ "id": 3 }]` to associate existing extras, or new add-on fields to create and attach an extra. Sending an empty list removes all associations from that menu item.

Order item requests can optionally include `addOns: [{ "addOnId": 3, "quantity": 1 }]`. The order API verifies the add-on is available and associated with that menu item, gets its price from the database, and stores a name/price snapshot with the ordered item. Item and order totals include add-ons, while existing orders retain their original add-on names and prices.

## Menu item images

Menu items keep their image URL in the existing `imageUrl` field. Set `MENU_IMAGE_STORAGE_DIR` to a persistent writable directory for uploaded images; when unset, uploads are stored in `uploads/menu-items` under the backend working directory. `POST /api/menu/images` accepts a multipart `file` (JPEG, PNG, or GIF, up to 5 MB) and returns an `imageUrl` such as `/uploads/menu-items/{generated-name}.png`. The URL can then be sent through the existing menu-item create/update API. Menu-item URLs supplied directly must use HTTP or HTTPS.

This project has no cloud image-storage integration. The local directory is appropriate for a single backend instance with persistent disk; deployments with multiple or ephemeral instances should mount shared persistent storage or migrate this endpoint to managed object storage.

## Order management APIs

Orders start as `PENDING` (the placed state) with payment status `UNPAID`. Allowed status changes are `PENDING → CONFIRMED → PREPARING → READY → COMPLETED`; cancellation is allowed only from `PENDING` or `CONFIRMED`. `COMPLETED` represents served orders.

* `GET /api/orders/restaurant/{restaurantId}` returns a restaurant's orders for the kitchen display.
* `GET /api/orders/table/{tableId}` returns the order history for a table.
* `PATCH /api/orders/{id}/status` accepts `{"status":"CONFIRMED"}` and enforces forward lifecycle transitions.
* `POST /api/orders/{id}/cancel` cancels an order when cancellation is still allowed.

Order responses include creation/update timestamps, payment status, optional customer name/phone, table details, item quantity and server-priced unit/subtotal snapshots, and the order total. The customer tracking page polls the order API, while the manager KDS at `/admin/orders` refreshes the restaurant queue automatically.