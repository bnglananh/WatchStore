package WEB.WATCH.controller;

import WEB.WATCH.model.Product;
import WEB.WATCH.service.CategoryService;
import WEB.WATCH.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;


    //Danh sách sản phẩm
    @GetMapping
    public String listProducts(Model model){

        model.addAttribute(
                "products",
                productService.getAllProducts()
        );

        return "admin/products/list";
    }



    //Form thêm sản phẩm
    @GetMapping("/add")
    public String showAddForm(Model model){

        model.addAttribute(
                "product",
                new Product()
        );

        model.addAttribute(
                "categories",
                categoryService.getAllCategories()
        );

        return "admin/products/add";
    }


    //Lưu sản phẩm
    @PostMapping("/save")
    public String saveProduct(
            @Valid @ModelAttribute Product product,
            BindingResult bindingResult,
            @RequestParam(value="imageFiles", required=false) MultipartFile[] imageFiles,
            Model model
    ) throws Exception {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return product.getId() != null ? "admin/products/edit" : "admin/products/add";
        }

        productService.saveProduct(product, imageFiles);
        return "redirect:/admin/products";
    }


    //Form cập nhật
    @GetMapping("/edit/{id}")
    public String editProduct(
            @PathVariable Long id,
            Model model){

        model.addAttribute(
                "product",
                productService.getProductById(id)
        );

        model.addAttribute(
                "categories",
                categoryService.getAllCategories()
        );

        return "admin/products/edit";
    }


    //Xóa
    @PostMapping("/delete/{id}")
    public String deleteProduct(
            @PathVariable Long id){

        productService.deleteProduct(id);

        return "redirect:/admin/products";
    }

}