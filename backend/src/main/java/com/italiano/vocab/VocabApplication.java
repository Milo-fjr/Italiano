package com.italiano.vocab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.italiano.vocab.mapper")
public class VocabApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabApplication.class, args);
    }
}
