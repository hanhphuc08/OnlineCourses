package com.example.demo.model;

public class role {
	private String roleID;
    private String roleName;

    public role() {}

    public role(String roleID, String roleName) {
        this.roleID = roleID;
        this.roleName = roleName;
    }


    public String getRoleID() {
        return roleID;
    }

    public void setRoleID(String roleID) {
        this.roleID = roleID;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

}
