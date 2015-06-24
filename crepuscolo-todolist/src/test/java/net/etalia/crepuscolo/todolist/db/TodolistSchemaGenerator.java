package net.etalia.crepuscolo.todolist.db;

import net.etalia.crepuscolo.test.db.SchemaGenerator;

public class TodolistSchemaGenerator extends SchemaGenerator {

	public TodolistSchemaGenerator(String packageName) throws Exception {
		super(packageName);
	}

	public static void main(String[] args) throws Exception {
		TodolistSchemaGenerator gen = new TodolistSchemaGenerator("net.etalia.crepuscolo.todolist.domain");
		gen.generate(Dialect.HSQL);
	}

}
