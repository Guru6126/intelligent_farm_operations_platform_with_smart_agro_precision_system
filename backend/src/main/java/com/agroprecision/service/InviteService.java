package com.agroprecision.service;

import com.agroprecision.dto.AuthDtos.*;
import com.agroprecision.entity.InviteToken;
import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.InviteTokenRepository;
import com.agroprecision.repository.TableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InviteService {

    private final InviteTokenRepository invites;
    private final TableRepository table;
    private final String frontendUrl;

    public InviteService(InviteTokenRepository invites, TableRepository table,
                         @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.invites = invites;
        this.table = table;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    @Transactional
    public InviteCreated createInvite(String farmId, String createdBy) {
        List<Map<String, Object>> farms = table.query(
            "SELECT id, name FROM farms WHERE id = ?", List.of(farmId));
        if (farms.isEmpty()) throw ApiException.bad("Farm not found.");

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);

        InviteToken inv = new InviteToken();
        inv.setId(UUID.randomUUID().toString());
        inv.setToken(token);
        inv.setFarmId(farmId);
        inv.setCreatedBy(createdBy);
        inv.setExpiresAt(LocalDateTime.now().plusDays(7));
        invites.save(inv);

        String url = frontendUrl + "/auth?invite=" + token;
        return new InviteCreated(token, url, inv.getExpiresAt().toString());
    }

    public InvitePreview previewInvite(String token) {
        InviteToken inv = invites.findByToken(token)
            .orElseThrow(() -> ApiException.bad("Invite link is invalid."));
        if (inv.isExpired()) throw ApiException.bad("Invite link has expired.");
        if (inv.isUsed()) throw ApiException.bad("Invite link has already been used.");

        List<Map<String, Object>> farms = table.query(
            "SELECT id, name FROM farms WHERE id = ?", List.of(inv.getFarmId()));
        String farmName = farms.isEmpty() ? "Unknown Farm" : String.valueOf(farms.get(0).get("name"));

        return new InvitePreview(inv.getFarmId(), farmName, inv.getExpiresAt().toString());
    }

    @Transactional
    public void consumeInvite(String token, String userId) {
        InviteToken inv = invites.findByToken(token)
            .orElseThrow(() -> ApiException.bad("Invite link is invalid."));
        if (inv.isExpired()) throw ApiException.bad("Invite link has expired.");
        if (inv.isUsed()) throw ApiException.bad("Invite link has already been used.");

        table.execute(
            "INSERT IGNORE INTO farm_members (id, farm_id, user_id) VALUES (?, ?, ?)",
            List.of(UUID.randomUUID().toString(), inv.getFarmId(), userId));

        inv.setUsedAt(LocalDateTime.now());
        inv.setUsedBy(userId);
        invites.save(inv);
    }
}

