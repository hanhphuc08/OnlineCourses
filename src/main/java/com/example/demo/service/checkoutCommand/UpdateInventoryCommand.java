package com.example.demo.service.checkoutCommand;

import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.users;
import com.example.demo.repository.CourseRepository;
import com.example.demo.model.orderDetail;

public class UpdateInventoryCommand implements CheckoutCommand {
	
	private final CourseRepository courseRepository;

    public UpdateInventoryCommand(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		for (orderDetail detail : order.getOrderDetails()) {
	        course course = courseRepository.findById(detail.getCourseID());
	        if (course == null) {
	            throw new RuntimeException("Khóa học không tồn tại: " + detail.getCourseID());
	        }
	        if (course.getQuantity() <= 0) {
	            throw new RuntimeException("Khóa học " + course.getTitle() + " đã hết hàng!");
	        }
	        courseRepository.decrementQuantity(detail.getCourseID());
	    }
	}
	
}
