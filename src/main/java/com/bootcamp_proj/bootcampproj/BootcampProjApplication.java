package com.bootcamp_proj.bootcampproj;

import com.bootcamp_proj.bootcampproj.standalone_services.CdrGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@SpringBootApplication
@ComponentScan(basePackages = {"com.bootcamp_proj"})
public class BootcampProjApplication {
	public static final String PSQL_HOST = "jdbc:postgresql://localhost:5432/bootcamp_pro";
	public static final String PSQL_USER = "postgres";
	public static final String PSQL_PASS = "dvpsql";

	public static void main(String[] args) {
		SpringApplication.run(BootcampProjApplication.class, args);
		
		System.out.println("start");
        CdrGenerator cdrGenerator = CdrGenerator.getInstance();
		try {
			cdrGenerator.switchEmulator();
		} catch (InterruptedException e) {
            System.out.println(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("finish");
	}
}
