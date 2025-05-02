package com.example.demo.model;

import java.time.LocalDateTime;

public class EnrolledCourse {
	private course course;
    private LocalDateTime orderDate;
    
    
	public EnrolledCourse(course course, LocalDateTime orderDate) {
		super();
		this.course = course;
		this.orderDate = orderDate;
	}
	public course getCourse() {
		return course;
	}
	public void setCourse(course course) {
		this.course = course;
	}
	public LocalDateTime getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}
	
    

}
