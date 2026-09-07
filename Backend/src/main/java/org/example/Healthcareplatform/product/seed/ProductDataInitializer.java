package org.example.Healthcareplatform.product.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.product.entity.Product;
import org.example.Healthcareplatform.product.entity.Product.ProductCategory;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        productRepository.deleteAll();
        log.info("Seeding {} sample healthcare products...", sampleProducts().size());

        List<Product> products = sampleProducts();
        productRepository.saveAll(products);

        log.info("Successfully seeded {} products.", products.size());
    }

    private List<Product> sampleProducts() {
        return List.of(
                // ==================== PAIN_RELIEF ====================
                Product.builder()
                        .name("Paracetamol 500mg Tablets")
                        .description("Effective pain relief for headaches, migraines, toothaches, period pain, and fever reduction. Each tablet contains 500mg paracetamol BP. Suitable for adults and children over 12 years. Fast-acting formula provides relief within 30 minutes.")
                        .price(new BigDecimal("4.99"))
                        .category(ProductCategory.PAIN_RELIEF)
                        .imageUrl("/images/products/paracetamol.png")
                        .stockQuantity(500)
                        .manufacturer("MediPharm Laboratories")
                        .dosage("Adults: 1-2 tablets every 4-6 hours. Maximum 8 tablets in 24 hours. Do not exceed recommended dose.")
                        .ingredients("Paracetamol BP 500mg, Maize Starch, Povidone, Magnesium Stearate, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("Rare but may include: allergic reactions (skin rash, swelling), blood disorders with prolonged high doses. Seek immediate medical attention if overdose is suspected.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Sarah M.", 5, "Always works quickly for my headaches. A must-have in every medicine cabinet.", null),
                                new Product.Review("David L.", 4, "Effective and affordable. Works well for mild fever too.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Ibuprofen 400mg Capsules")
                        .description("Non-steroidal anti-inflammatory drug (NSAID) for relief from rheumatic and muscular pain, backache, neuralgia, migraine, dental pain, and fever. Also reduces inflammation and swelling.")
                        .price(new BigDecimal("7.49"))
                        .category(ProductCategory.PAIN_RELIEF)
                        .imageUrl("/images/products/ibuprofen.png")
                        .stockQuantity(350)
                        .manufacturer("PharmaCare International")
                        .dosage("Adults: 1 capsule every 6-8 hours as needed. Maximum 3 capsules (1200mg) in 24 hours. Take with food or milk.")
                        .ingredients("Ibuprofen 400mg, Microcrystalline Cellulose, Colloidal Silicon Dioxide, Magnesium Stearate, Gelatin Capsule Shell")
                        .prescriptionRequired(false)
                        .sideEffects("May cause: stomach upset, heartburn, nausea. Long-term use: risk of stomach ulcers, kidney issues. Not recommended for asthmatics or those with stomach ulcers. Avoid if pregnant.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("James K.", 5, "Best anti-inflammatory I've used. Helps with my back pain tremendously.", null)
                        ))
                        .build(),

                // ==================== VITAMINS ====================
                Product.builder()
                        .name("Vitamin D3 2000 IU Softgels")
                        .description("High-potency Vitamin D3 supplement to support bone health, immune function, and mood regulation. Each softgel provides 2000 IU of Vitamin D3 (Cholecalciferol). Essential for calcium absorption and maintaining healthy teeth and bones.")
                        .price(new BigDecimal("12.99"))
                        .category(ProductCategory.VITAMINS)
                        .imageUrl("/images/products/vitamin-d3.png")
                        .stockQuantity(700)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1 softgel daily with a meal, or as directed by your healthcare professional.")
                        .ingredients("Vitamin D3 (Cholecalciferol) 50mcg (2000 IU), Soybean Oil, Gelatin, Glycerin, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("Generally well-tolerated. Excessive doses may cause hypercalcemia (high calcium levels), weakness, or nausea. Rarely may cause allergic reactions.")
                        .ratings(4.7)
                        .reviews(List.of(
                                new Product.Review("Emily R.", 5, "My Vitamin D levels improved significantly after 3 months. Highly recommend!", null),
                                new Product.Review("Michael T.", 4, "Good quality and easy to swallow. Noticed improved energy levels.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Vitamin C 1000mg with Zinc")
                        .description("High-strength Vitamin C combined with Zinc for enhanced immune support. Antioxidant protection against free radicals. Supports collagen production for healthy skin. Buffered formula gentle on stomach.")
                        .price(new BigDecimal("9.99"))
                        .category(ProductCategory.VITAMINS)
                        .imageUrl("/images/products/vitamin-c.png")
                        .stockQuantity(850)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1 tablet daily with water after a meal. Can be taken up to 2 tablets during cold season.")
                        .ingredients("Ascorbic Acid (Vitamin C) 1000mg, Zinc Gluconate 15mg, Calcium Carbonate, Cellulose, Stearic Acid")
                        .prescriptionRequired(false)
                        .sideEffects("High doses may cause mild gastrointestinal discomfort or diarrhea. Very rarely: kidney stones with prolonged mega-dosing. Generally very safe.")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Lisa C.", 5, "Since taking this daily, I rarely get sick. Perfect immune booster!", null),
                                new Product.Review("John H.", 4, "Good quality supplement. The zinc addition makes a real difference.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Daily Multivitamin Complete")
                        .description("Complete daily multivitamin formula with 23 essential vitamins and minerals. Includes B-complex, iron, calcium, magnesium, and important trace minerals. Ideal for maintaining overall health and filling nutritional gaps in your diet.")
                        .price(new BigDecimal("15.49"))
                        .category(ProductCategory.WELLNESS)
                        .imageUrl("/images/products/multivitamin.png")
                        .stockQuantity(600)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1 tablet daily with breakfast. Do not exceed recommended dose.")
                        .ingredients("Vitamin A, Vitamin B1-B12 Complex, Vitamin C, Vitamin D3, Vitamin E, Vitamin K, Calcium, Iron, Magnesium, Zinc, Selenium, Copper, Manganese, Chromium, Iodine, Folic Acid, Biotin")
                        .prescriptionRequired(false)
                        .sideEffects("Generally well-tolerated. Iron may cause mild constipation in sensitive individuals. Avoid taking with tea or coffee (reduces absorption).")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Robert P.", 5, "Complete multivitamin with everything I need. Feel more energetic daily.", null),
                                new Product.Review("Angela W.", 4, "Comprehensive formula at a great price. Tablets are a bit large though.", null)
                        ))
                        .build(),

                // ==================== DIGESTIVE_HEALTH ====================
                Product.builder()
                        .name("Omeprazole 20mg Gastro-Resistant Capsules")
                        .description("Proton pump inhibitor for the treatment of acid reflux, heartburn, GERD, and gastric ulcers. Reduces stomach acid production providing long-lasting relief. One capsule provides relief for up to 24 hours.")
                        .price(new BigDecimal("11.99"))
                        .category(ProductCategory.DIGESTIVE_HEALTH)
                        .imageUrl("/images/products/omeprazole.png")
                        .stockQuantity(300)
                        .manufacturer("MediPharm Laboratories")
                        .dosage("1 capsule (20mg) once daily in the morning before food. Complete treatment course as prescribed (usually 4-8 weeks).")
                        .ingredients("Omeprazole 20mg, Sugar Spheres, Sodium Lauryl Sulfate, Methacrylic Acid Copolymer, Talc, Gelatin Capsule")
                        .prescriptionRequired(true)
                        .sideEffects("Common: headache, nausea, abdominal pain, diarrhea or constipation. Rare: vitamin B12 deficiency with long-term use, increased risk of bone fractures. Do not use for more than 14 days without medical advice.")
                        .ratings(4.2)
                        .reviews(List.of(
                                new Product.Review("Patricia D.", 5, "Life-changing for my chronic acid reflux. 24-hour relief really works.", null),
                                new Product.Review("George B.", 3, "Works well but takes a few days to notice full effect.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Probiotic 50 Billion CFU")
                        .description("Advanced multi-strain probiotic with 50 billion CFU from 12 clinically-studied strains including Lactobacillus and Bifidobacterium. Supports digestive health, immune function, and restores gut flora balance. Patented delayed-release capsules survive stomach acid.")
                        .price(new BigDecimal("22.99"))
                        .category(ProductCategory.DIGESTIVE_HEALTH)
                        .imageUrl("/images/products/probiotic.png")
                        .stockQuantity(450)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1 capsule daily on an empty stomach, preferably in the morning. Store in a cool, dry place.")
                        .ingredients("Lactobacillus Acidophilus, Lactobacillus Plantarum, Bifidobacterium Lactis, Bifidobacterium Longum, Lactobacillus Rhamnosus, Bifidobacterium Bifidum, FOS Prebiotic Fiber, Magnesium Stearate")
                        .prescriptionRequired(false)
                        .sideEffects("Mild bloating or gas may occur during the first few days as gut flora adjusts. Generally very safe. Refrigerate after opening for maximum potency.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Catherine F.", 5, "After antibiotics, this restored my digestion within a week. Amazing!", null),
                                new Product.Review("Mark S.", 4, "High quality probiotic. Noticed less bloating after meals.", null)
                        ))
                        .build(),

                // ==================== RESPIRATORY ====================
                Product.builder()
                        .name("Cetirizine 10mg Antihistamine Tablets")
                        .description("Non-drowsy antihistamine for relief of hay fever, seasonal allergies, pet allergies, dust mite allergies, and urticaria (hives). Provides 24-hour relief from sneezing, runny nose, itchy eyes, and skin rashes.")
                        .price(new BigDecimal("6.49"))
                        .category(ProductCategory.RESPIRATORY)
                        .imageUrl("/images/products/cetirizine.png")
                        .stockQuantity(650)
                        .manufacturer("PharmaCare International")
                        .dosage("Adults and children over 12: 1 tablet (10mg) once daily. Children 6-12: half tablet (5mg) twice daily.")
                        .ingredients("Cetirizine Dihydrochloride 10mg, Lactose Monohydrate, Microcrystalline Cellulose, Colloidal Silica, Magnesium Stearate, Hypromellose, Titanium Dioxide")
                        .prescriptionRequired(false)
                        .sideEffects("Uncommon: mild drowsiness, dry mouth, headache. Rarely: dizziness, fatigue. Avoid alcohol as it may increase drowsiness. Consult doctor if pregnant or breastfeeding.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Helen J.", 5, "Best allergy medicine I've tried. No drowsiness and works all day.", null),
                                new Product.Review("Tom R.", 4, "Excellent for hay fever season. Only slight dry mouth occasionally.", null)
                        ))
                        .build(),

                // ==================== HEART_HEALTH ====================
                Product.builder()
                        .name("Omega-3 Fish Oil 1000mg")
                        .description("Premium fish oil supplement rich in EPA (180mg) and DHA (120mg) to support heart health, brain function, joint mobility, and vision. Molecularly distilled for purity. Free from heavy metals and contaminants.")
                        .price(new BigDecimal("14.99"))
                        .category(ProductCategory.HEART_HEALTH)
                        .imageUrl("/images/products/omega3.png")
                        .stockQuantity(500)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1-2 softgels daily with meals. For therapeutic doses, consult your healthcare provider.")
                        .ingredients("Fish Oil Concentrate 1000mg (providing EPA 180mg, DHA 120mg), Gelatin, Glycerin, Purified Water, Vitamin E (as Mixed Tocopherols)")
                        .prescriptionRequired(false)
                        .sideEffects("Mild fishy aftertaste or burps. Taking with food minimizes this. High doses may thin blood — consult doctor if on blood thinners or before surgery.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Daniel M.", 5, "No fishy aftertaste at all. Great quality. My cholesterol improved.", null),
                                new Product.Review("Karen T.", 4, "Good heart health supplement. Taking it for years with positive results.", null)
                        ))
                        .build(),

                // ==================== MEDICAL_DEVICES ====================
                Product.builder()
                        .name("Automatic Digital Blood Pressure Monitor")
                        .description("Clinically validated upper arm blood pressure monitor with large LCD display and one-touch operation. Detects irregular heartbeat, stores 120 readings with date/time, and averages last 3 readings. Includes adjustable cuff (22-42cm).")
                        .price(new BigDecimal("39.99"))
                        .category(ProductCategory.MEDICAL_DEVICES)
                        .imageUrl("/images/products/bp-monitor.png")
                        .stockQuantity(150)
                        .manufacturer("MediTech Devices")
                        .dosage("")
                        .ingredients("Electronic medical device. Battery powered (4x AA included). Cuff circumference: 22-42cm. CE certified and clinically validated.")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Richard A.", 5, "Very accurate readings compared to my doctor's office. Easy to use.", null),
                                new Product.Review("Barbara L.", 5, "Excellent monitor for tracking hypertension. The memory function is great.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Digital Infrared Thermometer")
                        .description("Non-contact infrared forehead thermometer for fast, accurate temperature measurement in 1 second. Features fever alarm, 32 memory readings, and switchable °C/°F display. Suitable for all ages — infants, children, and adults.")
                        .price(new BigDecimal("24.99"))
                        .category(ProductCategory.MEDICAL_DEVICES)
                        .imageUrl("/images/products/thermometer.png")
                        .stockQuantity(280)
                        .manufacturer("MediTech Devices")
                        .dosage("")
                        .ingredients("Electronic medical device. Battery powered (2x AAA included). Measurement range: 34.0°C - 43.0°C. Accuracy: ±0.2°C. FDA approved.")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Susan P.", 5, "So quick and easy! Perfect for checking my kids without disturbing them.", null),
                                new Product.Review("Frank W.", 4, "Accurate and reliable. The fever alarm is a useful feature.", null)
                        ))
                        .build(),

                // ==================== FIRST_AID ====================
                Product.builder()
                        .name("Complete First Aid Kit — Family Size")
                        .description("Comprehensive family first aid kit with 160 medical-grade items organized in compartments. Includes bandages, sterile dressings, antiseptic wipes, scissors, tweezers, emergency blanket, CPR mask, and more. Wall-mountable hard case.")
                        .price(new BigDecimal("29.99"))
                        .category(ProductCategory.FIRST_AID)
                        .imageUrl("/images/products/first-aid.png")
                        .stockQuantity(120)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("")
                        .ingredients("Contains: 40x adhesive bandages, 10x sterile gauze pads, 2x elastic bandages, 20x antiseptic wipes, 1x scissors, 1x tweezers, 4x safety pins, 1x CPR face mask, 1x emergency foil blanket, 6x butterfly closures, vinyl gloves, first aid guide booklet")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.7)
                        .reviews(List.of(
                                new Product.Review("Michelle R.", 5, "Very complete kit. Everything you need in one place. Great for home and travel.", null),
                                new Product.Review("Chris D.", 5, "Well-organized and comprehensive. The hard case keeps everything protected.", null)
                        ))
                        .build(),

                // ==================== SKIN_CARE ====================
                Product.builder()
                        .name("Broad Spectrum Sunscreen SPF 50+")
                        .description("Dermatologist-tested broad spectrum sunscreen with UVA/UVB protection. Water-resistant for up to 80 minutes. Lightweight, non-greasy formula enriched with Vitamin E and Aloe Vera. Suitable for all skin types including sensitive skin. 150ml pump bottle.")
                        .price(new BigDecimal("13.49"))
                        .category(ProductCategory.SKIN_CARE)
                        .imageUrl("/images/products/sunscreen.png")
                        .stockQuantity(400)
                        .manufacturer("DermaCare Skincare")
                        .dosage("Apply liberally 15 minutes before sun exposure. Reapply every 2 hours or after swimming, sweating, or towel drying.")
                        .ingredients("Avobenzone 3%, Octocrylene 10%, Homosalate 10%, Octisalate 5%, Vitamin E (Tocopheryl Acetate), Aloe Barbadensis Leaf Extract, Purified Water, Glycerin")
                        .prescriptionRequired(false)
                        .sideEffects("Rarely: skin irritation or allergic rash. Discontinue use if irritation persists. For external use only. Avoid contact with eyes.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Natalie K.", 5, "Best sunscreen I've used. No white cast and doesn't feel greasy at all.", null),
                                new Product.Review("Peter B.", 4, "Good protection, pleasant scent. Works well for my sensitive skin.", null)
                        ))
                        .build(),

                // ==================== DIABETES_CARE ====================
                Product.builder()
                        .name("Blood Glucose Test Strips — 50 Count")
                        .description("High-precision blood glucose test strips compatible with most major glucose meters. Provides accurate results in 5 seconds with tiny 0.5µL blood sample. No coding required. ISO 15197:2013 certified for accuracy.")
                        .price(new BigDecimal("18.99"))
                        .category(ProductCategory.DIABETES_CARE)
                        .imageUrl("/images/products/glucose-strips.png")
                        .stockQuantity(250)
                        .manufacturer("MediTech Devices")
                        .dosage("For in-vitro diagnostic use only. Follow your glucose meter instructions. Use fresh capillary whole blood sample.")
                        .ingredients("Glucose oxidase based electrochemical biosensor strip. Contains glucose oxidase enzyme, mediator, and buffer components. Packaged in moisture-resistant vial with desiccant.")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.2)
                        .reviews(List.of(
                                new Product.Review("Andrew G.", 4, "Accurate and affordable strips. Fast results help me manage my diabetes.", null),
                                new Product.Review("Dorothy M.", 4, "Good quality test strips. Consistent results every time.", null)
                        ))
                        .build(),

                // ==================== PERSONAL_CARE ====================
                Product.builder()
                        .name("Advanced Hand Sanitizer Gel 500ml")
                        .description("Hospital-grade alcohol-based hand sanitizer gel with 70% ethyl alcohol. Kills 99.99% of common germs and bacteria. Contains moisturizing agents to prevent skin dryness. Fast-drying, non-sticky formula with refreshing aloe scent. Pump bottle.")
                        .price(new BigDecimal("8.99"))
                        .category(ProductCategory.PERSONAL_CARE)
                        .imageUrl("/images/products/hand-sanitizer.png")
                        .stockQuantity(1000)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("Apply sufficient amount to cover hands. Rub thoroughly for 20 seconds until dry. No water needed. For external use only.")
                        .ingredients("Ethyl Alcohol 70% v/v, Purified Water, Glycerin, Propylene Glycol, Carbomer, Aminomethyl Propanol, Aloe Barbadensis Leaf Juice, Fragrance")
                        .prescriptionRequired(false)
                        .sideEffects("Avoid contact with eyes, open wounds, or broken skin. Supervise children during use. Flammable — keep away from fire or flame. May cause dryness with very frequent use — moisturize as needed.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Jennifer H.", 5, "Gentle on hands unlike other sanitizers. The pump bottle is very convenient.", null),
                                new Product.Review("Kevin L.", 4, "Good quality sanitizer. Doesn't leave sticky residue.", null)
                        ))
                        .build(),

                // ==================== BABY_CARE (NEW) ====================
                Product.builder()
                        .name("Baby Diaper Rash Cream with Zinc Oxide")
                        .description("Gentle, pediatrician-recommended diaper rash cream with 12% zinc oxide. Creates a protective barrier while soothing irritated skin. Hypoallergenic formula free from parabens, phthalates, and fragrances. Safe for daily use from birth. 100g tube.")
                        .price(new BigDecimal("9.49"))
                        .category(ProductCategory.BABY_CARE)
                        .imageUrl("/images/products/diaper-cream.png")
                        .stockQuantity(380)
                        .manufacturer("DermaCare Skincare")
                        .dosage("Apply liberally to clean, dry skin at every diaper change. Reapply as needed.")
                        .ingredients("Zinc Oxide 12%, Petrolatum, Mineral Oil, Lanolin, Beeswax, Vitamin E, Aloe Barbadensis Leaf Extract, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("Rarely: mild skin irritation or redness. Discontinue if rash worsens. For external use only. Keep out of reach of children.")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Amanda L.", 5, "Cleared my baby's rash overnight. Best cream we've tried. Gentle and effective.", null),
                                new Product.Review("Rachel S.", 4, "Great for sensitive skin. Only downside is it's quite thick and hard to wash off.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Infant Saline Nasal Spray — 30ml")
                        .description("Sterile isotonic saline nasal spray for newborns and infants. Gently moisturizes and clears nasal passages, relieving congestion from colds, allergies, or dry air. Drug-free and preservative-free. Soft nozzle designed for tiny nostrils.")
                        .price(new BigDecimal("5.99"))
                        .category(ProductCategory.BABY_CARE)
                        .imageUrl("/images/products/baby-saline.png")
                        .stockQuantity(450)
                        .manufacturer("PharmaCare International")
                        .dosage("2-3 drops per nostril as needed. Can be used before feeding to ease breathing. Suitable from birth.")
                        .ingredients("Purified Water, Sodium Chloride 0.9% (Isotonic), Disodium Phosphate, Potassium Phosphate")
                        .prescriptionRequired(false)
                        .sideEffects("Generally none. Temporary mild stinging possible. Discontinue use if irritation persists.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Jessica M.", 5, "Helps my baby breathe so much easier at night. Drug-free is a huge plus.", null),
                                new Product.Review("Daniel K.", 4, "Simple but effective. The soft tip is perfect for newborns.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Organic Baby Body Lotion — 250ml")
                        .description("Ultra-mild daily body lotion for baby's delicate skin. Certified organic formula with chamomile, calendula, and shea butter. Hypoallergenic and pH-balanced. Pediatrician and dermatologist tested. Free from mineral oils, SLS, and artificial colors.")
                        .price(new BigDecimal("11.99"))
                        .category(ProductCategory.BABY_CARE)
                        .imageUrl("/images/products/baby-lotion.png")
                        .stockQuantity(320)
                        .manufacturer("DermaCare Skincare")
                        .dosage("Apply daily after bath to slightly damp skin. Massage gently. Suitable from 1 month onward.")
                        .ingredients("Aqua, Butyrospermum Parkii (Shea) Butter, Glycerin, Calendula Officinalis Extract, Chamomilla Recutita Extract, Helianthus Annuus Seed Oil, Glyceryl Stearate, Cetearyl Alcohol, Tocopherol (Vitamin E)")
                        .prescriptionRequired(false)
                        .sideEffects("Very rarely: mild irritation or rash in extremely sensitive skin. Patch test first. For external use only.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Olivia P.", 5, "Love the organic ingredients. Baby's skin is so soft and no irritation at all.", null),
                                new Product.Review("Matthew W.", 4, "Really gentle formula. The pump bottle is very convenient. Pleasant light scent.", null)
                        ))
                        .build(),

                // ==================== ELDERLY_CARE (NEW) ====================
                Product.builder()
                        .name("Glucosamine & Chondroitin Joint Support")
                        .description("Advanced joint health supplement with Glucosamine 1500mg and Chondroitin 1200mg. Supports cartilage repair, reduces joint stiffness, and improves mobility. Ideal for seniors with osteoarthritis or age-related joint discomfort. 180 tablets (3-month supply).")
                        .price(new BigDecimal("24.99"))
                        .category(ProductCategory.ELDERLY_CARE)
                        .imageUrl("/images/products/glucosamine.png")
                        .stockQuantity(280)
                        .manufacturer("NutraLife Wellness")
                        .dosage("2 tablets daily with meals. For best results, take consistently for at least 8 weeks. May split into morning/evening doses.")
                        .ingredients("Glucosamine Sulfate 1500mg, Chondroitin Sulfate 1200mg, MSM (Methylsulfonylmethane) 500mg, Vitamin C 60mg, Manganese 2mg, Cellulose, Magnesium Stearate")
                        .prescriptionRequired(false)
                        .sideEffects("Generally well-tolerated. Mild: bloating, nausea, or heartburn initially. Shellfish-derived — avoid if allergic to shellfish. Diabetics should monitor blood sugar levels.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Margaret T.", 5, "After 2 months, my knee pain is significantly reduced. I can walk stairs again!", null),
                                new Product.Review("Harold B.", 4, "Works well for joint stiffness. Takes about a month to notice the difference.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Easy-Grip Weekly Pill Organizer")
                        .description("Large-capacity weekly pill organizer with 4 daily compartments (morning, noon, evening, bedtime) and easy-open lids designed for arthritic hands. Color-coded and Braille-embossed. Removable daily trays for travel convenience. BPA-free plastic.")
                        .price(new BigDecimal("12.49"))
                        .category(ProductCategory.ELDERLY_CARE)
                        .imageUrl("/images/products/pill-organizer.png")
                        .stockQuantity(220)
                        .manufacturer("MediTech Devices")
                        .dosage("")
                        .ingredients("BPA-free plastic. Dimensions: 22cm x 15cm x 4cm. 28 compartments total (7 days x 4 doses). Dishwasher safe (remove from case first).")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.7)
                        .reviews(List.of(
                                new Product.Review("Eleanor R.", 5, "The large grips are perfect for my arthritis. Never miss a dose now!", null),
                                new Product.Review("William F.", 5, "Well-designed. The Braille dots are thoughtful. My wife uses it daily.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Calcium + Vitamin D3 for Seniors — 500mg")
                        .description("Calcium citrate with Vitamin D3 specially formulated for seniors. Supports bone density, reduces osteoporosis risk, and promotes muscle function. Highly absorbable citrate form — gentler on stomach than calcium carbonate. 240 tablets.")
                        .price(new BigDecimal("16.99"))
                        .category(ProductCategory.ELDERLY_CARE)
                        .imageUrl("/images/products/calcium-d3.png")
                        .stockQuantity(310)
                        .manufacturer("NutraLife Wellness")
                        .dosage("2 tablets daily with food, preferably in divided doses (morning and evening) for optimal absorption.")
                        .ingredients("Calcium Citrate 500mg (elemental calcium), Vitamin D3 400 IU, Magnesium Oxide 100mg, Zinc Gluconate 7.5mg, Microcrystalline Cellulose, Stearic Acid, Croscarmellose Sodium")
                        .prescriptionRequired(false)
                        .sideEffects("Occasional: mild constipation or bloating. Taking with food and plenty of water minimizes this. Safe for long-term use. Consult doctor if you have kidney stones or hypercalcemia.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Dorothy A.", 5, "My doctor recommended this and my bone density scan improved significantly.", null),
                                new Product.Review("George H.", 4, "Easy to swallow and no stomach upset. Good quality calcium for seniors.", null)
                        ))
                        .build(),

                // ==================== SKIN_CARE (MORE) ====================
                Product.builder()
                        .name("Hydrocortisone Cream 1% — 30g")
                        .description("Mild corticosteroid cream for relief of eczema, dermatitis, insect bites, and skin allergies. Reduces redness, itching, and inflammation. Suitable for adults and children over 2 years. Fast-acting formula with moisturizing base.")
                        .price(new BigDecimal("7.99"))
                        .category(ProductCategory.SKIN_CARE)
                        .imageUrl("/images/products/hydrocortisone.png")
                        .stockQuantity(420)
                        .manufacturer("DermaCare Skincare")
                        .dosage("Apply thin layer to affected area 2-3 times daily. Do not use on face for more than 5 days. For external use only.")
                        .ingredients("Hydrocortisone 1% w/w, White Soft Paraffin, Liquid Paraffin, Cetostearyl Alcohol, Phenoxyethanol, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("Prolonged use may cause skin thinning or stretch marks. Avoid broken or infected skin. Do not use near eyes. If no improvement after 7 days, consult doctor.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Sandra L.", 5, "Works within hours on my eczema flare-ups. A staple in my medicine cabinet.", null),
                                new Product.Review("Timothy C.", 4, "Very effective for insect bites. Stops the itching almost immediately.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Scar Reduction Silicone Gel")
                        .description("Medical-grade silicone gel for reducing the appearance of old and new scars including surgical scars, burns, and injury marks. Creates a self-drying, breathable protective layer that flattens and softens scar tissue. Clinically proven. 15g tube.")
                        .price(new BigDecimal("19.99"))
                        .category(ProductCategory.SKIN_CARE)
                        .imageUrl("/images/products/scar-gel.png")
                        .stockQuantity(200)
                        .manufacturer("DermaCare Skincare")
                        .dosage("Apply a very thin layer twice daily. Allow to dry completely before clothing contact. Use for minimum 8 weeks for visible results.")
                        .ingredients("Polydimethylsiloxane, Silicone Dioxide, Polyhydrosiloxane, Vitamin E Acetate, Squalane")
                        .prescriptionRequired(false)
                        .sideEffects("Rarely: temporary redness or itching at application site. Not for open wounds. Wait until wound is fully closed before use.")
                        .ratings(4.2)
                        .reviews(List.of(
                                new Product.Review("Rebecca M.", 5, "My C-section scar is barely visible after 3 months. Amazing product.", null),
                                new Product.Review("Phil J.", 4, "Good results on old surgical scar. Takes patience but it works.", null)
                        ))
                        .build(),

                // ==================== RESPIRATORY (MORE) ====================
                Product.builder()
                        .name("Menthol Chest Rub — 100g")
                        .description("Soothing menthol and eucalyptus chest rub for relief from cough, cold, and nasal congestion. Warms the chest and helps clear airways for easier breathing. Suitable for adults and children over 2 years. Non-greasy formula.")
                        .price(new BigDecimal("6.99"))
                        .category(ProductCategory.RESPIRATORY)
                        .imageUrl("/images/products/menthol-gel.png")
                        .stockQuantity(550)
                        .manufacturer("PharmaCare International")
                        .dosage("Massage gently onto chest and back 2-3 times daily. May also apply to soles of feet at bedtime. Cover with warm clothing after application.")
                        .ingredients("Menthol 2.8%, Eucalyptus Oil 1.5%, Camphor 5.3%, White Soft Paraffin, Light Liquid Paraffin, Thymol")
                        .prescriptionRequired(false)
                        .sideEffects("For external use only. Do not apply to nostrils or open wounds. May cause skin sensitivity in some individuals. Avoid contact with eyes.")
                        .ratings(4.1)
                        .reviews(List.of(
                                new Product.Review("Brian K.", 5, "Old-school remedy that still works the best. Helps me sleep when I'm congested.", null),
                                new Product.Review("Lisa D.", 4, "Very effective for chesty coughs. The menthol scent is strong but comforting.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Herbal Cough Syrup with Honey & Ivy")
                        .description("Natural cough syrup combining honey, ivy leaf extract, and marshmallow root to soothe dry and productive coughs. Coats and protects irritated throat membranes. Non-drowsy formula suitable for adults and children over 6 years. 200ml bottle.")
                        .price(new BigDecimal("10.49"))
                        .category(ProductCategory.RESPIRATORY)
                        .imageUrl("/images/products/cough-syrup.png")
                        .stockQuantity(380)
                        .manufacturer("NutraLife Wellness")
                        .dosage("Adults: 10ml 3 times daily. Children 6-12: 5ml 3 times daily. Shake well before use. Do not exceed recommended dose.")
                        .ingredients("Hedera Helix (Ivy Leaf) Extract, Honey, Althaea Officinalis (Marshmallow) Root Extract, Glycerol, Citric Acid, Potassium Sorbate, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("Generally well-tolerated. Rarely: mild gastrointestinal upset. Contains honey — not suitable for infants under 12 months. Diabetics should account for sugar content.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Claire H.", 5, "Finally a natural cough remedy that actually works. Tastes pleasant too.", null),
                                new Product.Review("Martin G.", 4, "Throat feels soothed after using. Good alternative to chemical cough medicines.", null)
                        ))
                        .build(),

                // ==================== HEART_HEALTH (MORE) ====================
                Product.builder()
                        .name("Coenzyme Q10 200mg Softgels")
                        .description("High-absorption CoQ10 (Ubiquinone) for cardiovascular health and cellular energy production. Supports healthy blood pressure, heart muscle function, and reduces statin-related muscle fatigue. 120 softgels (4-month supply).")
                        .price(new BigDecimal("21.99"))
                        .category(ProductCategory.HEART_HEALTH)
                        .imageUrl("/images/products/coq10.png")
                        .stockQuantity(270)
                        .manufacturer("NutraLife Wellness")
                        .dosage("1 softgel daily with a meal containing fat for optimal absorption. Can be taken long-term.")
                        .ingredients("Coenzyme Q10 (Ubiquinone) 200mg, Soybean Oil, Gelatin, Glycerin, Purified Water, Beeswax, Soy Lecithin, Natural Vitamin E")
                        .prescriptionRequired(false)
                        .sideEffects("Rarely: mild insomnia, digestive upset, or headache. May interact with blood-thinning medications — consult doctor if on warfarin.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Richard W.", 5, "Great for energy levels and my blood pressure improved after starting this.", null),
                                new Product.Review("Susan M.", 4, "Helped with the muscle pain I had from statins. Good quality CoQ10.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Low-Dose Aspirin 81mg Enteric-Coated")
                        .description("Low-dose aspirin for cardiovascular protection. Enteric-coated tablets protect the stomach while providing antiplatelet benefits. Used for prevention of heart attack and stroke in at-risk adults. 180 tablets (6-month supply).")
                        .price(new BigDecimal("8.49"))
                        .category(ProductCategory.HEART_HEALTH)
                        .imageUrl("/images/products/aspirin-low.png")
                        .stockQuantity(400)
                        .manufacturer("MediPharm Laboratories")
                        .dosage("1 tablet (81mg) daily with water after food, or as directed by your physician. Do not crush or chew.")
                        .ingredients("Acetylsalicylic Acid 81mg, Methacrylic Acid Copolymer (enteric coating), Triethyl Citrate, Talc, Lactose, Corn Starch")
                        .prescriptionRequired(true)
                        .sideEffects("Increased bleeding risk, stomach irritation. Do not use if: allergic to NSAIDs, have active gastric ulcers, bleeding disorders, or are in the third trimester of pregnancy. Children under 16 should not use (Reye's syndrome risk).")
                        .ratings(4.1)
                        .reviews(List.of(
                                new Product.Review("Kenneth R.", 4, "Doctor recommended for heart health. The enteric coating prevents stomach issues.", null),
                                new Product.Review("Marie P.", 4, "Good low-dose aspirin. Easy to swallow and great for long-term use.", null)
                        ))
                        .build(),

                // ==================== DIABETES_CARE (MORE) ====================
                Product.builder()
                        .name("Blood Glucose Monitor Starter Kit")
                        .description("Complete blood glucose monitoring system with meter, 25 test strips, 25 lancets, lancing device, and carrying case. Large backlit display, 500-reading memory, 7/14/30-day averages, and meal markers. No coding required. ISO 15197:2013 certified.")
                        .price(new BigDecimal("34.99"))
                        .category(ProductCategory.DIABETES_CARE)
                        .imageUrl("/images/products/glucose-meter.png")
                        .stockQuantity(180)
                        .manufacturer("MediTech Devices")
                        .dosage("Wash hands before testing. Insert test strip into meter, prick finger with lancing device, apply blood drop to strip. Results in 5 seconds.")
                        .ingredients("Kit includes: Blood glucose meter, 25 test strips, 25 sterile lancets (30G), adjustable lancing device, carrying case, 2x AAA batteries, quick start guide, logbook")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Arthur B.", 5, "Complete kit with everything needed to start. Readings match my lab results.", null),
                                new Product.Review("Helen S.", 5, "Very easy to use. The large display is great for seniors. Highly recommended.", null)
                        ))
                        .build(),

                // ==================== PAIN_RELIEF (MORE) ====================
                Product.builder()
                        .name("Aspirin 300mg Soluble Tablets")
                        .description("Fast-acting soluble aspirin for relief from mild to moderate pain including headache, migraine, toothache, neuralgia, sore throat, and period pain. Also reduces fever. Dissolve in water for rapid absorption and gentle stomach action. 32 tablets.")
                        .price(new BigDecimal("5.49"))
                        .category(ProductCategory.PAIN_RELIEF)
                        .imageUrl("/images/products/aspirin-low.png")
                        .stockQuantity(480)
                        .manufacturer("MediPharm Laboratories")
                        .dosage("Adults: dissolve 1-2 tablets in water every 4-6 hours. Maximum 12 tablets in 24 hours. Always take dissolved — never swallow whole.")
                        .ingredients("Acetylsalicylic Acid 300mg, Sodium Carbonate, Citric Acid Anhydrous, Sodium Saccharin")
                        .prescriptionRequired(false)
                        .sideEffects("Gastric irritation possible. Not for children under 16 (Reye's syndrome risk). Avoid if: have asthma, stomach ulcers, are pregnant, or take blood thinners. Do not take with other NSAIDs.")
                        .ratings(4.0)
                        .reviews(List.of(
                                new Product.Review("Linda M.", 4, "Works faster than regular aspirin. The dissolvable form is much gentler on my stomach.", null),
                                new Product.Review("Steve K.", 4, "Good for quick headache relief. The taste isn't great but it works well.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Cooling Menthol Pain Relief Gel — 100g")
                        .description("Fast-acting topical analgesic gel with menthol for relief of muscular aches, strains, sprains, back pain, and arthritis. Cooling formula penetrates deep for targeted pain relief without oral medication. Non-greasy, non-staining. Suitable for adults and children over 12.")
                        .price(new BigDecimal("9.99"))
                        .category(ProductCategory.PAIN_RELIEF)
                        .imageUrl("/images/products/menthol-gel.png")
                        .stockQuantity(400)
                        .manufacturer("PharmaCare International")
                        .dosage("Apply generously to affected area 3-4 times daily. Massage gently until absorbed. Wash hands after application.")
                        .ingredients("Menthol 3% w/w, Camphor 1% w/w, Eucalyptus Oil 2%, Carbomer, Triethanolamine, Isopropyl Alcohol, Glycerin, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("For external use only. Avoid contact with eyes, mucous membranes, or broken skin. May cause temporary cooling or tingling sensation. Discontinue if skin irritation occurs.")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Mark W.", 5, "Incredible relief for my lower back pain after sports. Instant cooling sensation.", null),
                                new Product.Review("Judy L.", 4, "Great for shoulder muscle tension. Much better than taking pills for muscle pain.", null)
                        ))
                        .build(),

                // ==================== WELLNESS (MORE) ====================
                Product.builder()
                        .name("Magnesium Glycinate 400mg — 180 Capsules")
                        .description("Premium chelated magnesium glycinate for optimal absorption and gentle digestion. Supports muscle relaxation, nervous system health, sleep quality, and energy metabolism. Non-buffered formula — no digestive discomfort. 3-month supply.")
                        .price(new BigDecimal("18.99"))
                        .category(ProductCategory.WELLNESS)
                        .imageUrl("/images/products/magnesium.png")
                        .stockQuantity(420)
                        .manufacturer("NutraLife Wellness")
                        .dosage("2 capsules daily with food, preferably in the evening to support restful sleep. Can be split into morning and evening doses.")
                        .ingredients("Magnesium (as Magnesium Glycinate Chelate) 400mg, Vegetable Cellulose Capsule, Rice Flour, Magnesium Stearate (vegetable source)")
                        .prescriptionRequired(false)
                        .sideEffects("Generally very well-tolerated. Unlike magnesium oxide, glycinate form rarely causes digestive issues. Excessive intake may cause loose stools. People with kidney issues should consult doctor.")
                        .ratings(4.7)
                        .reviews(List.of(
                                new Product.Review("Nancy F.", 5, "Best magnesium I've taken. Helps tremendously with sleep and leg cramps at night.", null),
                                new Product.Review("Alan T.", 5, "No digestive issues unlike other forms. Feel calmer and sleep deeper.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Melatonin 5mg Natural Sleep Aid")
                        .description("Fast-dissolve melatonin tablets for occasional sleeplessness, jet lag, and shift work sleep disorder. Helps regulate the body's natural sleep-wake cycle. Nighttime berry flavor. 120 quick-dissolve tablets.")
                        .price(new BigDecimal("12.99"))
                        .category(ProductCategory.WELLNESS)
                        .imageUrl("/images/products/melatonin.png")
                        .stockQuantity(500)
                        .manufacturer("NutraLife Wellness")
                        .dosage("Place 1 tablet under tongue 30 minutes before bedtime. Allow to dissolve completely. Do not exceed recommended dose.")
                        .ingredients("Melatonin 5mg, Mannitol, Crospovidone, Natural Berry Flavor, Stevia Extract, Magnesium Stearate")
                        .prescriptionRequired(false)
                        .sideEffects("May cause drowsiness — do not drive or operate machinery after taking. Occasional: morning grogginess, vivid dreams, or mild headache. Not intended for long-term daily use. Not recommended for pregnant or nursing women.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Diana M.", 5, "Finally sleeping through the night. No groggy feeling the next morning either.", null),
                                new Product.Review("Roger P.", 4, "Great for travel jet lag. The dissolvable format is super convenient.", null)
                        ))
                        .build(),

                // ==================== PERSONAL_CARE (MORE) ====================
                Product.builder()
                        .name("Sensitive Toothpaste with Fluoride — 100ml")
                        .description("Dentist-recommended sensitive toothpaste with 1450ppm fluoride for cavity protection and potassium nitrate to relieve tooth sensitivity. Low-abrasion formula safe for enamel. Provides 24/7 sensitivity protection with twice daily brushing.")
                        .price(new BigDecimal("7.99"))
                        .category(ProductCategory.PERSONAL_CARE)
                        .imageUrl("/images/products/toothpaste.png")
                        .stockQuantity(600)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("Brush teeth thoroughly twice daily for 2 minutes. Do not swallow. Spit out after brushing. Suitable for adults and children over 12 years.")
                        .ingredients("Sodium Fluoride 0.315% (1450ppm Fluoride), Potassium Nitrate 5%, Hydrated Silica, Sorbitol, Glycerin, Sodium Lauryl Sulfate, Xanthan Gum, Titanium Dioxide, Aroma")
                        .prescriptionRequired(false)
                        .sideEffects("Generally none with normal use. Excessive ingestion of fluoride may cause dental fluorosis in children. If irritation occurs, discontinue use.")
                        .ratings(4.2)
                        .reviews(List.of(
                                new Product.Review("Carol B.", 5, "My sensitivity improved dramatically after just 2 weeks. Best toothpaste for sensitive teeth.", null),
                                new Product.Review("Derek J.", 4, "Good relief from cold sensitivity. The mint flavor is pleasant without burning.", null)
                        ))
                        .build(),

                // ==================== FIRST_AID (MORE) ====================
                Product.builder()
                        .name("Adhesive Bandages Variety Pack — 100 Count")
                        .description("Assorted flexible fabric bandages in multiple sizes for cuts, scrapes, and minor wounds. Strong adhesive with non-stick pad. Breathable, waterproof material. Includes fingertip, knuckle, and standard shapes. Latex-free and hypoallergenic.")
                        .price(new BigDecimal("7.99"))
                        .category(ProductCategory.FIRST_AID)
                        .imageUrl("/images/products/first-aid.png")
                        .stockQuantity(750)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("Clean wound thoroughly, dry skin, apply bandage. Change daily or when wet/dirty.")
                        .ingredients("Flexible woven fabric, hypoallergenic acrylic adhesive, non-stick absorbent pad")
                        .prescriptionRequired(false)
                        .sideEffects("Rarely: skin irritation from adhesive. Discontinue if rash develops.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Tom H.", 5, "Great variety pack. Fabric bandages stay on much better than plastic ones.", null),
                                new Product.Review("Anna W.", 4, "Perfect for a family with kids. Every size you could need.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Antiseptic Wound Spray — 100ml")
                        .description("No-sting antiseptic spray for cleaning minor cuts, scrapes, burns, and abrasions. Kills 99.9% of germs without alcohol. Non-irritating formula with moisturizers. Can be used on face and body.")
                        .price(new BigDecimal("9.49"))
                        .category(ProductCategory.FIRST_AID)
                        .imageUrl("/images/products/antiseptic-spray.png")
                        .stockQuantity(380)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("Spray directly onto affected area from 10-15cm distance. Allow to air dry. No need to rinse. Use 2-3 times daily.")
                        .ingredients("Benzalkonium Chloride 0.13%, Aloe Barbadensis Leaf Extract, Glycerin, Polysorbate 20, Citric Acid, Purified Water")
                        .prescriptionRequired(false)
                        .sideEffects("For external use only. Avoid contact with eyes. Discontinue if irritation occurs.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Karen S.", 5, "No sting at all! My kids don't cry when I use this. Much better than alcohol.", null),
                                new Product.Review("Robert D.", 4, "Effective and gentle. Great for everyday cuts and scrapes.", null)
                        ))
                        .build(),

                // ==================== MEDICAL_DEVICES (MORE) ====================
                Product.builder()
                        .name("Pulse Oximeter Fingertip Monitor")
                        .description("Portable SpO2 and pulse rate monitor with OLED display. Measures blood oxygen saturation and pulse rate in 8 seconds. Suitable for sports, aviation, and home health monitoring. FDA cleared.")
                        .price(new BigDecimal("29.99"))
                        .category(ProductCategory.MEDICAL_DEVICES)
                        .imageUrl("/images/products/oximeter.png")
                        .stockQuantity(210)
                        .manufacturer("MediTech Devices")
                        .dosage("Insert finger into device, press button. Results display in 8 seconds. Auto-powers off after 10 seconds.")
                        .ingredients("Electronic medical device. ABS plastic shell. OLED display. 2x AAA batteries (included). SpO2 70-99%, Pulse 30-250 bpm. Accuracy: ±2%.")
                        .prescriptionRequired(false)
                        .sideEffects("Remove nail polish for accurate readings. Cold hands may affect accuracy.")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Frank G.", 5, "Accurate and easy to use. Great for monitoring my COPD at home.", null),
                                new Product.Review("Patricia N.", 5, "Compact and reliable. Bought for my husband post-surgery recovery.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Digital Personal Scale with BMI")
                        .description("Precision digital bathroom scale with tempered glass platform. Measures up to 180kg/400lb with 0.1kg accuracy. Auto-calibrating step-on technology. Large backlit LCD shows weight and BMI. Sleek modern design.")
                        .price(new BigDecimal("22.99"))
                        .category(ProductCategory.MEDICAL_DEVICES)
                        .imageUrl("/images/products/scale.png")
                        .stockQuantity(190)
                        .manufacturer("MediTech Devices")
                        .dosage("Place on hard flat surface. Step on with bare feet. Stand still until reading stabilizes.")
                        .ingredients("Tempered glass platform (6mm), ABS base, 4 high-precision sensors, LCD display. 2x CR2032 batteries (included). Capacity: 5-180kg.")
                        .prescriptionRequired(false)
                        .sideEffects("")
                        .ratings(4.3)
                        .reviews(List.of(
                                new Product.Review("Laura M.", 5, "Sleek design and accurate readings. The BMI display is a nice bonus.", null),
                                new Product.Review("James C.", 4, "Good quality scale. Easy to use and matches my doctor's readings.", null)
                        ))
                        .build(),

                // ==================== OTHER ====================
                Product.builder()
                        .name("Compression Socks — 3 Pairs")
                        .description("Graduated compression socks (15-20 mmHg) for improved circulation, reduced leg fatigue, and varicose vein prevention. Ideal for travel, pregnancy, long hours standing/sitting, and post-surgery recovery. Breathable cotton blend. Unisex sizing.")
                        .price(new BigDecimal("16.99"))
                        .category(ProductCategory.OTHER)
                        .imageUrl("/images/products/compression-socks.jpg")
                        .stockQuantity(340)
                        .manufacturer("MediTech Devices")
                        .dosage("Wear during waking hours. Remove before sleeping. Machine washable. Replace every 3-6 months.")
                        .ingredients("Cotton 65%, Nylon 25%, Spandex 10%. Graduated compression: 15-20 mmHg at ankle.")
                        .prescriptionRequired(false)
                        .sideEffects("Ensure proper sizing. Consult doctor if you have peripheral neuropathy or arterial disease.")
                        .ratings(4.4)
                        .reviews(List.of(
                                new Product.Review("Megan R.", 5, "Lifesaver during my pregnancy! Reduced swelling dramatically. Very comfortable.", null),
                                new Product.Review("David L.", 5, "Great for long flights. My legs feel fresh even after 12 hours of travel.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Reusable Hot & Cold Therapy Pack")
                        .description("Versatile gel pack for hot or cold therapy. Microwaveable for heat therapy (muscle relaxation, stiffness) or freezable for cold therapy (swelling, inflammation, injuries). Flexible even when frozen. Includes soft fabric sleeve with velcro strap. 28cm x 15cm.")
                        .price(new BigDecimal("11.49"))
                        .category(ProductCategory.OTHER)
                        .imageUrl("/images/products/hot-cold-pack.png")
                        .stockQuantity(290)
                        .manufacturer("SafeGuard Medical Supplies")
                        .dosage("Cold: freeze 2+ hours, apply 15-20 min. Heat: microwave 30 sec, knead, test temp, apply 15-20 min. Always use sleeve.")
                        .ingredients("Non-toxic silica gel, water, propylene glycol. Fabric sleeve: cotton-polyester blend with velcro.")
                        .prescriptionRequired(false)
                        .sideEffects("Do not apply directly to skin without sleeve. Do not overheat. Check temp before applying to children or elderly.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Sarah J.", 5, "Stays flexible even when frozen. The sleeve is soft and the strap holds it in place.", null),
                                new Product.Review("Paul T.", 4, "Great for post-workout recovery. Both hot and cold work perfectly.", null)
                        ))
                        .build(),

                // ==================== Rx PRESCRIPTION PRODUCTS ====================
                Product.builder()
                        .name("Amoxicillin 500mg Antibiotic")
                        .description("Broad-spectrum penicillin antibiotic for bacterial infections. Treats pneumonia, bronchitis, ear infections, strep throat, and skin infections. Take with food for best absorption.")
                        .price(new BigDecimal("28.99"))
                        .category(ProductCategory.DIGESTIVE_HEALTH)
                        .imageUrl("/images/products/antibiotic.png")
                        .stockQuantity(75)
                        .manufacturer("PharmaCare Laboratories")
                        .dosage("Adults: 1 tablet (500mg) every 8 hours for 7-10 days. Children: 25-50mg/kg/day divided in 3 doses. Take with food. Complete full course even if symptoms improve.")
                        .ingredients("Active: Amoxicillin 500mg (as amoxicillin trihydrate). Inactive: Microcrystalline cellulose, magnesium stearate, croscarmellose sodium, gelatin (capsule).")
                        .prescriptionRequired(true)
                        .sideEffects("May cause diarrhea, nausea, rash, or yeast infection. Seek medical help if allergic reaction (swelling, difficulty breathing). Not for use in people with penicillin allergy.")
                        .ratings(4.7)
                        .reviews(List.of(
                                new Product.Review("Jessica M.", 5, "Cleared up my sinus infection in 3 days. No side effects.", null),
                                new Product.Review("Robert K.", 4, "Works well but gave me mild diarrhea. Took with food and it helped.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Metformin 500mg — Diabetes Management")
                        .description("First-line oral hypoglycemic for type 2 diabetes. Improves insulin sensitivity and reduces glucose production in the liver. Helps maintain healthy blood sugar levels.")
                        .price(new BigDecimal("18.49"))
                        .category(ProductCategory.DIABETES_CARE)
                        .imageUrl("/images/products/metformin.png")
                        .stockQuantity(120)
                        .manufacturer("GlucaPharma Inc.")
                        .dosage("Initial: 1 tablet (500mg) twice daily with meals. May increase to 1000mg twice daily based on blood glucose levels. Maximum: 2000-3000mg/day.")
                        .ingredients("Active: Metformin hydrochloride 500mg. Inactive: Hypromellose, magnesium stearate, polyethylene glycol, titanium dioxide (tablet coating).")
                        .prescriptionRequired(true)
                        .sideEffects("Most common: GI upset, metallic taste, vitamin B12 deficiency risk. Rare but serious: lactic acidosis (weakness, trouble breathing, irregular heartbeat).")
                        .ratings(4.6)
                        .reviews(List.of(
                                new Product.Review("Linda P.", 5, "Been on it for 5 years. A1C went from 8.2 to 6.5. No major side effects.", null),
                                new Product.Review("David S.", 4, "Works but the taste is terrible. I chew a mint right after taking it.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Lisinopril 10mg — Blood Pressure Control")
                        .description("ACE inhibitor for managing high blood pressure, heart failure, and post-heart attack recovery. Widens blood vessels to lower blood pressure and reduce strain on the heart.")
                        .price(new BigDecimal("15.99"))
                        .category(ProductCategory.HEART_HEALTH)
                        .imageUrl("/images/products/lisinopril.png")
                        .stockQuantity(150)
                        .manufacturer("CardioMed Pharmaceuticals")
                        .dosage("High BP: start 10mg once daily, increase to 20-40mg. Heart failure: 5mg twice daily, titrate to 20-40mg/day. Take at same time daily.")
                        .ingredients("Active: Lisinopril 10mg. Inactive: Magnesium stearate, mannitol, calcium hydrogen phosphate, talc.")
                        .prescriptionRequired(true)
                        .sideEffects("Dry persistent cough (most common), dizziness, headache, fatigue, elevated potassium. Report swelling of face/lips/tongue immediately.")
                        .ratings(4.5)
                        .reviews(List.of(
                                new Product.Review("Margaret H.", 5, "Lowered my BP from 160/95 to 128/82 in 2 months. The cough went away after switching brands.", null),
                                new Product.Review("Thomas R.", 4, "Works great but the cough is real. Worth it for the BP control.", null)
                        ))
                        .build(),

                Product.builder()
                        .name("Cetirizine Hydrochloride 10mg Rx")
                        .description("Second-generation antihistamine for severe allergic reactions, chronic urticaria, and perennial allergic rhinitis. Non-drowsy formula with 24-hour relief.")
                        .price(new BigDecimal("22.79"))
                        .category(ProductCategory.RESPIRATORY)
                        .imageUrl("/images/products/cetirizine-rx.png")
                        .stockQuantity(200)
                        .manufacturer("AllerGuard Pharmaceuticals")
                        .dosage("Adults and children 6+: 1 tablet (10mg) once daily. May increase to 20mg/day if needed for severe allergies. Renal impairment: reduce to 5mg daily.")
                        .ingredients("Active: Cetirizine hydrochloride 10mg. Inactive: Lactose monohydrate, microcrystalline cellulose, colloidal silicon dioxide, magnesium stearate.")
                        .prescriptionRequired(true)
                        .sideEffects("Drowsiness (uncommon), dry mouth, fatigue, headache, sore throat. Avoid alcohol and other CNS depressants.")
                        .ratings(4.8)
                        .reviews(List.of(
                                new Product.Review("Anna L.", 5, "My seasonal allergies are completely gone. No drowsiness at all — better than OTC.", null),
                                new Product.Review("Michael B.", 5, "Been taking this daily for 3 years. Zero side effects, full 24-hour relief.", null)
                        ))
                        .build()

        );
    }
}
