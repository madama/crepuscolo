package net.etalia.crepuscolo.services;

import net.etalia.crepuscolo.domain.BaseEntity;


public interface CreationService {

	/**
	 * Create an instance for a given domain class
	 * @param clazz The domain class
	 * @return The instance
	 */
	public <T extends BaseEntity> T newInstance(Class<T> clazz);

	/**
	 * Return the instance for the given identifier without load it,
	 * only the id is setted.
	 * @param id The identifier to set
	 * @return The instance
	 */
	public <T extends BaseEntity> T getEmptyInstance(String id);

	/**
	 * Generate and assign an ID to the given object
	 * @param obj
	 */
	public void assignId(BaseEntity obj);

}
