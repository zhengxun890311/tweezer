package com.skcc.tweezer.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.skcc.tweezer.models.Reply;
import com.skcc.tweezer.models.Tweet;
import com.skcc.tweezer.models.User;
import com.skcc.tweezer.services.UserService;
import com.skcc.tweezer.validators.UserValidator;


@Controller
public class UsersCtrl {
	@Autowired
	private UserService uS;

	@Autowired
	private UserValidator uV;
	
	@Autowired
	private MessageSource mS;
	
	@GetMapping("/")
	public String index(@ModelAttribute("userObj") User user) {
		return "loginreg.jsp";
	}
	
    @PostMapping("/registration")
    public String registerUser(@Valid @ModelAttribute("userObj") User user, BindingResult result, HttpSession session) {
    	uV.validate(user,  result);
    	if (result.hasErrors()) {
    		return "loginreg.jsp";
    	} else {
    		User u = uS.registerUser(user);
    		SendEmail sendEmail = new SendEmail();
    		sendEmail.sendEmail(u);
    		session.setAttribute("userId",  u.getId());
    		return "redirect:/home";
    	}
    }
    
    @PostMapping("/login")
    public String loginUser(@ModelAttribute("user") User user, @RequestParam("email") String email, @RequestParam("password") String password, Model model, HttpSession session) {
    	boolean isAuthenticated = uS.authenticateUser(email, password);
    	if (isAuthenticated) {
    		User u = uS.findByEmail(email);
    		session.setAttribute("userId", u.getId());
    		return "redirect:/home";
    	} else {
    		model.addAttribute("error", "invalid credentials");
    		return "loginreg.jsp";
    	}
    }
    
    @GetMapping("/home")
    public String home(@ModelAttribute("tweetObj") Tweet tweet, Model model, @ModelAttribute("replyObj") Reply reply, HttpSession session) {
    	Long userId = (Long) session.getAttribute("userId");
    	if (userId==null) {
    		return "redirect:/";
    	}else {
    		User u = uS.findUserById(userId);
    		System.out.println(u.getUserPhotoPath());
    		model.addAttribute("user", u);
//    		for (User user: u.getUserFollowing()) {
//    			System.out.println(user.getFirstName());
//    		}
    		model.addAttribute("following", u.getUserFollowing());
    		model.addAttribute("loggedUser", uS.findUserById(userId));
    		model.addAttribute("followingTweets", uS.getFollowingTweets(userId));
    		return "home.jsp";    		   		
    	}
    }
    
    @GetMapping("/editprofile")
    public String editUser(@ModelAttribute("user") User user, Model model, HttpSession session) {
    	Long userId = (Long) session.getAttribute("userId");
    	model.addAttribute("user", uS.findUserById(userId));
    	return "editprofile.jsp";
    }
    
    @PostMapping("/editprofile")
    public String updateUser(@Valid @ModelAttribute("user") User user, 
    		BindingResult result, 
    		Model model, 
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request,
    		HttpSession session,
    		@RequestParam(value = "myfile") MultipartFile image) {
		String path = request.getSession().getServletContext().getRealPath("/images");
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
		String random_photo_name = UUID.randomUUID().toString().replaceAll("-", "");
		try {
			image.transferTo(new File(path + "/" + random_photo_name + "." + "jpg"));
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String url = "images/" + random_photo_name + "." + "jpg";
		System.out.println("database url is：" + url);
		user.setUserPhotoPath(url);
    	List<String> messages = new ArrayList<String>();
    	if (result.hasErrors()) {
			for (Object object: result.getAllErrors()) {
				if (object instanceof FieldError) {
					FieldError fieldError = (FieldError) object;
					String error = mS.getMessage(fieldError, null);
					messages.add(error);
				}
			}
			redirectAttributes.addFlashAttribute("messages", messages);
			return "redirect:/editprofile";
    	} else {
    		uS.updateUser(user.getId(), user.getFirstName(), user.getLastName(), user.getBirthday(),user.getUserPhotoPath());
    		return "redirect:/home";
    	}
    }
    
    @GetMapping("/users/{id}")
    public String show(Model model, @ModelAttribute("followUserObj") User followUser, @ModelAttribute("unfollowUserObj") User unfollowUser, @ModelAttribute("replyObj") Reply reply, @PathVariable("id") Long id, HttpSession session) {
    	Long userId = (Long) session.getAttribute("userId");
    	model.addAttribute("user", uS.findUserById(id));
    	model.addAttribute("loggedUser", uS.findUserById(userId));
    	return "profile.jsp";
    }
    
//    @PostMapping("/followUser")
//    public String follow(@ModelAttribute("user") User following, HttpSession session) {
//    	Long userId = (Long) session.getAttribute("userId");
//    	User user = uS.findUserById(userId);
//    	User follower = uS.findUserById(following.getId());
//    	uS.followUser(userId, following.getId());
////    	uS.saveFriend(friendship);
////    	uS.saveFriendship(user, follower);
//    	return "redirect:/users/" + following.getId();
//    }
    
//    @GetMapping("/users/{id}")
//    public String show(Model model, @PathVariable("id") Long id, @ModelAttribute("friendship") Friendship friendship, HttpSession session) {
//    	model.addAttribute("user", uS.findUserById(id));
//    	model.addAttribute("loggeduser", session.getAttribute("userId"));
//    	return "profile.jsp";
//    }
//    
    @PostMapping("/followUser")
    public String follow(@ModelAttribute("followUserObj") User following, HttpSession session) {
    	Long userId = (Long) session.getAttribute("userId");
//    	User user = uS.findUserById(userId);
//    	User follower = uS.findUserById(following.getId());
    	uS.followUser(userId, following.getId());
    	return "redirect:/users/" + following.getId();
    }
    
    @PostMapping("/unfollowUser")
    public String unfollow(@ModelAttribute("unfolowUserObj") User unfollow, HttpSession session) {
    	Long userId = (Long) session.getAttribute("userId");
    	uS.unfollowUser(userId, unfollow.getId());
    	return "redirect:/users/" + unfollow.getId();
    }
 
    @RequestMapping("/logout")
    public String logout(HttpSession session) {
    	session.invalidate();
    	return "redirect:/";
    }
}