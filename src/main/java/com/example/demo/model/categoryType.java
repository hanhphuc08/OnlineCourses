package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

public class categoryType {
	private int categoryTypeID;
	private String categoryTypeName;
	private String description;
	private LocalDateTime createDate;
	private LocalDateTime updateDate;
	private List<category> categories;
	
	
	
	public categoryType() {
		super();
	}
	public int getCategoryTypeID() {
		return categoryTypeID;
	}
	public void setCategoryTypeID(int categoryTypeID) {
		this.categoryTypeID = categoryTypeID;
	}
	public String getCategoryTypeName() {
		return categoryTypeName;
	}
	public void setCategoryTypeName(String categoryTypeName) {
		this.categoryTypeName = categoryTypeName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public LocalDateTime getCreateDate() {
		return createDate;
	}
	public void setCreateDate(LocalDateTime createDate) {
		this.createDate = createDate;
	}
	public LocalDateTime getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(LocalDateTime updateDate) {
		this.updateDate = updateDate;
	}
	public List<category> getCategories() {
        return categories;
    }

    public void setCategories(List<category> categories) {
        this.categories = categories;
    }
	
	
	

}
