package com.nextenso.radius.agent.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.radius.AuthenticationManager;
import com.nextenso.proxylet.radius.AuthenticationRule;



public class AuthenticationManagerImpl
		implements AuthenticationManager {

	private ArrayList<AuthenticationRule> _secrets = new ArrayList<AuthenticationRule>();
	private static final Logger LOGGER = Logger.getLogger("agent.radius.authenticator.manager");

	/**
	 * @see java.util.ArrayList#trimToSize()
	 */
	public void trimToSize() {
		_secrets.trimToSize();
	}

	/**
	 * @param minCapacity
	 */
	public void ensureCapacity(int minCapacity) {
		_secrets.ensureCapacity(minCapacity);
	}

	/**
	 * @see java.util.List#size()
	 */
	public int size() {
		return _secrets.size();
	}

	/**
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return _secrets.isEmpty();
	}

	/**
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return _secrets.contains(o);
	}

	/**
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object o) {
		return _secrets.indexOf(o);
	}

	/**
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object o) {
		return _secrets.lastIndexOf(o);
	}

	/**
	 * @see java.util.List#iterator()
	 */
	public Iterator<AuthenticationRule> iterator() {
		return _secrets.iterator();
	}

	/**
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return _secrets.containsAll(c);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<AuthenticationRule> listIterator() {
		return _secrets.listIterator();
	}

	/**
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return _secrets.toArray();
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<AuthenticationRule> listIterator(int index) {
		return _secrets.listIterator(index);
	}

	/**
	 * @see java.util.List#toArray(T[])
	 */
	public <T> T[] toArray(T[] a) {
		return _secrets.toArray(a);
	}

	/**
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		return _secrets.removeAll(c);
	}

	/**
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		return _secrets.retainAll(c);
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public AuthenticationRule get(int index) {
		return _secrets.get(index);
	}

	/**
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public AuthenticationRule set(int index, AuthenticationRule element) {
		return _secrets.set(index, element);
	}

	/**
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(AuthenticationRule e) {
		return _secrets.add(e);
	}

	/**
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, AuthenticationRule element) {
		_secrets.add(index, element);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return _secrets.toString();
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List<AuthenticationRule> subList(int fromIndex, int toIndex) {
		return _secrets.subList(fromIndex, toIndex);
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public AuthenticationRule remove(int index) {
		return _secrets.remove(index);
	}

	/**
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		return _secrets.remove(o);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		return _secrets.equals(o);
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		_secrets.clear();
	}

	/**
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends AuthenticationRule> c) {
		return _secrets.addAll(c);
	}

	/**
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection<? extends AuthenticationRule> c) {
		return _secrets.addAll(index, c);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return _secrets.hashCode();
	}

	/**
	 * @see com.nextenso.proxylet.radius.AuthenticationManager#getRule(int)
	 */
	public AuthenticationRule getRule(int ip) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getProxySecret: manager= " + this);
		}
		for (AuthenticationRule rule : this) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("getProxySecret: for client= " + ip + ", trying  rule=" + rule);
			}
			if (rule.match(ip)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("getProxySecret: for client= " + ip + ", applying client rule=" + rule);
				}
				return rule;
			}
		}
		return null;
	}

}
