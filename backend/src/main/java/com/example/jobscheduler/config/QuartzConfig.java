package com.example.jobscheduler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import jakarta.annotation.PostConstruct;
import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;

/**
 * Configuration for Quartz scheduler.
 * This class sets up the Quartz scheduler with Spring integration.
 */
@Configuration // Marks this class as a configuration class for Spring.
public class QuartzConfig {

    @Autowired
    private ApplicationContext applicationContext; // Spring application context for managing beans.

    /**
     * Creates a job factory that autowires jobs with Spring beans.
     * This ensures that Quartz jobs can use Spring-managed beans.
     *
     * @return the SpringBeanJobFactory
     */
    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext); // Set the application context for autowiring.
        return jobFactory;
    }

    /**
     * Creates the scheduler factory bean.
     * This factory bean is responsible for creating and configuring the Quartz scheduler.
     *
     * @return the SchedulerFactoryBean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(springBeanJobFactory()); // Use the custom job factory for autowiring.
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true); // Wait for jobs to complete before shutting down.
        schedulerFactory.setAutoStartup(true); // Automatically start the scheduler on application startup.
        return schedulerFactory;
    }

    /**
     * Gets the scheduler from the factory bean.
     * The scheduler is the main interface for managing Quartz jobs and triggers.
     *
     * @return the Scheduler
     */
    @Bean
    public Scheduler scheduler() {
        return schedulerFactoryBean().getScheduler(); // Retrieve the scheduler instance from the factory bean.
    }

    /**
     * A custom JobFactory that autowires Quartz job beans with Spring context.
     * This ensures that Quartz jobs can use Spring-managed dependencies.
     */
    public static final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

        private ApplicationContext applicationContext; // Spring application context for autowiring.

        /**
         * Sets the application context for this job factory.
         *
         * @param applicationContext the Spring application context
         * @throws BeansException if there is an issue with the application context
         */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
        }

        /**
         * Creates a job instance and autowires it with Spring-managed beans.
         *
         * @param bundle the TriggerFiredBundle containing job details
         * @return the job instance with dependencies autowired
         * @throws Exception if there is an issue creating the job instance
         */
        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle); // Create the job instance using the parent class.
            applicationContext.getAutowireCapableBeanFactory().autowireBean(job); // Autowire the job with Spring beans.
            return job;
        }
    }
}