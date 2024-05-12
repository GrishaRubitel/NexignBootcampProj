package com.bootcamp_proj.bootcampproj;

import com.bootcamp_proj.bootcampproj.standalone_services.BrtHandler;
import com.bootcamp_proj.bootcampproj.standalone_services.CdrGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@SpringBootApplication
@ComponentScan(basePackages = {"com.bootcamp_proj"})
public class BootcampProjApplication {
	public static void main(String[] args) {
		SpringApplication.run(BootcampProjApplication.class, args);

		System.out.println("start");
		BrtHandler brt = BrtHandler.getInstance();
		brt.startWithExistingFile();
		System.out.println("end");
		System.out.println("start");
		brt.startWithExistingFile();
		System.out.println("end");
		System.out.println("start");
		brt.startWithExistingFile();
		System.out.println("end");
		System.out.println("start");
		brt.startWithExistingFile();
		System.out.println("end");
		System.out.println("start");
		brt.startWithExistingFile();
		System.out.println("end");
		System.out.println("start");System.out.println("start");System.out.println("start");System.out.println("start");
		CdrGenerator cdr = CdrGenerator.getInstance();
        try {
            cdr.switchEmulator();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


	}
}