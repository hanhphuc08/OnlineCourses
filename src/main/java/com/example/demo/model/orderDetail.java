package com.example.demo.model;

import java.math.BigDecimal;

public class orderDetail {
	private int orderDetailID;
    private int orderID;
    private int courseID;
    private BigDecimal price;
    private course course;
    
    
    
	public int getOrderDetailID() {
		return orderDetailID;
	}
	public void setOrderDetailID(int orderDetailID) {
		this.orderDetailID = orderDetailID;
	}
	public int getOrderID() {
		return orderID;
	}
	public void setOrderID(int orderID) {
		this.orderID = orderID;
	}
	public int getCourseID() {
		return courseID;
	}
	public void setCourseID(int courseID) {
		this.courseID = courseID;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public course getCourse() {
		return course;
	}
	public void setCourse(course course) {
		this.course = course;
	}
    
    

}
