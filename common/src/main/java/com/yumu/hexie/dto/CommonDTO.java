package com.yumu.hexie.dto;

import java.io.Serializable;

/**
 * 通用dto
 * @author david
 *
 * @param <T>
 * @param <V>
 */
public class CommonDTO<T, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4769898288243201110L;
	
	private T data1;
	private V data2;
	
	public T getData1() {
		return data1;
	}
	public void setData1(T data1) {
		this.data1 = data1;
	}
	public V getData2() {
		return data2;
	}
	public void setData2(V data2) {
		this.data2 = data2;
	}
	
	

}
