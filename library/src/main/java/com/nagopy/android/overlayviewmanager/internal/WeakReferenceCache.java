/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.overlayviewmanager.internal;


import android.support.annotation.RestrictTo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public class WeakReferenceCache<T> {

    final List<WeakReference<T>> cacheSet;

    public WeakReferenceCache() {
        this.cacheSet = new ArrayList<>();
    }

    public synchronized void add(T obj) {
        Iterator<WeakReference<T>> iterator = cacheSet.iterator();
        while (iterator.hasNext()) {
            T cachedObj = iterator.next().get();
            if (cachedObj == null) {
                iterator.remove();
            } else if (cachedObj == obj) {
                Logger.v("Already cached. %s", obj);
                return;
            }
        }
        cacheSet.add(new WeakReference<>(obj));
    }

    public synchronized void remove(T obj) {
        Iterator<WeakReference<T>> iterator = cacheSet.iterator();
        while (iterator.hasNext()) {
            T cachedObj = iterator.next().get();
            if (cachedObj == null) {
                iterator.remove();
            } else if (cachedObj == obj) {
                iterator.remove();
                Logger.v("Removed. %s", obj);
                return;
            }
        }
    }

    public synchronized boolean isEmpty() {
        Iterator<WeakReference<T>> iterator = cacheSet.iterator();
        while (iterator.hasNext()) {
            T cachedObj = iterator.next().get();
            if (cachedObj == null) {
                iterator.remove();
            }
        }
        return cacheSet.isEmpty();
    }

    public synchronized boolean contains(T obj) {
        for (WeakReference<T> cache : cacheSet) {
            if (cache.get() == obj) {
                return true;
            }
        }
        return false;
    }
}
