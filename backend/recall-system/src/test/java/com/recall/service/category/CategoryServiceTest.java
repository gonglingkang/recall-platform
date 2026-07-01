package com.recall.service.category;

import com.recall.BaseTest;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dao.todo.TodoMapper;
import com.recall.dto.category.CategoryCreateReq;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.service.todo.TodoService;
import com.recall.vo.category.CategoryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分类管理业务逻辑测试（PRD 6.4）。
 * <p>
 * 两层分类合并单表，用 parentId 表达层级。
 *
 * @author recall
 */
@Transactional
class CategoryServiceTest extends BaseTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private TodoService todoService;
    @Autowired
    private TodoMapper todoMapper;

    @Test
    void createCategory_root_shouldCreateBigCategory() {
        loginAsNewUser();
        CategoryVO cat = categoryService.createCategory(newCatReq("工作"));
        assertNull(cat.getParentId(), "大分类 parentId 应为 null");
        assertNotNull(cat.getId());
    }

    @Test
    void createCategory_duplicateRootName_shouldThrow() {
        loginAsNewUser();
        categoryService.createCategory(newCatReq("工作"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(newCatReq("工作")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createCategory_subcategory_shouldInheritParentId() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        CategoryVO sub = categoryService.createCategory(newSubReq(catId, "会议"));
        assertEquals(catId, sub.getParentId(), "子分类 parentId 应指向大分类");
    }

    @Test
    void createCategory_subcategoryDuplicateNameInSameParent_shouldThrow() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        categoryService.createCategory(newSubReq(catId, "会议"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(newSubReq(catId, "会议")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createCategory_thirdLevel_shouldThrow() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        Long subId = categoryService.createCategory(newSubReq(catId, "会议")).getId();
        // 在子分类下再建子分类，应拒绝（最多两级）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(newSubReq(subId, "深一层")));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createCategory_subcategoryWithColor_shouldForceNull() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        CategoryCreateReq req = newSubReq(catId, "会议");
        req.setColor("#ff0000");
        CategoryVO sub = categoryService.createCategory(req);
        assertNull(sub.getColor(), "子分类 color 应强制为 null");
    }

    @Test
    void listTree_shouldBuildTwoLevelTree() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        categoryService.createCategory(newSubReq(catId, "会议"));
        categoryService.createCategory(newSubReq(catId, "文档"));

        List<CategoryVO> tree = categoryService.listTree();
        assertFalse(tree.isEmpty());
        CategoryVO work = tree.stream().filter(c -> c.getId().equals(catId)).findFirst().orElseThrow();
        assertEquals(2, work.getSubcategories().size(), "大分类下应有2个子分类");
    }

    @Test
    void deleteCategory_emptyRoot_shouldDelete() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("待删")).getId();
        categoryService.deleteCategory(catId);
        // 再查应 404
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(catId));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void deleteCategory_rootWithSubcategory_shouldThrow() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("待删")).getId();
        categoryService.createCategory(newSubReq(catId, "子"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(catId));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void deleteCategory_rootWithTodo_shouldThrow() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("待删")).getId();
        // 直接挂大分类下的待办
        TodoCreateReq req = new TodoCreateReq();
        req.setTitle("大分类任务");
        req.setCategoryId(catId);
        todoService.create(req);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(catId));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void deleteCategory_subcategory_shouldMigrateTodosToParent() {
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("工作")).getId();
        Long subId = categoryService.createCategory(newSubReq(catId, "会议")).getId();

        // 在子分类下建待办
        TodoCreateReq req = new TodoCreateReq();
        req.setTitle("子分类任务");
        req.setCategoryId(subId);
        Long todoId = todoService.create(req).getId();

        // 删除子分类，待办应迁移到父大分类
        categoryService.deleteCategory(subId);

        var todo = todoMapper.selectById(todoId);
        assertEquals(catId, todo.getCategoryId(), "待办应迁移到父大分类");
    }

    @Test
    void deleteCategory_subcategoryNotFound_shouldThrow404() {
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(999999L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void deleteCategory_crossUser_shouldThrow404() {
        // 用户 A 建大分类 + 子分类
        loginAsNewUser();
        Long catId = categoryService.createCategory(newCatReq("A的工作")).getId();
        Long subId = categoryService.createCategory(newSubReq(catId, "会议")).getId();
        // 用户 B 尝试删 A 的子分类，应 404
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(subId));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    /** 创建大分类请求（parentId=null） */
    private CategoryCreateReq newCatReq(String name) {
        CategoryCreateReq r = new CategoryCreateReq();
        r.setName(name);
        return r;
    }

    /** 创建子分类请求（parentId 指向大分类） */
    private CategoryCreateReq newSubReq(Long parentId, String name) {
        CategoryCreateReq r = new CategoryCreateReq();
        r.setParentId(parentId);
        r.setName(name);
        return r;
    }
}
