package net.etalia.crepuscolo.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.etalia.jalia.annotations.JsonInclude;
import net.etalia.jalia.annotations.JsonInclude.Include;
import net.etalia.jalia.annotations.JsonSetter;

public class PaginationList<T extends Jsonable> {

	private List<T> data = new ArrayList<T>();
	private Pagination pagination = new Pagination();

	@SuppressWarnings("unused")
	private PaginationList() {
	}

	public PaginationList(int offset, int count) {
		pagination.offset = offset;
		pagination.count = count;
	}

	public PaginationList<T> setFullData(Set<? extends T> fullData) {
		return setFullData(new ArrayList<T>(fullData));
	}

	@JsonSetter
	public PaginationList<T> setFullData(List<? extends T> fullData) {
		pagination.max = fullData.size();
		int to = pagination.offset + pagination.count;
		if (to > pagination.max) {
			to = pagination.max;
		}
		data = new ArrayList<T>(fullData.subList(pagination.offset, to));
		return this;
	}

	public PaginationList<T> setPartialData(Collection<? extends T> partData) {
		data = new ArrayList<T>(partData);
		return this;
	}
	
	public void setMax(int max) {
		pagination.max = max;
	}

	@JsonInclude(Include.NOT_NULL)
	public List<T> getData() {
		return data;
	}

	public Pagination getPagination() {
		return pagination;
	}

	public static class Pagination {
		public Integer offset = 0;
		public Integer count;
		public Integer max;
		public Pagination(){}

		public Integer getOffset() {
			return offset;
		}
		public Integer getCount() {
			return count;
		}
		public Integer getMax() {
			return max;
		}
	
	}

}
