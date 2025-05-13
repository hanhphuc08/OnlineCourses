package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.promotion;
import com.example.demo.repository.PromotionRepository;

@Service
public class PromotionService {
	@Autowired
    private PromotionRepository promotionRepository;

    public List<promotion> getAllPromotions() {
        return promotionRepository.getAllPromotions();
    }

    public promotion getPromotionById(int id) {
        return promotionRepository.findById(id);
    }

    public void addPromotion(promotion promotion) {
        promotionRepository.addPromotion(promotion);
    }

    public void updatePromotion(promotion promotion) {
        promotionRepository.updatePromotion(promotion);
    }

    public promotion findByCode(String code) {
        return promotionRepository.findByCode(code);
    }

    public promotion findByCodeAndCourseId(String code, int courseId) {
        return promotionRepository.findByCodeAndCourseId(code, courseId);
    }

    public String validateCoupon(String code, int courseId, int userId) {
        return promotionRepository.validateCoupon(code, courseId, userId);
    }

    public void saveUserPromotion(int promotionId, int userId) {
        promotionRepository.saveUserPromotion(promotionId, userId);
    }

    public List<promotion> getPromotionsPaginated(int page, int size, String search, String status) {
        return promotionRepository.findPromotionsPaginated(page, size, search, status);
    }

    public long countPromotions(String search, String status) {
        return promotionRepository.countPromotions(search, status);
    }
}
