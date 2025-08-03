package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.CategoryDto;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.port.primary.FindCategoryUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveCategoryUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/category")
public class CategoryController {

    private final FindCategoryUseCase findCategoryUseCase;
    private final SaveCategoryUseCase saveCategoryUseCase;
    private final ModelMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<CategoryDto>> getAllCategories() {
        log.info("Request to get all categories.");
        List<Category> categories = findCategoryUseCase.getAll();
        log.info("Found {} categories.", categories.size());
        return new ResponseEntity<>(categories.stream()
                .peek(cat -> log.debug("Found category {}", cat))
                .map(cat -> mapper.map(cat, CategoryDto.class))
                .peek(dto -> log.debug("Mapped found category {}", dto))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

//    CategoryService categoryService;
//
    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    
    public  ResponseEntity<CategoryDto> addCategory(@RequestBody Category category) {
        log.info("Request to add category: {}", category);
        Category added = saveCategoryUseCase.add(category);
        log.info("Added category: {}", added);
        CategoryDto dto = mapper.map(added, CategoryDto.class);
        log.debug("Mapped domain object to Category DTO: {}", added);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<CategoryDto> getCategoryByName(@RequestParam String name) {
        log.info("Request to search category by name: {}", name);
        Category category = findCategoryUseCase.getByName(name);

        if (category == null) {
            log.warn("No category found with name: {}", name);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found category matching name: {}", name);
        CategoryDto dto = mapper.map(category, CategoryDto.class);
        log.debug("Mapped domain object to Category DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
//
//    @PutMapping("/{id}")
//    public Category editCategory(@RequestBody Category category, @PathVariable Long id) {
//        return categoryService.editCategory(category, id);
//    }
//
//    @DeleteMapping("/{id}")
//    public void deleteCategory(@PathVariable Long id) {
//        categoryService.deleteCategory(id);
//    }
//
//    @GetMapping("/{id}")
//    public Category findCategory(@PathVariable Long id) {
//        return categoryService.findCategory(id);
//    }
//
//    @GetMapping
//    public List<Category> findAllCategories() {
//        return categoryService.findAllCategories();
//    }

}
