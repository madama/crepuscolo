package net.etalia.crepuscolo.todolist.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.todolist.domain.User;
import net.etalia.crepuscolo.utils.HttpException;
import net.etalia.crepuscolo.utils.ParMap;

@Controller
public class TodolistAPIImpl implements TodolistAPI {

	protected Log log = LogFactory.getLog(TodolistAPIImpl.class);

	@Autowired
	private StorageService storage;

	@Autowired
	private CreationService creation;

	@Autowired
	private AuthService authService;

	@Override
	@Transactional
	public User addUser(User user) {
		User exist = storage.load(User.Queries.BY_EMAIL, new ParMap("email", user.getEmail()));
		if (exist != null) {
			log.debug("User already exist!");
			throw new HttpException().statusCode(500).errorCode("U01").message("User Already Exist");
		}
		creation.assignId(user);
		user.setPassword(authService.hidePassword((String) user.getExtraData("password")));
		user = storage.save(user);
		return user;
	}

}
