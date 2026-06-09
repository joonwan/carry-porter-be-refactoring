package com.e101.carry_porter;

import org.springframework.boot.SpringApplication;

public class TestCarryPorterApplication {

	public static void main(String[] args) {
		SpringApplication.from(CarryPorterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
