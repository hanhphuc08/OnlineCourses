package com.example.demo.model;

import java.time.LocalDateTime;

public class learningPath {
	private int learningPathID;
	private course course;
	private int stepNumber;
	private String title;
	private LocalDateTime createAt;
	public learningPath() {
		super();
	}
	public int getLearningPathID() {
		return learningPathID;
	}
	public void setLearningPathID(int learningPathID) {
		this.learningPathID = learningPathID;
	}
	public course getCourse() {
		return course;
	}
	public void setCourse(course course) {
		this.course = course;
	}
	public int getStepNumber() {
		return stepNumber;
	}
	public void setStepNumber(int stepNumber) {
		this.stepNumber = stepNumber;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public LocalDateTime getCreateAt() {
		return createAt;
	}
	public void setCreateAt(LocalDateTime createAt) {
		this.createAt = createAt;
	}
	
	
	

}
