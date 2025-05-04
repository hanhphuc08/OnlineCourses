package com.example.demo.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.orderDetail;

@Repository
public class OrderRepository {
	private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	
	private order mapRowToOrder(ResultSet rs, int rowNum) throws SQLException {
        order order = new order();
        order.setOrderID(rs.getInt("OrderID"));
        order.setUserID(rs.getInt("UserID"));
        order.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        order.setPromotionID(rs.getObject("PromotionID") != null ? rs.getInt("PromotionID") : null);
        order.setOrderDate(rs.getTimestamp("orderDate").toLocalDateTime());
        order.setOrderStatus(rs.getString("OrderStatus"));
        return order;
    }

    private orderDetail mapRowToOrderDetail(ResultSet rs, int rowNum) throws SQLException {
        orderDetail detail = new orderDetail();
        detail.setOrderDetailID(rs.getInt("OrderDetailID"));
        detail.setOrderID(rs.getInt("OrderID"));
        detail.setCourseID(rs.getInt("CourseID"));
        detail.setPrice(rs.getBigDecimal("Price"));

        course course = new course();
        course.setCourseID(rs.getInt("CourseID"));
        course.setTitle(rs.getString("Title"));
        course.setDescription(rs.getString("Description"));
        course.setPrices(rs.getBigDecimal("Prices"));
        course.setImage(rs.getString("Image"));
        
        detail.setCourse(course);

        return detail;
    }
	
	
    public order save(order order) {
    	logger.info("Lưu đơn hàng cho userId: {}", order.getUserID());
        String sqlOrder = "INSERT INTO orders (UserID, TotalAmount, PromotionID, OrderDate, OrderStatus) VALUES (?, ?, ?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, order.getUserID());
                ps.setBigDecimal(2, order.getTotalAmount());
                ps.setObject(3, order.getPromotionID(), java.sql.Types.INTEGER);
                ps.setTimestamp(4, Timestamp.valueOf(order.getOrderDate()));
                ps.setString(5, order.getOrderStatus());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new RuntimeException("Không thể lấy OrderID sau khi lưu đơn hàng");
            }
            int orderId = key.intValue();
            order.setOrderID(orderId);
            logger.info("Đã lưu đơn hàng với OrderID: {}", orderId);

            // Lưu orderDetails
            List<orderDetail> details = order.getOrderDetails();
            if (details != null && !details.isEmpty()) {
                for (orderDetail detail : details) {
                    saveOrderDetail(detail, orderId);
                }
            } else {
                logger.warn("Danh sách orderDetails rỗng cho OrderID: {}", orderId);
            }
            return order;
        } catch (Exception e) {
            logger.error("Lỗi khi lưu đơn hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu đơn hàng: " + e.getMessage());
        }
    }
    
    public void saveOrderDetail(orderDetail detail, int orderId) {
    	logger.info("Lưu orderDetail cho OrderID: {}, CourseID: {}", orderId, detail.getCourseID());
        String sqlDetail = "INSERT INTO orderDetail (OrderID, CourseID, Price, PromotionID) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sqlDetail, orderId, detail.getCourseID(), detail.getPrice(), detail.getPromotionID());
        } catch (Exception e) {
            logger.error("Lỗi khi lưu orderDetail cho OrderID {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể lưu chi tiết đơn hàng: " + e.getMessage());
        }
    
    }
	
    public order findById(int orderId) {
        String sql = "SELECT * FROM orders WHERE OrderID = ?";
        order order = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, this::mapRowToOrder);

        String detailSql = "SELECT od.*, c.CourseID, c.Title, c.Description, c.Prices, c.Image, c.Duration " +
                "FROM orderDetail od JOIN course c ON od.CourseID = c.CourseID " +
                "WHERE od.OrderID = ?";
        List<orderDetail> details = jdbcTemplate.query(detailSql, new Object[]{orderId}, this::mapRowToOrderDetail);
        order.setOrderDetails(details);

        return order;
    }
    
    
	public List<order> findByUserIdAndStatuses(int userId, List<String> statuses) {
        String sql = "SELECT o.orderID, o.userID, o.orderDate, o.orderStatus, o.totalAmount, o.promotionID, " +
                     "od.orderDetailID, od.orderID, od.courseID, od.price, od.promotionID AS od_promotionID, " +
                     "c.courseID AS course_id, c.title, c.description, c.prices, c.image, c.duration " +
                     "FROM orders o " +
                     "JOIN orderDetail od ON o.orderID = od.orderID " +
                     "JOIN course c ON od.courseID = c.courseID " +
                     "WHERE o.userID = ? AND o.orderStatus IN (" +
                     String.join(",", statuses.stream().map(s -> "?").toList()) + ")";

        try {
            List<Object> params = new ArrayList<>();
            params.add(userId);
            params.addAll(statuses);

            List<order> orders = new ArrayList<>();
            jdbcTemplate.query(sql, params.toArray(), (ResultSet rs) -> {
                order currentOrder = null;
                int currentOrderId = -1;

                while (rs.next()) {
                    int orderId = rs.getInt("orderID");
                    if (orderId != currentOrderId) {
                        currentOrder = new order();
                        currentOrder.setOrderID(orderId);
                        currentOrder.setUserID(rs.getInt("userID"));
                        currentOrder.setOrderDate(rs.getTimestamp("orderDate").toLocalDateTime());
                        currentOrder.setOrderStatus(rs.getString("orderStatus"));
                        currentOrder.setTotalAmount(rs.getBigDecimal("totalAmount"));
                        currentOrder.setPromotionID(rs.getInt("promotionID") == 0 ? null : rs.getInt("promotionID"));
                        currentOrder.setOrderDetails(new ArrayList<>());
                        orders.add(currentOrder);
                        currentOrderId = orderId;
                    }

                    orderDetail detail = new orderDetail();
                    detail.setOrderDetailID(rs.getInt("orderDetailID"));
                    detail.setOrderID(rs.getInt("orderID"));
                    detail.setCourseID(rs.getInt("courseID"));
                    detail.setPrice(rs.getBigDecimal("price"));
                    detail.setPromotionID(rs.getInt("od_promotionID") == 0 ? null : rs.getInt("od_promotionID"));

                    course course = new course();
                    course.setCourseID(rs.getInt("course_id"));
                    course.setTitle(rs.getString("title"));
                    course.setDescription(rs.getString("description"));
                    course.setPrices(rs.getBigDecimal("prices"));
                    course.setImage(rs.getString("image"));
                    Timestamp durationTimestamp = rs.getTimestamp("duration");
                    if (durationTimestamp != null) {
                        course.setDuration(durationTimestamp.toLocalDateTime());
                    } else {
                        course.setFormattedDuration("Không xác định");
                    }
                    detail.setCourse(course);

                    currentOrder.getOrderDetails().add(detail);
                }
                return orders;
            });
            logger.info("Tìm thấy {} đơn hàng cho userId {} với trạng thái {}", orders.size(), userId, statuses);
            return orders;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy đơn hàng cho userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách đơn hàng: " + e.getMessage());
        }
    }
	
	public List<order> findByUserId(int userId) {
        String sql = "SELECT o.orderID, o.userID, o.orderDate, o.orderStatus, o.totalAmount, o.promotionID, " +
                     "od.orderDetailID, od.orderID, od.courseID, od.price, od.promotionID AS od_promotionID, " +
                     "c.courseID AS course_id, c.title, c.description, c.prices, c.image, c.duration " +
                     "FROM orders o " +
                     "JOIN orderDetail od ON o.orderID = od.orderID " +
                     "JOIN course c ON od.courseID = c.courseID " +
                     "WHERE o.userID = ?";

        try {
            List<order> orders = new ArrayList<>();
            jdbcTemplate.query(sql, new Object[]{userId}, (ResultSet rs) -> {
                order currentOrder = null;
                int currentOrderId = -1;

                while (rs.next()) {
                    int orderId = rs.getInt("orderID");
                    if (orderId != currentOrderId) {
                        currentOrder = new order();
                        currentOrder.setOrderID(orderId);
                        currentOrder.setUserID(rs.getInt("userID"));
                        currentOrder.setOrderDate(rs.getTimestamp("orderDate").toLocalDateTime());
                        currentOrder.setOrderStatus(rs.getString("orderStatus"));
                        currentOrder.setTotalAmount(rs.getBigDecimal("totalAmount"));
                        currentOrder.setPromotionID(rs.getInt("promotionID") == 0 ? null : rs.getInt("promotionID"));
                        currentOrder.setOrderDetails(new ArrayList<>());
                        orders.add(currentOrder);
                        currentOrderId = orderId;
                    }

                    orderDetail detail = new orderDetail();
                    detail.setOrderDetailID(rs.getInt("orderDetailID"));
                    detail.setOrderID(rs.getInt("orderID"));
                    detail.setCourseID(rs.getInt("courseID"));
                    detail.setPrice(rs.getBigDecimal("price"));
                    detail.setPromotionID(rs.getInt("od_promotionID") == 0 ? null : rs.getInt("od_promotionID"));

                    course course = new course();
                    course.setCourseID(rs.getInt("course_id"));
                    course.setTitle(rs.getString("title"));
                    course.setDescription(rs.getString("description"));
                    course.setPrices(rs.getBigDecimal("prices"));
                    course.setImage(rs.getString("image"));
                    Timestamp durationTimestamp = rs.getTimestamp("duration");
                    if (durationTimestamp != null) {
                        course.setDuration(durationTimestamp.toLocalDateTime());
                    } else {
                        course.setFormattedDuration("Không xác định");
                    }
                    
                    detail.setCourse(course);

                    currentOrder.getOrderDetails().add(detail);
                }
                return orders;
            });
            logger.info("Tìm thấy {} đơn hàng cho userId {}", orders.size(), userId);
            return orders;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy đơn hàng cho userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

}
