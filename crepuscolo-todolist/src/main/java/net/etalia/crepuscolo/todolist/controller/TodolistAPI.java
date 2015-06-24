package net.etalia.crepuscolo.todolist.controller;

import net.etalia.crepuscolo.todolist.domain.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public interface TodolistAPI {

	@RequestMapping(value="/user", method=RequestMethod.POST)
	public @ResponseBody @ResponseStatus(HttpStatus.CREATED) User addUser(@RequestBody User user);

}
