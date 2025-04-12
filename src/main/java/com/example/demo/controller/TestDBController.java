package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class TestDBController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@GetMapping("/test-db")
	@ResponseBody
	public String testDatabaseConnection() {
		try {
			Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "Kết nối thành công! " + result;
			
		} catch (Exception e) {
			return "Kết nối thất bại: " + e.getMessage();
		}

	}
	

}
