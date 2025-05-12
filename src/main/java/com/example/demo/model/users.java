package com.example.demo.model;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

public class users {
	 private int userID;
	    private String email;
	    private String phoneNumber;
	    private String fullname;
	    private String address;
	    private String gender;
	    private String password;
	    private String emailCode;
	    private LocalDateTime createDate;
	    private LocalDateTime updateDate;
	    private role role;
	private int status;
	private List<order> orders;
	
	    public users() {}

	
	    public int getUserID() {
	        return userID;
	    }

	    public void setUserID(int userID) {
	        this.userID = userID;
	    }

	    public String getEmail() {
	        return email;
	    }

	    public void setEmail(String email) {
	        this.email = email;
	    }

	    public String getPhoneNumber() {
	        return phoneNumber;
	    }

	    public void setPhoneNumber(String phoneNumber) {
	        if (phoneNumber != null) {
	            if (phoneNumber.startsWith("+84")) {
	                this.phoneNumber = "0" + phoneNumber.substring(3);
	            } else if (phoneNumber.startsWith("84")) {
	                this.phoneNumber = "0" + phoneNumber.substring(2);
	            } else if (phoneNumber.startsWith("0")) {
	                this.phoneNumber = phoneNumber;
	            } else {
	                this.phoneNumber = "0" + phoneNumber;
	            }
	        } else {
	            this.phoneNumber = phoneNumber;
	        }
	    }

	    public String getFullname() {
	        return fullname;
	    }

	    public void setFullname(String fullname) {
	        this.fullname = fullname;
	    }

	    public String getAddress() {
	        return address;
	    }

	    public void setAddress(String address) {
	        this.address = address;
	    }

	    public String getGender() {
	        return gender;
	    }

	    public void setGender(String gender) {
	        this.gender = gender;
	    }

	    public String getPassword() {
	        return password;
	    }

	    public void setPassword(String password) {
	        this.password = password;
	    }

	    public String getEmailCode() {
	        return emailCode;
	    }

	    public void setEmailCode(String emailCode) {
	        this.emailCode = emailCode;
	    }

	    public LocalDateTime getCreateDate() {
	        return createDate;
	    }

	    public void setCreateDate(LocalDateTime date) {
	        this.createDate = date;
	    }

	    public LocalDateTime getUpdateDate() {
	        return updateDate;
	    }

	    public void setUpdateDate(LocalDateTime updateDate) {
	        this.updateDate = updateDate;
	    }

	    public role getRole() {
	        return role;
	    }

	    public void setRole(role role) {
	        this.role = role;
	    }

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	public List<order> getOrders() { return orders; }
	public void setOrders(List<order> orders) { this.orders = orders; }
}
