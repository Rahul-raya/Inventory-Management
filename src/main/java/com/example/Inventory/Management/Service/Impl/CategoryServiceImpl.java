package com.example.Inventory.Management.Service.Impl;

import com.example.Inventory.Management.Entity.Category;
import com.example.Inventory.Management.Exception.CategoryNotFoundException;
import com.example.Inventory.Management.Repository.CategoryRepository;
import com.example.Inventory.Management.Service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        
        if (!category.getProducts().isEmpty()) {
            throw new RuntimeException("Cannot delete category that has associated products");
        }
        
        categoryRepository.deleteById(id);
    }

    @Override
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = getCategoryById(id);
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        return categoryRepository.save(existingCategory);
    }
}