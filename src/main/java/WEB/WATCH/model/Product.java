package WEB.WATCH.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;
    private String material;
    private String brand;
    private String warranty;
    
    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm không được âm")
    private Double price;
    
    @Min(value = 0, message = "Giá giảm không được âm")
    private Double discountPrice;
    private String description;
    
    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock;
    private String image;
    @Column(name = "is_featured")
    private Boolean featured;

    @ManyToOne
    @JoinColumn(name="category_id")
    private Category category = new Category();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

}
