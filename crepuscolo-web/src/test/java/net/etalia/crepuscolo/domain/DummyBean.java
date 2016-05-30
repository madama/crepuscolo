package net.etalia.crepuscolo.domain;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import net.etalia.crepuscolo.auth.MockCheck;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.jalia.annotations.JsonDefaultFields;

@JsonDefaultFields("name,email,age")
@Entity
public class DummyBean extends BaseEntity {

	private String name;
	private String email;
	private int age;
	
	
	@NotNull(message="Name is required", groups={Default.class, DummyBean.class})
	@Length(min=2,max=50)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Email
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Range(min=18,max=110,message="Too young or too old")
	@MockCheck(testValue="getterCheck")
	public int getAge() {
		return age;
	}
	@MockCheck(testValue="setterCheck")
	public void setAge(int age) {
		this.age = age;
	}
	
}
