package com.agroprecision.service;

import com.agroprecision.exception.ApiException;
import com.agroprecision.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** Farm-level authorization rules: admin sees all; owners manage; members (field workers) view. */
@Service
public class AccessService {
    private final JdbcTemplate db;

    public AccessService(JdbcTemplate db) { this.db = db; }

    public boolean canManage(AuthUser u, String farmId) {
        if (farmId == null) return false;
        if (u.isAdmin()) return true;
        Integer n = db.queryForObject("SELECT COUNT(*) FROM farms WHERE id = ? AND owner_id = ?", Integer.class, farmId, u.id());
        return n != null && n > 0;
    }

    public boolean canView(AuthUser u, String farmId) {
        if (canManage(u, farmId)) return true;
        Integer n = db.queryForObject("SELECT COUNT(*) FROM farm_members WHERE farm_id = ? AND user_id = ?", Integer.class, farmId, u.id());
        return n != null && n > 0;
    }

    public void requireManage(AuthUser u, String farmId) { if (!canManage(u, farmId)) throw ApiException.forbidden(); }
    public void requireView(AuthUser u, String farmId) { if (!canView(u, farmId)) throw ApiException.forbidden(); }

    /** Farm ids visible to the user; null means "all" (admin). */
    public List<String> visibleFarmIds(AuthUser u) {
        if (u.isAdmin()) return null;
        return db.queryForList("SELECT id FROM farms WHERE owner_id = ? UNION SELECT farm_id FROM farm_members WHERE user_id = ?",
                String.class, u.id(), u.id());
    }
}
