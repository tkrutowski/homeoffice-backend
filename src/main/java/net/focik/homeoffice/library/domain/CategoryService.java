package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.exception.CategoryAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.CategoryNotFoundException;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.port.secondary.CategoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category addCategory(Category category) {
        Optional<Category> optionalCategory = categoryRepository.findByName(category.getName());
        if (optionalCategory.isPresent()) {
            throw new CategoryAlreadyExistException(category);
        }
        return categoryRepository.add(category);
    }

    public Category editCategory(Category categoryToEdit, Integer id) {
        Optional<Category> categoryById = categoryRepository.findById(id);
        if (categoryById.isEmpty()) {
            throw new CategoryNotFoundException(id);
        }
        categoryById.get().setName(categoryToEdit.getName());

        return categoryRepository.edit(categoryById.get()).get();
    }

    public void deleteCategory(Integer id) {
        categoryRepository.delete(id);
    }

    public Category findCategory(Integer id) {
        Optional<Category> categoryById = categoryRepository.findById(id);
        if (categoryById.isEmpty()) {
            throw new CategoryNotFoundException(id);
        }
        return categoryById.get();
    }

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Set<Category> validCategories(Set<Category> categories) {
        Set<Category> categorySet = new HashSet<>();
        List<Category> all = categoryRepository.findAll();
        for (Category category : categories) {
            String name = category.getName().trim();
            Optional<Category> first = all.stream()
                    .filter(cat -> StringUtils.containsIgnoreCase(cat.getName(), name))
                    .findFirst();

            categorySet.add(first.orElse(category));
        }
        return categorySet;
    }

    public Category findCategoryByName(String name) {
        return categoryRepository.findByName(name).orElse(null);
    }
}
