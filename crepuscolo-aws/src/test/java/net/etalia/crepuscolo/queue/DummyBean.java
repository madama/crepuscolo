package net.etalia.crepuscolo.queue;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.jalia.annotations.JsonDefaultFields;

@JsonDefaultFields("name,email,age")
public class DummyBean extends BaseEntity {

	private String name;
	private String email;
	private int age;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}

}
