package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

public class category {
	private int categoryID;
	private String categoryName;
	private String description;
	private int categoryTypeID;
	private LocalDateTime createDate;
	private LocalDateTime updateDate;
	
	private List<course> courses;
	
	public List<course> getCourses() {
		return courses;
	}


	public void setCourses(List<course> courses) {
		this.courses = courses;
	}


	public category() {}

  
    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    

    public int getCategoryTypeID() {
		return categoryTypeID;
	}


	public void setCategoryTypeID(int categoryTypeID) {
		this.categoryTypeID = categoryTypeID;
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

}
