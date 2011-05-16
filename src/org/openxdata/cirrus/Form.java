package org.openxdata.cirrus;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class Form {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	Key id;

	@Persistent
	@Unique
	String path;

	@Persistent
	Blob formContent;

	public Blob getFormContent() {
		return formContent;
	}

	public void setFormContent(Blob formContent) {
		this.formContent = formContent;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Key getId() {
		return id;
	}
}
