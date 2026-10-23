package com.legacyvault.config;

import com.legacyvault.job.DmsTriggerQuartzJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Quartz Scheduler:
 * Khởi tạo JobDetail và Trigger chạy theo cú pháp cron cấu hình trong application.yml.
 * Tham khảo: https://www.baeldung.com/quartz
 */
@Configuration
public class QuartzConfig {

    public static final String DMS_JOB_GROUP = "dms-jobs";
    public static final String DMS_JOB_NAME = "dmsTriggerQuartzJob";
    public static final String DMS_TRIGGER_NAME = "dmsTriggerCron";

    @Value("${dms.scheduler.cron:0 * * ? * *}")
    private String dmsCronExpression;

    @Bean
    public JobDetail dmsJobDetail() {
        return JobBuilder.newJob(DmsTriggerQuartzJob.class)
                .withIdentity(DMS_JOB_NAME, DMS_JOB_GROUP)
                .withDescription("Job kiểm tra Dead Man's Switch để phát hiện Vault Owner mất liên lạc và kích hoạt bàn giao")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dmsJobTrigger(JobDetail dmsJobDetail) {
        // Cấu hình chạy theo cú pháp Cron từ file application.yml (mặc định: "0 * * ? * *" = mỗi 1 phút 1 lần)
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(dmsCronExpression);

        return TriggerBuilder.newTrigger()
                .forJob(dmsJobDetail)
                .withIdentity(DMS_TRIGGER_NAME, DMS_JOB_GROUP)
                .withDescription("Trigger Quartz chạy theo cú pháp cron: " + dmsCronExpression)
                .withSchedule(scheduleBuilder)
                .build();
    }
}
