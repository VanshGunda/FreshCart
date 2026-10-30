package com.freshcart.services;

import com.freshcart.dto.ProductRequest;
import com.freshcart.entities.Cart;
import com.freshcart.entities.Product;
import com.freshcart.entities.Role;
import com.freshcart.entities.User;
import com.freshcart.repos.CartRepo;
import com.freshcart.repos.ProductRepo;
import com.freshcart.repos.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepo userRepo;
    private final CartRepo cartRepo;
    private final ProductRepo productRepo;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean seedProducts;
    private final boolean repairInvalidImageUrls;

    public DataSeeder(UserRepo userRepo, CartRepo cartRepo, ProductRepo productRepo, PasswordEncoder passwordEncoder,
                      @Value("${app.admin.username:}") String adminUsername,
                      @Value("${app.admin.password:}") String adminPassword,
                      @Value("${app.data-seeder.seed-products:true}") boolean seedProducts,
                      @Value("${app.data-seeder.repair-invalid-image-urls:false}") boolean repairInvalidImageUrls) {
        this.userRepo = userRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.seedProducts = seedProducts;
        this.repairInvalidImageUrls = repairInvalidImageUrls;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        if (seedProducts && productRepo.count() == 0) seedSampleProducts();
        if (repairInvalidImageUrls) repairInvalidImageUrls();
    }

    private void seedAdmin() {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin seeding skipped. Set APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD.");
            return;
        }
        String normalizedUsername = adminUsername.trim().toLowerCase();
        User admin = userRepo.findByUsername(normalizedUsername).orElseGet(User::new);
        admin.setUsername(normalizedUsername);
        if (admin.getId() == null) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
        }
        admin.setRole(Role.ADMIN);
        admin.setVerified(true);
        admin.setActive(true);
        admin.setDeletedAt(null);
        admin.setOtp(null);
        admin.setOtpExpiry(null);
        User saved = userRepo.save(admin);
        if (cartRepo.findByUser(saved).isEmpty()) {
            Cart cart = new Cart();
            cart.setUser(saved);
            cartRepo.save(cart);
        }
        log.info("FreshCart admin account initialized for {}", saved.getUsername());
    }

    private void repairInvalidImageUrls() {
        List<Product> products = productRepo.findAll();
        int repaired = 0;

        for (Product product : products) {
            String url = product.getImageUrl();
            if (!isLegacyLocalImageUrl(url)) continue;

            product.setImageUrl(defaultImageForCategory(product.getCategory()));
            productRepo.save(product);
            repaired++;
        }

        if (repaired > 0) {
            log.info("Repaired {} legacy local product image URL(s)", repaired);
        }
    }

    private boolean isLegacyLocalImageUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String normalized = url.trim().toLowerCase();
        return normalized.contains("localhost:")
                || normalized.contains("127.0.0.1:")
                || normalized.contains(":7070/")
                || normalized.contains(":7071/")
                || normalized.contains(":37857/");
    }

    private String defaultImageForCategory(String category) {
        if (category == null) {
            return "https://images.unsplash.com/photo-1542838132-92c53300491e";
        }
        return switch (category.trim().toLowerCase()) {
            case "fruits" -> "https://images.unsplash.com/photo-1610832958506-aa56368176cf";
            case "vegetables" -> "https://images.unsplash.com/photo-1542838132-92c53300491e";
            case "dairy" -> "https://images.unsplash.com/photo-1550583724-b2692b85b150";
            case "bakery" -> "https://images.unsplash.com/photo-1509440159596-0249088772ff";
            case "staples" -> "https://images.unsplash.com/photo-1586201375761-83865001e31c";
            case "beverages" -> "https://images.unsplash.com/photo-1600271886742-f049cd451bba";
            default -> "https://images.unsplash.com/photo-1542838132-92c53300491e";
        };
    }

    private void seedSampleProducts() {
        List<Product> products = List.of(
            product("Fresh Bananas", "Naturally sweet Cavendish bananas", "45.00", 100, "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e", "FreshFarm", "Fruits", "1 kg"),
            product("Red Apples", "Crisp and juicy red apples", "140.00", 80, "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6", "FreshFarm", "Fruits", "1 kg"),
            product("Full Cream Milk", "Fresh dairy milk", "68.00", 60, "https://images.unsplash.com/photo-1550583724-b2692b85b150", "DailyFresh", "Dairy", "1 L"),
            product("Brown Bread", "Soft whole-wheat brown bread", "55.00", 50, "https://images.unsplash.com/photo-1509440159596-0249088772ff", "BakeHouse", "Bakery", "400 g"),
            product("Basmati Rice", "Long-grain premium basmati rice", "175.00", 100, "https://images.unsplash.com/photo-1586201375761-83865001e31c", "GoldenGrain", "Staples", "1 kg"),
            product("Tomatoes", "Farm-fresh ripe tomatoes", "40.00", 120, "https://images.unsplash.com/photo-1546094096-0df4bcaaa337", "FreshFarm", "Vegetables", "1 kg"),
            product("Potatoes", "Everyday kitchen potatoes", "35.00", 120, "https://images.unsplash.com/photo-1518977676601-b53f82aba655", "FreshFarm", "Vegetables", "1 kg"),
            product("Orange Juice", "Refreshing 100% fruit drink", "120.00", 40, "https://images.unsplash.com/photo-1600271886742-f049cd451bba", "SunSip", "Beverages", "1 L")
        );
        productRepo.saveAll(products);
        log.info("Seeded {} FreshCart sample products", products.size());
    }

    private Product product(String name, String description, String price, int stock, String imageUrl, String brand, String category, String unit) {
        Product p = new Product();
        p.setName(name); p.setDescription(description); p.setPrice(new BigDecimal(price));
        p.setStock(stock); p.setImageUrl(imageUrl); p.setBrand(brand); p.setCategory(category); p.setUnit(unit); p.setActive(true);
        return p;
    }
}
