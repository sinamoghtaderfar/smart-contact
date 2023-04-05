package com.smart.controller;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Random;

@Controller
public class ForgotController {

    Random random = new Random(10000);
    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    //email id form open handler
    @RequestMapping("/forgot")
    public String openEmail(){

        return "forgot_email_form";
    }

    @PostMapping("/send-opt")
    public String sendOPT(@RequestParam("email")String email, HttpSession session){
        System.out.println(email);
        //generate opt of 4 digit

        int otp = random.nextInt(9999999);

        System.out.println("opt" + otp);

        // send otp to email

        String subject = "OPT From SCM";
        String message = " OTP = "+otp;
        String to = email;

        boolean flag = this.emailService.sendEmail(email,subject,message);
        if(flag){
            session.setAttribute("myotp",otp);
            session.setAttribute("email",email);
            return "verify_otp";
        }else {
            session.setAttribute("message","Check your email id !!");
            return "forgot_email_form";
        }

    }

    //verify otp
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") int otp,HttpSession session){

        int myOtp = (int)session.getAttribute("myotp");
        String email = (String) session.getAttribute("email");

        if(myOtp == otp){

            //password change form;

            User user = this.userRepository.getUserByUserName(email);

            if(user == null){
                //send error message
                session.setAttribute("message","User dose not exist with this email !!");
                return "forgot_email_form";

            }else {
                //send change password form
                return "password_change_form";
            }
        }else {
            session.setAttribute("message","you have entered wrong otp !!");
            return "verify_otp";
        }
    }

    //change password save
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("newPassword") String newPassword,HttpSession session){

        String email = (String) session.getAttribute("email");
        User user = this.userRepository.getUserByUserName(email);
        user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
        this.userRepository.save(user);

        return "redirect:/signin?change=password changed successfully";

    }
}
