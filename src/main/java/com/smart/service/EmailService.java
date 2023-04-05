package com.smart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    public boolean sendEmail(String toEmail, String subject, String body){
        boolean f =false;
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("moghtaderfarsina@gmail.com");
            message.setTo(toEmail);
            message.setText(body);
            message.setSubject(subject);
            mailSender.send(message);

            System.out.println("Mail send successfully");
            f=true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return f;
    }
}
