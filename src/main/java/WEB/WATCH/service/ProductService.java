package WEB.WATCH.service;

import WEB.WATCH.model.Product;
import WEB.WATCH.model.ProductImage;
import WEB.WATCH.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;

import java.util.List;

@Service
@Transactional
public class ProductService {

        @Autowired
        private ProductRepository productRepository;

        public List<Product> getAllProducts() {
                return productRepository.findAll();
        }

        public Product getProductById(Long id) {
                Product product = productRepository.findById(id).orElse(null);
                if (product != null) {
                    // Force initialization of lazy collection
                    product.getImages().size();
                }
                return product;
        }

        public Product saveProduct(
                        Product product,
                        MultipartFile[] imageFiles)
                        throws IOException {

                Product productToSave;
                if (product.getId() != null) {
                        productToSave = productRepository.findById(product.getId())
                                        .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));
                        
                        // Cập nhật các trường thông tin cơ bản từ form
                        productToSave.setName(product.getName());
                        productToSave.setMaterial(product.getMaterial());
                        productToSave.setBrand(product.getBrand());
                        productToSave.setWarranty(product.getWarranty());
                        productToSave.setPrice(product.getPrice());
                        productToSave.setDiscountPrice(product.getDiscountPrice() != null ? product.getDiscountPrice() : 0.0);
                        productToSave.setDescription(product.getDescription());
                        productToSave.setStock(product.getStock());
                        productToSave.setFeatured(product.getFeatured() != null ? product.getFeatured() : false);
                        productToSave.setCategory(product.getCategory());
                        
                        // Trường images được giữ nguyên (lazy loaded) và sẽ được thêm vào ở dưới nếu có upload mới
                } else {
                        productToSave = product;
                        if (productToSave.getDiscountPrice() == null) {
                                productToSave.setDiscountPrice(0.0);
                        }
                        if (productToSave.getFeatured() == null) {
                                productToSave.setFeatured(false);
                        }
                }

                if (imageFiles != null && imageFiles.length > 0 && !imageFiles[0].isEmpty()) {
                        File dir = new File("uploads");
                        if (!dir.exists()) {
                                dir.mkdirs();
                        }

                        for (int i = 0; i < imageFiles.length; i++) {
                                MultipartFile imageFile = imageFiles[i];
                                if (!imageFile.isEmpty()) {
                                        String fileName = System.currentTimeMillis()
                                                        + "_" + (i + 1) + "_" +
                                                        imageFile.getOriginalFilename();

                                        File dest = new File(
                                                        dir.getAbsolutePath(),
                                                        fileName);

                                        imageFile.transferTo(dest);

                                        // Cập nhật ảnh chính nếu có upload mới (hoặc là sản phẩm mới)
                                        if (i == 0) {
                                                productToSave.setImage(fileName);
                                        }

                                        // Thêm vào danh sách ảnh chi tiết
                                        ProductImage productImage = new ProductImage();
                                        productImage.setUrl(fileName);
                                        productImage.setProduct(productToSave);
                                        if (productToSave.getImages() == null) {
                                                productToSave.setImages(new java.util.ArrayList<>());
                                        }
                                        productToSave.getImages().add(productImage);
                                }
                        }
                }

                return productRepository.save(productToSave);
        }

        public Product updateProduct(
                        Long id,
                        Product product) {

                Product oldProduct = productRepository
                                .findById(id)
                                .orElseThrow();

                oldProduct.setName(
                                product.getName());

                oldProduct.setMaterial(
                                product.getMaterial());

                oldProduct.setBrand(
                                product.getBrand());

                oldProduct.setWarranty(
                                product.getWarranty());

                oldProduct.setPrice(
                                product.getPrice());

                oldProduct.setDiscountPrice(
                                product.getDiscountPrice() != null ? product.getDiscountPrice() : 0.0);

                oldProduct.setDescription(
                                product.getDescription());

                oldProduct.setStock(
                                product.getStock());

                oldProduct.setFeatured(
                                product.getFeatured() != null ? product.getFeatured() : false);

                oldProduct.setCategory(
                                product.getCategory());

                return productRepository
                                .save(oldProduct);
        }

        public void deleteProduct(
                        Long id) {

                productRepository.deleteById(id);

        }

        public List<Product> getRelatedProducts(Long productId, int limit) {
                Product current = productRepository.findById(productId).orElse(null);
                if (current == null) return java.util.Collections.emptyList();

                List<Product> related = new java.util.ArrayList<>();

                // 1. Cùng danh mục
                if (current.getCategory() != null) {
                        related.addAll(productRepository.findByCategoryIdAndIdNot(
                                current.getCategory().getId(), productId));
                }

                // 2. Nếu chưa đủ, bổ sung cùng thương hiệu
                if (related.size() < limit && current.getBrand() != null && !current.getBrand().isBlank()) {
                        List<Product> byBrand = productRepository.findByBrandAndIdNot(current.getBrand(), productId);
                        for (Product p : byBrand) {
                                if (!related.contains(p)) related.add(p);
                        }
                }

                return related.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        }

}