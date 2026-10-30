CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    otp VARCHAR(100) NULL,
    otp_expiry DATETIME NULL,
    verified BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE products (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL,
    image_url VARCHAR(500) NULL,
    brand VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE carts (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user_id (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id INT NOT NULL AUTO_INCREMENT,
    quantity INT NOT NULL,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_cart_product (cart_id, product_id),
    KEY idx_cart_items_cart_id (cart_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id INT NOT NULL AUTO_INCREMENT,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(300) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    razorpay_order_id VARCHAR(100) NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    user_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_razorpay_order (razorpay_order_id),
    UNIQUE KEY uk_orders_razorpay_payment (razorpay_payment_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id INT NOT NULL AUTO_INCREMENT,
    quantity INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_product_id (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE payment_attempts (
    id INT NOT NULL AUTO_INCREMENT,
    razorpay_order_id VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    cart_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempt_razorpay_order (razorpay_order_id),
    CONSTRAINT fk_payment_attempt_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
