package com.freshcart.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final boolean devLogOtp;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.otp.dev-log:false}") boolean devLogOtp,
                        @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.devLogOtp = devLogOtp;
        this.from = from;
    }

    public void sendOtp(String to, String otp) {
        if (enabled) {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!from.isBlank()) message.setFrom(from);
            message.setTo(to);
            message.setSubject("FreshCart email verification OTP");
            message.setText("Your FreshCart verification OTP is " + otp + ". It expires in 5 minutes.");
            mailSender.send(message);
        } else if (devLogOtp) {
            log.warn("FreshCart OTP for {}: {} (development logging enabled)", to, otp);
        }
    }
}
