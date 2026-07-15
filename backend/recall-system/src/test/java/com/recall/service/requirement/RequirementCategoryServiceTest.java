package com.recall.service.requirement;

import com.recall.BaseTest;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.requirement.RequirementCategoryCreateReq;
import com.recall.dto.requirement.RequirementCategoryUpdateReq;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.vo.requirement.RequirementCategoryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 需求分类 Service 测试。
 * <p>
 * 重点：两级分类树查询、创建/编辑/删除、2层约束、名称唯一、删除拒绝规则、需求绑定校验、权限隔离。
 *
 * @author recall
 */
@Transactional
class RequirementCategoryServiceTest extends BaseTest {

    @Autowired
    private RequirementCategoryService requirementCategoryService;
    @Autowired
    private RequirementService requirementService;

    // ===================== 树查询 =====================

    @Test
    void listTree_shouldReturnTwoLevels() {
        loginAsNewUser();
        Long mainId = createMain("功能优化");
        Long sub1 = createSub(mainId, "登录优化");
        Long sub2 = createSub(mainId, "支付优化");

        List<RequirementCategoryVO> tree = requirementCategoryService.listTree();
        assertEquals(1, tree.size(), "1个主分类");
        RequirementCategoryVO main = tree.get(0);
        assertEquals("功能优化", main.getName());
        assertEquals(2, main.getSubcategories().size(), "2个子分类");
        assertNull(main.getParentId(), "主分类 parentId 为 null");
    }

    @Test
    void listTree_emptyWhenNoCategory() {
        loginAsNewUser();
        assertTrue(requirementCategoryService.listTree().isEmpty());
    }

    @Test
    void listTree_sortedBySortOrderThenId() {
        loginAsNewUser();
        RequirementCategoryCreateReq b = new RequirementCategoryCreateReq();
        b.setName("B分类");
        b.setSortOrder(2);
        requirementCategoryService.createCategory(b);
        RequirementCategoryCreateReq a = new RequirementCategoryCreateReq();
        a.setName("A分类");
        a.setSortOrder(1);
        requirementCategoryService.createCategory(a);

        List<RequirementCategoryVO> tree = requirementCategoryService.listTree();
        assertEquals("A分类", tree.get(0).getName(), "sortOrder=1 排前");
        assertEquals("B分类", tree.get(1).getName());
    }

    // ===================== 创建 =====================

    @Test
    void create_mainCategory_withColor() {
        loginAsNewUser();
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setName("主分类");
        req.setColor("#FF0000");
        RequirementCategoryVO vo = requirementCategoryService.createCategory(req);
        assertNull(vo.getParentId(), "主分类 parentId 为 null");
        assertEquals("#FF0000", vo.getColor(), "主分类可设颜色");
    }

    @Test
    void create_subCategory_colorForcedNull() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setParentId(mainId);
        req.setName("子分类");
        req.setColor("#FF0000"); // 子分类传颜色应被忽略
        RequirementCategoryVO vo = requirementCategoryService.createCategory(req);
        assertEquals(mainId, vo.getParentId());
        assertNull(vo.getColor(), "子分类颜色强制 null");
    }

    @Test
    void create_subCategory_parentNotMain_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        Long subId = createSub(mainId, "子分类1");
        // 在子分类下再建子分类 -> 超过2层
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setParentId(subId);
        req.setName("子子分类");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.createCategory(req));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateNameSameParent_shouldThrow() {
        loginAsNewUser();
        createMain("同名分类");
        BusinessException ex = assertThrows(BusinessException.class, () -> createMain("同名分类"));
        assertEquals(ResultCode.REQUIREMENT_CATEGORY_NAME_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateNameDifferentParent_ok() {
        loginAsNewUser();
        Long main1 = createMain("主分类A");
        Long main2 = createMain("主分类B");
        createSub(main1, "同名子分类");
        // 不同父下同名子分类允许
        Long subId = createSub(main2, "同名子分类");
        RequirementCategoryVO vo = requirementCategoryService.listTree().stream()
                .filter(c -> main2.equals(c.getId())).findFirst().orElseThrow()
                .getSubcategories().get(0);
        assertEquals("同名子分类", vo.getName());
    }

    // ===================== 编辑 =====================

    @Test
    void update_renameToExistingSameParent_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        createSub(mainId, "子分类A");
        Long subB = createSub(mainId, "子分类B");
        // 子分类B 改名为 子分类A -> 同父冲突
        RequirementCategoryUpdateReq upd = new RequirementCategoryUpdateReq();
        upd.setName("子分类A");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.updateCategory(subB, upd));
        assertEquals(ResultCode.REQUIREMENT_CATEGORY_NAME_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void update_subCategorySetColor_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        Long subId = createSub(mainId, "子分类");
        RequirementCategoryUpdateReq upd = new RequirementCategoryUpdateReq();
        upd.setName("子分类");
        upd.setColor("#FF0000");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.updateCategory(subId, upd));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void update_mainCategoryColor_ok() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        RequirementCategoryUpdateReq upd = new RequirementCategoryUpdateReq();
        upd.setName("主分类");
        upd.setColor("#00FF00");
        RequirementCategoryVO vo = requirementCategoryService.updateCategory(mainId, upd);
        assertEquals("#00FF00", vo.getColor());
    }

    // ===================== 删除 =====================

    @Test
    void delete_hasSub_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        createSub(mainId, "子分类");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.deleteCategory(mainId));
        assertEquals(ResultCode.REQUIREMENT_CATEGORY_HAS_SUB.getCode(), ex.getCode());
    }

    @Test
    void delete_mainHasRequirement_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        createReqWithCategory("需求A", mainId, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.deleteCategory(mainId));
        assertEquals(ResultCode.REQUIREMENT_CATEGORY_HAS_REQUIREMENT.getCode(), ex.getCode());
    }

    @Test
    void delete_subHasRequirement_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        Long subId = createSub(mainId, "子分类");
        createReqWithCategory("需求A", mainId, subId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.deleteCategory(subId));
        assertEquals(ResultCode.REQUIREMENT_CATEGORY_HAS_REQUIREMENT.getCode(), ex.getCode());
    }

    @Test
    void delete_noRequirement_ok() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        Long subId = createSub(mainId, "子分类");
        // 先删子分类（无需求），再删主分类
        requirementCategoryService.deleteCategory(subId);
        requirementCategoryService.deleteCategory(mainId);
        assertTrue(requirementCategoryService.listTree().isEmpty());
    }

    // ===================== 需求绑定校验 =====================

    @Test
    void validateCategoryBinding_subParentMismatch_shouldThrow() {
        loginAsNewUser();
        Long mainA = createMain("主分类A");
        Long mainB = createMain("主分类B");
        Long subB = createSub(mainB, "主分类B的子分类");
        // 把 mainB 的子分类绑到 mainA -> 4713
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.validateCategoryBinding(mainA, subB));
        assertEquals(ResultCode.REQUIREMENT_SUB_CATEGORY_PARENT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void validateCategoryBinding_categoryNull_shouldThrow() {
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.validateCategoryBinding(null, null));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void validateCategoryBinding_subIsMain_shouldThrow() {
        loginAsNewUser();
        Long mainId = createMain("主分类");
        Long main2 = createMain("主分类2");
        // 把主分类当子分类传
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.validateCategoryBinding(mainId, main2));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    // ===================== 权限隔离 =====================

    @Test
    void dataIsolation_userCannotAccessOthersCategory() {
        loginAsNewUser();
        Long mainId = createMain("私有分类");
        loginAsNewUser(); // 切换用户
        RequirementCategoryUpdateReq upd = new RequirementCategoryUpdateReq();
        upd.setName("篡改");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementCategoryService.updateCategory(mainId, upd));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void create_subCategory_otherUserParent_should404() {
        loginAsNewUser();
        Long mainId = createMain("用户A主分类");
        loginAsNewUser(); // 切换用户B
        // B 用 A 的主分类建子分类 -> 越权 404
        BusinessException ex = assertThrows(BusinessException.class, () -> createSub(mainId, "B的子分类"));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    // ===================== 辅助 =====================

    private Long createMain(String name) {
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setName(name);
        return requirementCategoryService.createCategory(req).getId();
    }

    private Long createSub(Long parentId, String name) {
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setParentId(parentId);
        req.setName(name);
        return requirementCategoryService.createCategory(req).getId();
    }

    private void createReqWithCategory(String title, Long categoryId, Long subCategoryId) {
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle(title);
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(categoryId);
        req.setSubCategoryId(subCategoryId);
        requirementService.create(req);
    }
}
