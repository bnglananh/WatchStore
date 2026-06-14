package WEB.WATCH.service;

import WEB.WATCH.model.Category;
import WEB.WATCH.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;


    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }


    public Category getCategoryById(Long id){
        return categoryRepository
                .findById(id)
                .orElse(null);
    }


    public Category saveCategory(
            Category category){

        return categoryRepository
                .save(category);
    }


    public Category updateCategory(
            Long id,
            Category category){

        Category oldCategory=
                categoryRepository
                        .findById(id)
                        .orElseThrow();

        oldCategory.setName(
                category.getName()
        );

        return categoryRepository
                .save(oldCategory);
    }

    public void deleteCategory(Long id){
        categoryRepository.deleteById(id);
    }

}