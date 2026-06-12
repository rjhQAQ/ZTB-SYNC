package org.example.ztbsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("org.example.ztbsync.mapper")
public class ZtbSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZtbSyncApplication.class, args);
    }

}
