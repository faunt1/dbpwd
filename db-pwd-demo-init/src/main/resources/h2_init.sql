-- ========================================
-- H2 Demo Database Initialization Script
-- ========================================
-- 所有 demo 数据库使用统一的表结构和初始数据
-- 
-- 表设计：
--   users    - 用户表
--   products - 产品表
--   orders   - 订单表
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 产品表
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 初始用户数据
INSERT INTO users (username, email, password) VALUES 
    ('alice', 'alice@example.com', 'password123'),
    ('bob', 'bob@example.com', 'password456'),
    ('charlie', 'charlie@example.com', 'password789');

-- 初始产品数据
INSERT INTO products (name, description, price, stock, category) VALUES 
    ('Laptop', 'High-performance laptop', 1299.99, 50, 'Electronics'),
    ('Smartphone', 'Latest smartphone model', 899.99, 100, 'Electronics'),
    ('Java Programming', 'Comprehensive Java guide', 49.99, 200, 'Books');

-- 初始订单数据
INSERT INTO orders (user_id, product_name, amount, quantity) VALUES 
    (1, 'Laptop', 1299.99, 1),
    (2, 'Smartphone', 899.99, 1),
    (1, 'Java Programming', 49.99, 2);
