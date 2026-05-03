package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.CategoryNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.eshop.app.seed.model.CategoryNode.*;

/**
 * Code-based implementation of CategoryDataProvider.
 * 
 * <p>
 * Provides a comprehensive, hierarchical category structure hardcoded in Java.
 * This is the default implementation for development and test environments.
 *
 * <p>
 * <b>Categories Included:</b>
 * <ul>
 * <li>Fashion & Apparel (Men, Women, Kids, Footwear, Accessories)</li>
 * <li>Electronics (Mobiles, Computers, TV & Appliances, Cameras, Smart
 * Devices)</li>
 * <li>Home & Living (Furniture, Home Décor, Kitchen & Dining, Home
 * Improvement)</li>
 * <li>Beauty, Health & Personal Care</li>
 * <li>Grocery & Essentials (Food, Beverages, Household, Fruits &
 * Vegetables)</li>
 * <li>Sports, Fitness & Outdoor</li>
 * <li>Toys, Kids & Baby</li>
 * <li>Books, Office & Stationery</li>
 * <li>Automotive</li>
 * <li>Industrial & B2B</li>
 * <li>Digital Products</li>
 * <li>Luxury & Specialty</li>
 * <li>Services</li>
 * </ul>
 *
 * <p>
 * <b>Total Categories:</b> ~200+
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Profile({ "dev", "test", "local" })
public class CodeBasedCategoryDataProvider implements CategoryDataProvider {

    @Override
    public List<CategoryNode> getCategoryHierarchy() {
        log.debug("Building code-based category hierarchy");

        return List.of(
                // Fashion & Apparel
                        // Fashion (Re-structured as per Enterprise Standards)
                        of("Fashion",
                                        // Men
                                        of("Men",
                                                        withChildren("Clothing", "T-Shirts", "Shirts", "Jeans",
                                                                        "Trousers", "Ethnic Wear", "Innerwear",
                                                                        "Sportswear", "chinos", "cargo pants",
                                                                        "joggers"),
                                                        withChildren("Footwear", "Men's Casual Shoes",
                                                                        "Men's Sports Shoes", "Men's Formal Shoes",
                                                                        "Men's Sandals"),
                                                        withChildren("Accessories", "Wallets", "Belts", "Men's Watches",
                                                                        "Sunglasses", "Caps & Hats")),

                                        // Women
                                        of("Women",
                                                        withChildren("Clothing", "Tops", "Dresses", "Sarees", "Kurtis",
                                                                        "Jeans", "Leggings", "Lingerie & Sleepwear",
                                                                        "trousers", "chinos", "cargo pants", "joggers"),
                                                        withChildren("Footwear", "Women's Casual Shoes", "Heels",
                                                                        "Women's Sports Shoes", "Boots"),
                                                        withChildren("Accessories", "Handbags", "Jewelry",
                                                                        "Women's Watches", "Scarves")),

                                        // Kids
                                        of("Kids",
                                                        withChildren("Clothing", "Boys Clothing", "Girls Clothing",
                                                                        "Infant Wear"),
                                                        withChildren("Footwear", "Kids' Casual Shoes",
                                                                        "Kids' Sports Shoes", "School Shoes"),
                                                        withChildren("Accessories", "School Bags", "Kids' Watches")),
                                        // Unisex styles
                                        of("Unisex styles",
                                                        withChildren("Clothing", "T-Shirts", "Shirts", "Jeans",
                                                                        "Trousers", "Ethnic Wear", "Innerwear",
                                                                        "Sportswear", "chinos", "cargo pants",
                                                                        "joggers"),
                                                        withChildren("Footwear", "Men's Casual Shoes",
                                                                        "Men's Sports Shoes", "Men's Formal Shoes",
                                                                        "Men's Sandals"),
                                                        withChildren("Accessories", "Wallets", "Belts", "Men's Watches",
                                                                        "Sunglasses", "Caps & Hats"))),

                // Electronics
                of("Electronics",
                        withChildren("Mobiles & Accessories", "Smartphones", "Feature Phones", "Cases & Covers",
                                "Chargers & Cables", "Power Banks"),
                        withChildren("Computers", "Laptops", "Desktops", "Monitors", "Keyboards & Mouse",
                                "Storage Devices"),
                        withChildren("TV & Appliances", "Smart TVs", "Set-top Boxes", "Speakers", "Soundbars"),
                        withChildren("Cameras", "DSLR", "Mirrorless", "Action Cameras"),
                        withChildren("Smart Devices", "Smart Watches", "Fitness Bands", "Smart Home Devices")),

                // Home & Living
                of("Home & Living",
                        withChildren("Furniture", "Sofa", "Beds", "Tables & Chairs", "Wardrobes"),
                        withChildren("Home Decor", "Wall Art", "Lamps & Lighting", "Clocks", "Showpieces"),
                        withChildren("Kitchen & Dining", "Cookware", "Dinner Sets", "Kitchen Tools"),
                        withChildren("Home Improvement", "Electricals", "Plumbing", "Hardware Tools")),

                // Beauty, Health & Personal Care
                of("Beauty, Health & Personal Care",
                        withChildren("Beauty", "Makeup", "Skincare", "Haircare", "Personal Care"),
                        withChildren("Grooming", "Hygiene Products"),
                        withChildren("Health", "Supplements", "Medical Equipment", "Fitness Monitoring Devices"),
                        withChildren("Fragrances", "Perfumes", "Deodorants")),

                // Grocery & Essentials - **FIXED HIERARCHY**
                of("Grocery & Essentials",
                        withChildren("Food", "Rice, Wheat & Pulses", "Snacks & Packaged Foods", "Spices"),
                        withChildren("Beverages", "Tea & Coffee", "Soft Drinks", "Health Drinks"),
                        withChildren("Household Essentials", "Cleaning Supplies", "Detergents", "Paper Products"),
                        // ✅ FIXED: Proper nested hierarchy for Fruits & Vegetables
                        of("Fruits & Vegetables",
                                of("Fresh Fruits",
                                        withChildren("Common Fruits", "Apples", "Bananas", "Oranges", "Grapes"),
                                        withChildren("Seasonal Fruits", "Mangoes", "Pomegranates", "Watermelon"),
                                        withChildren("Exotic Fruits", "Kiwi", "Dragon Fruit", "Avocado",
                                                "Blueberries")),
                                of("Vegetables",
                                        withChildren("Leafy Vegetables", "Spinach", "Lettuce", "Fenugreek",
                                                "Coriander"),
                                        withChildren("Root Vegetables", "Potato", "Onion", "Carrot", "Beetroot"),
                                        withChildren("Gourds & Pods", "Tomato", "Brinjal", "Lady Finger",
                                                "Bottle Gourd", "Beans"),
                                        withChildren("Cruciferous Vegetables", "Cabbage", "Cauliflower", "Broccoli"),
                                        withChildren("Exotic Vegetables", "Zucchini", "Bell Peppers", "Asparagus"),
                                        withChildren("Frozen & Processed Vegetables", "Frozen Veg Mix",
                                                "Cut Vegetables")),
                                withChildren("Dry Fruits", "Almonds", "Cashews", "Pistachios", "Raisins", "Dates"),
                                withChildren("Organic & Special", "Organic Fruits", "Organic Vegetables",
                                        "Pesticide-Free Produce", "Hydroponic Produce")),
                        leaf("Dairy")),

                // Sports, Fitness & Outdoor
                of("Sports, Fitness & Outdoor",
                        withChildren("Sports Equipment", "Cricket", "Football", "Badminton"),
                        withChildren("Fitness", "Gym Equipment", "Yoga Accessories"),
                        withChildren("Outdoor", "Camping Gear", "Trekking Equipment", "Cycling Accessories")),

                // Toys, Kids & Baby
                of("Toys, Kids & Baby",
                        withChildren("Toys", "Educational Toys", "Action Figures", "Board Games"),
                        withChildren("Baby Care", "Diapers", "Baby Food", "Baby Grooming"),
                        withChildren("Kids Essentials", "School Bags", "Stationery")),

                // Books, Office & Stationery
                of("Books, Office & Stationery",
                        withChildren("Books", "Academic", "Fiction", "Non-Fiction", "Competitive Exams"),
                        withChildren("Stationery", "Pens & Notebooks", "Art Supplies"),
                        withChildren("Office Supplies", "Printers", "Office Furniture")),

                // Automotive
                of("Automotive",
                        withChildren("Vehicle Accessories", "Seat Covers", "Helmets", "Car Electronics"),
                        withChildren("Spare Parts", "Engine Parts", "Tyres", "Batteries"),
                        withChildren("Tools", "Car Care Tools", "Repair Kits")),

                // Industrial & B2B
                of("Industrial & B2B",
                        withChildren("Machinery", "Manufacturing Equipment"),
                        withChildren("Electricals", "Switches", "Wires & Cables"),
                        withChildren("Safety", "Helmets", "Gloves", "Industrial Shoes"),
                        withChildren("Packaging", "Boxes", "Labels")),

                // Digital Products
                of("Digital Products",
                        withChildren("Software", "Antivirus", "Business Tools"),
                        withChildren("Digital Content", "E-books", "Music"),
                        withChildren("Online Services", "Subscriptions", "Cloud Services"),
                        withChildren("Courses", "Programming", "Design", "Marketing")),

                // Luxury & Specialty
                of("Luxury & Specialty",
                        withChildren("Luxury Fashion", "Designer Wear"),
                        withChildren("Jewelry", "Gold", "Diamond"),
                        withChildren("Collectibles", "Art", "Antiques"),
                        withChildren("Handmade", "Crafts", "Custom Products")),

                // Services
                withChildren("Services", "Repairs & Maintenance", "Home Services", "Freelancing Services",
                        "Event Services"));
    }

    @Override
    public String getProviderName() {
        return "code-based";
    }
}
