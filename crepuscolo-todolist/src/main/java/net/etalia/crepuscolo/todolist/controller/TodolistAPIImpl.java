package net.etalia.crepuscolo.todolist.controller;

import java.util.logging.Logger;

import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.todolist.domain.User;
import net.etalia.crepuscolo.utils.HttpException;
import net.etalia.crepuscolo.utils.ParMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class TodolistAPIImpl implements TodolistAPI {

	protected final static Logger log = Logger.getLogger(TodolistAPIImpl.class.getName());

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
			log.fine("User already exist!");
			throw new HttpException().statusCode(500).errorCode("U01").message("User Already Exist");
		}
		creation.assignId(user);
		user.setPassword(authService.hidePassword((String) user.getExtraData("password")));
		user = storage.save(user);
		return user;
	}

}
