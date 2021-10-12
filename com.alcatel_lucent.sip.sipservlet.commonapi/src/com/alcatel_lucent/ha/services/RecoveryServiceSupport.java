// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.ha.services;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.servlet.ServletContext;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering2.StopWatch;
import com.alcatel.as.service.metering2.util.SlidingAverage;
import com.alcatel.as.session.distributed.MarkException;
import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.Session.Attribute;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;
import com.alcatel.as.session.distributed.TransactionListener;
import com.alcatel.as.session.distributed.event.SessionEvent;
import com.alcatel.as.session.distributed.event.SessionEventFilter;
import com.alcatel.as.session.distributed.event.SessionListener;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.osgi.ServiceRegistry;

/**
 * 
 * Abstract Generics implementation of the flattable framework over the distributed session <br>
 * <T> is the type of the root of object the ha context will received <br>
 * One subclass defines <T> as javax.servlet.sip.SipApplicationSession <br>
 * The implementation writes Flattable objects into Map that is directly bound over into the Distibuted Session
 * attributes step.
 */
abstract public class RecoveryServiceSupport<T extends Flattable> implements
		RecoveryService<T>, Runnable {
	protected SessionManager _sm;
	protected SessionType _type;
	private String _typename = "sipagent";
	public static boolean isNewHAEnabled = false;
	protected static final String APPNAME = "._APP";
	protected Map<String, ServletContext> _contextMap;

	Sampler _hasizeSampler;
	private int _hasessions = 0;
	protected SlidingAverage _hasizeCounter = new SlidingAverage();
	protected SlidingAverage _rttpassivationCounter = new SlidingAverage();
	protected SlidingAverage _elapspassivationCounter = new SlidingAverage();
	protected SlidingAverage _rttactivationCounter = new SlidingAverage();
	protected SlidingAverage _elapsactivationCounter = new SlidingAverage();
	Executor _sipexecutor;

	protected boolean _uacmarkeroncallid = false;
	private ServiceRegistry _registry;

	public void bind(PlatformExecutor executor) {
		_sipexecutor = executor;
	}

	public void setEnv(Dictionary<String, String> dic) {
		_uacmarkeroncallid = ConfigHelper.getBoolean(dic,
				"sipagent.agent.uacmarkeroncallid", false);
	}

	public void setSessionType(String type) {
		_typename = type;
	}

	static class HAContextImpl implements HAContext {
		String _id;
		int _counter = 0;
		List<Flattable> _list = new ArrayList<Flattable>();
		List<Flattable> _addedlist = new LinkedList<Flattable>();
		Set<Integer> _rootSet = new HashSet<Integer>();
		private static Map<String, Object> NULLMAP = new HashMap<String, Object>();

		public HAContextImpl(String id) {
			_id = id;
		}

		public String toString() {
			return _list.toString();
		}

		public String id() {
			return _id;
		}

		protected void setId(String id) {
			_id = id;
		}

		public void register(Flattable o) {
			_list.add(o);
		}

		public void registerRoot(int key) {
			_rootSet.add(key);
		}

		void unregisterFlattable(Flattable o) {
			_list.remove(o);
		}

		public int unregister(Flattable o) {
			if (_rootSet == null)
				return -1;
			_rootSet.remove(o.key(0));
			unregisterFlattable(o);
			return _rootSet.size();
		}

		public void visit(Map<String, Object> map, Flattable o)
				throws IllegalArgumentException, IllegalAccessException {
			o.key(++_counter);
			for (FlatField f : o.fields()) {
				f.write(o, map, this);
			}
		}

		public Map<String, Object> passivate() throws IllegalArgumentException,
				IllegalAccessException, SecurityException, NoSuchFieldException {
			Map<String, Object> map = new HashMap<String, Object>();
			passivate(map);
			return map;
		}

		public Map<String, Object> passivate(Map<String, Object> map)
				throws IllegalArgumentException, IllegalAccessException,
				SecurityException, NoSuchFieldException {
			boolean added = false;
			for (Flattable object : _list) {
				if (object.diff(false) == null) {
					if (object.write(map, this)) {
						if (!added)
							added = true;
						registerRoot(object.key(0));
						// activate the diff mode
						object.diff(true);
					}
				} else {
					this.visitDiff(map, object);
				}
			}
			for (Iterator<Flattable> it = _addedlist.iterator(); it.hasNext();) {
				Flattable flattable = it.next();
				flattable.diff(true);
				register(flattable);
				it.remove();
			}
			// write roots
			if (added) {
				StringBuilder builder = new StringBuilder();
				for (int root : _rootSet) {
					builder.append(root).append(Flattable.VALSEP);
				}
				map.put(Flattable.KEYROOT,
						builder.delete(
								builder.length() - Flattable.VALSEP.length(),
								builder.length()).toString());
			}
			return map;
		}

		public Map<String, Object> passivate(final Session ds)
				throws IllegalArgumentException, IllegalAccessException,
				SecurityException, NoSuchFieldException {
			if (_list == null)
				return NULLMAP;
			Map<String, Object> map = passivate(new Map<String, Object>() {
				int size = 0;

				public void clear() {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public boolean containsKey(Object key) {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public boolean containsValue(Object value) {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public Set<java.util.Map.Entry<String, Object>> entrySet() {
					Set<java.util.Map.Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
					List<Attribute> attrNames = null;
					try {
						attrNames = ds.getAttributes();
					} catch (SessionException e) {
						logger.error("Cannot getAttributes on DS: " + e, e);
						return null;
					}
					Iterator<Attribute> iter = attrNames.iterator();
					while (iter.hasNext()) {
						final Attribute attribute = (Attribute) iter.next();
						set.add(new java.util.Map.Entry<String, Object>() {

							public String getKey() {
								return attribute.getName();
							}

							public Object getValue() {
								return attribute.getValue();
							}

							public Object setValue(Object value) {
								throw new UnsupportedOperationException(
										"Method of java.util.Map.Entry wrapper for Attribute not implemented");
							}
						});
					}

					return set;
				}

				public Object get(Object key) {
					if (logger.isDebugEnabled())
						logger.debug("get key: " + key);
					Object value = null;
					try {
						value = ds.getAttribute((String) key);
					} catch (SessionException e) {
						logger.error("Cannot get on key: " + key, e);
					}
					return value;
				}

				public boolean isEmpty() {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public Set<String> keySet() {
					// throw new UnsupportedOperationException(
					// "Method of Map wrapper for DS not implemented");
					try {
						return ds.keySet();
					} catch (SessionException e) {
						logger.error("Cannot get KeySet on DS", e);
					}

					return null;

				}

				public Object put(String key, Object value) {
					if (logger.isDebugEnabled())
						logger.debug("put key: " + key + " = " + value
								+ "| class:" + value.getClass());// ,new
					// IllegalStateException("TRACE"));
					// Must work with new getAttribute API
					// Bug with readExternal of SipSession that overload HA
					if (javax.servlet.sip.SipServletMessage.class
							.isAssignableFrom(value.getClass()))
						return value;
					if (!Serializable.class.isAssignableFrom(value.getClass())) {
						if (logger.isDebugEnabled())
							logger.debug("The value is not serializable !"
									+ key);
						return value;
					}
					try {
						// if ("K".equals(key))
						ds.setAttribute(key, (Serializable) value);
						size++;
					} catch (SessionException e) {
						logger.error("setAttribute error:" + key, e);
					}
					return value;
				}

				public void putAll(Map<? extends String, ? extends Object> t) {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public Object remove(Object key) {
					try {
						// don't get the value (not required)
						ds.removeAttribute((String) key, false);
					} catch (SessionException e) {
						logger.error("removeAttribute error:" + key, e);
					}
					return null;
				}

				public int size() {
					return size;
				}

				public Collection<Object> values() {
					throw new UnsupportedOperationException(
							"Method of Map wrapper for DS not implemented");
				}

				public String toString() {

					String out = "Map(DS): ";
					try {
						for (String key : ds.keySet()) {
							out = out.concat(key
									+ "="
									+ ds.getAttribute((String) key, Thread
											.currentThread()
											.getContextClassLoader()) + " ");
						}
					} catch (SessionException e) {
						logger.error("Cannot display DS: " + e, e);
						return "";
					}

					return out;
				}
			});

			return map;
		}

		public List<Flattable> content() {
			return _list;
		}

		private int computeKey(Flattable value) {
			@SuppressWarnings("rawtypes")
			Class valueclass = value.getClass();
			int lkey = ++_counter;
			if (javax.servlet.sip.SipServletMessage.class
					.isAssignableFrom(valueclass)) {
				if (javax.servlet.sip.SipServletRequest.class
						.isAssignableFrom(valueclass)) {
					if (lkey % 2 != 0) {
						lkey++;
					}
				} else {
					if (lkey % 2 == 0) {
						lkey++;
					}

				}
			}
			return lkey;
		}

		public int registerOnVisiting(Flattable value) {
			_addedlist.add(value);
			return value.key(computeKey(value));
		}

		public boolean unregisterOnVisiting(Flattable value) {
			boolean r = false;
			if (r = _addedlist.remove(value)) {
				value.key(0);
			}
			return r;
		}

		public void visitDiff(Map<String, Object> map, Flattable object)
				throws IllegalArgumentException, IllegalAccessException,
				SecurityException, NoSuchFieldException {

			com.alcatel_lucent.ha.services.Set diff = (com.alcatel_lucent.ha.services.Set) object
					.diff(false);
			if (diff != null) {
				if (logger.isDebugEnabled())
					logger.debug("visitDiff " + diff);
				for (String fieldname : diff.mod()) {
					String[] keys = fieldname.split(Flattable.MAPSEP);
					FlatField flatfield = FlattableSupport.getFlatField(
							object.fields(), keys[0]);
					if (keys.length == 1)
						flatfield.write(object, map, this);
					else
						flatfield.writeDiff(object, map, this, keys[1]);
				}
				for (String fieldname : diff.del()) {
					String[] keys = fieldname.split(Flattable.MAPSEP);
					FlatField flatfield = FlattableSupport.getFlatField(
							object.fields(), keys[0]);
					if (keys.length <= 1) {
						throw new IllegalStateException(
								"cannot remove no collection field");
					} else {
						flatfield.remove(object, map, this, keys[1]);
					}
				}
				object.diff(true).clear();
			}
		}

		public void visit(Flattable flattable, Map<String, Object> map)
				throws IllegalArgumentException, IllegalAccessException {
			for (FlatField f : flattable.fields()) {
				f.read(flattable, map);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.alcatel_lucent.ha.services.HAContext#destroy()
		 */
		@Override
		public void destroy() {
			_list = null;
			_rootSet = null;
			_addedlist = null;
		}
	}

	protected Flattable createSub(String subkey,
			@SuppressWarnings("rawtypes") Class subclass)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		return createSub(Integer.parseInt(subkey), subclass);
	}

	protected Flattable createSub(int subkey,
			@SuppressWarnings("rawtypes") Class subclass)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (logger.isDebugEnabled())
			logger.debug("subkey: " + subkey + " subclass: " + subclass);
		@SuppressWarnings("unchecked")
		Flattable subobject = (Flattable) subclass.getConstructor()
				.newInstance();
		subobject.key(subkey);
		subobject.diff(true);
		return subobject;
	}

	private void activateContext(HAContext context, Map<String, Object> map)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException, InstantiationException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		Object kroot = map.get(Flattable.KEYROOT);
		if (kroot == null)
			throw new IllegalStateException(
					"cannot activate the context missing " + Flattable.KEYROOT);
		// instantiate the object from the map
		// first round : identify the flattables with their key (object tree
		// building)
		String[] roots = kroot.toString().split(Flattable.VALSEP);
		List<Flattable> objects = new LinkedList<Flattable>();
		Set<ClassLoader> loaders = new HashSet<ClassLoader>();
		for (String root : roots) {
			Flattable object = createRoot(context);
			int keyroot = Integer.parseInt(root);
			object.key(keyroot);
			context.registerRoot(keyroot);
			ClassLoader oldloader = switchToApplicationClassLoader(map, root
					+ RecoveryServiceSupport.APPNAME);
			FlattableField.readObjects(map, object, objects, this);
			if (oldloader != null) {
				loaders.add(Thread.currentThread().getContextClassLoader());
				Thread.currentThread().setContextClassLoader(oldloader);
			}
		}
		// second round, fill the flattables with their values from the map
		// (object leaves building)
		// TODO : needs to change the structure to quickly identify flattable
		// object application appartenance
		ClassLoader oldloader = switchToApplicationClassLoader(map, roots[0]
				+ RecoveryServiceSupport.APPNAME);
		for (Flattable object : objects) {
			context.register(object);
			object.diff(true);
			if (logger.isDebugEnabled()) {
				logger.debug("fill flattable " + object.key(0) + ":"
						+ object.getClass().getName());
			}
			object.read(context, map);
			if (oldloader != null)
				Thread.currentThread().setContextClassLoader(oldloader);
		}
		// third round , notify the flattables read has been done
		for (Flattable object : objects) {
			object.readDone();
		}
		Thread.currentThread().setContextClassLoader(oldloader);
	}

	public HAContext context(Map<String, Object> map, String id)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException, InstantiationException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		Object kroot = map.get(Flattable.KEYROOT);
		if (kroot == null)
			throw new IllegalArgumentException("missing mandatory key: "
					+ Flattable.KEYROOT);
		HAContext context = new HAContextImpl(id);

		activateContext(context, map);

		return context;
	}

	public HAContext context(String id) {
		// return new HAContextImpl(id);
		String tmp;
		if ("".equalsIgnoreCase(id)) {
			tmp = _type.createSmartKey();// .getSessionKey();
		} else
			tmp = id;
		return new HAContextImpl(tmp);
	}

	public boolean unpassivate(final HAContext context) {
		if (context == null) {
			logger.warn("unpassivate on null context.");
			return false;
		}
		boolean status = true;
		if (logger.isInfoEnabled())
			logger.info("unpassivate context " + context.id() + " ...");
		try {
			if (_sm != null) {
				int flag = 0;

				flag = Transaction.TX_CREATE_GET | Transaction.TX_SERIALIZED;

				_sm.execute(new Transaction(_type, context.id(), flag) {
					private static final long serialVersionUID = 1L;

					public void execute(Session ds) throws SessionException {
						if (ds == null) {
							if (logger.isInfoEnabled())
								logger.info("execute on session null");
							return;
						}
						if (logger.isInfoEnabled())
							logger.info("destroy session : "
									+ ds.getSessionId());
						ds.destroy(Integer.MAX_VALUE);

					}
				}, new TransactionListener() {

					public void transactionCompleted(Transaction arg0,
							Serializable result) {
						if (result != null) {
							if (_hasessions > 0)
								_hasessions--;
							else
								logger.warn("unpassivate completed with _hasessions negative");
							if (logger.isInfoEnabled())
								logger.info("unpassivate done.");
						}

					}

					public void transactionFailed(Transaction arg0,
							SessionException arg1) {
						logger.error("unpassivate transactionFailed ", arg1);
					}
				});
			} else {
				if (logger.isInfoEnabled())
					logger.info("Nothing to do as there is no DS SessionManager");
			}
		} catch (Throwable t) {
			logger.error("unpassivate error: " + t.getMessage(), t);
			status = false;
		} finally {
			context.destroy();
		}
		return status;
	}

	public boolean passivate(final HAContext context,
			final PassivationCallback cb) {
		if (logger.isInfoEnabled())
			logger.info("passivate...");
		try {
			if (_sm != null) {
				int flag = Transaction.TX_CREATE_GET
						| Transaction.TX_SERIALIZED;

				_sm.execute(new Transaction(_type, context.id(), flag) {
					private static final long serialVersionUID = 1L;

					public void execute(Session ds) throws SessionException {
						if (ds == null) {
							return;
						}
						boolean created = ds.created();
						try {
							Map<String, Object> map = null;
							try (StopWatch watch = _elapspassivationCounter
									.startWatch()) {
								map = ((HAContextImpl) context).passivate(ds);
							}
							if (map != null && map.size() != 0) {
								if (logger.isDebugEnabled()) {
									logger.debug("commit session  "
											+ ds.getSessionId() + " with ("
											+ map.size() + " ) attributes  ");
								}
								ds.commit(ds.getSize());
							} else {
								ds.rollback(null);
							}
						} catch (Throwable e) {
							logger.error(
									"passivate error on " + ds.getSessionId(),
									e);
							throw new SessionException(e);
						} finally {
							if (created) {
								_hasessions++;
							}
						}
					}
				}, new TransactionListener() {
					private StopWatch watch = _rttpassivationCounter
							.startWatch();

					public void transactionCompleted(Transaction t,
							Serializable arg1) {
						watch.close();
						if (arg1 != null) {
							Integer size = (Integer) arg1;
							_hasizeCounter.update(size);
							if (logger.isInfoEnabled()) {
								logger.info("passivate done  ["
										+ t.getSessionId() + "] of size="
										+ size + "(" + _hasessions + ")");
							}
						} else {
							if (logger.isInfoEnabled()) {
								logger.info("passivate aborted.");
							}
						}
						if (cb != null)
							cb.passivated(true);
					}

					public void transactionFailed(Transaction t,
							SessionException arg1) {
						watch.close();
						if (logger.isDebugEnabled())
							logger.debug("passivation failed.");
						if (cb != null)
							cb.passivated(false);
					}
				});
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("<passivate>");
					logger.debug(context.passivate());
					logger.debug("</passivate>");
				}
			}
		} catch (Throwable t) {
			logger.error("passivate error : " + t.getMessage(), t);
			if (cb != null)
				cb.passivated(false);
			return false;
		}

		return true;
	}

	public boolean activate(final HAContext context, final ActivationCallback cb) {

		try {
			if (_sm != null) {
				if (logger.isInfoEnabled())
					logger.info("activate with  id=" + context.id());

				final String[] keys = recoveryKeys(context.id());
				if (keys == null && cb != null) {
					cb.activated(false);
				} else {
					activateKeys(context, keys, cb);
					_hasessions++;
				}

			} else {
				if (logger.isDebugEnabled())
					logger.debug("no activation done.");
				if (cb != null)
					cb.activated(false);
			}
		} catch (Exception e) {
			logger.error("activate error " + context.id(), e);
			if (cb != null)
				cb.activated(false);
			return false;
		}
		return true;
	}

	abstract protected ClassLoader switchToApplicationClassLoader(
			Map<String, Object> contextMap, String appname);

	private void activateKey(final HAContext context, final String key,
			final ActivationCallback cb, TransactionListener tl) {
		int flag = 0;

		if (logger.isDebugEnabled())
			logger.debug("activateKey " + key);
		flag = Transaction.TX_GET | Transaction.TX_SERIALIZED
				| Transaction.TX_IF_NOT_MARKED;

		((HAContextImpl) context).setId(key);
		_sm.execute(new Transaction(_type, key, flag) {
			private static final long serialVersionUID = 1L;

			public void execute(final Session ds) throws SessionException {
				if (ds == null) {
					if (logger.isDebugEnabled())
						logger.debug("execute on session null, exit.");
					return;
				}
				try {
					if (logger.isDebugEnabled())
						logger.debug("execute activate transaction");
					try (StopWatch watch = _elapsactivationCounter.startWatch()) {
						activateContext(context, new Map<String, Object>() {

							public void clear() {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public boolean containsKey(Object key) {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public boolean containsValue(Object value) {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public Set<java.util.Map.Entry<String, Object>> entrySet() {
								Set<java.util.Map.Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
								List<Attribute> attrNames = null;
								try {
									attrNames = ds.getAttributes(Thread
											.currentThread()
											.getContextClassLoader());
								} catch (SessionException e) {
									logger.error("Cannot getAttributes on DS: "
											+ e, e);
									return null;
								}
								Iterator<Attribute> iter = attrNames.iterator();

								while (iter.hasNext()) {
									final Attribute attribute = (Attribute) iter
											.next();
									set.add(new java.util.Map.Entry<String, Object>() {

										public String getKey() {
											return attribute.getName();
										}

										public Object getValue() {
											return attribute.getValue();
										}

										public Object setValue(Object value) {
											throw new UnsupportedOperationException(
													"Method of java.util.Map.Entry wrapper for Attribute not implemented");
										}
									});
								}

								return set;
							}

							public Object get(Object key) {
								Object value = null;
								try {
									value = ds.getAttribute((String) key,
											Thread.currentThread()
													.getContextClassLoader());
								} catch (SessionException e) {
									logger.error(
											"Cannot getAttribute on DS for key: "
													+ key, e);
								}
								return value;
							}

							public boolean isEmpty() {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public Set<String> keySet() {
								// throw new UnsupportedOperationException(
								// "Method of Map wrapper for DS not implemented");
								try {
									return ds.keySet();
								} catch (SessionException e) {
									logger.error("session.keySet() error", e);
								}

								return null;
							}

							public Object put(String key, Object value) {
								try {
									ds.setAttribute(key, (Serializable) value);
								} catch (SessionException e) {
									logger.error("session.put error", e);
								}
								return value;
							}

							public void putAll(
									Map<? extends String, ? extends Object> t) {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public Object remove(Object key) {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public int size() {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public Collection<Object> values() {
								throw new UnsupportedOperationException(
										"Method of Map wrapper for DS not implemented");
							}

							public String toString() {
								String out = "Map(DS): ";
								try {
									for (String key : ds.keySet()) {
										out = out.concat(key
												+ "="
												+ ds.getAttribute(
														(String) key,
														Thread.currentThread()
																.getContextClassLoader())
												+ " ");
									}
								} catch (SessionException e) {
									logger.error(
											"execute activate transaction eror when printing  session",
											e);
									return "";
								}

								return out;

							}
						});
					}
					ds.mark();
					ds.commit("OK");
				} catch (IllegalStateException e) {
					if (logger.isInfoEnabled())
						logger.info("execute  activate  transaction  fails ", e);
					logger.error("activation failed due to inconsistent session "
							+ ds.getSessionId());
					ds.rollback(null);
					if (cb != null)
						cb.activated(false);
				} catch (Exception e) {
					logger.error("execute  activate  transaction  fails ", e);
					logger.error("execute activate transaction rollback");
					ds.rollback(null);
					if (cb != null)
						cb.activated(false);
				}

			}
		}, tl);
	}

	private void activateKeys(final HAContext context, final String[] keys,
			final ActivationCallback cb) {
		TransactionListener tl = new TransactionListener() {
			StopWatch watch = _rttactivationCounter.startWatch();
			int count = keys.length;

			boolean status = false;

			public void transactionCompleted(Transaction t, Serializable arg1) {
				watch.close();
				count--;
				status = arg1 != null;
				if (count == 0 && cb != null)
					cb.activated(status);
			}

			public void transactionFailed(Transaction t, SessionException arg1) {
				status = arg1 instanceof MarkException;
				if (status) {
					logger.info("session already activated (marked)");
				}
				watch.close();
				count--;
				if (count == 0 && cb != null)
					cb.activated(status);
			}
		};
		if (logger.isInfoEnabled()) {
			logger.info("activateKeys  " + Arrays.asList(keys));
		}
		for (final String key : keys) {
			activateKey(context, key, cb, tl);
		}
	}

	void bind(ServiceRegistry registry) {
		_registry = registry;
	}

	protected void start() {
		logger.warn("start");
		_sipexecutor.execute(this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run() {
		if (logger.isInfoEnabled()) {
			logger.info("initializing"
					+ ((System
							.getProperty("com.alcatel.sip.sipservlet.services.NoProactiveRecovery") != null) ? "Proactive disabled!"
							: "Proactive enabled"));
		}
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(SessionType.TYPE_NAME, _typename);
		logger.info("addSessionType " + props.values());
		try {
			if (System
					.getProperty("com.alcatel.sip.sipservlet.services.NoProactiveRecovery") == null) {
				_type = _sm.addSessionType(props, new SessionListener() {
					@Override
					public void handleEvent(List<SessionEvent> arg0) {
						final String sessionId = arg0.get(0).getSessionId();
						if (logger.isInfoEnabled()) {
							logger.info("sessionDidActivate proactive for "
									+ sessionId);
						}
						final HAContext ctx = context(sessionId);
						activate(ctx, new ActivationCallback() {
							public void activated(boolean ok) {
								if (!ok) {
									logger.error("sessionDidActivate failed for session:"
											+ sessionId);
								}
							}
						});

					}
				}, new SessionEventFilter(
						SessionEventFilter.EVENT_SESSION_ACTIVATED));

			} else {
				_type = _sm.addSessionType(props);
			}
			final String tn = _type.getType();
			logger.info("registering SessionType " + tn);
			_registry.registerService(null, SessionType.class.getName(), _type,
					new Hashtable() {
						private static final long serialVersionUID = 1L;

						{
							put("name", tn);
						}
					});
			logger.info("initialized");
		} catch (IllegalArgumentException ile) {
			logger.error("initialization failed", ile);
		}
	}

	public void bind(SessionManager sm) {
		_sm = sm;
	}

	abstract protected String[] recoveryKeys(String o);

	public int[] getCounters(int[] array, int pos) {
		return array;
	}

}
