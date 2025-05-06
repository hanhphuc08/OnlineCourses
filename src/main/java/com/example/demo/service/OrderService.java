package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.order;
import com.example.demo.repository.OrderRepository;

@Service
public class OrderService {
	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
	
	@Autowired
    private OrderRepository orderRepository;
	
	 public List<order> findByUserId(int userId) {
	        logger.info("Lấy danh sách đơn hàng cho userId: {}", userId);
	        return orderRepository.findByUserId(userId);
	    }

	    public long countPendingOrders() {
	        logger.info("Đếm số đơn hàng đang chờ xử lý");
	        return orderRepository.countPendingOrders();
	    }
	    
	    public List<Object[]> findRecentOrders(int limit) {
	        logger.info("Lấy {} đơn hàng gần đây nhất", limit);
	        return orderRepository.findRecentOrders(limit);
	    }
	    
	    public List<Object[]> findAllOrders(int page, int size, String search, String status) {
	        logger.info("Lấy danh sách đơn hàng: page={}, size={}, search={}, status={}", page, size, search, status);
	        return orderRepository.findAllOrders(page, size, search, status);
	    }

	    public long countAllOrders(String search, String status) {
	        logger.info("Đếm tổng số đơn hàng: search={}, status={}", search, status);
	        return orderRepository.countAllOrders(search, status);
	    }

	    public Object[] findOrderDetailsById(int orderId) {
	        logger.info("Lấy chi tiết đơn hàng với OrderID: {}", orderId);
	        Object[] orderDetails = orderRepository.findOrderDetailsById(orderId);
	        List<Object[]> orderDetailItems = orderRepository.findOrderDetailItems(orderId);
	        orderDetails[9] = orderDetailItems;
	        return orderDetails;
	    }
	    public void updateOrderStatus(int orderId, String status) {
	        logger.info("Cập nhật trạng thái đơn hàng {} thành {}", orderId, status);
	        orderRepository.updateOrderStatus(orderId, status);
	    }

}
