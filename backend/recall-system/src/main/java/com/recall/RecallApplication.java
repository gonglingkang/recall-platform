package com.recall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 待办系统启动类。
 * <p>
 * 扫描范围：com.recall（含 common 模块的 GlobalExceptionHandler 等）。
 * Mapper 扫描：com.recall.dao 下按业务细分的各 Mapper。
 *
 * @author recall
 */
@SpringBootApplication
@MapperScan("com.recall.dao.**")
public class RecallApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecallApplication.class, args);
    }
}
