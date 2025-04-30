package com.example.demo.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.model.payment;
import com.example.demo.model.paymentMethod;

@Repository
public class PaymentRepository {
	private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	private payment mapRowToPayment(ResultSet rs, int rowNum) throws SQLException {
		
        payment payment = new payment();
        payment.setPaymentID(rs.getInt("PaymentID"));
        payment.setOrderID(rs.getInt("OrderID"));
        payment.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());
        payment.setPaymentStatus(rs.getString("PaymentStatus"));
        payment.setCurrency(rs.getString("Currency"));
        return payment;
    }

    private paymentMethod mapRowToPaymentMethod(ResultSet rs, int rowNum) throws SQLException {
    	
        paymentMethod method = new paymentMethod();
        method.setPaymentMethodID(rs.getInt("PaymentMethodID"));
        method.setPaymentID(rs.getInt("PaymentID"));
        method.setMethodType(rs.getString("MethodType"));
        method.seteWalletProvider(rs.getString("EWalletProvider"));
        method.seteWalletTransactionID(rs.getObject("EWalletTransactionID") != null ? rs.getLong("EWalletTransactionID") : null);
        return method;
    }
    
    public payment save(payment payment) {
    	
    	logger.info("Lưu payment cho OrderID: {}", payment.getOrderID());
        String sqlPayment = "INSERT INTO payment (OrderID, Amount, Currency, PaymentStatus, CreateDate, QrCodeUrl) VALUES (?, ?, ?, ?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlPayment, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, payment.getOrderID());
                ps.setBigDecimal(2, payment.getAmount());
                ps.setString(3, payment.getCurrency());
                ps.setString(4, payment.getPaymentStatus());
                ps.setTimestamp(5, Timestamp.valueOf(payment.getCreateDate()));
                ps.setString(6, payment.getQrCodeUrl());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new RuntimeException("Không thể lấy PaymentID sau khi lưu payment");
            }
            int paymentId = key.intValue();
            payment.setPaymentID(paymentId);
            logger.info("Đã lưu payment với PaymentID: {}", paymentId);

            // Lưu paymentMethod
            paymentMethod method = payment.getPaymentMethod();
            if (method != null) {
                savePaymentMethod(method, paymentId);
            } else {
                logger.warn("paymentMethod null cho PaymentID: {}", paymentId);
            }
            return payment;
        } catch (Exception e) {
            logger.error("Lỗi khi lưu payment: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu payment: " + e.getMessage());
        }
    }
    
    private void savePaymentMethod(paymentMethod method, int paymentId) {
    	String sql = "INSERT INTO paymentMethod (PaymentID, MethodType, EWalletProvider, EWalletTransactionID) " +
                "VALUES (?, ?, ?, ?)";
    	
		jdbcTemplate.update(sql, 
		       paymentId, 
		       method.getMethodType(), 
		       method.geteWalletProvider(), 
		       method.geteWalletTransactionID()
		   );
    }
    
    public void updatePaymentStatus(int paymentId, String status) {
        String sql = "UPDATE payment SET PaymentStatus = ? WHERE PaymentID = ?";
        jdbcTemplate.update(sql, status, paymentId);
    }
    
    public payment findByOrderId(int orderId) {
    	
    	String sql = "SELECT * FROM payment WHERE OrderID = ?";
        try {
            payment payment = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, this::mapRowToPayment);
            
            String methodSql = "SELECT * FROM paymentMethod WHERE PaymentID = ?";
            paymentMethod method = jdbcTemplate.queryForObject(methodSql, new Object[]{payment.getPaymentID()}, this::mapRowToPaymentMethod);
            payment.setPaymentMethod(method);
            
            return payment;
            
        } catch (Exception e) {
            return null;
        }
    }
    

}
