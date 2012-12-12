/*
 * Copyright (C) 2012 - 2012 NHN Corporation
 * All rights reserved.
 *
 * This file is part of The nGrinder software distribution. Refer to
 * the file LICENSE which is part of The nGrinder distribution for
 * licensing details. The nGrinder distribution is available on the
 * Internet at http://nhnopensource.org/ngrinder
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ngrinder.user.controller;

import static org.ngrinder.common.util.Preconditions.checkArgument;
import static org.ngrinder.common.util.Preconditions.checkNotEmpty;
import static org.ngrinder.common.util.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.controller.NGrinderBaseController;
import org.ngrinder.model.Permission;
import org.ngrinder.model.Role;
import org.ngrinder.model.User;
import org.ngrinder.user.service.UserContext;
import org.ngrinder.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User management controller.
 * 
 * @author JunHo Yoon
 * @since 3.0
 * 
 */
@Controller
@RequestMapping("/user")
public class UserController extends NGrinderBaseController {

	@Autowired
	private UserService userService;

	/**
	 * Get user list on the given role.
	 * 
	 * @param model
	 *            model
	 * @param roleName
	 *            role
	 * @param keywords
	 *            search keyword.
	 * @return user/userList
	 */
	@PreAuthorize("hasAnyRole('A')")
	@RequestMapping("/list")
	public String getUserList(ModelMap model, @RequestParam(required = false) String roleName,
					@RequestParam(required = false) String keywords) {

		List<User> userList = null;
		if (StringUtils.isEmpty(keywords)) {
			userList = userService.getAllUserByRole(roleName);
		} else {
			userList = userService.getUserListByKeyWord(keywords);
			model.put("keywords", keywords);
		}

		model.addAttribute("userList", userList);
		EnumSet<Role> roleSet = EnumSet.allOf(Role.class);
		model.addAttribute("roleSet", roleSet);
		model.addAttribute("roleName", roleName);

		return "user/userList";
	}

	/**
	 * Get user detail page.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            mode
	 * @param userId
	 *            user to get
	 * @return "user/userDetail"
	 */
	@RequestMapping("/detail")
	@PreAuthorize("hasAnyRole('A') or #user.userId == #userId")
	public String getUserDetail(User user, final ModelMap model, @RequestParam(required = false) final String userId) {

		List<User> userList = userService.getAllUserByRole(null);
		model.addAttribute("userList", userList);
		EnumSet<Role> roleSet = EnumSet.allOf(Role.class);
		model.addAttribute("roleSet", roleSet);

		User retrievedUser = userService.getUserById(userId);

		getUserShareList(retrievedUser, model);
		model.addAttribute("user", retrievedUser);
		return "user/userDetail";
	}

	/**
	 * Save or Update user detail info.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            model
	 * @param updatedUser
	 *            user to be updated.
	 * @param followersStr
	 *            user Id list that current will share his permission to.
	 * @return "redirect:/user/list" if current user change his info, otheriwise return "redirect:/"
	 */
	@RequestMapping("/save")
	@PreAuthorize("hasAnyRole('A') or #user.id == #updatedUser.id")
	public String saveOrUpdateUserDetail(User user, ModelMap model, @ModelAttribute("user") User updatedUser,
					@RequestParam(required = false) String followersStr) {
		checkArgument(updatedUser.validate());
		if (user.getRole() == Role.USER) {
			// General user can not change their role.
			User updatedUserInDb = userService.getUserById(updatedUser.getUserId());
			checkNotNull(updatedUserInDb);
			updatedUser.setRole(updatedUserInDb.getRole());

			// prevent user to modify with other user id
			checkArgument(updatedUserInDb.getId().equals(updatedUser.getId()), "Illegal request to update user:%s",
							updatedUser);
		}
		if (updatedUser.exist()) {
			userService.modifyUser(updatedUser, followersStr);
		} else {
			userService.saveUser(updatedUser);
		}
		model.clear();
		if (user.getId().equals(updatedUser.getId())) {
			return "redirect:/";
		} else {
			return "redirect:/user/list";
		}
	}

	/**
	 * Delete users.
	 * 
	 * @param model
	 *            model
	 * @param userIds
	 *            comma separated user ids.
	 * @return "redirect:/user/list"
	 */
	@PreAuthorize("hasAnyRole('A')")
	@RequestMapping("/delete")
	public String deleteUser(ModelMap model, @RequestParam String userIds) {
		String[] ids = userIds.split(",");
		ArrayList<String> aListNumbers = new ArrayList<String>(Arrays.asList(ids));
		userService.deleteUsers(aListNumbers);
		model.clear();
		return "redirect:/user/list";
	}

	/**
	 * Check the user id existence.
	 * 
	 * @param model
	 *            model
	 * @param userId
	 *            userId to be checked
	 * @return success json if true.
	 */
	@PreAuthorize("hasAnyRole('A')")
	@RequestMapping("/checkUserId")
	@ResponseBody
	public String checkUserId(ModelMap model, @RequestParam String userId) {
		User user = userService.getUserById(userId);
		return (user == null) ? returnSuccess() : returnError();
	}

	/**
	 * Get the current user profile.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            model
	 * @return "user/userInfo"
	 */
	@RequestMapping("/profile")
	public String userProfile(User user, ModelMap model) {
		checkNotEmpty(user.getUserId(), "UserID should not be NULL!");
		User newUser = userService.getUserByIdWithoutCache(user.getUserId());
		model.addAttribute("user", newUser);
		model.addAttribute("action", "profile");
		getUserShareList(newUser, model);
		return "user/userInfo";
	}

	/**
	 * Get the follower list.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            model
	 * @return "user/userOptionGroup"
	 */
	@RequestMapping("/switchUserList")
	public String switchUserList(User user, ModelMap model) {
		User currUser = userService.getUserByIdWithoutCache(user.getUserId());
		checkNotNull(currUser);
		if (user.getRole().hasPermission(Permission.SWITCH_TO_ANYONE)) {
			List<User> allUserByRole = userService.getAllUserByRole(Role.USER.getFullName());
			model.addAttribute("shareUserList", allUserByRole);
		} else {
			model.addAttribute("shareUserList", currUser.getOwners());
		}
		return "user/userOptionGroup";
	}

	/**
	 * Switch user identity.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            model
	 * @param switchUserId
	 *            the user who will switch.
	 * 
	 * @return redirect:/perftest/list
	 */
	@RequestMapping("/switchUser")
	public String switchUser(User user, ModelMap model,
					@RequestParam(required = false, defaultValue = "") String switchUserId,
					HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		Cookie cookie = new Cookie("switchUser", switchUserId);
		cookie.setPath("/");
		// Delete Cookie if empty switchUser
		if (StringUtils.isEmpty(switchUserId)) {
			cookie.setMaxAge(0);
		}
		httpServletResponse.addCookie(cookie);

		model.clear();
		return "redirect:/perftest/list";
	}

	/**
	 * Get user list that current user will be shared, excluding current user.
	 * 
	 * @param user
	 *            current user
	 * @param model
	 *            model
	 */
	private void getUserShareList(User user, ModelMap model) {
		if (user == null) {
			return;
		}

		List<User> currFollowers = user.getFollowers();
		List<User> userList = new ArrayList<User>();
		String userId = user.getUserId();

		for (User u : userService.getAllUserByRole(Role.USER.getFullName())) {
			if (u.getUserId().equals(userId)) {
				continue;
			}

			userList.add(u.getUserBaseInfo());
		}

		model.addAttribute("followers", currFollowers);
		model.addAttribute("shareUserList", userList);
	}
}
