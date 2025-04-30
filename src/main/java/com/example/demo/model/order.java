package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class order {
	private int orderID;
    private int userID;
    private BigDecimal totalAmount;
    private Integer promotionID; 
    private LocalDateTime orderDate;
    private String orderStatus; 
    private List<orderDetail> orderDetails;
    
    
    
    
	public int getOrderID() {
		return orderID;
	}
	public void setOrderID(int orderID) {
		this.orderID = orderID;
	}
	public int getUserID() {
		return userID;
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}
	public Integer getPromotionID() {
		return promotionID;
	}
	public void setPromotionID(Integer promotionID) {
		this.promotionID = promotionID;
	}
	public LocalDateTime getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}
	public String getOrderStatus() {
		return orderStatus;
	}
	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}
	public List<orderDetail> getOrderDetails() {
		return orderDetails;
	}
	public void setOrderDetails(List<orderDetail> orderDetails) {
		this.orderDetails = orderDetails;
	}
    
    

}
