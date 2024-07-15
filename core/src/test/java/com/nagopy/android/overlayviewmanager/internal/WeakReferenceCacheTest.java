package com.nagopy.android.overlayviewmanager.internal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.ref.WeakReference;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeakReferenceCacheTest {

    WeakReferenceCache<Object> weakReferenceCache;

    @Mock
    Object obj;

    WeakReference<Object> clearedRef;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        clearedRef = new WeakReference<>(null);
        weakReferenceCache = new WeakReferenceCache<>();
    }

    @Test
    public void add() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);

        weakReferenceCache.add(obj);
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(false));
        assertThat(weakReferenceCache.cacheSet.size(), is(1));
        assertThat(weakReferenceCache.cacheSet.iterator().next().get(), is(obj));
    }

    @Test
    public void add_alreadyAdded() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);
        weakReferenceCache.cacheSet.add(new WeakReference<>(obj));

        weakReferenceCache.add(obj);
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(false));
        assertThat(weakReferenceCache.cacheSet.size(), is(1));
        assertThat(weakReferenceCache.cacheSet.iterator().next().get(), is(obj));
    }

    @Test
    public void remove() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);
        weakReferenceCache.cacheSet.add(new WeakReference<>(obj));

        weakReferenceCache.remove(obj);
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
    }

    @Test
    public void remove_notCached() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));

        weakReferenceCache.remove(obj);
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
    }

    @Test
    public void isEmpty() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        boolean ret = weakReferenceCache.isEmpty();
        assertThat(ret, is(true));
    }

    @Test
    public void isEmpty_clearedAll() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);

        boolean ret = weakReferenceCache.isEmpty();
        assertThat(ret, is(true));
    }

    @Test
    public void isEmpty_notEmpty() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);
        weakReferenceCache.cacheSet.add(new WeakReference<>(obj));

        boolean ret = weakReferenceCache.isEmpty();
        assertThat(ret, is(false));
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(false));
        assertThat(weakReferenceCache.cacheSet.size(), is(1));
        assertThat(weakReferenceCache.cacheSet.iterator().next().get(), is(obj));
    }

    @Test
    public void contains() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(new WeakReference<>(obj));

        boolean ret = weakReferenceCache.contains(obj);
        assertThat(ret, is(true));
    }


    @Test
    public void contains_2() throws Exception {
        assertThat(weakReferenceCache.cacheSet.isEmpty(), is(true));
        weakReferenceCache.cacheSet.add(clearedRef);
        weakReferenceCache.cacheSet.add(new WeakReference<>(obj));

        boolean ret = weakReferenceCache.contains(this);
        assertThat(ret, is(false));
    }

}