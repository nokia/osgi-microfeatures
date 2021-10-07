/// *
// * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
// */
// package com.nsn.ood.cls.core.util;
//
// import static org.junit.Assert.assertArrayEquals;
// import static org.junit.Assert.assertTrue;
//
// import java.nio.charset.Charset;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.TimeUnit;
// import java.util.function.Predicate;
//
// import org.apache.commons.lang3.RandomStringUtils;
// import org.apache.commons.lang3.RandomUtils;
// import org.apache.commons.lang3.tuple.Pair;
// import org.junit.Ignore;
// import org.junit.Test;
//
// import com.google.common.base.Stopwatch;
// import com.google.common.hash.Hasher;
// import com.google.common.hash.Hashing;
//
//
/// **
// * @author wro50095
// *
// */
// @Ignore
// public class MapPerformanceTest {
//
// @Test
// public void test10Elements() throws Exception {
// final Pair<Set<String>, List<Model>> testData = prepareTestData(2000000);
// final Stopwatch sw = Stopwatch.createUnstarted();
// final Set<String> fullSet = testData.getLeft();
// final Set<String> filteredKeys = fullSet.stream().filter(new Predicate<String>() {
// @Override
// public boolean test(final String e) {
// return RandomUtils.nextInt(0, 99) <= 60;
// }
// }).collect(Collectors.<String> toSet());
//
// resetClock(sw);
// final byte[] resultMap = performTaskWithMap(testData, filteredKeys);
// final long mapTime = sw.elapsed(TimeUnit.MICROSECONDS);
//
// resetClock(sw);
// final byte[] resultList = performTaskWithList(testData, filteredKeys);
// final long listTime = sw.elapsed(TimeUnit.MICROSECONDS);
//
// assertArrayEquals(resultList, resultMap);
// System.out.println("List time: " + listTime);
// System.out.println("Map time: " + mapTime);
// System.out.println("Gain: " + (1.0 - ((1.0 * mapTime) / listTime)));
// assertTrue(listTime > mapTime);
//
// }
//
// private void resetClock(final Stopwatch sw) {
// sw.reset();
// sw.start();
// }
//
// private byte[] performTaskWithMap(final Pair<Set<String>, List<Model>> prepareTestData, final Set<String> keys) {
// final SHA512Calculator calc = new SHA512Calculator();
// final List<Model> values = prepareTestData.getRight();
// final Map<String, Model> mapModelToKey = new HashMap<>();
// for (final Model model : values) {
// mapModelToKey.put(model.key, model);
// }
//
// for (final String key : keys) {
// final Model model = mapModelToKey.get(key);
// calc.add(model);
// }
// return calc.getHash();
//
// }
//
// private byte[] performTaskWithList(final Pair<Set<String>, List<Model>> prepareTestData, final Set<String> keys) {
// final SHA512Calculator calc = new SHA512Calculator();
// final List<Model> values = prepareTestData.getRight();
// for (final String key : keys) {
// final Model model = findElementInList(key, values);
// calc.add(model);
// }
// return calc.getHash();
// }
//
// private Model findElementInList(final String key, final List<Model> values) {
// for (final Model model : values) {
// if (model.key.equals(key)) {
// return model;
// }
// }
// return null;
// }
//
// public Pair<Set<String>, List<Model>> prepareTestData(int size) {
// final Set<String> keys = new HashSet<>();
// final List<Model> values = new ArrayList<>();
//
// while (size-- > 0) {
// final String key = RandomStringUtils.random(10);
// keys.add(key);
// values.add(new Model(key));
// }
// Collections.shuffle(values);
// return Pair.of(keys, values);
//
// }
//
// private static class Model {
// private final String key;
// private final int a;
// private final long b;
// private final double c;
//
// public Model(final String key) {
// this.key = key;
// this.a = RandomUtils.nextInt(1, 100);
// this.b = RandomUtils.nextLong(1, 10000);
// this.c = RandomUtils.nextDouble(1.0, 1000000.0);
// }
//
// @Override
// public int hashCode() {
// final int prime = 31;
// int result = 1;
// result = (prime * result) + this.a;
// result = (prime * result) + (int) (this.b ^ (this.b >>> 32));
// long temp;
// temp = Double.doubleToLongBits(this.c);
// result = (prime * result) + (int) (temp ^ (temp >>> 32));
// result = (prime * result) + ((this.key == null) ? 0 : this.key.hashCode());
// return result;
// }
//
// @Override
// public boolean equals(final Object obj) {
// if (this == obj) {
// return true;
// }
// if (obj == null) {
// return false;
// }
// if (!(obj instanceof Model)) {
// return false;
// }
// final Model other = (Model) obj;
// if (this.a != other.a) {
// return false;
// }
// if (this.b != other.b) {
// return false;
// }
// if (Double.doubleToLongBits(this.c) != Double.doubleToLongBits(other.c)) {
// return false;
// }
// if (this.key == null) {
// if (other.key != null) {
// return false;
// }
// } else if (!this.key.equals(other.key)) {
// return false;
// }
// return true;
// }
//
// }
//
// private static class SHA512Calculator {
// private final Hasher hasher = Hashing.sha512().newHasher();
//
// public void add(final String s) {
// this.hasher.putString(s, Charset.forName("UTF-8"));
// }
//
// public void add(final Model model) {
// add(model.a);
// add(model.b);
// add(model.c);
// add(model.key);
// }
//
// private void add(final int i) {
// this.hasher.putInt(i);
// }
//
// private void add(final double d) {
// this.hasher.putDouble(d);
// }
//
// private void add(final long l) {
// this.hasher.putLong(l);
// }
//
// private byte[] getHash() {
// return this.hasher.hash().asBytes();
// }
//
// }
// }
