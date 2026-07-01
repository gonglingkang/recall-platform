package com.recall.config;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.recall.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 用户数据隔离拦截器（PRD 3.2 / 13.3 强制要求）。
 * <p>
 * 对「按用户隔离」的资源表（todos / categories / subcategories / sprint_items /
 * objectives / key_results）的所有 SELECT/UPDATE/DELETE 自动注入 {@code user_id = 当前用户ID} 条件，
 * 从机制上杜绝「仅凭资源 ID 越权访问他人数据」。
 * <p>
 * <b>说明</b>：本拦截器做兜底防护；业务层仍应在显式查询时带上 userId，可读性更好、双保险。
 * users 表不参与（其自身即用户表，仅在登录/注册时按 username/email 查询）。
 *
 * @author recall
 */
@Slf4j
@Component
public class UserDataIsolationInterceptor extends DataPermissionInterceptor {

    /** 需要强制按 user_id 隔离的表名集合（小写） */
    private static final Set<String> ISOLATED_TABLES = Set.of(
            "todos", "categories", "subcategories",
            "sprint_items", "objectives", "key_results"
    );

    public UserDataIsolationInterceptor() {
        super(new IsolationDataPermissionHandler());
    }

    /**
     * 数据权限处理器：为隔离表注入 user_id = currentUserId。
     */
    private static class IsolationDataPermissionHandler implements DataPermissionHandler {

        @Override
        public Expression getSqlSegment(Expression where, String whereSegment) {
            Long userId = UserContextHolder.getUserId();
            // 未登录场景（如登录/注册）不注入，由业务层控制
            if (userId == null) {
                return where;
            }
            // whereSegment 形如 "xxx where ..."，仅当涉及隔离表时注入
            if (!involvesIsolatedTable(whereSegment)) {
                return where;
            }
            EqualsTo userIdCond = new EqualsTo(new Column("user_id"), new LongValue(userId));
            if (where == null) {
                return userIdCond;
            }
            return new AndExpression(where, userIdCond);
        }

        private boolean involvesIsolatedTable(String whereSegment) {
            if (whereSegment == null) {
                return false;
            }
            String lower = whereSegment.toLowerCase();
            for (String table : ISOLATED_TABLES) {
                if (lower.contains(table)) {
                    return true;
                }
            }
            return false;
        }
    }
}
