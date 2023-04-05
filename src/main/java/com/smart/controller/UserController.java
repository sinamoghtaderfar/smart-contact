package com.smart.controller;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.Myorder;
import com.smart.entities.User;
import com.smart.helper.Message;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import com.razorpay.*;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private MyOrderRepository myOrderRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME "+userName);

		//get the user using usernamne(Email)

		User user = userRepository.getUserByUserName(userName);

		System.out.println("USER "+user);

		model.addAttribute("user",user);

	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		String userName = principal.getName();
		System.out.println("USERNAME" + userName);
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}

	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());

		return "normal/add_contact_form";
	}

	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,HttpSession session) {
		try {

		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);

		//processing and uploading file..

		if(file.isEmpty())
		{
			//if the file is empty then try our message
			System.out.println("File is empty");
			contact.setImage("contact.png");
		}
		else {
			//file the file to folder and update the name to contact
//			String generateRandomFileName = String.valueOf(java.util.UUID.randomUUID());
			contact.setImage(file.getOriginalFilename());

			File saveFile=new ClassPathResource("static/img").getFile();

			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());

			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			System.out.println("Image is uploaded");

		}

		user.getContacts().add(contact);

		contact.setUser(user);

		this.userRepository.save(user);

		System.out.println("DATA "+contact);

		System.out.println("Added to data base");

		//message success contact
		session.setAttribute("message", new Message("Your contact is added !! Add more..", "success") );

		}catch (Exception e) {
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
		//message error contact
			session.setAttribute("message", new Message("Some went wrong !! Try again..", "danger") );

		}

		return "normal/add_contact_form";
	}

	//show contacts handler
	//per page = 5[n]
	//current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page ,Model m,Principal principal) {
		m.addAttribute("title","Show User Contacts");
		//contact ki list ko bhejni hai

		String userName = principal.getName();

		User user = this.userRepository.getUserByUserName(userName);

		//currentPage-page
		//Contact Per page - 5
		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);

		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());

		return "normal/show_contacts";
	}
	// show particular contact details...
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal){
		System.out.println("cId "+cId);

		Optional<Contact> contactOptional =  this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		// chcek contact id Which user
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
			if(user.getId() == contact.getUser().getId()){
				model.addAttribute("contact",contact);
				model.addAttribute("title",contact.getName());
			}

		return "normal/contact_detail";
	}
	// delete contact handler
	@GetMapping("/delete/{cid}")
	@Transactional
	public String deleteContact(@PathVariable("cid")Integer cId,Model model,Principal principal,HttpSession session) throws IOException {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		// check...assignment..
		//delete photo
		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
		File deleteFile = new ClassPathResource("static/img").getFile();
		File file1 = new File(deleteFile,oldContactDetail.getImage());
		file1.delete();

		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);

		session.setAttribute("message",new Message("Contact delete successfully","success"));
		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model){
		model.addAttribute("title","Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact",contact);
		return "normal/update_form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update",method = RequestMethod.POST)
	public String updateHandle(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Model model,HttpSession session,Principal principal){
		try{
			//old contact details
			Contact oldContactDetails = this.contactRepository.findById(contact.getcId()).get();
			//img
			if(!file.isEmpty()){
				//delete old photo and update new photo

				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile,oldContactDetails.getImage());
				file1.delete();

				// update

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			}else {
				contact.setImage(oldContactDetails.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());

			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message",new Message("Your contact is updated...","success"));
		}catch (Exception e){
			e.printStackTrace();
		}
//		System.out.println("contact" + contact.getName());
//		System.out.println("contact" + contact.getcId());

		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model){
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}

	//open settings handler
	@GetMapping("/settings")
	public String openSettings(){

		return "normal/settings";
	}

	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("old-Password") String oldPassword, @RequestParam("new-Password") String newPassword, Principal principal,HttpSession session){
		System.out.println("old pass " + oldPassword);
		System.out.println("new pass " + newPassword);

		String userName = principal.getName();

		User currentUser = this.userRepository.getUserByUserName(userName);

		System.out.println(currentUser.getPassword());

		if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword())){
			//change password

			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message",new Message("Your password is successfully change","success"));

		}else {
			//error
			session.setAttribute("message",new Message("Pleas enter correct old password","danger"));
			return "redirect:/user/settings";

		}

		return "redirect:/user/index";
	}

//	__________________________________________________

	// creaiting order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception {

//		System.out.println("order controller execute");
		System.out.println(data);

		int amount = Integer.parseInt(data.get("amount").toString());

		var client = new RazorpayClient("rzp_test_Ypve9lgozIqArN","dJZYZMnwZB5CusqgYWNoFdfV");

		JSONObject object = new JSONObject();
		object.put("amount",amount*100);
		object.put("currency","INR");
		object.put("receipt","txn_235425");

		//creating new order

		Order order = client.Orders.create(object);

		System.out.println(order);
		//save order in db
		Myorder myorder = new Myorder();
		myorder.setAmount(order.get("amount")+"");
		myorder.setOrderId(order.get("id"));
		myorder.setPaymentId(null);
		myorder.setStatus("create");
		myorder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myorder.setReceipt(order.get("receipt"));

		this.myOrderRepository.save(myorder);

		//if you want can save this to your data...
		return order.toString();
	}
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String ,Object> data){

		Myorder myorder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());

		myorder.setPaymentId(data.get("payment_id").toString());
		myorder.setStatus(data.get("status").toString());
		this.myOrderRepository.save(myorder);

		System.out.println(data);

		return ResponseEntity.ok(Map.of("msg","updated"));
	}
}
