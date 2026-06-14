package WEB.WATCH.controller;

import WEB.WATCH.model.Category;
import WEB.WATCH.service.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    //Danh sách
    @GetMapping
    public String listCategories(
            Model model){

        model.addAttribute(
                "categories",
                categoryService.getAllCategories()
        );

        return "admin/categories/list";
    }



    //Form thêm
    @GetMapping("/add")
    public String addCategoryForm(
            Model model){

        model.addAttribute(
                "category",
                new Category()
        );

        return "admin/categories/add";
    }


    //Lưu
    @PostMapping("/save")
    public String saveCategory(
            @ModelAttribute Category category){

        categoryService.saveCategory(
                category
        );

        return "redirect:/admin/categories";
    }


    //Form sửa
    @GetMapping("/edit/{id}")
    public String editCategory(
            @PathVariable Long id,
            Model model){

        model.addAttribute(
                "category",
                categoryService.getCategoryById(id)
        );

        return "admin/categories/edit";
    }


    //Xóa
    @PostMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Long id){

        categoryService.deleteCategory(
                id
        );

        return "redirect:/admin/categories";
    }

}