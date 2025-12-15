package com.ureca.uhyu.domain.brand.repository;

import com.ureca.uhyu.domain.brand.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CategoryRepository extends JpaRepository<Category, Long> {



    List<Category> findByCategoryNameContaining(String categoryName);

    List<Category> findByCategoryName(String categoryName);
}
