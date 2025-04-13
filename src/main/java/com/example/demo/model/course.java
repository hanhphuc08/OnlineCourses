package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class course {
	
	private int courseID;
    private String title;
    private String description;
    private BigDecimal prices;
    private int quantity;
    private courseStatus status;
    private LocalDateTime duration;
    private LocalDateTime createAt;
    private String image;
    private category category;
	public course() {
		super();
	}
	public int getCourseID() {
		return courseID;
	}
	public void setCourseID(int courseID) {
		this.courseID = courseID;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BigDecimal getPrices() {
		return prices;
	}
	public void setPrices(BigDecimal prices) {
		this.prices = prices;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public courseStatus getStatus() {
		return status;
	}
	public void setStatus(courseStatus status) {
		this.status = status;
	}
	public LocalDateTime getDuration() {
		return duration;
	}
	public void setDuration(LocalDateTime duration) {
		this.duration = duration;
	}
	public LocalDateTime getCreateAt() {
		return createAt;
	}
	public void setCreateAt(LocalDateTime createAt) {
		this.createAt = createAt;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public category getCategory() {
		return category;
	}
	public void setCategory(category category) {
		this.category = category;
	}
    
    	

}
