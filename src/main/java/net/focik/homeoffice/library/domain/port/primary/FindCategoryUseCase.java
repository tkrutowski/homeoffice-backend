package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Category;

import java.util.List;

public interface FindCategoryUseCase {
    Category getById(Integer idCategory);

    Category getByName(String name);

    List<Category> getFromString(String categories);

    List<Category> getAll();
}
