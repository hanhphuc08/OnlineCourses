package com.example.demo.model;

public class address {
	 private int addressID;
	    private int userID;
	    private String nation;
	    private String province;
	    private String district;
	    private String village;
	    private String detail;

	    public address() {}


	    public int getAddressID() {
	        return addressID;
	    }

	    public void setAddressID(int addressID) {
	        this.addressID = addressID;
	    }

	    public int getUserID() {
	        return userID;
	    }

	    public void setUserID(int userID) {
	        this.userID = userID;
	    }

	    public String getNation() {
	        return nation;
	    }

	    public void setNation(String nation) {
	        this.nation = nation;
	    }

	    public String getProvince() {
	        return province;
	    }

	    public void setProvince(String province) {
	        this.province = province;
	    }

	    public String getDistrict() {
	        return district;
	    }

	    public void setDistrict(String district) {
	        this.district = district;
	    }

	    public String getVillage() {
	        return village;
	    }

	    public void setVillage(String village) {
	        this.village = village;
	    }

	    public String getDetail() {
	        return detail;
	    }

	    public void setDetail(String detail) {
	        this.detail = detail;
	    }

}
