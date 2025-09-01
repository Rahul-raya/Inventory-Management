package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Category;
import java.util.List;

public interface CategoryService {
    Category saveCategory(Category category);
    Category getCategoryById(Long id);
    List<Category> getAllCategories();
    void deleteCategory(Long id);
    Category updateCategory(Long id, Category category);
}
