package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.port.secondary.CategoryRepository;
import net.focik.homeoffice.library.infrastructure.dto.CategoryDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {
    private final CategoryDtoRepository categoryDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Category add(Category category) {
        CategoryDbDto categoryDbDto = mapper.map(category, CategoryDbDto.class);
        if (categoryDbDto.getId() == 0) {
            categoryDbDto.setId(null);
        }
        CategoryDbDto saved = categoryDtoRepository.save(categoryDbDto);
        return mapper.map(saved, Category.class);
    }

    @Override
    public Optional<Category> edit(Category category) {
        CategoryDbDto dbDto = categoryDtoRepository.save(mapper.map(category, CategoryDbDto.class));
        return Optional.of(mapper.map(dbDto, Category.class));
    }

    @Override
    public void delete(Integer id) {
        categoryDtoRepository.deleteById(id);
    }

    @Override
    public Optional<Category> findById(Integer id) {
        return categoryDtoRepository.findById(id)
                .map(categoryDbDto -> mapper.map(categoryDbDto, Category.class));
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryDtoRepository.findCategoryDtoByNameIgnoreCase(name)
                .map(categoryDbDto -> mapper.map(categoryDbDto, Category.class));
    }

    @Override
    public List<Category> findAll() {
        List<Category> categoryList = new ArrayList<>();
        categoryDtoRepository
                .findAll()
                .iterator()
                .forEachRemaining(categoryDbDto -> categoryList.add(mapper.map(categoryDbDto, Category.class)));
        return categoryList;
    }
}
