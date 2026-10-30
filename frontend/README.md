# FreshCart Online Grocery Store Frontend

React + Vite frontend built from the supplied ECOM frontend as an architectural reference. The supplied project was used for folder/component conventions only; this application is completely FreshCart/grocery focused.

## Backend

Default API base URL: `http://localhost:8080`

You can override it with:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Run locally

Requirements: Node.js 18+ and npm.

```bash
npm install
npm run dev
```

Open `http://localhost:5173`.

## Main pages

- `/` FreshCart home
- `/shop` grocery catalog/search/filter
- `/product/:id` product details
- `/register` registration
- `/verify-otp` OTP verification
- `/login` JWT login
- `/cart` authenticated cart
- `/checkout` Razorpay checkout + delivery validation
- `/orders` customer order history
- `/account` authenticated profile
- `/admin` ADMIN-only dashboard, products, users and orders

## API mapping

- `POST /auth/reg`
- `POST /auth/verify-otp`
- `POST /auth/login`
- `GET /product/**`
- `GET /user/profile`
- `GET /user/{id}`
- `GET /cart/getC`
- `POST /cart/add/{productId}`
- `PUT /cart/increase/{cartItemId}`
- `PUT /cart/decrease/{cartItemId}`
- `PUT /cart/item/{id}`
- `DELETE /cart/item/{id}`
- `POST /cart/clear/{cartId}`
- `GET /order/MyOrders`
- `POST /order/create-order`
- `POST /order/place-order`
- `DELETE /order/deleteItem/{id}` is intentionally not exposed as a customer UI action; the cart UI uses the dedicated `/cart/item/{id}` endpoint.
- `/admin/**` is available only to the ADMIN role returned by the backend login response.

## Logout

The backend is stateless JWT authentication. The frontend logout action removes the JWT and cached user information from local storage and routes back to home.

## Razorpay

Checkout dynamically loads the official Razorpay checkout script. The backend remains responsible for validating the Razorpay order, signature, amount and captured payment before creating the FreshCart order.

### Payment navigation behavior

After Razorpay payment success, FreshCart confirms the payment/order with the backend, refreshes the cart, and redirects to `/shop`. A successfully confirmed order leaves the cart empty. Failed, cancelled, dismissed, or rejected payments also redirect to `/shop`, but the cart is preserved so the customer can retry.

## Feature-complete additions

- Wishlist with product-card heart controls
- Ratings and reviews, restricted to users who have received the product in a delivered order
- Coupon application during checkout; discount and payable amount are recalculated by the backend
- Multiple saved addresses with default-address support
- Order detail pages with status timeline and tracking history
- Admin dashboard with revenue, low-stock monitoring, order filters and coupon management
- Product autocomplete suggestions, related products and recently viewed products
- Notifications with unread counter and polling-based refresh
- Back-in-stock subscriptions
- Profile editing, phone number and password change

Admin cart access is supported and remains scoped to the logged-in admin account. Customer order history uses `/orders`; administrators use `/admin` and the admin Orders tab for all customer orders.
