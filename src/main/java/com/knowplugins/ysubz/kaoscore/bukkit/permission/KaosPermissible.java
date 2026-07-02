package com.knowplugins.ysubz.kaoscore.bukkit.permission;

import com.knowplugins.ysubz.kaoscore.bukkit.permission.manager.PermissionManager;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;

import java.util.UUID;

public final class KaosPermissible extends PermissibleBase {

    private final PermissionManager manager;
    private final UUID uuid;

    public KaosPermissible(Permissible permissible, PermissionManager manager, UUID uuid) {
        super(permissible);
        this.manager = manager;
        this.uuid = uuid;
    }

    @Override
    public boolean hasPermission(String inName) {
        if (inName == null || inName.trim().isEmpty()) {
            return false;
        }

        // Respect Bukkit attachments (and op) first.
        if (super.isPermissionSet(inName)) {
            return super.hasPermission(inName);
        }

        PermissionManager pm = this.manager;
        if (pm != null && uuid != null) {
            Boolean value = pm.getPermissionValue(uuid, inName);
            if (value != null) {
                return value;
            }
        }

        return super.hasPermission(inName);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        if (perm == null) {
            return false;
        }
        return hasPermission(perm.getName());
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        if (super.isPermissionSet(name)) {
            return true;
        }

        PermissionManager pm = this.manager;
        if (pm != null && uuid != null) {
            return pm.getPermissionValue(uuid, name) != null;
        }

        return false;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        if (perm == null) {
            return false;
        }
        return isPermissionSet(perm.getName());
    }
}

