package com.dbpwd.demo.mybatis.dao;

import com.dbpwd.demo.mybatis.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Product Mapper接口
 */
public interface ProductMapper {

    /**
     * 插入产品
     */
    @Insert("INSERT INTO products (name, description, price, stock, category) VALUES (#{name}, #{description}, #{price}, #{stock}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    /**
     * 根据ID查询产品
     */
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectById(Long id);

    /**
     * 查询所有产品
     */
    @Select("SELECT * FROM products ORDER BY id")
    List<Product> selectAll();

    /**
     * 更新产品
     */
    @Update("UPDATE products SET name = #{name}, description = #{description}, price = #{price}, stock = #{stock}, category = #{category} WHERE id = #{id}")
    int update(Product product);

    /**
     * 删除产品
     */
    @Delete("DELETE FROM products WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据分类查询产品
     */
    @Select("SELECT * FROM products WHERE category = #{category} ORDER BY id")
    List<Product> selectByCategory(String category);
}
