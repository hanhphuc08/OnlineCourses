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
	
	
	 public long countPendingOrders() {
	        String sql = "SELECT COUNT(*) FROM orders WHERE OrderStatus = 'PENDING'";
	        try {
	            Long count = jdbcTemplate.queryForObject(sql, Long.class);
	            logger.info("Đếm số đơn hàng đang chờ xử lý: {}", count);
	            return count != null ? count : 0;
	        } catch (Exception e) {
	            logger.error("Lỗi khi đếm số đơn hàng đang chờ xử lý: {}", e.getMessage());
	            throw new RuntimeException("Lỗi khi đếm số đơn hàng đang chờ xử lý: " + e.getMessage());
	        }
	    }
	 
	 public List<Object[]> findRecentOrders(int limit) {
	        String sql = "SELECT o.OrderID, o.OrderDate, o.OrderStatus, u.Fullname, MIN(c.Title) as CourseTitle " +
	                     "FROM orders o " +
	                     "JOIN users u ON o.UserID = u.UserID " +
	                     "JOIN orderDetail od ON o.OrderID = od.OrderID " +
	                     "JOIN course c ON od.CourseID = c.CourseID " +
	                     "GROUP BY o.OrderID, o.OrderDate, o.OrderStatus, u.Fullname " +
	                     "ORDER BY o.OrderDate DESC " +
	                     "LIMIT ?";
	        try {
	            return jdbcTemplate.query(sql, new Object[]{limit}, (rs, rowNum) -> new Object[]{
	                rs.getInt("OrderID"),
	                rs.getTimestamp("OrderDate").toLocalDateTime(),
	                rs.getString("OrderStatus"),
	                rs.getString("Fullname"),
	                rs.getString("CourseTitle")
	            });
	        } catch (Exception e) {
	            logger.error("Lỗi khi lấy danh sách đơn hàng gần đây: {}", e.getMessage());
	            throw new RuntimeException("Lỗi khi lấy danh sách đơn hàng gần đây: " + e.getMessage());
	        }
	    }
	 
	 
	 public List<Object[]> findAllOrders(int page, int size, String search, String status) {
	        StringBuilder sql = new StringBuilder(
	            "SELECT o.OrderID, o.OrderDate, o.OrderStatus, u.Fullname, o.TotalAmount, MIN(c.Title) as CourseTitle " +
	            "FROM orders o " +
	            "JOIN users u ON o.UserID = u.UserID " +
	            "JOIN orderDetail od ON o.OrderID = od.OrderID " +
	            "JOIN course c ON od.CourseID = c.CourseID "
	        );

	        List<Object> params = new ArrayList<>();
	        if (search != null && !search.trim().isEmpty()) {
	            sql.append("WHERE (o.OrderID LIKE ? OR u.Fullname LIKE ?) ");
	            params.add("%" + search + "%");
	            params.add("%" + search + "%");
	        }
	        if (status != null && !status.equals("all")) {
	            sql.append(search != null && !search.trim().isEmpty() ? "AND " : "WHERE ");
	            sql.append("o.OrderStatus = ? ");
	            params.add(status);
	        }

	        sql.append("GROUP BY o.OrderID, o.OrderDate, o.OrderStatus, u.Fullname, o.TotalAmount " +
	                   "ORDER BY o.OrderDate DESC " +
	                   "LIMIT ? OFFSET ?");
	        params.add(size);
	        params.add(page * size);

	        try {
	            return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> new Object[]{
	                rs.getInt("OrderID"),
	                rs.getTimestamp("OrderDate").toLocalDateTime(),
	                rs.getString("OrderStatus"),
	                rs.getString("Fullname"),
	                rs.getBigDecimal("TotalAmount"),
	                rs.getString("CourseTitle")
	            });
	        } catch (Exception e) {
	            logger.error("Lỗi khi lấy danh sách tất cả đơn hàng: {}", e.getMessage());
	            throw new RuntimeException("Lỗi khi lấy danh sách tất cả đơn hàng: " + e.getMessage());
	        }
	    }

	    public long countAllOrders(String search, String status) {
	        StringBuilder sql = new StringBuilder(
	            "SELECT COUNT(DISTINCT o.OrderID) " +
	            "FROM orders o " +
	            "JOIN users u ON o.UserID = u.UserID " +
	            "JOIN orderDetail od ON o.OrderID = od.OrderID " +
	            "JOIN course c ON od.CourseID = c.CourseID "
	        );

	        List<Object> params = new ArrayList<>();
	        if (search != null && !search.trim().isEmpty()) {
	            sql.append("WHERE (o.OrderID LIKE ? OR u.Fullname LIKE ?) ");
	            params.add("%" + search + "%");
	            params.add("%" + search + "%");
	        }
	        if (status != null && !status.equals("all")) {
	            sql.append(search != null && !search.trim().isEmpty() ? "AND " : "WHERE ");
	            sql.append("o.OrderStatus = ? ");
	            params.add(status);
	        }

	        try {
	            Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
	            return count != null ? count : 0;
	        } catch (Exception e) {
	            logger.error("Lỗi khi đếm tổng số đơn hàng: {}", e.getMessage());
	            throw new RuntimeException("Lỗi khi đếm tổng số đơn hàng: " + e.getMessage());
	        }
	    }

	    public Object[] findOrderDetailsById(int orderId) {
	        String sql = "SELECT o.OrderID, o.OrderDate, o.OrderStatus, o.TotalAmount, o.PromotionID, " +
	                     "u.UserID, u.Fullname, u.Email, u.PhoneNumber " +
	                     "FROM orders o " +
	                     "JOIN users u ON o.UserID = u.UserID " +
	                     "WHERE o.OrderID = ?";
	        try {
	            return jdbcTemplate.queryForObject(sql, new Object[]{orderId}, (rs, rowNum) -> {
	                Object[] result = new Object[12];
	                result[0] = rs.getInt("OrderID");
	                result[1] = rs.getTimestamp("OrderDate").toLocalDateTime();
	                result[2] = rs.getString("OrderStatus");
	                result[3] = rs.getBigDecimal("TotalAmount");
	                result[4] = rs.getObject("PromotionID") != null ? rs.getInt("PromotionID") : null;
	                result[5] = rs.getInt("UserID");
	                result[6] = rs.getString("Fullname");
	                result[7] = rs.getString("Email");
	                result[8] = rs.getString("PhoneNumber");
	                return result;
	            });
	        } catch (Exception e) {
	            logger.error("Lỗi khi lấy chi tiết đơn hàng {}: {}", orderId, e.getMessage());
	            throw new RuntimeException("Lỗi khi lấy chi tiết đơn hàng: " + e.getMessage());
	        }
	    }

	    public List<Object[]> findOrderDetailItems(int orderId) {
	        String sql = "SELECT od.OrderID, od.CourseID, od.Price, c.Title, c.Image, od.PromotionID, p.Code AS PromotionCode, p.DiscountPercentage " +
	                     "FROM orderDetail od " +
	                     "JOIN course c ON od.CourseID = c.CourseID " +
	                     "LEFT JOIN promotion p ON od.PromotionID = p.PromotionID " +
	                     "WHERE od.OrderID = ?";
	        try {
	            return jdbcTemplate.query(sql, new Object[]{orderId}, (rs, rowNum) -> new Object[]{
	                rs.getInt("OrderID"),
	                rs.getInt("CourseID"),
	                rs.getBigDecimal("Price"),
	                rs.getString("Title"),
	                rs.getString("Image"),
	                rs.getObject("PromotionID") != null ? rs.getInt("PromotionID") : null,
	                rs.getString("PromotionCode"),
	                rs.getBigDecimal("DiscountPercentage")
	            });
	        } catch (Exception e) {
	            logger.error("Lỗi khi lấy chi tiết khóa học cho đơn hàng {}: {}", orderId, e.getMessage());
	            return new ArrayList<>();
	        }
	    }

	    public void updateOrderStatus(int orderId, String status) {
	        String sql = "UPDATE orders SET OrderStatus = ? WHERE OrderID = ?";
	        try {
	            int rowsAffected = jdbcTemplate.update(sql, status, orderId);
	            if (rowsAffected == 0) {
	                throw new RuntimeException("Không tìm thấy đơn hàng với OrderID: " + orderId);
	            }
	            logger.info("Cập nhật trạng thái đơn hàng {} thành {}", orderId, status);
	        } catch (Exception e) {
	            logger.error("Lỗi khi cập nhật trạng thái đơn hàng {}: {}", orderId, e.getMessage());
	            throw new RuntimeException("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
	        }
	    }

}
