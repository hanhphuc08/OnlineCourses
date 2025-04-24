package com.example.demo.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class course {
	
	private int courseID;
    private String title;
    private String description;
    private BigDecimal prices;
    private String formattedPrice;
    private int quantity;
    private courseStatus status;
    private LocalDateTime duration;
    private LocalDateTime createAt;
    private String image;
    private category category;
    private List<learningPath> learningPaths;
    
    private String categoryName;
    
    
    public String getFormattedPrice() {
		return formattedPrice;
	}

	public void setFormattedPrice(String formattedPrice) {
		this.formattedPrice = formattedPrice;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public List<learningPath> getLearningPaths() {
		return learningPaths;
	}

	public void setLearningPaths(List<learningPath> learningPaths) {
		this.learningPaths = learningPaths;
	}
	private String formattedDuration;
    
    public String getFormattedDuration() {
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }
    
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
        if (prices != null) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            this.formattedPrice = formatter.format(prices) + " VNĐ";
        } else {
            this.formattedPrice = "Chưa có thông tin";
        }
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
        if (duration != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            this.formattedDuration = duration.format(formatter);
        } else {
            this.formattedDuration = "Không xác định";
        }
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
