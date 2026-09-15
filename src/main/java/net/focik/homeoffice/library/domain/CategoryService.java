package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.CategoryAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.CategoryCanNotBeDeletedException;
import net.focik.homeoffice.library.domain.exception.CategoryNotFoundException;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.library.domain.port.secondary.CategoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

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
        Long booksByCategory = bookRepository.countBooksByCategoryId(id);
        if (booksByCategory > 0) {
            log.warn("Category with ID {} cannot be deleted — associated books found (count: {}).", id, booksByCategory);
            throw new CategoryCanNotBeDeletedException("książki. (" + booksByCategory + ")");
        }
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
    return categories.stream()
            .map(category -> categoryRepository.findById(category.getId())
                    .orElseThrow(() -> new CategoryNotFoundException(category.getId())))
            .collect(Collectors.toSet());
}

    public Category findCategoryByName(String name) {
        return categoryRepository.findByName(name).orElse(null);
    }

    public List<Category> getFromString(String categories) {
        log.debug("Trying to get categories from categories string {}", categories);

        List<Category> parsedCategories = Arrays.stream(categories.trim().split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(this::findCategoryByName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        log.debug("Found categories {}", parsedCategories);
        return parsedCategories.stream().toList();
    }
}
