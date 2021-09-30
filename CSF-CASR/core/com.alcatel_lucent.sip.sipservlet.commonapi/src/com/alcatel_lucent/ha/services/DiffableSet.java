package com.alcatel_lucent.ha.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

     class DiffableSet implements Set {

        java.util.Set<String> modified = new HashSet<String>();
        java.util.Set<String> removed = new HashSet<String>();

        public boolean add(String e) {
            removed.remove(e);
            return modified.add(e);
        }

        public void add(int index, String element) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int index, Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            if (modified != null)
                modified.clear();
            if (removed != null)
                removed.clear();
        }

        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        public String get(int index) {
            throw new UnsupportedOperationException();
        }

        public int indexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }

        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        public ListIterator<String> listIterator() {
            throw new UnsupportedOperationException();
        }

        public ListIterator<String> listIterator(int index) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            modified.remove(o);
            return removed.add((String) o);
        }

        public String remove(int index) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        public String set(int index, String element) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            throw new UnsupportedOperationException();
        }

        public java.util.List<String> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        public java.util.Set<String> del() {
            return removed;
        }

        public java.util.Set<String> mod() {
            return modified;
        }
        public String toString() {
            return "m="+modified.toString()+",removed="+removed.toString();
        }

}
