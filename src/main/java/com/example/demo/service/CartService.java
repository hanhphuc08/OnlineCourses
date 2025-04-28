package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.cart;
import com.example.demo.repository.CartRepository;

@Service
public class CartService {
	@Autowired
    private CartRepository cartRepository;

    public void addCourseToCart(int userId, int courseId) {
        cartRepository.addToCart(userId, courseId, 1);
    }

    public List<cart> getCartByUserId(int userId) {
        return cartRepository.findByUserId(userId);
    }
    
    public void removeFromCart(int cartId) {
        Optional<cart> cartItem = cartRepository.findById(cartId);
        if (!cartItem.isPresent()) {
            throw new RuntimeException("Mục giỏ hàng không tồn tại!");
        }
        cartRepository.deleteById(cartId);
    }
    
   
  
}
